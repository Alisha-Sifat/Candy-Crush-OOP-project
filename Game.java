import com.candycrush.Candy;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Game {
    JFrame frame;
    TopPanel topPanel;
    final int SIZE        = 8;
    final int PIXEL_SIZE  = 600;
    final int SQUARE_SIZE = PIXEL_SIZE / SIZE;

    Level         currentLevel;
    LevelProgress progress;
    int movesLeft;
    int score = 0;

    Board board = new Board(SIZE);

    Image[] candyImages = new Image[6];
    Image BG, SELECTOR, bombImg;

    Point selected = null;

    JPanel panel;

    // ── Fall animation ────────────────────────────────────────────────────
    private final Map<String, Float> candyOffsets = new HashMap<>();
    final float FALL_SPEED = 18f;

    // ── Swipe animation ───────────────────────────────────────────────────
    private ArrayList<float[]> swipeEffects = new ArrayList<>();
    private int swipeTicksRemaining = 0;
    private static final int SWIPE_DURATION = 20;

    // ── Lightning animation ───────────────────────────────────────────────
    private ArrayList<float[]> lightningBolts = new ArrayList<>();
    private int lightningTicksRemaining = 0;
    private static final int LIGHTNING_DURATION = 30;

    // ── Bomb+Stripe combo ─────────────────────────────────────────────────
    private ArrayList<Point> pendingConversionPoints = new ArrayList<>();
    private int pendingConversionColor = -1;
    private int conversionPauseTicks   = 0;
    private static final int CONVERSION_PAUSE_DURATION = 18;

    private Random rng = new Random();
    private javax.swing.Timer animTimer;

    // ── Phase state machine ───────────────────────────────────────────────
    private enum Phase { IDLE, LIGHTNING, CONVERSION_PAUSE, SWIPE, FALLING }
    private Phase phase = Phase.IDLE;

    // ── Hint system (all logic lives in HintSystem.java) ─────────────────
    private HintSystem hintSystem;

    // ─────────────────────────────────────────────────────────────────────
    //  STARTUP
    // ─────────────────────────────────────────────────────────────────────
    public void run() throws IOException {
        frame = new JFrame("Sugar Rush Saga");
        String basePath = System.getProperty("user.dir") + "/src/images/";

        BG       = ImageIO.read(new File(basePath + "b.png"));
        SELECTOR = ImageIO.read(new File(basePath + "s.png"));
        bombImg  = ImageIO.read(new File(basePath + "bomb.png"));

        for (int i = 0; i < 6; i++) {
            candyImages[i] = ImageIO.read(new File(basePath + (i + 1) + ".png"));
        }

        board.initBoard(candyImages);
        hintSystem = new HintSystem(board, SQUARE_SIZE, SIZE);

        panel = new JPanel() {
            protected void paintComponent(Graphics g) { render(g); }
        };
        panel.setPreferredSize(new Dimension(PIXEL_SIZE, PIXEL_SIZE));

        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (phase == Phase.IDLE) {
                    hintSystem.reset();
                    handleClick(e);
                    panel.repaint();
                }
            }
        });

        animTimer = new javax.swing.Timer(16, e -> {
            // ── Delegate hint logic to HintSystem ────────────────────────
            if (phase == Phase.IDLE) hintSystem.tick();
            else                     hintSystem.cancelHint();

            switch (phase) {
                case LIGHTNING:        stepLightning();       break;
                case CONVERSION_PAUSE: stepConversionPause(); break;
                case SWIPE:            stepSwipe();           break;
                case FALLING:          stepFalling();         break;
                default: break;
            }
            panel.repaint();
        });
        animTimer.start();

        frame.setLayout(new BorderLayout());
        topPanel = new TopPanel();
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(panel,    BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        topPanel.update(score, movesLeft, progress);
        frame.setTitle("Sugar Rush Saga — " + currentLevel.getGoalDescription());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────
    public void startLevel(Level level) {
        try {
            this.currentLevel = level;
            this.movesLeft    = level.getMaxMoves();
            this.score        = 0;
            this.progress     = new LevelProgress(level);
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CLICK HANDLER
    // ─────────────────────────────────────────────────────────────────────
    private void handleClick(MouseEvent e) {
        int x = e.getX() / SQUARE_SIZE;
        int y = e.getY() / SQUARE_SIZE;

        if (selected == null) {
            selected = new Point(x, y);
            return;
        }

        Point second = new Point(x, y);

        if (!isNeighbor(selected, second)) {
            selected = new Point(x, y);
            return;
        }

        Candy candyA = board.get(selected.x, selected.y);
        Candy candyB = board.get(second.x,   second.y);

        // ── 1. Double-striped combo ───────────────────────────────────────
        if (candyA instanceof StripedCandy && candyB instanceof StripedCandy) {
            StripedCandy scA = (StripedCandy) candyA;
            StripedCandy scB = (StripedCandy) candyB;

            swipeEffects.clear();
            swipeEffects.add(new float[]{ selected.x, selected.y, scA.isHorizontal() ? 1f : 0f, 0f });
            swipeEffects.add(new float[]{ second.x,   second.y,   scB.isHorizontal() ? 1f : 0f, 0f });

            ArrayList<Point> cA = board.clearRowOrColumn(selected, scA);
            ArrayList<Point> cB = board.clearRowOrColumn(second,   scB);
            board.set(selected.x, selected.y, null);
            board.set(second.x,   second.y,   null);

            for (Point p : cA) notifyClearedAt(p);
            for (Point p : cB) notifyClearedAt(p);

            addScore(cA.size() + cB.size() + 2);

            movesLeft--;
            updateUI();
            checkGameEnd();

            swipeTicksRemaining = SWIPE_DURATION;
            phase = Phase.SWIPE;
            selected = null;
            return;
        }

        // ── 2. Bomb + Striped combo ───────────────────────────────────────
        if ((candyA instanceof BombCandy && candyB instanceof StripedCandy) ||
            (candyB instanceof BombCandy && candyA instanceof StripedCandy)) {

            int   color      = (candyA instanceof StripedCandy) ? candyA.getType() : candyB.getType();
            Point bombOrigin = (candyA instanceof BombCandy) ? selected : second;

            board.set(selected.x, selected.y, null);
            board.set(second.x,   second.y,   null);

            progress.notifyBombBurst();

            ArrayList<Point> targets = board.findCandiesOfColor(color);

            lightningBolts.clear();
            for (Point t : targets) {
                lightningBolts.add(new float[]{ bombOrigin.x, bombOrigin.y, t.x, t.y, 0f });
            }

            pendingConversionPoints.clear();
            pendingConversionPoints.addAll(targets);
            pendingConversionColor = color;

            addScore(2);

            movesLeft--;
            updateUI();
            checkGameEnd();

            lightningTicksRemaining = LIGHTNING_DURATION;
            if (lightningBolts.isEmpty()) {
                board.convertCandiesAtPoints(pendingConversionPoints, pendingConversionColor, candyImages);
                conversionPauseTicks = CONVERSION_PAUSE_DURATION;
                phase = Phase.CONVERSION_PAUSE;
            } else {
                phase = Phase.LIGHTNING;
            }
            selected = null;
            return;
        }

        // ── 3. Normal bomb swap ───────────────────────────────────────────
        if ((candyA instanceof BombCandy && !(candyB instanceof BombCandy)) ||
            (candyB instanceof BombCandy && !(candyA instanceof BombCandy))) {

            int colorToDestroy = (candyA instanceof BombCandy) ? candyB.getType() : candyA.getType();
            board.swap(selected, second);

            Point bombAfter = (board.get(second.x, second.y) instanceof BombCandy) ? second : selected;
            board.set(bombAfter.x, bombAfter.y, null);

            progress.notifyBombBurst();

            ArrayList<Point> destroyed = board.clearColor(colorToDestroy);

            for (Point p : destroyed) progress.notifyCandyCleared(colorToDestroy);

            addScore(destroyed.size() + 1);

            lightningBolts.clear();
            for (Point d : destroyed) {
                lightningBolts.add(new float[]{ bombAfter.x, bombAfter.y, d.x, d.y, 0f });
            }
            pendingConversionColor = -1;

            movesLeft--;
            updateUI();
            checkGameEnd();

            lightningTicksRemaining = LIGHTNING_DURATION;
            phase = lightningBolts.isEmpty() ? Phase.IDLE : Phase.LIGHTNING;
            if (phase == Phase.IDLE) afterLightning();
            selected = null;
            return;
        }

        // ── 4. Normal / striped match ─────────────────────────────────────
        board.swap(selected, second);
        ArrayList<Board.MatchGroup> groups = board.findMatchGroups();

        if (groups.isEmpty()) {
            board.swap(selected, second);
            selected = null;
            return;
        }

        // ── Snapshot original candy types BEFORE createSpecials converts ────
        // any anchor cell into a StripedCandy/BombCandy.  This lets us count
        // that candy toward COLLECT_CANDY goals (real Candy Crush behaviour:
        // all N candies in a 4-in-a-row count, including the one that becomes
        // the striped candy).
        java.util.Map<String, Integer> preSpecialTypes = new java.util.HashMap<>();
        for (Board.MatchGroup g : groups)
            for (Point p : g.points) {
                Candy c = board.get(p.x, p.y);
                if (c != null) preSpecialTypes.put(p.x + "," + p.y, c.getType());
            }

        ArrayList<Point> specials = board.createSpecials(
                groups, selected, second, candyImages, bombImg);

        for (Point sp : specials) {
            Candy created = board.get(sp.x, sp.y);
            if (created instanceof StripedCandy) {
                progress.notifyStripedCreated();
            }
            // Count the candy that BECAME a special — it was a real candy
            // of the matched colour and must be included in the collection tally.
            Integer origType = preSpecialTypes.get(sp.x + "," + sp.y);
            if (origType != null && origType >= 0) {
                progress.notifyCandyCleared(origType);
            }
        }

        ArrayList<Point> toRemove = new ArrayList<>();
        for (Board.MatchGroup g : groups) {
            for (Point p : g.points) {
                boolean isSpecial = false;
                for (Point sp : specials) {
                    if (sp.x == p.x && sp.y == p.y) { isSpecial = true; break; }
                }
                if (!isSpecial) toRemove.add(p);
            }
        }

        // Count the remaining (non-anchor) cleared candies
        for (Point p : toRemove) {
            Candy c = board.get(p.x, p.y);
            if (c != null) progress.notifyCandyCleared(c.getType());
        }

        addScore(toRemove.size());

        movesLeft--;
        updateUI();
        checkGameEnd();

        ArrayList<float[]> stripedSnapshots = snapshotStripedEffects(toRemove);
        ArrayList<Point> extras = board.removeMatches(toRemove);

        addScore(extras.size());

        swipeEffects.clear();
        swipeEffects.addAll(stripedSnapshots);

        if (!swipeEffects.isEmpty()) {
            swipeTicksRemaining = SWIPE_DURATION;
            phase = Phase.SWIPE;
        } else {
            afterSwipe();
        }

        selected = null;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ANIMATION STEPS
    // ─────────────────────────────────────────────────────────────────────
    private void stepLightning() {
        if (lightningTicksRemaining > 0) {
            float step = 1.0f / LIGHTNING_DURATION;
            for (float[] bolt : lightningBolts) bolt[4] = Math.min(1f, bolt[4] + step);
            lightningTicksRemaining--;
        } else {
            lightningBolts.clear();
            if (pendingConversionColor >= 0) {
                board.convertCandiesAtPoints(
                        pendingConversionPoints, pendingConversionColor, candyImages);
                conversionPauseTicks = CONVERSION_PAUSE_DURATION;
                phase = Phase.CONVERSION_PAUSE;
            } else {
                phase = Phase.IDLE;
                afterLightning();
            }
        }
    }

    private void stepConversionPause() {
        if (conversionPauseTicks > 0) {
            conversionPauseTicks--;
        } else {
            swipeEffects.clear();
            for (Point p : pendingConversionPoints) {
                Candy c = board.get(p.x, p.y);
                if (!(c instanceof StripedCandy)) continue;
                StripedCandy sc = (StripedCandy) c;
                swipeEffects.add(new float[]{ p.x, p.y, sc.isHorizontal() ? 1f : 0f, 0f });
                ArrayList<Point> cleared = board.clearRowOrColumn(p, sc);
                for (Point cp : cleared) notifyClearedAt(cp);
                addScore(cleared.size());
                board.set(p.x, p.y, null);
            }

            pendingConversionPoints.clear();
            pendingConversionColor = -1;

            swipeTicksRemaining = SWIPE_DURATION;
            phase = swipeEffects.isEmpty() ? Phase.IDLE : Phase.SWIPE;
            if (phase == Phase.IDLE) afterSwipe();

            updateUI();
            checkGameEnd();
        }
    }

    private void stepSwipe() {
        if (swipeTicksRemaining > 0) {
            float step = 1.0f / SWIPE_DURATION;
            for (float[] sw : swipeEffects) sw[3] = Math.min(1f, sw[3] + step);
            swipeTicksRemaining--;
        } else {
            swipeEffects.clear();
            phase = Phase.IDLE;
            afterSwipe();
        }
    }

    private void afterSwipe() {
        board.applyGravity();
        boolean[][] wasEmpty = captureEmpty();
        board.refill(candyImages);
        startFalling(wasEmpty);
    }

    private void afterLightning() {
        board.applyGravity();
        boolean[][] wasEmpty = captureEmpty();
        board.refill(candyImages);
        startFalling(wasEmpty);
    }

    private void stepFalling() {
        boolean still = false;
        for (String key : new ArrayList<>(candyOffsets.keySet())) {
            float off = candyOffsets.get(key);
            if (off < 0) {
                off = Math.min(0f, off + FALL_SPEED);
                candyOffsets.put(key, off);
                if (off < 0) still = true;
            }
        }
        if (!still) {
            candyOffsets.clear();
            phase = Phase.IDLE;
            processCascade();
        }
    }

    private void startFalling(boolean[][] wasEmpty) {
        candyOffsets.clear();
        boolean any = false;
        for (int col = 0; col < SIZE; col++) {
            for (int row = 0; row < SIZE; row++) {
                if (board.get(col, row) == null) continue;
                int gaps = 0;
                for (int r2 = row + 1; r2 < SIZE; r2++) if (wasEmpty[r2][col]) gaps++;
                if (gaps > 0) {
                    candyOffsets.put(col + "," + row, (float) -(gaps * SQUARE_SIZE));
                    any = true;
                }
            }
        }
        phase = any ? Phase.FALLING : Phase.IDLE;
        if (!any) processCascade();
    }

    private boolean[][] captureEmpty() {
        boolean[][] e = new boolean[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++)
            for (int col = 0; col < SIZE; col++)
                e[row][col] = (board.get(col, row) == null);
        return e;
    }

    private void processCascade() {
        ArrayList<Board.MatchGroup> groups = board.findMatchGroups();
        if (groups.isEmpty()) return;

        ArrayList<Point> toRemove = new ArrayList<>();
        for (Board.MatchGroup g : groups) toRemove.addAll(g.points);

        for (Point p : toRemove) {
            Candy c = board.get(p.x, p.y);
            if (c != null) progress.notifyCandyCleared(c.getType());
        }

        addScore(toRemove.size());

        ArrayList<float[]> snapshots = snapshotStripedEffects(toRemove);
        ArrayList<Point> extras = board.removeMatches(toRemove);
        addScore(extras.size());

        swipeEffects.clear();
        swipeEffects.addAll(snapshots);

        updateUI();
        checkGameEnd();

        if (!swipeEffects.isEmpty()) {
            swipeTicksRemaining = SWIPE_DURATION;
            phase = Phase.SWIPE;
        } else {
            afterSwipe();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GAME END — FANCY DIALOGS
    // ─────────────────────────────────────────────────────────────────────
    private void checkGameEnd() {
        if (progress.isGoalComplete()) {
            int next = currentLevel.getLevelNumber() + 1;
            if (next > SaveManager.loadLevel()) SaveManager.saveLevel(next);
            animTimer.stop();
            SwingUtilities.invokeLater(() -> showWinDialog(next));
            return;
        }
        if (movesLeft <= 0) {
            animTimer.stop();
            SwingUtilities.invokeLater(this::showLoseDialog);
        }
    }

    /** ★ Fancy win dialog with Next Level / Map buttons ★ */
    private void showWinDialog(int nextLevelNum) {
        JDialog dialog = new JDialog(frame, true);
        dialog.setUndecorated(true);
        dialog.setSize(360, 340);
        dialog.setLocationRelativeTo(frame);

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Outer gold ring
                GradientPaint outer = new GradientPaint(0, 0, new Color(255, 200, 0),
                        0, getHeight(), new Color(255, 120, 0));
                g2.setPaint(outer);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 32, 32));
                // Inner card
                GradientPaint inner = new GradientPaint(0, 18, new Color(255, 252, 220),
                        0, getHeight(), new Color(255, 235, 160));
                g2.setPaint(inner);
                g2.fill(new RoundRectangle2D.Float(8, 8, getWidth() - 16, getHeight() - 16, 24, 24));
            }
        };
        content.setLayout(null);
        content.setOpaque(false);

        // Trophy
        JLabel trophy = new JLabel("🏆", SwingConstants.CENTER);
        trophy.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        trophy.setBounds(0, 18, 360, 80);
        content.add(trophy);

        // YOU WIN!
        JLabel winLbl = new JLabel("YOU WIN!", SwingConstants.CENTER);
        winLbl.setFont(new Font("Arial Black", Font.BOLD, 32));
        winLbl.setForeground(new Color(200, 80, 0));
        winLbl.setBounds(0, 100, 360, 42);
        content.add(winLbl);

        // Goal completed
        JLabel goalLbl = new JLabel(currentLevel.getGoalDescription(), SwingConstants.CENTER);
        goalLbl.setFont(new Font("Arial", Font.PLAIN, 13));
        goalLbl.setForeground(new Color(100, 70, 0));
        goalLbl.setBounds(0, 145, 360, 22);
        content.add(goalLbl);

        // Score
        JLabel scoreLbl = new JLabel("⭐  Score: " + score + "  ⭐", SwingConstants.CENTER);
        scoreLbl.setFont(new Font("Arial", Font.BOLD, 17));
        scoreLbl.setForeground(new Color(160, 100, 0));
        scoreLbl.setBounds(0, 168, 360, 28);
        content.add(scoreLbl);

        // ── Map button
        JButton mapBtn = makeFancyButton("🗺  Map", new Color(80, 150, 255));
        mapBtn.setBounds(28, 225, 140, 52);
        mapBtn.addActionListener(e -> {
            dialog.dispose();
            frame.dispose();
            JFrame mapFrame = new JFrame("Level Map");
            mapFrame.setSize(500, 800);
            mapFrame.add(new LevelMap());
            mapFrame.setVisible(true);
            mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });
        content.add(mapBtn);

        // ── Next Level button (only if another level exists)
        boolean hasNext = nextLevelNum <= LevelManager.getLevels().size();
        if (hasNext) {
            JButton nextBtn = makeFancyButton("▶  Next Level", new Color(50, 190, 90));
            nextBtn.setBounds(192, 225, 140, 52);
            nextBtn.addActionListener(e -> {
                dialog.dispose();
                frame.dispose();
                Level nextLvl = LevelManager.getLevels().get(nextLevelNum - 1);
                new Game().startLevel(nextLvl);
            });
            content.add(nextBtn);
        }

        dialog.setContentPane(content);
        try { dialog.setShape(new RoundRectangle2D.Float(0, 0, 360, 340, 32, 32)); }
        catch (Exception ignored) {}
        dialog.setVisible(true);
    }

    /** ★ Fancy lose dialog with Try Again / Map buttons ★ */
    private void showLoseDialog() {
        JDialog dialog = new JDialog(frame, true);
        dialog.setUndecorated(true);
        dialog.setSize(340, 310);
        dialog.setLocationRelativeTo(frame);

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Outer red ring
                GradientPaint outer = new GradientPaint(0, 0, new Color(220, 60, 60),
                        0, getHeight(), new Color(120, 20, 20));
                g2.setPaint(outer);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 32, 32));
                // Inner card
                GradientPaint inner = new GradientPaint(0, 8, new Color(255, 240, 240),
                        0, getHeight(), new Color(255, 210, 210));
                g2.setPaint(inner);
                g2.fill(new RoundRectangle2D.Float(8, 8, getWidth() - 16, getHeight() - 16, 24, 24));
            }
        };
        content.setLayout(null);
        content.setOpaque(false);

        // Broken heart
        JLabel emoji = new JLabel("💔", SwingConstants.CENTER);
        emoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 58));
        emoji.setBounds(0, 18, 340, 72);
        content.add(emoji);

        // OUT OF MOVES
        JLabel lbl = new JLabel("OUT OF MOVES", SwingConstants.CENTER);
        lbl.setFont(new Font("Arial Black", Font.BOLD, 26));
        lbl.setForeground(new Color(180, 30, 30));
        lbl.setBounds(0, 93, 340, 36);
        content.add(lbl);

        // Progress text
        JLabel progressLbl = new JLabel(progress.getProgressText(), SwingConstants.CENTER);
        progressLbl.setFont(new Font("Arial", Font.PLAIN, 14));
        progressLbl.setForeground(new Color(100, 40, 40));
        progressLbl.setBounds(0, 133, 340, 24);
        content.add(progressLbl);

        // Need label
        JLabel needLbl = new JLabel("(need " + progress.getTarget() + ")", SwingConstants.CENTER);
        needLbl.setFont(new Font("Arial", Font.PLAIN, 12));
        needLbl.setForeground(new Color(140, 60, 60));
        needLbl.setBounds(0, 156, 340, 20);
        content.add(needLbl);

        // ── Try Again button
        JButton retryBtn = makeFancyButton("🔄  Try Again", new Color(240, 100, 50));
        retryBtn.setBounds(25, 200, 140, 52);
        retryBtn.addActionListener(e -> {
            dialog.dispose();
            frame.dispose();
            new Game().startLevel(currentLevel);
        });
        content.add(retryBtn);

        // ── Map button
        JButton mapBtn = makeFancyButton("🗺  Map", new Color(80, 150, 255));
        mapBtn.setBounds(178, 200, 137, 52);
        mapBtn.addActionListener(e -> {
            dialog.dispose();
            frame.dispose();
            JFrame mapFrame = new JFrame("Level Map");
            mapFrame.setSize(500, 800);
            mapFrame.add(new LevelMap());
            mapFrame.setVisible(true);
            mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });
        content.add(mapBtn);

        dialog.setContentPane(content);
        try { dialog.setShape(new RoundRectangle2D.Float(0, 0, 340, 310, 32, 32)); }
        catch (Exception ignored) {}
        dialog.setVisible(true);
    }

    /** Shared pill-button factory used by win & lose dialogs. */
    private JButton makeFancyButton(String text, Color baseColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top    = getModel().isRollover() ? baseColor.brighter() : baseColor;
                Color bottom = getModel().isPressed()  ? baseColor.darker().darker() : baseColor.darker();
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                // Highlight sheen
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fill(new RoundRectangle2D.Float(4, 2, getWidth() - 8, getHeight() / 2 - 2, 14, 14));
                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  RENDER
    // ─────────────────────────────────────────────────────────────────────
    private void render(Graphics g) {
        g.drawImage(BG, 0, 0, PIXEL_SIZE, PIXEL_SIZE, null);

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Candy c = board.get(col, row);
                if (c == null) continue;

                int drawX = col * SQUARE_SIZE;
                int drawY = row * SQUARE_SIZE;

                String key = col + "," + row;
                if (candyOffsets.containsKey(key))
                    drawY += candyOffsets.get(key).intValue();

                g.drawImage(c.getImage(), drawX, drawY, SQUARE_SIZE, SQUARE_SIZE, null);

                if (c instanceof StripedCandy)
                    drawStripeOverlay(g, (StripedCandy) c, drawX, drawY);
            }
        }

        // Hint highlight (drawn above candies, below selector)
        if (hintSystem.isActive() && phase == Phase.IDLE) {
            hintSystem.draw(g);
        }

        if (selected != null) {
            g.drawImage(SELECTOR,
                    selected.x * SQUARE_SIZE, selected.y * SQUARE_SIZE,
                    SQUARE_SIZE, SQUARE_SIZE, null);
        }

        if (phase == Phase.LIGHTNING || phase == Phase.CONVERSION_PAUSE)
            drawLightning(g);

        if (phase == Phase.SWIPE)
            drawSwipeEffects(g);
    }

    private void drawStripeOverlay(Graphics g, StripedCandy sc, int drawX, int drawY) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(drawX, drawY, SQUARE_SIZE, SQUARE_SIZE);
        int s  = SQUARE_SIZE;
        int bw = s / 5;
        g2.setColor(new Color(255, 255, 255, 110));
        if (sc.isHorizontal()) {
            g2.fillRect(drawX, drawY + s/2 - bw/2 - bw, s, bw);
            g2.fillRect(drawX, drawY + s/2 - bw/2 + bw, s, bw);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(2f));
            int cy = drawY + s/2;
            g2.drawLine(drawX + 4,     cy, drawX + s/4,     cy);
            g2.drawLine(drawX + 4,     cy, drawX + 8,       cy - 4);
            g2.drawLine(drawX + 4,     cy, drawX + 8,       cy + 4);
            g2.drawLine(drawX + s - 4, cy, drawX + s - s/4, cy);
            g2.drawLine(drawX + s - 4, cy, drawX + s - 8,   cy - 4);
            g2.drawLine(drawX + s - 4, cy, drawX + s - 8,   cy + 4);
        } else {
            g2.fillRect(drawX + s/2 - bw/2 - bw, drawY, bw, s);
            g2.fillRect(drawX + s/2 - bw/2 + bw, drawY, bw, s);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(2f));
            int cx = drawX + s/2;
            g2.drawLine(cx, drawY + 4,     cx, drawY + s/4);
            g2.drawLine(cx, drawY + 4,     cx - 4, drawY + 8);
            g2.drawLine(cx, drawY + 4,     cx + 4, drawY + 8);
            g2.drawLine(cx, drawY + s - 4, cx, drawY + s - s/4);
            g2.drawLine(cx, drawY + s - 4, cx - 4, drawY + s - 8);
            g2.drawLine(cx, drawY + s - 4, cx + 4, drawY + s - 8);
        }
        g2.dispose();
    }

    private void drawLightning(Graphics g) {
        if (lightningBolts.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int half = SQUARE_SIZE / 2;
        for (float[] bolt : lightningBolts) {
            float prog = bolt[4];
            if (prog <= 0f) continue;
            int ox = (int) bolt[0] * SQUARE_SIZE + half;
            int oy = (int) bolt[1] * SQUARE_SIZE + half;
            int tx = (int) bolt[2] * SQUARE_SIZE + half;
            int ty = (int) bolt[3] * SQUARE_SIZE + half;
            int cx = ox + (int) ((tx - ox) * prog);
            int cy = oy + (int) ((ty - oy) * prog);
            float alpha = prog < 0.7f ? 1f : 1f - (prog - 0.7f) / 0.3f;
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g2.setColor(new Color(255, 255, 200, a / 3));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawJagged(g2, ox, oy, cx, cy, 4);
            g2.setColor(new Color(255, 255, 80, a));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawJagged(g2, ox, oy, cx, cy, 4);
            g2.setColor(new Color(255, 255, 255, a));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(ox, oy, cx, cy);
        }
        g2.dispose();
    }

    private void drawJagged(Graphics2D g2, int x1, int y1, int x2, int y2, int segs) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        float px = -dy / len, py = dx / len;
        float max = SQUARE_SIZE * 0.35f;
        int[] xs = new int[segs + 1], ys = new int[segs + 1];
        xs[0] = x1; ys[0] = y1; xs[segs] = x2; ys[segs] = y2;
        for (int i = 1; i < segs; i++) {
            float t = (float) i / segs;
            float off = (rng.nextFloat() * 2 - 1) * max;
            xs[i] = Math.round(x1 + t * dx + px * off);
            ys[i] = Math.round(y1 + t * dy + py * off);
        }
        for (int i = 0; i < segs; i++) g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
    }

    private void drawSwipeEffects(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (float[] sw : swipeEffects) {
            int col  = (int) sw[0];
            int row  = (int) sw[1];
            boolean horizClear = sw[2] == 1f;
            float prog     = sw[3];
            float eased = 1f - (1f - prog) * (1f - prog);
            float alpha;
            if      (prog < 0.15f) alpha = prog / 0.15f;
            else if (prog < 0.75f) alpha = 1f;
            else                   alpha = 1f - (prog - 0.75f) / 0.25f;
            int a = Math.max(0, Math.min(255, (int) (alpha * 230)));
            if (horizClear) {
                int rowY    = row * SQUARE_SIZE;
                int thick   = SQUARE_SIZE;
                int originX = col * SQUARE_SIZE + SQUARE_SIZE / 2;
                int rReach  = (int) ((PIXEL_SIZE - originX) * eased);
                int lReach  = (int) (originX * eased);
                drawBeamH(g2, originX, originX + rReach, rowY, thick, a, true);
                drawBeamH(g2, originX - lReach, originX, rowY, thick, a, false);
            } else {
                int colX    = col * SQUARE_SIZE;
                int thick   = SQUARE_SIZE;
                int originY = row * SQUARE_SIZE + SQUARE_SIZE / 2;
                int dReach  = (int) ((PIXEL_SIZE - originY) * eased);
                int uReach  = (int) (originY * eased);
                drawBeamV(g2, colX, originY, originY + dReach, thick, a, true);
                drawBeamV(g2, colX, originY - uReach, originY, thick, a, false);
            }
        }
        g2.dispose();
    }

    private void drawBeamH(Graphics2D g2, int x1, int x2, int rowY,
                            int thick, int a, boolean leadRight) {
        if (x2 <= x1) return;
        int pad = thick / 4;
        g2.setColor(new Color(255, 240, 120, a / 4));
        g2.fillRect(x1, rowY - pad, x2 - x1, thick + pad * 2);
        g2.setColor(new Color(255, 220, 80, a / 2));
        g2.fillRect(x1, rowY, x2 - x1, thick);
        g2.setColor(new Color(255, 255, 255, a));
        g2.fillRect(x1, rowY + thick / 4, x2 - x1, thick / 2);
        int ex = leadRight ? x2 - 6 : x1;
        g2.setColor(new Color(255, 255, 255, Math.min(255, a + 40)));
        g2.fillRect(ex, rowY, 6, thick);
    }

    private void drawBeamV(Graphics2D g2, int colX, int y1, int y2,
                            int thick, int a, boolean leadDown) {
        if (y2 <= y1) return;
        int pad = thick / 4;
        g2.setColor(new Color(255, 240, 120, a / 4));
        g2.fillRect(colX - pad, y1, thick + pad * 2, y2 - y1);
        g2.setColor(new Color(255, 220, 80, a / 2));
        g2.fillRect(colX, y1, thick, y2 - y1);
        g2.setColor(new Color(255, 255, 255, a));
        g2.fillRect(colX + thick / 4, y1, thick / 2, y2 - y1);
        int ey = leadDown ? y2 - 6 : y1;
        g2.setColor(new Color(255, 255, 255, Math.min(255, a + 40)));
        g2.fillRect(colX, ey, thick, 6);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────
    private void addScore(int amount) {
        score += amount;
        progress.notifyScoreUpdated(score);
        frame.setTitle("Score: " + score);
    }

    private void notifyClearedAt(Point p) {
        Candy c = board.get(p.x, p.y);
        if (c != null) progress.notifyCandyCleared(c.getType());
    }

    private ArrayList<float[]> snapshotStripedEffects(ArrayList<Point> matches) {
        ArrayList<float[]> result = new ArrayList<>();
        for (Point p : matches) {
            Candy c = board.get(p.x, p.y);
            if (!(c instanceof StripedCandy)) continue;
            StripedCandy sc = (StripedCandy) c;
            result.add(new float[]{ p.x, p.y, sc.isHorizontal() ? 1f : 0f, 0f });
        }
        return result;
    }

    private boolean isNeighbor(Point a, Point b) {
        return (Math.abs(a.x - b.x) == 1 && a.y == b.y) ||
               (Math.abs(a.y - b.y) == 1 && a.x == b.x);
    }

    private void updateUI() {
        topPanel.update(score, movesLeft, progress);
    }
}
