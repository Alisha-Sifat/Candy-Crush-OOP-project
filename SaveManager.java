import java.io.*;

public class SaveManager {

    public static void saveLevel(int level) {
        try {
            FileWriter writer = new FileWriter("save.txt");
            writer.write(String.valueOf(level));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int loadLevel() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("save.txt"));
            return Integer.parseInt(reader.readLine());
        } catch (Exception e) {
            return 1;
        }
    }
}