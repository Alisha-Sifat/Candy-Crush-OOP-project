import java.util.ArrayList;

public class LevelManager {

    public static ArrayList<Level> getLevels() {
        ArrayList<Level> levels = new ArrayList<>();

        levels.add(new Level(1, 200, 20));
        levels.add(new Level(2, 400, 18));
        levels.add(new Level(3, 600, 16));
        levels.add(new Level(4, 900, 15));
        levels.add(new Level(5, 1200, 14));
        levels.add(new Level(6, 1500, 12));

        return levels;
    }
}