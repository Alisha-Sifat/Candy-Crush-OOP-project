import com.candycrush.Candy;
import java.awt.*;
import java.awt.event.*;
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
    LevelProgress progress;      // ← tracks goal progress
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

        panel = new JPanel() {
            protected void paintComponent(Graphics g) { render(g); }
        };
        panel.setPreferredSize(new Dimension(PIXEL_SIZE, PIXEL_SIZE));

        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (phase == Phase.IDLE) {
                    handleClick(e);
                    panel.repaint();
                }
            }
        });

        animTimer = new javax.swing.Timer(16, e -> {
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

        // Show goal description as a brief tooltip at startup
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
            this.progress     = new LevelProgress(level);   // fresh tracker
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

            // Notify progress for every candy cleared
            for (Point p : cA) notifyClearedAt(p);
            for (Point p : cB) notifyClearedAt(p);

            addScore((cA.size() + cB.size()) * 40);

            // Double-stripe costs a move
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

            // The bomb itself is "burst"
            progress.notifyBombBurst();

            ArrayList<Point> targets = board.findCandiesOfColor(color);

            lightningBolts.clear();
            for (Point t : targets) {
                lightningBolts.add(new float[]{ bombOrigin.x, bombOrigin.y, t.x, t.y, 0f });
            }

            pendingConversionPoints.clear();
            pendingConversionPoints.addAll(targets);
            pendingConversionColor = color;

            addScore(50); // bomb+stripe activation bonus

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

            // Bomb is burst
            progress.notifyBombBurst();

            ArrayList<Point> destroyed = board.clearColor(colorToDestroy);

            // Notify for every candy destroyed by the bomb
            for (Point p : destroyed) progress.notifyCandyCleared(colorToDestroy);

            addScore(destroyed.size() * 30);

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
            board.swap(selected, second); // revert
            selected = null;
            return;
        }

        // Create specials (4-in-a-row → striped, 5+ → bomb)
        ArrayList<Point> specials = board.createSpecials(
                groups, selected, second, candyImages, bombImg);

        // Notify for each striped candy created
        for (Point sp : specials) {
            Candy created = board.get(sp.x, sp.y);
            if (created instanceof StripedCandy) {
                progress.notifyStripedCreated();
            }
        }

        // Collect points to remove (skip special anchors)
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

        // Notify for each normal candy being cleared
        for (Point p : toRemove) {
            Candy c = board.get(p.x, p.y);
            if (c != null) progress.notifyCandyCleared(c.getType());
        }

        // Score per candy: 20 for 3-match, 30 for 4-match, 50 for 5+ match
        int matchScore = 0;
        for (Board.MatchGroup g : groups) {
            int sz = g.size();
            int perCandy = sz >= 5 ? 50 : sz == 4 ? 30 : 20;
            matchScore += sz * perCandy;
        }
        addScore(matchScore);

        movesLeft--;
        updateUI();
        checkGameEnd();

        // Snapshot striped directions BEFORE removeMatches nulls them
        ArrayList<float[]> stripedSnapshots = snapshotStripedEffects(toRemove);
        ArrayList<Point> extras = board.removeMatches(toRemove);

        // Notify for extra candies cleared by striped effects
        for (Point ep : extras) {
            Candy c = board.get(ep.x, ep.y);
            // The cell is already null after removeMatches; use type from position
            // (extras are already nulled in board — score them but type unknown)
        }
        addScore(extras.size() * 25); // 25 pts per extra candy from striped

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
                addScore(cleared.size() * 25); // striped beam from bomb+stripe combo
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

        // Cascade bonus: 25 per candy (slightly more than base 3-match)
        addScore(toRemove.size() * 25);

        ArrayList<float[]> snapshots = snapshotStripedEffects(toRemove);
        ArrayList<Point> extras = board.removeMatches(toRemove);
        addScore(extras.size() * 25); // cascade striped extras

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
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /** Increment score and notify progress tracker. */
    private void addScore(int amount) {
        score += amount;
        progress.notifyScoreUpdated(score);
        frame.setTitle("Score: " + score);
    }

    /** Notify progress that a candy at grid position p was cleared. */
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

    /**
     * Checks win/loss after every move.
     * Win  → goal is complete (regardless of moves left).
     * Loss → moves ran out before goal was reached.
     */
    private void checkGameEnd() {
        if (progress.isGoalComplete()) {
            // Save progress and unlock next level
            int next = currentLevel.getLevelNumber() + 1;
            if (next > SaveManager.loadLevel()) SaveManager.saveLevel(next);

            JOptionPane.showMessageDialog(frame,
                    "🎉 Level Complete!\n" + currentLevel.getGoalDescription(),
                    "You Win!", JOptionPane.INFORMATION_MESSAGE);
            frame.dispose();
            new LevelSelect().showLevels();
            return;
        }

        if (movesLeft <= 0) {
            JOptionPane.showMessageDialog(frame,
                    "😢 Out of moves!\n"
                    + progress.getProgressText() + " (need " + progress.getTarget() + ")",
                    "Game Over", JOptionPane.WARNING_MESSAGE);
            frame.dispose();
            new LevelSelect().showLevels();
        }
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
            float progress     = sw[3];
            float eased = 1f - (1f - progress) * (1f - progress);
            float alpha;
            if      (progress < 0.15f) alpha = progress / 0.15f;
            else if (progress < 0.75f) alpha = 1f;
            else                       alpha = 1f - (progress - 0.75f) / 0.25f;
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
}
