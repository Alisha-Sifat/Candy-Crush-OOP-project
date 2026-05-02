import java.io.*;

/**
 * Persists the highest unlocked level number to save.txt.
 * Default = 1 (only Level 1 unlocked on first run).
 * After completing level N, call saveLevel(N+1) to unlock the next.
 */
public class SaveManager {

    private static final String FILE = "save.txt";

    /** Save the highest unlocked level number. */
    public static void saveLevel(int level) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println(level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the highest unlocked level number.
     * Returns 1 if no save file exists (only Level 1 unlocked).
     */
    public static int loadLevel() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            int val = Integer.parseInt(br.readLine().trim());
            return Math.max(1, val); // never return 0
        } catch (Exception e) {
            return 1; // first run — only Level 1 unlocked
        }
    }
}
