import com.candycrush.Candy;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

public class Board {
    private Candy[][] grid;
    private int size;
    private Random random = new Random();

    public Board(int size) {
        this.size = size;
        grid = new Candy[size][size];
    }

    public Candy get(int x, int y) {
        return grid[y][x];
    }

    public void set(int x, int y, Candy candy) {
        grid[y][x] = candy;
    }

    public int getSize() {
        return size;
    }

    public void swap(Point a, Point b) {
        Candy temp = grid[a.y][a.x];
        grid[a.y][a.x] = grid[b.y][b.x];
        grid[b.y][b.x] = temp;
    }

    // Checks ALL 4 directions properly
    private boolean wouldCreateMatch(int x, int y, int type) {
        // Horizontal
        int left = 0;
        while (x - left - 1 >= 0
                && grid[y][x - left - 1] != null
                && grid[y][x - left - 1].getType() == type) {
            left++;
        }
        int right = 0;
        while (x + right + 1 < size
                && grid[y][x + right + 1] != null
                && grid[y][x + right + 1].getType() == type) {
            right++;
        }
        if (left + right >= 2) return true;

        // Vertical
        int up = 0;
        while (y - up - 1 >= 0
                && grid[y - up - 1][x] != null
                && grid[y - up - 1][x].getType() == type) {
            up++;
        }
        int down = 0;
        while (y + down + 1 < size
                && grid[y + down + 1][x] != null
                && grid[y + down + 1][x].getType() == type) {
            down++;
        }
        if (up + down >= 2) return true;

        return false;
    }

    // Nuclear option — after everything is placed, scan and fix ANY remaining match
    public void resolveAllMatches(java.awt.Image[] images) {
        int totalTypes = images.length;
        ArrayList<Point> matches = findMatches();

        int safetyCounter = 0;
        while (!matches.isEmpty() && safetyCounter < 100) {
            for (Point p : matches) {
                ArrayList<Integer> safeTypes = new ArrayList<>();
                for (int t = 0; t < totalTypes; t++) {
                    Candy original = grid[p.y][p.x];
                    grid[p.y][p.x] = null;           // temporarily remove
                    if (!wouldCreateMatch(p.x, p.y, t)) {
                        safeTypes.add(t);
                    }
                    grid[p.y][p.x] = original;        // restore
                }
                int type = safeTypes.isEmpty()
                        ? random.nextInt(totalTypes)
                        : safeTypes.get(random.nextInt(safeTypes.size()));
                grid[p.y][p.x] = new NormalCandy(type, images[type]);
            }
            matches = findMatches();
            safetyCounter++;
        }
    }

    public void initBoard(java.awt.Image[] images) {
        int totalTypes = images.length;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                ArrayList<Integer> safeTypes = new ArrayList<>();
                for (int t = 0; t < totalTypes; t++) {
                    if (!wouldCreateMatch(x, y, t)) {
                        safeTypes.add(t);
                    }
                }
                int type = safeTypes.isEmpty()
                        ? random.nextInt(totalTypes)
                        : safeTypes.get(random.nextInt(safeTypes.size()));
                grid[y][x] = new NormalCandy(type, images[type]);
            }
        }
        resolveAllMatches(images); // clean sweep at the end
    }

    public ArrayList<Point> findMatches() {
        ArrayList<Point> result = new ArrayList<>();

        // Horizontal
        for (int y = 0; y < size; y++) {
            int count = 1;
            for (int x = 1; x < size; x++) {
                if (grid[y][x] != null && grid[y][x - 1] != null &&
                        grid[y][x].getType() == grid[y][x - 1].getType()) {
                    count++;
                } else {
                    if (count >= 3) {
                        for (int i = 0; i < count; i++)
                            result.add(new Point(x - 1 - i, y));
                    }
                    count = 1;
                }
            }
            if (count >= 3) {
                for (int i = 0; i < count; i++)
                    result.add(new Point(size - 1 - i, y));
            }
        }

        // Vertical
        for (int x = 0; x < size; x++) {
            int count = 1;
            for (int y = 1; y < size; y++) {
                if (grid[y][x] != null && grid[y - 1][x] != null &&
                        grid[y][x].getType() == grid[y - 1][x].getType()) {
                    count++;
                } else {
                    if (count >= 3) {
                        for (int i = 0; i < count; i++)
                            result.add(new Point(x, y - 1 - i));
                    }
                    count = 1;
                }
            }
            if (count >= 3) {
                for (int i = 0; i < count; i++)
                    result.add(new Point(x, size - 1 - i));
            }
        }

        return result;
    }

    public void removeMatches(ArrayList<Point> matches) {
        for (Point p : matches) {
            if (grid[p.y][p.x] != null) {
                grid[p.y][p.x].onMatch();
                grid[p.y][p.x] = null;
            }
        }
    }

    public void applyGravity() {
        for (int x = 0; x < size; x++) {
            for (int y = size - 1; y > 0; y--) {
                if (grid[y][x] == null && grid[y - 1][x] != null) {
                    grid[y][x] = grid[y - 1][x];
                    grid[y - 1][x] = null;
                    y = size;
                }
            }
        }
    }

    public void refill(java.awt.Image[] images) {
        int totalTypes = images.length;
        // Bottom to top so wouldCreateMatch has full context
        for (int x = 0; x < size; x++) {
            for (int y = size - 1; y >= 0; y--) {
                if (grid[y][x] == null) {
                    ArrayList<Integer> safeTypes = new ArrayList<>();
                    for (int t = 0; t < totalTypes; t++) {
                        if (!wouldCreateMatch(x, y, t)) {
                            safeTypes.add(t);
                        }
                    }
                    int type = safeTypes.isEmpty()
                            ? random.nextInt(totalTypes)
                            : safeTypes.get(random.nextInt(safeTypes.size()));
                    grid[y][x] = new NormalCandy(type, images[type]);
                }
            }
        }
        resolveAllMatches(images); // guaranteed clean after every refill
    }
}