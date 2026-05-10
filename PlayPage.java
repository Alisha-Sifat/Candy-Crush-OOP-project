import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PlayPage extends JPanel {

    private Image bg;

    public PlayPage() {
        bg = new ImageIcon("src/images/Playpage.jpeg").getImage();
        setLayout(null); // manual positioning

        // ── Simple PLAY button ────────────────────────────────────────────
        JButton playBtn = new JButton("▶  PLAY");
        playBtn.setBounds(110, 490, 280, 80);           // same position as before
        playBtn.setFont(new Font("Arial Black", Font.BOLD, 28));
        playBtn.setBackground(new Color(220, 50, 120)); // candy pink
        playBtn.setForeground(Color.WHITE);
        playBtn.setFocusPainted(false);                 // removes default focus border
        playBtn.setBorderPainted(false);                // cleaner look
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        playBtn.addActionListener(e -> onPlayClicked());

        add(playBtn);
    }

    // ── Navigation ────────────────────────────────────────────────────────
    private void onPlayClicked() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.dispose();

        JFrame mapFrame = new JFrame("Level Map");
        mapFrame.setSize(500, 800);
        mapFrame.add(new LevelMap());
        mapFrame.setVisible(true);
        mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // ── Draw background image ─────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
    }
}
