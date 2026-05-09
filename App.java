import javax.swing.*;

public class App {
    public static void main(String[] args) {

        // ── Reset progress every launch ──────────────────────────────────
        // This ensures only Level 1 is unlocked when the app starts.
        // Levels unlock progressively as you beat them during this session,
        // but everything resets the next time you run the program.
        SaveManager.saveLevel(1);

        JFrame frame = new JFrame("Sugar Rush Saga");
        frame.setSize(500, 800);
        frame.add(new PlayPage());
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
