import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class LevelSelect {

    public void showLevels() {
        JFrame frame = new JFrame("Select Level");
        frame.setSize(400, 500);
        frame.setLayout(new GridLayout(3, 2));

        ArrayList<Level> levels = LevelManager.getLevels();

        for (Level lvl : levels) {
            JButton btn = new JButton("Level " + lvl.getLevelNumber());

            btn.addActionListener(e -> {
                frame.dispose();
                Game game = new Game();
                game.startLevel(lvl);
            });

            frame.add(btn);
        }

        frame.setVisible(true);
    }
}
