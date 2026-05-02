import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Redesigned top HUD panel — fully custom-painted.
 * Three candy-crush-style "bubbles":
 *   LEFT  → Moves remaining (turns red when ≤ 5)
 *   CENTER → Goal progress
 *   RIGHT → Score
 *
 * Also contains the Hint button (💡) on the far right.
 */
public class TopPanel extends JPanel {

    private int    score      = 0;
    private int    moves      = 0;
    private String goalText   = "Goal";
    private boolean movesLow  = false;   // red flash when ≤ 5

    // Hint button bounds
    private Rectangle hintBounds = new Rectangle();
    private boolean   hintHover  = false;

    // Callback — set by Game.java
    private Runnable onHintClicked;

    public TopPanel() {
        setPreferredSize(new Dimension(600, 88));
        setOpaque(false);  // we paint our own background

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                boolean over = hintBounds.contains(e.getPoint());
                if (over != hintHover) { hintHover = over; repaint(); }
                setCursor(over
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (hintBounds.contains(e.getPoint()) && onHintClicked != null)
                    onHintClicked.run();
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                hintHover = false; repaint();
            }
        });
    }

    public void setOnHintClicked(Runnable r) { this.onHintClicked = r; }

    // ── Called by Game.java every move ────────────────────────────────────

    public void update(int score, int moves, LevelProgress progress) {
        this.score    = score;
        this.moves    = moves;
        this.goalText = progress.getProgressText();
        this.movesLow = moves <= 5;
        repaint();
    }

    /** Legacy overload. */
    public void update(int score, int moves, int targetScore) {
        this.score    = score;
        this.moves    = moves;
        this.goalText = "Target: " + targetScore;
        this.movesLow = moves <= 5;
        repaint();
    }

    // ── Paint ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // ── Background bar ────────────────────────────────────────────────
        GradientPaint bg = new GradientPaint(0, 0, new Color(180, 0, 120),
                                             w, 0, new Color(100, 0, 180));
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        // Bottom shine line
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRect(0, h - 3, w, 3);

        // ── Three info bubbles ────────────────────────────────────────────
        int bubbleW = 110, bubbleH = 62;
        int gap     = (w - bubbleW * 3 - 44) / 4;  // 44 = hint btn width + margin
        int baseY   = (h - bubbleH) / 2;

        int x1 = gap;
        int x2 = x1 + bubbleW + gap;
        int x3 = x2 + bubbleW + gap;

        // Moves
        Color movesTop = movesLow ? new Color(255, 80, 80) : new Color(255, 180, 0);
        Color movesBot = movesLow ? new Color(180, 20, 20) : new Color(200, 100, 0);
        drawBubble(g2, x1, baseY, bubbleW, bubbleH,
                   "MOVES", String.valueOf(moves), movesTop, movesBot);

        // Goal
        drawBubble(g2, x2, baseY, bubbleW, bubbleH,
                   "GOAL", goalText,
                   new Color(80, 200, 255), new Color(20, 100, 200));

        // Score
        drawBubble(g2, x3, baseY, bubbleW, bubbleH,
                   "SCORE", String.valueOf(score),
                   new Color(120, 255, 120), new Color(20, 160, 20));

        // ── Hint button ────────────────────────────────────────────────────
        int hx = w - 46, hy = baseY + 4, hw = 38, hh = 54;
        hintBounds = new Rectangle(hx, hy, hw, hh);
        drawHintButton(g2, hx, hy, hw, hh);
    }

    private void drawBubble(Graphics2D g2, int x, int y, int w, int h,
                             String label, String value, Color top, Color bot) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 3, y + 4, w, h, 20, 20);

        // Body
        GradientPaint gp = new GradientPaint(x, y, top, x, y + h, bot);
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, h, 20, 20);

        // Border
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 20, 20);

        // Gloss
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(x + 5, y + 4, w - 10, h / 3, 14, 14);

        // Label (small, top)
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.setColor(new Color(255, 255, 255, 200));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, x + w/2 - fm.stringWidth(label)/2, y + 17);

        // Value (big, bottom)
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        fm = g2.getFontMetrics();
        // Shrink font if value too wide
        String val = value;
        while (fm.stringWidth(val) > w - 8 && g2.getFont().getSize() > 10) {
            g2.setFont(g2.getFont().deriveFont((float)g2.getFont().getSize() - 1));
            fm = g2.getFontMetrics();
        }
        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(val, x + w/2 - fm.stringWidth(val)/2 + 1, y + h - 12 + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(val, x + w/2 - fm.stringWidth(val)/2, y + h - 12);
    }

    private void drawHintButton(Graphics2D g2, int x, int y, int w, int h) {
        Color top = hintHover ? new Color(255, 230, 60)  : new Color(255, 200, 0);
        Color bot = hintHover ? new Color(200, 140, 0)   : new Color(180, 100, 0);

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 2, y + 3, w, h, 16, 16);

        GradientPaint gp = new GradientPaint(x, y, top, x, y + h, bot);
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, h, 16, 16);

        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 16, 16);

        // Lightbulb emoji or symbol
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        FontMetrics fm = g2.getFontMetrics();
        String icon = "💡";
        g2.drawString(icon, x + w/2 - fm.stringWidth(icon)/2, y + h/2 + fm.getAscent()/2 - 4);
    }
}
