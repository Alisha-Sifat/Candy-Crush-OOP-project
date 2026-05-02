import com.candycrush.Candy;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Game {

    final int SIZE       = 8;
    final int PIXEL_SIZE = 600;
    final int SQUARE_SIZE = PIXEL_SIZE / SIZE;

    Board board = new Board(SIZE);

    Image[] candyImages = new Image[6];
    Image BG, SELECTOR, bombImg;
    // No separate stripedImg needed — stripe is drawn procedurally over candy color

    Point selected = null;
    int score = 0;

    JFrame frame = new JFrame("Score: 0");
    JPanel panel;

    // ── Candy colors for stripe tint (matches your 6 candy types) ───────
    // Adjust these to match your actual candy image colors
    private static final Color[] CANDY_COLORS = {
        new Color(220,  60,  60),   // 0 red
        new Color( 60, 160, 220),   // 1 blue
        new Color( 60, 200,  80),   // 2 green
        new Color(240, 200,  40),   // 3 yellow
        new Color(180,  80, 220),   // 4 purple
        new Color(240, 130,  40),   // 5 orange
    };

    // ── Fall animation ───────────────────────────────────────────────────
    private final Map<String, Float> candyOffsets = new HashMap<>();
    private boolean isAnimating = false;
    final float FALL_SPEED = 18f;

    // ── Swipe animation (striped candy effect) ───────────────────────────
    // Each entry: { x (grid), y (grid), isHorizontal (1/0), progress 0→1 }
    private ArrayList<float[]> swipeEffects  = new ArrayList<>();
    private boolean isSwipe = false;
    private int swipeTicksRemaining = 0;
    private static final int SWIPE_DURATION = 20; // ~0.33 s

    // ── Lightning animation (bomb effect) ────────────────────────────────
    // Each entry: { originX, originY, targetX, targetY, progress }
    private ArrayList<float[]> lightningBolts = new ArrayList<>();
    private boolean isLightning = false;
    private int lightningTicksRemaining = 0;
    private static final int LIGHTNING_DURATION = 30;

    private Random rng = new Random();
    private javax.swing.Timer animTimer;

    // ── State machine ─────────────────────────────────────────────────────
    // Ensures swipe plays before gravity, lightning plays before gravity, etc.
    private enum Phase { IDLE, SWIPE, LIGHTNING, FALLING }
    private Phase phase = Phase.IDLE;

    public void run() throws IOException {
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
                case SWIPE:     stepSwipe();     break;
                case LIGHTNING: stepLightning(); break;
                case FALLING:   stepFalling();   break;
                default: break;
            }
            panel.repaint();
        });
        animTimer.start();

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // ───────────────────────────────────────────────────────────────────
    //  CLICK HANDLER
    // ───────────────────────────────────────────────────────────────────
    private void handleClick(MouseEvent e) {
        int x = e.getX() / SQUARE_SIZE;
        int y = e.getY() / SQUARE_SIZE;

        if (selected == null) {
            selected = new Point(x, y);
            return;
        }

        Point second = new Point(x, y);

        if (!isNeighbor(selected, second)) {
            selected = new Point(x, y); // re-select
            return;
        }

        Candy candyA = board.get(selected.x, selected.y);
        Candy candyB = board.get(second.x, second.y);

        // ── Bomb swap: detect before swapping ────────────────────────────
        boolean bombSwap = false;
        Point  bombPos   = null;
        int    colorToDestroy = -1;

        if (candyA instanceof BombCandy && !(candyB instanceof BombCandy)) {
            bombSwap = true;
            colorToDestroy = candyB.getType();
        } else if (candyB instanceof BombCandy && !(candyA instanceof BombCandy)) {
            bombSwap = true;
            colorToDestroy = candyA.getType();
        }

        board.swap(selected, second);

        if (bombSwap) {
            // Find where the bomb ended up after the swap
            Point bombAfter = (board.get(second.x, second.y) instanceof BombCandy)
                              ? second : selected;

            board.set(bombAfter.x, bombAfter.y, null);
            ArrayList<Point> destroyed = board.clearColor(colorToDestroy);

            score += destroyed.size() + 1;
            frame.setTitle("Score: " + score);

            // Build lightning bolts
            lightningBolts.clear();
            for (Point d : destroyed) {
                lightningBolts.add(new float[]{ bombAfter.x, bombAfter.y, d.x, d.y, 0f });
            }
            lightningTicksRemaining = LIGHTNING_DURATION;
            phase = lightningBolts.isEmpty() ? Phase.IDLE : Phase.LIGHTNING;
            if (phase == Phase.IDLE) afterLightning();

        } else {
            // ── Normal / striped swap ─────────────────────────────────────
            ArrayList<Board.MatchGroup> groups = board.findMatchGroups();

            if (groups.isEmpty()) {
                board.swap(selected, second); // revert — no match
            } else {
                // Create special candies for groups of 4 or 5+
                ArrayList<Point> specials = board.createSpecials(
                        groups, selected, second, candyImages, bombImg);

                // Collect all matched points; exclude special anchor cells
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

                score += toRemove.size();
                frame.setTitle("Score: " + score);

                // Snapshot striped candy directions BEFORE removeMatches nulls them
                ArrayList<float[]> stripedSnapshots = snapshotStripedEffects(toRemove);

                ArrayList<Point> extras = board.removeMatches(toRemove);
                score += extras.size();
                frame.setTitle("Score: " + score);

                buildSwipeEffects(stripedSnapshots);

                if (!swipeEffects.isEmpty()) {
                    swipeTicksRemaining = SWIPE_DURATION;
                    phase = Phase.SWIPE;
                } else {
                    afterSwipe();
                }
            }
        }

        selected = null;
    }

    // ── Snapshot striped candies BEFORE board.removeMatches() nulls them ──
    // Returns ready-to-use swipe entries: { gridCol, gridRow, horizClear(1/0), progress }
    // horizClear=1 means the bar sweeps horizontally (row wipe)
    // horizClear=0 means the bar sweeps vertically   (column wipe)
    private ArrayList<float[]> snapshotStripedEffects(ArrayList<Point> matches) {
        ArrayList<float[]> result = new ArrayList<>();
        for (Point p : matches) {
            Candy c = board.get(p.x, p.y);
            if (!(c instanceof StripedCandy)) continue;
            StripedCandy sc = (StripedCandy) c;
            // sc.isHorizontal()==true  → clears the ROW  → bar sweeps horizontally → horizClear=1
            // sc.isHorizontal()==false → clears the COL  → bar sweeps vertically   → horizClear=0
            result.add(new float[]{ p.x, p.y, sc.isHorizontal() ? 1f : 0f, 0f });
        }
        return result;
    }

    private void buildSwipeEffects(ArrayList<float[]> snapshots) {
        swipeEffects.clear();
        swipeEffects.addAll(snapshots);
    }

    // ───────────────────────────────────────────────────────────────────
    //  SWIPE ANIMATION
    // ───────────────────────────────────────────────────────────────────
    private void stepSwipe() {
        if (swipeTicksRemaining > 0) {
            float step = 1.0f / SWIPE_DURATION;
            for (float[] sw : swipeEffects) sw[3] = Math.min(1f, sw[3] + step);
            swipeTicksRemaining--;
        } else {
            phase = Phase.IDLE;
            swipeEffects.clear();
            afterSwipe();
        }
    }

    private void afterSwipe() {
        board.applyGravity();
        boolean[][] wasEmpty = captureEmpty();
        board.refill(candyImages);
        startFalling(wasEmpty);
    }

    // ───────────────────────────────────────────────────────────────────
    //  LIGHTNING ANIMATION
    // ───────────────────────────────────────────────────────────────────
    private void stepLightning() {
        if (lightningTicksRemaining > 0) {
            float step = 1.0f / LIGHTNING_DURATION;
            for (float[] bolt : lightningBolts) bolt[4] = Math.min(1f, bolt[4] + step);
            lightningTicksRemaining--;
        } else {
            phase = Phase.IDLE;
            lightningBolts.clear();
            afterLightning();
        }
    }

    private void afterLightning() {
        board.applyGravity();
        boolean[][] wasEmpty = captureEmpty();
        board.refill(candyImages);
        startFalling(wasEmpty);
    }

    // ───────────────────────────────────────────────────────────────────
    //  FALL ANIMATION
    // ───────────────────────────────────────────────────────────────────
    private boolean[][] captureEmpty() {
        boolean[][] e = new boolean[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++)
            for (int col = 0; col < SIZE; col++)
                e[row][col] = (board.get(col, row) == null);
        return e;
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

    private void processCascade() {
        ArrayList<Board.MatchGroup> groups = board.findMatchGroups();
        if (groups.isEmpty()) return;

        ArrayList<Point> toRemove = new ArrayList<>();
        for (Board.MatchGroup g : groups) toRemove.addAll(g.points);

        score += toRemove.size();
        frame.setTitle("Score: " + score);

        // Snapshot striped candy directions BEFORE removeMatches nulls them
        ArrayList<float[]> stripedSnapshots = snapshotStripedEffects(toRemove);

        ArrayList<Point> extras = board.removeMatches(toRemove);
        score += extras.size();
        frame.setTitle("Score: " + score);

        buildSwipeEffects(stripedSnapshots);

        if (!swipeEffects.isEmpty()) {
            swipeTicksRemaining = SWIPE_DURATION;
            phase = Phase.SWIPE;
        } else {
            afterSwipe();
        }
    }

    // ───────────────────────────────────────────────────────────────────
    //  RENDER
    // ───────────────────────────────────────────────────────────────────
    private void render(Graphics g) {
        g.drawImage(BG, 0, 0, PIXEL_SIZE, PIXEL_SIZE, null);

        // Candies
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

                // Draw stripe overlay for StripedCandies
                if (c instanceof StripedCandy) {
                    drawStripeOverlay(g, (StripedCandy) c, drawX, drawY);
                }
            }
        }

        // Selector
        if (selected != null) {
            g.drawImage(SELECTOR,
                    selected.x * SQUARE_SIZE, selected.y * SQUARE_SIZE,
                    SQUARE_SIZE, SQUARE_SIZE, null);
        }

        // Swipe effect
        if (phase == Phase.SWIPE) drawSwipeEffects(g);

        // Lightning effect
        if (phase == Phase.LIGHTNING) drawLightning(g);
    }

    // ── Stripe overlay: two white diagonal bands drawn on the candy ──────
    /**
     * Draws a stripe pattern over the candy cell to indicate StripedCandy.
     * Two bright diagonal bands give the classic "striped" look.
     * The stripe direction indicator (horizontal arrows / vertical arrows)
     * is drawn as a subtle white arrow on top.
     */
    private void drawStripeOverlay(Graphics g, StripedCandy sc, int drawX, int drawY) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(drawX, drawY, SQUARE_SIZE, SQUARE_SIZE);

        int s = SQUARE_SIZE;
        // Two diagonal white bands
        g2.setColor(new Color(255, 255, 255, 110));
        int bw = s / 5; // band width

        if (sc.isHorizontal()) {
            // Horizontal stripe → draws horizontal bands → clears ROW
            // Two horizontal bars across the candy
            g2.fillRect(drawX, drawY + s/2 - bw/2 - bw, s, bw);
            g2.fillRect(drawX, drawY + s/2 - bw/2 + bw, s, bw);
            // Arrow indicators pointing left and right
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(2f));
            int cy = drawY + s/2;
            // left arrow
            g2.drawLine(drawX + 4, cy, drawX + s/4, cy);
            g2.drawLine(drawX + 4, cy, drawX + 4 + 4, cy - 4);
            g2.drawLine(drawX + 4, cy, drawX + 4 + 4, cy + 4);
            // right arrow
            g2.drawLine(drawX + s - 4, cy, drawX + s - s/4, cy);
            g2.drawLine(drawX + s - 4, cy, drawX + s - 4 - 4, cy - 4);
            g2.drawLine(drawX + s - 4, cy, drawX + s - 4 - 4, cy + 4);
        } else {
            // Vertical stripe → draws vertical bands → clears COLUMN
            g2.fillRect(drawX + s/2 - bw/2 - bw, drawY, bw, s);
            g2.fillRect(drawX + s/2 - bw/2 + bw, drawY, bw, s);
            // Arrow indicators pointing up and down
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(2f));
            int cx = drawX + s/2;
            // up arrow
            g2.drawLine(cx, drawY + 4, cx, drawY + s/4);
            g2.drawLine(cx, drawY + 4, cx - 4, drawY + 4 + 4);
            g2.drawLine(cx, drawY + 4, cx + 4, drawY + 4 + 4);
            // down arrow
            g2.drawLine(cx, drawY + s - 4, cx, drawY + s - s/4);
            g2.drawLine(cx, drawY + s - 4, cx - 4, drawY + s - 4 - 4);
            g2.drawLine(cx, drawY + s - 4, cx + 4, drawY + s - 4 - 4);
        }

        g2.dispose();
    }

    // ── Swipe effect: two beams that burst outward from the candy cell ────
    // Each beam travels from the striped candy's position to the board edge.
    // horizClear=1 → two beams go left and right (row wipe)
    // horizClear=0 → two beams go up and down   (column wipe)
    private void drawSwipeEffects(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (float[] sw : swipeEffects) {
            int   originCol  = (int) sw[0];
            int   originRow  = (int) sw[1];
            boolean horizClear = sw[2] == 1f;
            float progress   = sw[3];   // 0 → 1

            // Smooth ease-out so beams start fast and slow at edges
            float eased = 1f - (1f - progress) * (1f - progress);

            // Alpha: fade in fast, hold, then fade out
            float alpha;
            if (progress < 0.15f)      alpha = progress / 0.15f;
            else if (progress < 0.75f) alpha = 1f;
            else                       alpha = 1f - (progress - 0.75f) / 0.25f;
            int a = Math.max(0, Math.min(255, (int)(alpha * 230)));

            int originPx, maxTravel;

            if (horizClear) {
                // ── Row wipe: beams go left and right from candy column ──────
                int rowY  = originRow * SQUARE_SIZE;
                int thick = SQUARE_SIZE;            // beam fills full cell height
                originPx  = originCol * SQUARE_SIZE + SQUARE_SIZE / 2; // center x
                maxTravel = PIXEL_SIZE;             // max distance a beam can travel

                // Beam going RIGHT
                int rightReach = (int)((PIXEL_SIZE - originPx) * eased);
                drawBeamH(g2, originPx, originPx + rightReach, rowY, thick, a, true);

                // Beam going LEFT
                int leftReach  = (int)(originPx * eased);
                drawBeamH(g2, originPx - leftReach, originPx, rowY, thick, a, false);

            } else {
                // ── Column wipe: beams go up and down from candy row ─────────
                int colX  = originCol * SQUARE_SIZE;
                int thick = SQUARE_SIZE;
                originPx  = originRow * SQUARE_SIZE + SQUARE_SIZE / 2; // center y
                maxTravel = PIXEL_SIZE;

                // Beam going DOWN
                int downReach = (int)((PIXEL_SIZE - originPx) * eased);
                drawBeamV(g2, colX, originPx, originPx + downReach, thick, a, true);

                // Beam going UP
                int upReach   = (int)(originPx * eased);
                drawBeamV(g2, colX, originPx - upReach, originPx, thick, a, false);
            }
        }

        g2.dispose();
    }

    /**
     * Draws one horizontal beam segment from x1 to x2 in the row at rowY.
     * leadingRight=true  → the leading (bright) edge is at x2
     * leadingRight=false → the leading edge is at x1
     */
    private void drawBeamH(Graphics2D g2, int x1, int x2, int rowY, int thick, int a,
                            boolean leadingRight) {
        if (x2 <= x1) return;
        int glowPad = thick / 4;

        // Outer glow
        g2.setColor(new Color(255, 240, 120, a / 4));
        g2.fillRect(x1, rowY - glowPad, x2 - x1, thick + glowPad * 2);

        // Core bright band (center half-height)
        g2.setColor(new Color(255, 255, 255, a));
        g2.fillRect(x1, rowY + thick / 4, x2 - x1, thick / 2);

        // Softer color fill behind the core
        g2.setColor(new Color(255, 220, 80, a / 2));
        g2.fillRect(x1, rowY, x2 - x1, thick);

        // Leading-edge spike: a thin very-bright strip at the front
        int edgeX = leadingRight ? x2 - 6 : x1;
        g2.setColor(new Color(255, 255, 255, Math.min(255, a + 40)));
        g2.fillRect(edgeX, rowY, 6, thick);
    }

    /**
     * Draws one vertical beam segment from y1 to y2 in the column at colX.
     * leadingDown=true  → the leading edge is at y2
     * leadingDown=false → the leading edge is at y1
     */
    private void drawBeamV(Graphics2D g2, int colX, int y1, int y2, int thick, int a,
                            boolean leadingDown) {
        if (y2 <= y1) return;
        int glowPad = thick / 4;

        // Outer glow
        g2.setColor(new Color(255, 240, 120, a / 4));
        g2.fillRect(colX - glowPad, y1, thick + glowPad * 2, y2 - y1);

        // Core
        g2.setColor(new Color(255, 255, 255, a));
        g2.fillRect(colX + thick / 4, y1, thick / 2, y2 - y1);

        // Color fill
        g2.setColor(new Color(255, 220, 80, a / 2));
        g2.fillRect(colX, y1, thick, y2 - y1);

        // Leading-edge spike
        int edgeY = leadingDown ? y2 - 6 : y1;
        g2.setColor(new Color(255, 255, 255, Math.min(255, a + 40)));
        g2.fillRect(colX, edgeY, thick, 6);
    }

    // ── Lightning effect (same as before, but cleaner) ───────────────────
    private void drawLightning(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int half = SQUARE_SIZE / 2;

        for (float[] bolt : lightningBolts) {
            float prog = bolt[4];
            if (prog <= 0f) continue;

            int ox = (int)bolt[0] * SQUARE_SIZE + half;
            int oy = (int)bolt[1] * SQUARE_SIZE + half;
            int tx = (int)bolt[2] * SQUARE_SIZE + half;
            int ty = (int)bolt[3] * SQUARE_SIZE + half;

            int cx = ox + (int)((tx - ox) * prog);
            int cy = oy + (int)((ty - oy) * prog);

            float alpha = prog < 0.7f ? 1f : 1f - (prog - 0.7f) / 0.3f;
            int a = Math.max(0, Math.min(255, (int)(alpha * 255)));

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
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len < 1f) return;
        float px = -dy / len, py = dx / len;
        float max = SQUARE_SIZE * 0.35f;

        int[] xs = new int[segs + 1], ys = new int[segs + 1];
        xs[0] = x1; ys[0] = y1; xs[segs] = x2; ys[segs] = y2;
        for (int i = 1; i < segs; i++) {
            float t = (float)i / segs;
            float off = (rng.nextFloat() * 2 - 1) * max;
            xs[i] = Math.round(x1 + t*dx + px*off);
            ys[i] = Math.round(y1 + t*dy + py*off);
        }
        for (int i = 0; i < segs; i++) g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private boolean isNeighbor(Point a, Point b) {
        return (Math.abs(a.x - b.x) == 1 && a.y == b.y) ||
               (Math.abs(a.y - b.y) == 1 && a.x == b.x);
    }
}