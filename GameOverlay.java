import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * A fancy win / lose overlay drawn directly on top of the game panel.
 * No JOptionPane — just pure Graphics2D, just like real Candy Crush.
 *
 * Usage:
 *   overlay = new GameOverlay(true,  score, level, onRetry, onMap, onNext);  // win
 *   overlay = new GameOverlay(false, score, level, onRetry, onMap, null);    // lose
 *   // then in render(): if (overlay != null) overlay.draw(g2, panelW, panelH);
 *   // and in animTimer: if (overlay != null) overlay.tick();
 */
public class GameOverlay {

    private final boolean  won;
    private final int      score;
    private final Level    level;

    private final Runnable onRetry;   // always present
    private final Runnable onMap;     // always present
    private final Runnable onNext;    // null if no next level

    // ── Animation ─────────────────────────────────────────────────────────
    private float   slideIn    = 0f;   // 0→1 card slides up from bottom
    private float   starAngle  = 0f;   // rotating sparkle ring
    private int     starCount  = 0;    // stars earned (0-3), computed once
    private boolean tickDone   = false;

    // Confetti / particles (win only)
    private final ArrayList<float[]> particles = new ArrayList<>(); // x,y,vx,vy,hue,alpha
    private final Random rng = new Random();

    // Button hit-boxes (set during draw, read in mousePressed)
    private Rectangle retryBounds = new Rectangle();
    private Rectangle mapBounds   = new Rectangle();
    private Rectangle nextBounds  = new Rectangle();

    // Hover state
    private int hovered = -1;  // -1=none, 0=retry, 1=map, 2=next

    public GameOverlay(boolean won, int score, Level level,
                       Runnable onRetry, Runnable onMap, Runnable onNext) {
        this.won     = won;
        this.score   = score;
        this.level   = level;
        this.onRetry = onRetry;
        this.onMap   = onMap;
        this.onNext  = onNext;

        // Stars: rough thresholds based on target score
        int target = level.getTargetAmount();
        if      (score >= target * 2)   starCount = 3;
        else if (score >= target * 1.3) starCount = 2;
        else if (score >= target)       starCount = 1;
        else                            starCount = 0;

        // Seed confetti
        if (won) {
            for (int i = 0; i < 60; i++) {
                particles.add(new float[]{
                    rng.nextFloat(),           // x (0-1 of panel)
                    -rng.nextFloat() * 0.3f,   // y (start above panel)
                    (rng.nextFloat() - 0.5f) * 0.004f,  // vx
                    rng.nextFloat() * 0.006f + 0.003f,  // vy (downward)
                    rng.nextFloat(),            // hue 0-1
                    1f                          // alpha
                });
            }
        }
    }

    // ── Called every timer tick (16 ms) ────────────────────────────────────
    public void tick() {
        slideIn    = Math.min(1f, slideIn + 0.05f);
        starAngle += 0.03f;

        for (float[] p : particles) {
            p[0] += p[2];
            p[1] += p[3];
            if (p[1] > 1.1f) {      // recycle off-bottom
                p[0] = rng.nextFloat();
                p[1] = -0.05f;
                p[3] = rng.nextFloat() * 0.006f + 0.003f;
            }
        }
    }

    // ── Mouse handling ─────────────────────────────────────────────────────
    public void mouseMoved(int mx, int my) {
        hovered = -1;
        if (retryBounds.contains(mx, my)) hovered = 0;
        else if (mapBounds.contains(mx, my)) hovered = 1;
        else if (nextBounds != null && nextBounds.contains(mx, my)) hovered = 2;
    }

    public void mousePressed(int mx, int my) {
        if (retryBounds.contains(mx, my) && onRetry != null) onRetry.run();
        else if (mapBounds.contains(mx, my) && onMap  != null) onMap.run();
        else if (nextBounds.contains(mx, my) && onNext != null) onNext.run();
    }

    // ── Main draw ──────────────────────────────────────────────────────────
    public void draw(Graphics g, int pw, int ph) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ── dim background ─────────────────────────────────────────────────
        float dimAlpha = slideIn * 0.65f;
        g2.setColor(new Color(0, 0, 0, (int)(dimAlpha * 255)));
        g2.fillRect(0, 0, pw, ph);

        // ── confetti ───────────────────────────────────────────────────────
        if (won) drawConfetti(g2, pw, ph);

        // ── card geometry ──────────────────────────────────────────────────
        int cardW = (int)(pw * 0.82f);
        int cardH = (int)(ph * 0.58f);
        int cardX = (pw - cardW) / 2;

