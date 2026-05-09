import java.awt.*;
import java.util.ArrayList;

/**
 * HintSystem — manages the idle-hint feature for Sugar Rush Saga.
 *
 * After the player is idle for ~3 seconds, this class:
 *   1. Scans the board for a valid swap (findHint).
 *   2. Draws a pulsing golden glow around those two candies.
 *
 * Usage in Game.java:
 *   • Create once:  hintSystem = new HintSystem(board, SQUARE_SIZE, SIZE);
 *   • Every IDLE tick:       hintSystem.tick();
 *   • Every non-IDLE tick:   hintSystem.cancelHint();
 *   • On player click:       hintSystem.reset();
 *   • During render (IDLE):  hintSystem.draw(g);
 */
public class HintSystem {

    // ── Dependencies ──────────────────────────────────────────────────────
    private final Board board;
    private final int   squareSize;
    private final int   boardSize;

    // ── State ─────────────────────────────────────────────────────────────
    private Point[] hintPair      = null;   // the two candies to highlight
    private float   hintPulse     = 0f;     // drives the glow animation
    private int     hintIdleTicks = 0;      // counts idle frames

    /** Frames of idle time before a hint appears (~3 s at 16 ms/tick). */
    private static final int HINT_DELAY_TICKS = 180;

    // ─────────────────────────────────────────────────────────────────────
    //  CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────────
    public HintSystem(Board board, int squareSize, int boardSize) {
        this.board      = board;
        this.squareSize = squareSize;
        this.boardSize  = boardSize;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PUBLIC API  (called by Game.java)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Call every animation tick while the game is in the IDLE phase.
     * Increments the idle counter and advances the pulse animation once
     * the delay has elapsed.
     */
    public void tick() {
        hintIdleTicks++;
        if (hintIdleTicks >= HINT_DELAY_TICKS) {
            if (hintPair == null) hintPair = findHint();
            hintPulse += 0.08f;
            if (hintPulse > 2 * Math.PI) hintPulse -= (float)(2 * Math.PI);
        }
    }

    /**
     * Call every animation tick while the game is NOT in IDLE phase
     * (i.e. during animations).  Suppresses any active hint.
     */
    public void cancelHint() {
        hintIdleTicks = 0;
        hintPair      = null;
        hintPulse     = 0f;
    }

    /**
     * Call when the player clicks / makes a move.
     * Resets the idle countdown so the hint timer starts fresh.
     */
    public void reset() {
        hintPair      = null;
        hintPulse     = 0f;
        hintIdleTicks = 0;
    }

    /** @return true while a hint is being shown on screen. */
    public boolean isActive() {
        return hintPair != null;
    }

    /**
     * Draws the pulsing golden glow around the two hint candies.
     * Call this from Game.render() only when phase == IDLE and isActive().
     */
    public void draw(Graphics g) {
        if (hintPair == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float alpha = (float)(0.45 + 0.45 * Math.sin(hintPulse));
        int   a     = Math.max(0, Math.min(255, (int)(alpha * 255)));

        // ── Wide outer glow ───────────────────────────────────────────────
        g2.setColor(new Color(255, 230, 0, a / 3));
        g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Point p : hintPair)
            g2.drawRoundRect(p.x * squareSize + 4, p.y * squareSize + 4,
                             squareSize - 8, squareSize - 8, 12, 12);

        // ── Sharp inner ring ──────────────────────────────────────────────
        g2.setColor(new Color(255, 220, 0, a));
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Point p : hintPair)
            g2.drawRoundRect(p.x * squareSize + 4, p.y * squareSize + 4,
                             squareSize - 8, squareSize - 8, 12, 12);

        // ── Subtle fill tint ─────────────────────────────────────────────
        g2.setColor(new Color(255, 255, 150, a / 5));
        for (Point p : hintPair)
            g2.fillRoundRect(p.x * squareSize + 4, p.y * squareSize + 4,
                             squareSize - 8, squareSize - 8, 12, 12);

        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PRIVATE — BOARD SCAN
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Scans every adjacent pair on the board and returns the first swap
     * that would produce a 3+ match, as [pointA, pointB].
     * Returns null if no valid move exists (board is stuck).
     */
    private Point[] findHint() {
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {

                // Try swapping right
                if (x + 1 < boardSize) {
                    Point a = new Point(x, y), b = new Point(x + 1, y);
                    board.swap(a, b);
                    boolean valid = !board.findMatchGroups().isEmpty();
                    board.swap(a, b);   // always restore
                    if (valid) return new Point[]{a, b};
                }

                // Try swapping down
                if (y + 1 < boardSize) {
                    Point a = new Point(x, y), b = new Point(x, y + 1);
                    board.swap(a, b);
                    boolean valid = !board.findMatchGroups().isEmpty();
                    board.swap(a, b);   // always restore
                    if (valid) return new Point[]{a, b};
                }
            }
        }
        return null;   // no valid move found
    }
}
