import javax.swing.*;
import java.awt.*;

public class TopPanel extends JPanel {

    private JLabel scoreLabel;
    private JLabel movesLabel;
    private JLabel targetLabel;

    public TopPanel() {
        setPreferredSize(new Dimension(600, 80));
        setBackground(new Color(255, 182, 193)); // soft candy pink
        setLayout(new BorderLayout());

        // LEFT → Moves
        movesLabel = new JLabel("Moves: 20");
        movesLabel.setFont(new Font("Arial", Font.BOLD, 16));
        movesLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

        // CENTER → Target
        targetLabel = new JLabel("Target: 500", SwingConstants.CENTER);
        targetLabel.setFont(new Font("Arial", Font.BOLD, 18));

        // RIGHT → Score
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));

        add(movesLabel, BorderLayout.WEST);
        add(targetLabel, BorderLayout.CENTER);
        add(scoreLabel, BorderLayout.EAST);
    }

    // 🔥 Update values dynamically
    public void update(int score, int moves, int target) {
        scoreLabel.setText("Score: " + score);
        movesLabel.setText("Moves: " + moves);
        targetLabel.setText("Target: " + target);
    }
}
