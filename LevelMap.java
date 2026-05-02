import javax.swing.*;
import java.awt.*;

public class LevelMap extends JPanel {

    private Image mapImage;

    public LevelMap() {
        // Load your map image
        mapImage = new ImageIcon("src/images/LevelMap.jpeg").getImage();

        setLayout(null); // IMPORTANT for manual positioning
        addMouseListener(new java.awt.event.MouseAdapter() {
    public void mouseClicked(java.awt.event.MouseEvent e) {
        System.out.println("X: " + e.getX() + " Y: " + e.getY());
    }
});
     addMouseListener(new java.awt.event.MouseAdapter() {
    public void mouseClicked(java.awt.event.MouseEvent e) {
        System.out.println("X: " + e.getX() + " Y: " + e.getY());
    }
});
        // Add level buttons (adjust positions according to your image)
        addLevelButton(220, 600, 1);
        addLevelButton(240, 520, 2);
        addLevelButton(260, 450, 3);
        addLevelButton(280, 380, 4);
        addLevelButton(300, 310, 5);
        addLevelButton(320, 240, 6);
    }

    private void addLevelButton(int x, int y, int levelNum) {

        JButton btn = new JButton();

        btn.setBounds(x - 30, y - 30, 60, 60);

        // Make invisible button
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);

        // Optional: show number
        btn.setText(String.valueOf(levelNum));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 18));

        btn.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.dispose();

            Level lvl = LevelManager.getLevels().get(levelNum - 1);
            new Game().startLevel(lvl);
        });

        add(btn);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background image
        g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);
    }
}