import javax.swing.*;
import java.awt.*;

public class PlayPage extends JPanel {

    private Image bg;

    public PlayPage() {
        bg = new ImageIcon("src/images/Playpage.jpeg").getImage();

        setLayout(null); // IMPORTANT (for placing button manually)

        // 🎮 PLAY BUTTON
        JButton playBtn = new JButton("PLAY");
        playBtn.setBounds(140, 500, 220, 70); // adjust position if needed

        playBtn.setFont(new Font("Arial", Font.BOLD, 26));
        playBtn.setBackground(new Color(255, 105, 180)); // pink
        playBtn.setForeground(Color.WHITE);
        playBtn.setFocusPainted(false);

        // 🎯 CLICK → GO TO MAP
        playBtn.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.dispose();

            JFrame mapFrame = new JFrame("Level Map");
            mapFrame.setSize(500, 800);
            mapFrame.add(new LevelMap());
            mapFrame.setVisible(true);
            mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });

        add(playBtn);
    }

    // 🎨 DRAW BACKGROUND IMAGE
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
    }
}