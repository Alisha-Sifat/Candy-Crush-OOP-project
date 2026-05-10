import java.awt.Graphics;
import java.awt.Image;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class App extends JFrame{
     public App() {
        setTitle("Game Menu");
        setSize(500, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundPanel panel = new BackgroundPanel();
        panel.setLayout(null);

        // ── Image-based Start Button (from MainMenu logic) ────────────────
        ImageIcon icon = new ImageIcon(
    getClass().getResource("/images/start.png")
);
        Image img = icon.getImage().getScaledInstance(280, 80, Image.SCALE_SMOOTH);
        JButton startButton = new JButton(new ImageIcon(img));

        // Position matches roughly where the PLAY button was in PlayPage
        startButton.setBounds(120, 500, 350, 90);

        // Make it look like a pure image — no borders, no grey box
        startButton.setBorderPainted(false);
        startButton.setContentAreaFilled(false);
        startButton.setFocusPainted(false);
        startButton.setOpaque(false);

        // ── Event Handling (from PlayPage's onPlayClicked logic) ──────────
        startButton.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(startButton);
            frame.dispose();

            JFrame mapFrame = new JFrame("Level Map");
            mapFrame.setSize(500, 800);
            mapFrame.add(new LevelMap());
            mapFrame.setVisible(true);
            mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });

        panel.add(startButton);
        add(panel);
    }
    

    // ── Background Panel (from MainMenu) ──────────────────────────────────
    class BackgroundPanel extends JPanel {
        Image bg = new ImageIcon(
    getClass().getResource("/images/Playpage.jpeg")
).getImage();

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

   public static void main(String[] args) {

    SwingUtilities.invokeLater(() -> {

        SaveManager.saveLevel(1);

        App app = new App();
        app.setVisible(true);
    });
}
}
