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

    /**
     * Checks whether placing a candy of the given type at (x, y)
     * would immediately form a horizontal or vertical match of 3+.
     */
    private boolean wouldCreateMatch(int x, int y, int type) {
        // Check horizontal: count how many of the same type are to the left
        int left = 0;
        while (x - left - 1 >= 0
                && grid[y][x - left - 1] != null
                && grid[y][x - left - 1].getType() == type) {
            left++;
        }
        if (left >= 2) return true;

        // Check vertical: count how many of the same type are above
        int up = 0;
        while (y - up - 1 >= 0
                && grid[y - up - 1][x] != null
                && grid[y - up - 1][x].getType() == type) {
            up++;
        }
        if (up >= 2) return true;

        return false;
    }

    /**
     * Fills the entire board ensuring no 3-in-a-row matches exist from the start.
     * Call this once during game initialisation instead of refill().
     */
    public void initBoard(java.awt.Image[] images) {
        int totalTypes = images.length; // e.g. 6
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Build a list of types that won't create an instant match
                ArrayList<Integer> safeTypes = new ArrayList<>();
                for (int t = 0; t < totalTypes; t++) {
                    if (!wouldCreateMatch(x, y, t)) {
                        safeTypes.add(t);
                    }
                }

                // Pick a random safe type (fall back to any type if list is empty)
                int type;
                if (!safeTypes.isEmpty()) {
                    type = safeTypes.get(random.nextInt(safeTypes.size()));
                } else {
                    type = random.nextInt(totalTypes); // edge case, very rare
                }

                grid[y][x] = new NormalCandy(type, images[type]);
            }
        }
    }

    public ArrayList<Point> findMatches() {
        ArrayList<Point> result = new ArrayList<>();

        // -------- HORIZONTAL --------
        for (int y = 0; y < size; y++) {
            int count = 1;
            for (int x = 1; x < size; x++) {
                if (grid[y][x] != null && grid[y][x - 1] != null &&
                        grid[y][x].getType() == grid[y][x - 1].getType()) {
                    count++;
                } else {
                    if (count >= 3) {
                        for (int i = 0; i < count; i++) {
                            result.add(new Point(x - 1 - i, y));
                        }
                    }
                    count = 1;
                }
            }
            if (count >= 3) {
                for (int i = 0; i < count; i++) {
                    result.add(new Point(size - 1 - i, y));
                }
            }
        }

        // -------- VERTICAL --------
        for (int x = 0; x < size; x++) {
            int count = 1;
            for (int y = 1; y < size; y++) {
                if (grid[y][x] != null && grid[y - 1][x] != null &&
                        grid[y][x].getType() == grid[y - 1][x].getType()) {
                    count++;
                } else {
                    if (count >= 3) {
                        for (int i = 0; i < count; i++) {
                            result.add(new Point(x, y - 1 - i));
                        }
                    }
                    count = 1;
                }
            }
            if (count >= 3) {
                for (int i = 0; i < count; i++) {
                    result.add(new Point(x, size - 1 - i));
                }
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
                    y = size; // restart column
                }
            }
        }
    }

    /**
     * Refills only the empty cells that appear AFTER gravity (mid-game drops).
     * Uses the same safe-placement logic so refilled candies don't create
     * instant matches either.
     */
    public void refill(java.awt.Image[] images) {
        int totalTypes = images.length;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
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
    }
}