        // Slide in from bottom
        float ease = easeOut(slideIn);
        int cardY = (int)(ph * 0.5f - cardH * 0.5f + (1f - ease) * ph * 0.5f);

        // Card shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(cardX + 6, cardY + 8, cardW, cardH, 40, 40);

        // Card background gradient
        Color top, bot;
        if (won) {
            top = new Color(255, 220, 60);
            bot = new Color(255, 140, 20);
        } else {
            top = new Color(80,  30, 130);
            bot = new Color(40,  10,  80);
        }
        GradientPaint cardBg = new GradientPaint(cardX, cardY, top, cardX, cardY + cardH, bot);
        g2.setPaint(cardBg);
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 40, 40);

        // Card border
        g2.setColor(new Color(255, 255, 255, 120));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 40, 40);

        // Inner gloss strip
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRoundRect(cardX + 8, cardY + 8, cardW - 16, cardH / 3, 30, 30);

        int cx = cardX + cardW / 2;

        // ── Title ──────────────────────────────────────────────────────────
        String title = won ? "LEVEL COMPLETE!" : "OUT OF MOVES!";
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, won ? 30 : 26));
        g2.setColor(new Color(0, 0, 0, 80));
        drawCentered(g2, title, cx + 2, cardY + 52);
        g2.setColor(Color.WHITE);
        drawCentered(g2, title, cx, cardY + 50);

        // ── Stars (win) or sad face (lose) ─────────────────────────────────
        int starY = cardY + 110;
        if (won) {
            drawStars(g2, cx, starY, starCount);
        } else {
            drawSadFace(g2, cx, starY - 10, 54);
        }

        // ── Score ──────────────────────────────────────────────────────────
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 20));
        g2.setColor(new Color(255, 255, 200));
        String scoreTxt = "Score: " + score;
        drawCentered(g2, scoreTxt, cx, cardY + 175);

        // ── Goal line ──────────────────────────────────────────────────────
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.setColor(new Color(255, 255, 255, 180));
        drawCentered(g2, level.getGoalDescription(), cx, cardY + 200);

        // ── Buttons ────────────────────────────────────────────────────────
        int btnY    = cardY + cardH - 80;
        int btnH    = 50;
        int btnGap  = 12;

        if (won && onNext != null) {
            // Three buttons: Retry | Map | Next
            int btnW = (cardW - btnGap * 4) / 3;
            int b1x  = cardX + btnGap;
            int b2x  = b1x + btnW + btnGap;
            int b3x  = b2x + btnW + btnGap;

            retryBounds = new Rectangle(b1x, btnY, btnW, btnH);
            mapBounds   = new Rectangle(b2x, btnY, btnW, btnH);
            nextBounds  = new Rectangle(b3x, btnY, btnW, btnH);

            drawBtn(g2, retryBounds, "↩ Retry",   new Color(200, 80,  80),  hovered == 0);
            drawBtn(g2, mapBounds,   "🗺 Map",    new Color(80,  130, 200), hovered == 1);
            drawBtn(g2, nextBounds,  "Next ▶",    new Color(60,  180, 80),  hovered == 2);
        } else {
            // Two buttons: Retry | Map
            int btnW = (cardW - btnGap * 3) / 2;
            int b1x  = cardX + btnGap;
            int b2x  = b1x + btnW + btnGap;

            retryBounds = new Rectangle(b1x, btnY, btnW, btnH);
            mapBounds   = new Rectangle(b2x, btnY, btnW, btnH);
            nextBounds  = new Rectangle(0, 0, 0, 0); // invisible

            drawBtn(g2, retryBounds, "↩ Try Again", new Color(220, 80,  80),  hovered == 0);
            drawBtn(g2, mapBounds,   "🗺 Level Map", new Color(80,  130, 200), hovered == 1);
        }

        // ── Sparkle ring (win) ─────────────────────────────────────────────
        if (won) drawSparkles(g2, cx, starY - 5, starAngle);
    }

    // ── Stars ──────────────────────────────────────────────────────────────
    private void drawStars(Graphics2D g2, int cx, int cy, int count) {
        int starSize = 44;
        int spacing  = 56;
        int[] xs = { cx - spacing, cx, cx + spacing };

        for (int i = 0; i < 3; i++) {
            boolean filled = i < count;
            drawStar(g2, xs[i], cy, starSize, filled);
        }
    }

    private void drawStar(Graphics2D g2, int cx, int cy, int size, boolean filled) {
        int pts = 5;
        double[] angles = new double[pts * 2];
        int[] px = new int[pts * 2], py = new int[pts * 2];
        for (int i = 0; i < pts * 2; i++) {
            double angle = Math.PI / pts * i - Math.PI / 2;
            double r = (i % 2 == 0) ? size / 2.0 : size / 4.5;
            px[i] = cx + (int)(Math.cos(angle) * r);
            py[i] = cy + (int)(Math.sin(angle) * r);
        }
        // Shadow
        int[] spx = new int[pts*2], spy = new int[pts*2];
        for (int i=0;i<pts*2;i++){spx[i]=px[i]+3;spy[i]=py[i]+3;}
        g2.setColor(new Color(0,0,0,80));
        g2.fillPolygon(spx, spy, pts*2);

        if (filled) {
            GradientPaint gp = new GradientPaint(cx, cy - size/2, new Color(255,240,60),
                                                 cx, cy + size/2, new Color(220,140,0));
            g2.setPaint(gp);
        } else {
            g2.setColor(new Color(100, 100, 100, 140));
        }
        g2.fillPolygon(px, py, pts * 2);
        g2.setColor(filled ? new Color(200, 120, 0) : new Color(60, 60, 60, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(px, py, pts * 2);
    }

    // ── Sad face (lose) ────────────────────────────────────────────────────
    private void drawSadFace(Graphics2D g2, int cx, int cy, int r) {
        // face
        g2.setColor(new Color(255, 210, 60));
        g2.fillOval(cx - r, cy - r, r*2, r*2);
        g2.setColor(new Color(200, 150, 0));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - r, cy - r, r*2, r*2);
        // eyes
        g2.setColor(new Color(60, 30, 0));
        g2.fillOval(cx - r/3 - 5, cy - r/4, 9, 9);
        g2.fillOval(cx + r/3 - 4, cy - r/4, 9, 9);
        // sad mouth arc
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - r/2, cy + r/6, r, r/2, 0, -180);
    }

    // ── Button ─────────────────────────────────────────────────────────────
    private void drawBtn(Graphics2D g2, Rectangle b, String label, Color col, boolean hover) {
        Color draw = hover ? col.brighter() : col;
        GradientPaint gp = new GradientPaint(b.x, b.y, draw.brighter(),
                                             b.x, b.y + b.height, draw.darker());
        g2.setPaint(gp);
        g2.fillRoundRect(b.x, b.y, b.width, b.height, 22, 22);

        // Gloss
        g2.setColor(new Color(255, 255, 255, hover ? 80 : 50));
        g2.fillRoundRect(b.x + 3, b.y + 3, b.width - 6, b.height / 2 - 3, 16, 16);

        // Border
        g2.setColor(new Color(255, 255, 255, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(b.x, b.y, b.width, b.height, 22, 22);

        // Label
        g2.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label,
            b.x + b.width/2  - fm.stringWidth(label)/2,
            b.y + b.height/2 + fm.getAscent()/2 - 2);
    }

    // ── Confetti ───────────────────────────────────────────────────────────
    private void drawConfetti(Graphics2D g2, int pw, int ph) {
        for (float[] p : particles) {
            int px2 = (int)(p[0] * pw);
            int py2 = (int)(p[1] * ph);
            Color c = Color.getHSBColor(p[4], 0.9f, 1f);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(p[5]*220)));
            g2.fillRect(px2, py2, 7, 7);
        }
    }

    // ── Sparkle ring ───────────────────────────────────────────────────────
    private void drawSparkles(Graphics2D g2, int cx, int cy, float angle) {
        int count = 8, r = 85;
        for (int i = 0; i < count; i++) {
            double a = angle + i * 2 * Math.PI / count;
            int sx = cx + (int)(Math.cos(a) * r);
            int sy = cy + (int)(Math.sin(a) * r);
            float hue = (float)i / count;
            g2.setColor(Color.getHSBColor(hue, 1f, 1f));
            drawSparkle(g2, sx, sy, 6);
        }
    }

    private void drawSparkle(Graphics2D g2, int cx, int cy, int r) {
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx, cy - r, cx, cy + r);
        g2.drawLine(cx - r, cy, cx + r, cy);
        g2.setStroke(new BasicStroke(1.5f));
        int d = (int)(r * 0.7);
        g2.drawLine(cx - d, cy - d, cx + d, cy + d);
        g2.drawLine(cx + d, cy - d, cx - d, cy + d);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void drawCentered(Graphics2D g2, String s, int cx, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, cx - fm.stringWidth(s) / 2, y);
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
