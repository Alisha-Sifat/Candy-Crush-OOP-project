import java.io.*;

public class SaveManager {

    private static final String FILE = "save.txt";

    public static void saveLevel(int level) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println(level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int loadLevel() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            return Integer.parseInt(br.readLine());
        } catch (Exception e) {
            return 1; // default → only level 1 unlocked
        }
    }
}