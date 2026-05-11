import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class TopPanel extends JPanel {

    private int currentScore = 0;
    private int currentMoves = 20;
    private String goalText = "Goal";
    private int goalProgress = 0;

    // Colors
    private final Color movesColor = new Color(255, 90, 130);
    private final Color scoreColor = new Color(80, 180, 255);
    private final Color goalColor  = new Color(120, 220, 80);

    public TopPanel() {
        setPreferredSize(new Dimension(600, 100));
    }

    // Main update method
    public void update(int score, int moves, LevelProgress progress) {

        currentScore = score;
        currentMoves = moves;

        goalText = progress.getProgressText();

        if (progress.getTarget() > 0) {
            goalProgress =
                    (int)(100.0 * progress.getProgress()
                    / progress.getTarget());

            goalProgress = Math.min(goalProgress, 100);
        }

        repaint();
    }

    // Old compatibility method
    public void update(int score, int moves, int targetScore) {

        currentScore = score;
        currentMoves = moves;

        goalText = "Target: " + targetScore;

        if (targetScore > 0) {
            goalProgress =
                    (int)(100.0 * score / targetScore);

            goalProgress = Math.min(goalProgress, 100);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int W = getWidth();

        // Background
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(255, 120, 180),
                0, getHeight(), new Color(200, 70, 140));

        g2.setPaint(bg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Badge sizes
        int gap = 10;
        int badgeW = (W - 40) / 3;

        // Draw badges
        drawBadge(g2, gap, 10,
                badgeW, 35,
                movesColor,
                "MOVES",
                String.valueOf(currentMoves));

        drawBadge(g2, gap * 2 + badgeW, 10,
                badgeW, 35,
                goalColor,
                "GOAL",
                goalText);

        drawBadge(g2, gap * 3 + badgeW * 2, 10,
                badgeW, 35,
                scoreColor,
                "SCORE",
                String.valueOf(currentScore));

        // Progress bar background
        int barX = 10;
        int barY = 60;
        int barW = W - 20;
        int barH = 15;

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(barX, barY, barW, barH, 20, 20);

        // Progress fill
        int fill = (int)(barW * goalProgress / 100.0);

        g2.setColor(new Color(50, 200, 50));
        g2.fillRoundRect(barX, barY, fill, barH, 20, 20);

        // Border
        g2.setColor(Color.DARK_GRAY);
        g2.drawRoundRect(barX, barY, barW, barH, 20, 20);
    }

    // Helper method for badges
    private void drawBadge(Graphics2D g2,
                           int x, int y,
                           int w, int h,
                           Color color,
                           String label,
                           String value) {

        // Badge body
        g2.setColor(color);

        g2.fill(new RoundRectangle2D.Float(
                x, y, w, h, 20, 20));

        // Label
        g2.setColor(Color.WHITE);

        g2.setFont(new Font("Arial", Font.BOLD, 10));

        FontMetrics fm1 = g2.getFontMetrics();

        int lx = x + (w - fm1.stringWidth(label)) / 2;

        g2.drawString(label, lx, y + 12);

        // Value
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        FontMetrics fm2 = g2.getFontMetrics();

        int vx = x + (w - fm2.stringWidth(value)) / 2;

        g2.drawString(value, vx, y + 28);
    }
}