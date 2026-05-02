import javax.swing.*;

public class App {
    public static void main(String[] args) {

        // Always reset to only Level 1 unlocked at the start of every run
        SaveManager.saveLevel(1);

        JFrame frame = new JFrame("Sugar Rush Saga");

        frame.setSize(500, 800);
        frame.add(new PlayPage());

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}