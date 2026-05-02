import com.candycrush.Candy;
import java.awt.*;
import java.util.ArrayList;

/**
 * Hint system — exactly like real Candy Crush.
 *
 * How it works:
 *   1. call findHint(board) → stores the best swap (two adjacent cells)
 *   2. Game.java calls draw(g, squareSize) every frame inside render()
 *      while isActive() is true — draws a pulsing glow on the two cells
 *   3. call tick() every timer tick to advance the animation
 *   4. The hint auto-expires after HINT_DURATION ticks (~3 seconds)
 *
 * If no valid swap exists (very rare), hint stays null and nothing is drawn.
 *
 * Also shows a "No moves!" idle timer: if the player hasn't moved for
 * IDLE_TICKS frames, the hint fires automatically (just like Candy Crush).
 */
public class HintSystem {

    // ── Timing ────────────────────────────────────────────────────────────
    private static final int HINT_DURATION = 180;  // ~3 s at 60 fps
    private static final int IDLE_TICKS    = 300;  // ~5 s of inactivity

    // ── State ─────────────────────────────────────────────────────────────
    private Point  hintA     = null;   // first  cell of best swap
    private Point  hintB     = null;   // second cell of best swap
    private int    ticksLeft = 0;
    private float  pulse     = 0f;
    private int    idleTicks = 0;

    private boolean active   = false;

    // ─────────────────────────────────────────────────────────────────────

    /** Returns true while the hint animation is visible. */
    public boolean isActive() { return active; }

    /** Call this every time the player makes a move (resets idle timer). */
    public void resetIdle() {
        idleTicks = 0;
        hide();
    }

    /**
     * Tick every timer frame (16 ms).
     * Advances pulse animation; counts idle time; auto-fires if idle too long.
     * Pass the board so the auto-hint can search it.
     */
    public void tick(Board board) {
        pulse = (float)(Math.sin(System.currentTimeMillis() / 250.0) * 0.5 + 0.5);

        if (active) {
            if (ticksLeft-- <= 0) hide();
        } else {
            idleTicks++;
            if (idleTicks >= IDLE_TICKS) {
                findHint(board);
                idleTicks = 0;
            }
        }
    }

    /**
     * Manually request a hint (player pressed 💡).
     * Finds the best swap and shows it immediately.
     */
    public void requestHint(Board board) {
        findHint(board);
        idleTicks = 0;
    }

    /** Hides the current hint. */
    public void hide() {
        active = false;
        hintA  = null;
        hintB  = null;
    }

    // ── Hint search ───────────────────────────────────────────────────────

    /**
     * Scans every adjacent pair, tries swapping, checks for 3-in-a-row.
     * Prefers moves that produce longer matches (greedy heuristic).
     */
    private void findHint(Board board) {
        int size = board.getSize();
        int bestScore = 0;
        Point bestA = null, bestB = null;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Try right neighbour
                if (x + 1 < size) {
                    Point a = new Point(x, y), b = new Point(x + 1, y);
                    int s = scoreSwap(board, a, b);
                    if (s > bestScore) { bestScore = s; bestA = a; bestB = b; }
                }
                // Try down neighbour
                if (y + 1 < size) {
                    Point a = new Point(x, y), b = new Point(x, y + 1);
                    int s = scoreSwap(board, a, b);
                    if (s > bestScore) { bestScore = s; bestA = a; bestB = b; }
                }
            }
        }

        if (bestA != null) {
            hintA     = bestA;
            hintB     = bestB;
            ticksLeft = HINT_DURATION;
            active    = true;
        }
    }

    /**
     * Temporarily swaps two cells, counts the matches, then swaps back.
     * Returns total matched cells (higher = better move).
     */
    private int scoreSwap(Board board, Point a, Point b) {
        board.swap(a, b);
        ArrayList<Point> matches = board.findMatches();
        int count = matches.size();
        board.swap(a, b); // undo
        return count;
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    /**
     * Draw the hint highlight on the board.
     * Call this inside Game.render(), after the candies are drawn.
     *
     * @param g          graphics context (the board panel's Graphics)
     * @param squareSize pixel size of one grid cell
     */
    public void draw(Graphics g, int squareSize) {
        if (!active || hintA == null || hintB == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawHintCell(g2, hintA, squareSize);
        drawHintCell(g2, hintB, squareSize);

        // Arrow between the two cells
        drawArrow(g2, hintA, hintB, squareSize);

        g2.dispose();
    }

    private void drawHintCell(Graphics2D g2, Point p, int sq) {
        int x = p.x * sq;
        int y = p.y * sq;

        // Outer glow rings (multiple passes, fading outward)
        for (int i = 12; i >= 2; i -= 2) {
            float alpha = (1f - (float)i / 14f) * 0.6f * (0.5f + pulse * 0.5f);
            g2.setColor(new Color(255, 230, 0, (int)(alpha * 255)));
            g2.setStroke(new BasicStroke(i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + i/2, y + i/2, sq - i, sq - i, 14, 14);
        }

        // Bright inner border
        float brightness = 0.7f + pulse * 0.3f;
        g2.setColor(new Color(1f, 0.9f, 0f, brightness));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x + 3, y + 3, sq - 6, sq - 6, 12, 12);

        // Subtle fill tint
        g2.setColor(new Color(255, 255, 100, (int)(40 + pulse * 30)));
        g2.fillRoundRect(x + 4, y + 4, sq - 8, sq - 8, 10, 10);
    }

    private void drawArrow(Graphics2D g2, Point a, Point b, int sq) {
        // Centre of each cell
        int ax = a.x * sq + sq / 2;
        int ay = a.y * sq + sq / 2;
        int bx = b.x * sq + sq / 2;
        int by = b.y * sq + sq / 2;

        float alpha = 0.6f + pulse * 0.4f;
        g2.setColor(new Color(255, 255, 255, (int)(alpha * 220)));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Line
        g2.drawLine(ax, ay, bx, by);

        // Arrowhead at b
        double angle = Math.atan2(by - ay, bx - ax);
        int ahs = 10; // arrowhead size
        int x1 = bx - (int)(Math.cos(angle - 0.5) * ahs);
        int y1 = by - (int)(Math.sin(angle - 0.5) * ahs);
        int x2 = bx - (int)(Math.cos(angle + 0.5) * ahs);
        int y2 = by - (int)(Math.sin(angle + 0.5) * ahs);
        g2.drawLine(bx, by, x1, y1);
        g2.drawLine(bx, by, x2, y2);
    }
}
