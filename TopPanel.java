import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * TopPanel — redesigned with a rich candy-pink gradient,
 * pill-style stat badges, and a live goal progress bar.
 */
public class TopPanel extends JPanel {

    private int   currentScore  = 0;
    private int   currentMoves  = 20;
    private String goalText     = "Goal";
    private int   goalProgress  = 0;   // 0–100 percent
    private String hintText     = "";  // shown when a hint fires

    // ── Badge colours
    private static final Color BADGE_MOVES  = new Color(255, 90, 130);   // hot pink
    private static final Color BADGE_SCORE  = new Color(80,  180, 255);  // sky blue
    private static final Color BADGE_GOAL   = new Color(120, 220, 80);   // lime green
    private static final Color BAR_FILL     = new Color(80,  220, 80);
    private static final Color BAR_BG       = new Color(255, 255, 255, 80);
    private static final Color TEXT_WHITE   = Color.WHITE;
    private static final Color TEXT_SHADOW  = new Color(0, 0, 0, 80);

    public TopPanel() {
        setPreferredSize(new Dimension(600, 100));
        setOpaque(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PUBLIC UPDATE API
    // ─────────────────────────────────────────────────────────────────────

    /** Primary update — called by Game.java after every move. */
    public void update(int score, int moves, LevelProgress progress) {
        this.currentScore = score;
        this.currentMoves = moves;
        this.goalText     = progress.getProgressText();
        // Compute percent complete (cap at 100)
        int pct = (progress.getTarget() > 0)
                ? (int)(100L * progress.getProgress() / progress.getTarget())
                : 0;
        this.goalProgress = Math.min(100, pct);
        repaint();
    }

    /** Legacy overload — keeps old callers compiling. */
    public void update(int score, int moves, int targetScore) {
        this.currentScore = score;
        this.currentMoves = moves;
        this.goalText     = "Target: " + targetScore;
        this.goalProgress = (targetScore > 0)
                ? Math.min(100, (int)(100L * score / targetScore)) : 0;
        repaint();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PAINT
    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();

        // ── Background gradient ───────────────────────────────────────────
        GradientPaint bg = new GradientPaint(
                0, 0,  new Color(255, 105, 175),   // candy pink
                W, H,  new Color(200, 60,  140));  // deeper rose
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        // Subtle diagonal sheen
        g2.setColor(new Color(255, 255, 255, 25));
        for (int x = 0; x < W + H; x += 18) {
            g2.drawLine(x, 0, x - H, H);
        }

        // ── Bottom separator line ─────────────────────────────────────────
        g2.setColor(new Color(180, 40, 110, 160));
        g2.fillRect(0, H - 3, W, 3);

        // ── Three badges ─────────────────────────────────────────────────
        //   Moves | Goal progress | Score

        int badgeY  = 10;
        int badgeH  = 36;
        int gap     = 10;
        int badgeW  = (W - gap * 4) / 3;

        // Moves badge
        drawBadge(g2, gap,                     badgeY, badgeW, badgeH,
                  BADGE_MOVES, "🕹 MOVES", String.valueOf(currentMoves));

        // Goal badge
        drawBadge(g2, gap * 2 + badgeW,       badgeY, badgeW, badgeH,
                  BADGE_GOAL,  "🎯 GOAL",  goalText);

        // Score badge
        drawBadge(g2, gap * 3 + badgeW * 2,   badgeY, badgeW, badgeH,
                  BADGE_SCORE, "⭐ SCORE", String.valueOf(currentScore));

        // ── Progress bar (full width, bottom area) ────────────────────────
        int barX = gap, barY = badgeY + badgeH + 8;
        int barW = W - gap * 2, barH = 12;

        // Background track
        g2.setColor(BAR_BG);
        g2.fill(new RoundRectangle2D.Float(barX, barY, barW, barH, barH, barH));

        // Fill
        int fillW = (int)(barW * goalProgress / 100.0);
        if (fillW > 0) {
            GradientPaint barFill = new GradientPaint(
                    barX, barY, new Color(120, 255, 100),
                    barX + fillW, barY, new Color(50, 200, 50));
            g2.setPaint(barFill);
            g2.fill(new RoundRectangle2D.Float(barX, barY, fillW, barH, barH, barH));

            // Sheen on bar
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fill(new RoundRectangle2D.Float(barX, barY, fillW, barH / 2, barH, barH));
        }

        // Border
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(barX, barY, barW, barH, barH, barH));

        g2.dispose();
    }

    /** Draws a pill-shaped badge with a small label above and a value below. */
    private void drawBadge(Graphics2D g2, int x, int y, int w, int h,
                           Color color, String label, String value) {

        // Shadow
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new RoundRectangle2D.Float(x + 2, y + 2, w, h, h, h));

        // Badge body
        GradientPaint gp = new GradientPaint(x, y, color.brighter(), x, y + h, color.darker());
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));

        // Top sheen
        g2.setColor(new Color(255, 255, 255, 70));
        g2.fill(new RoundRectangle2D.Float(x + 3, y + 2, w - 6, h / 2 - 2, h - 4, h - 4));

        // Label (small, top half)
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.setColor(TEXT_SHADOW);
        FontMetrics fmL = g2.getFontMetrics();
        int lx = x + (w - fmL.stringWidth(label)) / 2;
        g2.drawString(label, lx + 1, y + 14 + 1);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString(label, lx, y + 14);

        // Value (bold, bottom half) — truncate if too long
        String display = value;
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fmV = g2.getFontMetrics();
        while (fmV.stringWidth(display) > w - 8 && display.length() > 4) {
            display = display.substring(0, display.length() - 2) + "…";
        }
        int vx = x + (w - fmV.stringWidth(display)) / 2;
        g2.setColor(TEXT_SHADOW);
        g2.drawString(display, vx + 1, y + h - 7 + 1);
        g2.setColor(TEXT_WHITE);
        g2.drawString(display, vx, y + h - 7);
    }
}
