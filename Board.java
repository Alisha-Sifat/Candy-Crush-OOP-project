import com.candycrush.Candy;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

public class Board {
    private Candy[][] grid;
    private int size;
    private Random random = new Random();

    // ── Match group ──────────────────────────────────────────────────────
    /**
     * One contiguous run of same-colored candies in a straight line.
     * horizontal=true  → the run goes left-to-right
     * horizontal=false → the run goes top-to-bottom
     */
    public static class MatchGroup {
        public final ArrayList<Point> points;
        public final boolean horizontal;

        public MatchGroup(ArrayList<Point> points, boolean horizontal) {
            this.points = points;
            this.horizontal = horizontal;
        }

        public int size() { return points.size(); }
    }

    public Board(int size) {
        this.size = size;
        grid = new Candy[size][size];
    }

    public Candy get(int x, int y) { return grid[y][x]; }
    public void set(int x, int y, Candy c) { grid[y][x] = c; }
    public int getSize() { return size; }

    public void swap(Point a, Point b) {
        Candy temp = grid[a.y][a.x];
        grid[a.y][a.x] = grid[b.y][b.x];
        grid[b.y][b.x] = temp;
    }

    // ── Match detection ──────────────────────────────────────────────────

    /**
     * Returns every run of 3+ same-typed candies as a MatchGroup.
     * Each group carries its direction (horizontal / vertical).
     * StripedCandy and BombCandy participate via their getType() value.
     */
    public ArrayList<MatchGroup> findMatchGroups() {
        ArrayList<MatchGroup> groups = new ArrayList<>();

        // Horizontal
        for (int y = 0; y < size; y++) {
            int x = 0;
            while (x < size) {
                if (grid[y][x] == null) { x++; continue; }
                int type = grid[y][x].getType();
                int start = x;
                while (x < size && grid[y][x] != null && grid[y][x].getType() == type) x++;
                if (x - start >= 3) {
                    ArrayList<Point> pts = new ArrayList<>();
                    for (int i = start; i < x; i++) pts.add(new Point(i, y));
                    groups.add(new MatchGroup(pts, true));
                }
            }
        }

        // Vertical
        for (int x = 0; x < size; x++) {
            int y = 0;
            while (y < size) {
                if (grid[y][x] == null) { y++; continue; }
                int type = grid[y][x].getType();
                int start = y;
                while (y < size && grid[y][x] != null && grid[y][x].getType() == type) y++;
                if (y - start >= 3) {
                    ArrayList<Point> pts = new ArrayList<>();
                    for (int i = start; i < y; i++) pts.add(new Point(x, i));
                    groups.add(new MatchGroup(pts, false));
                }
            }
        }

        return groups;
    }

    /** Flat list version — used where group info isn't needed. */
    public ArrayList<Point> findMatches() {
        ArrayList<Point> result = new ArrayList<>();
        for (MatchGroup g : findMatchGroups()) result.addAll(g.points);
        return result;
    }

    // ── Special candy creation ───────────────────────────────────────────

    /**
     * After a swap, inspects match groups and places special candies.
     *
     * Bomb rule:     a SINGLE group of 5+ in a straight line → BombCandy
     * Striped rule:  a SINGLE group of exactly 4 in a line  → StripedCandy
     *   horizontal match → candy strips its COLUMN  (isHorizontal = false)
     *   vertical match   → candy strips its ROW     (isHorizontal = true)
     *
     * The anchor cell (where the special is placed) is whichever of the two
     * swapped cells belongs to the group. If neither does, we use index 0.
     *
     * @return points where a special was placed (exclude from normal removal)
     */
    public ArrayList<Point> createSpecials(
            ArrayList<MatchGroup> groups,
            Point swapA, Point swapB,
            java.awt.Image[] candyImages,
            java.awt.Image bombImg) {

        ArrayList<Point> specialPoints = new ArrayList<>();

        for (MatchGroup g : groups) {
            if (g.size() < 4) continue;

            // Find the anchor: prefer whichever swapped cell is in this group
            Point anchor = null;
            for (Point p : g.points) {
                if ((p.x == swapA.x && p.y == swapA.y) ||
                    (p.x == swapB.x && p.y == swapB.y)) {
                    anchor = p;
                    break;
                }
            }
            if (anchor == null) anchor = g.points.get(0);

            int type = grid[anchor.y][anchor.x] != null
                       ? grid[anchor.y][anchor.x].getType()
                       : 0;

            if (g.size() >= 5) {
                // 5 in a straight line → bomb
                grid[anchor.y][anchor.x] = new BombCandy(bombImg);

            } else {
                // Exactly 4 → striped, perpendicular to the match direction
                // g.horizontal=true  (row match)    → clears COLUMN → isHorizontal=false
                // g.horizontal=false (column match)  → clears ROW   → isHorizontal=true
                boolean stripeHoriz = !g.horizontal;
                grid[anchor.y][anchor.x] = new StripedCandy(
                        type, candyImages[type], stripeHoriz);
            }

            specialPoints.add(new Point(anchor.x, anchor.y));
        }

        return specialPoints;
    }

    // ── Match removal ────────────────────────────────────────────────────

    /**
     * Nulls out every point in the list. If a point holds a StripedCandy,
     * fires its row/column clear first.
     *
     * @return extra positions cleared by striped effects (for scoring)
     */
    public ArrayList<Point> removeMatches(ArrayList<Point> matches) {
        ArrayList<Point> extras = new ArrayList<>();
        for (Point p : matches) {
            if (grid[p.y][p.x] == null) continue;
            Candy c = grid[p.y][p.x];
            if (c instanceof StripedCandy) {
                extras.addAll(clearRowOrColumn(p, (StripedCandy) c));
            }
            grid[p.y][p.x] = null;
        }
        return extras;
    }

    // ── Row / column clear ───────────────────────────────────────────────

    /**
     * Clears the row (isHorizontal=true) or column (isHorizontal=false)
     * of the given striped candy. Returns cleared positions for animation.
     */
    public ArrayList<Point> clearRowOrColumn(Point p, StripedCandy sc) {
        ArrayList<Point> cleared = new ArrayList<>();
        if (sc.isHorizontal()) {
            for (int x = 0; x < size; x++) {
                if (x != p.x && grid[p.y][x] != null) {
                    cleared.add(new Point(x, p.y));
                    grid[p.y][x] = null;
                }
            }
        } else {
            for (int y = 0; y < size; y++) {
                if (y != p.y && grid[y][p.x] != null) {
                    cleared.add(new Point(p.x, y));
                    grid[y][p.x] = null;
                }
            }
        }
        return cleared;
    }

    // ── Bomb color clear ─────────────────────────────────────────────────

    /**
     * Bomb + Striped combo: converts every normal candy of targetType into a
     * StripedCandy (random direction), fires each one's row/column clear,
     * and returns swipe data for Game.java to animate.
     *
     * Format of each float[]: { gridCol, gridRow, horizClear(1/0) }
     * The board cells are nulled by clearRowOrColumn — the converted candy
     * cell itself is also nulled after firing.
     *
     * @param targetType  color index of the striped candy that was swapped
     * @param images      candy images array (to build the StripedCandy)
     * @return list of swipe descriptors for every candy that was converted
     */
    public ArrayList<float[]> convertColorToStriped(int targetType, java.awt.Image[] images) {
        ArrayList<float[]> swipes = new ArrayList<>();

        // First pass: find all matching normal candies and convert them
        ArrayList<Point> toFire = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[y][x] != null &&
                    grid[y][x].getType() == targetType &&
                    !(grid[y][x] instanceof BombCandy) &&
                    !(grid[y][x] instanceof StripedCandy)) {
                    // Random direction for each converted candy
                    boolean horiz = random.nextBoolean();
                    grid[y][x] = new StripedCandy(targetType, images[targetType], horiz);
                    toFire.add(new Point(x, y));
                }
            }
        }

        // Second pass: fire each converted candy's row/column clear
        // (do this after ALL conversions so clears don't wipe unconverted ones)
        for (Point p : toFire) {
            if (grid[p.y][p.x] == null) continue; // already cleared by another beam
            StripedCandy sc = (StripedCandy) grid[p.y][p.x];
            boolean horiz = sc.isHorizontal();
            swipes.add(new float[]{ p.x, p.y, horiz ? 1f : 0f });
            clearRowOrColumn(p, sc);
            grid[p.y][p.x] = null; // remove the converted candy itself
        }

        return swipes;
    }

    /**
     * Returns positions of all normal candies (not Bomb/Striped) of targetType.
     * Used by Game.java to know where to shoot lightning bolts before conversion.
     */
    public ArrayList<Point> findCandiesOfColor(int targetType) {
        ArrayList<Point> found = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[y][x] != null &&
                    grid[y][x].getType() == targetType &&
                    !(grid[y][x] instanceof BombCandy) &&
                    !(grid[y][x] instanceof StripedCandy)) {
                    found.add(new Point(x, y));
                }
            }
        }
        return found;
    }

    /**
     * Converts the candies at the given positions into StripedCandies (random direction).
     * Called after lightning finishes in a bomb+stripe combo, so the player sees
     * the board update before the swipe beams fire.
     * Skips cells that are already null (hit by something else first).
     */
    public void convertCandiesAtPoints(
            ArrayList<Point> points, int color, java.awt.Image[] images) {
        for (Point p : points) {
            if (grid[p.y][p.x] == null) continue;
            boolean horiz = random.nextBoolean();
            grid[p.y][p.x] = new StripedCandy(color, images[color], horiz);
        }
    }

    /**
     * Destroys all normal candies of targetType.
     * Returns their positions so Game.java can render lightning.
     */
    public ArrayList<Point> clearColor(int targetType) {
        ArrayList<Point> destroyed = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[y][x] != null &&
                    grid[y][x].getType() == targetType &&
                    !(grid[y][x] instanceof BombCandy) &&
                    !(grid[y][x] instanceof StripedCandy)) {
                    grid[y][x] = null;
                    destroyed.add(new Point(x, y));
                }
            }
        }
        return destroyed;
    }

    // ── Gravity & refill ─────────────────────────────────────────────────

    public void applyGravity() {
        for (int x = 0; x < size; x++) {
            boolean moved = true;
            while (moved) {
                moved = false;
                for (int y = size - 1; y > 0; y--) {
                    if (grid[y][x] == null && grid[y-1][x] != null) {
                        grid[y][x] = grid[y-1][x];
                        grid[y-1][x] = null;
                        moved = true;
                    }
                }
            }
        }
    }

    public void refill(java.awt.Image[] images) {
        int totalTypes = images.length;
        for (int x = 0; x < size; x++) {
            for (int y = size - 1; y >= 0; y--) {
                if (grid[y][x] == null) {
                    ArrayList<Integer> safe = new ArrayList<>();
                    for (int t = 0; t < totalTypes; t++) {
                        if (!wouldCreateMatch(x, y, t)) safe.add(t);
                    }
                    int type = safe.isEmpty()
                            ? random.nextInt(totalTypes)
                            : safe.get(random.nextInt(safe.size()));
                    grid[y][x] = new NormalCandy(type, images[type]);
                }
            }
        }
        resolveAllMatches(images);
    }

    // ── Board init ───────────────────────────────────────────────────────

    public void initBoard(java.awt.Image[] images) {
        int totalTypes = images.length;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                ArrayList<Integer> safe = new ArrayList<>();
                for (int t = 0; t < totalTypes; t++) {
                    if (!wouldCreateMatch(x, y, t)) safe.add(t);
                }
                int type = safe.isEmpty()
                        ? random.nextInt(totalTypes)
                        : safe.get(random.nextInt(safe.size()));
                grid[y][x] = new NormalCandy(type, images[type]);
            }
        }
        resolveAllMatches(images);
    }

    public void resolveAllMatches(java.awt.Image[] images) {
        int totalTypes = images.length;
        ArrayList<Point> matches = findMatches();
        int safety = 0;
        while (!matches.isEmpty() && safety < 100) {
            for (Point p : matches) {
                ArrayList<Integer> safe = new ArrayList<>();
                for (int t = 0; t < totalTypes; t++) {
                    Candy orig = grid[p.y][p.x];
                    grid[p.y][p.x] = null;
                    if (!wouldCreateMatch(p.x, p.y, t)) safe.add(t);
                    grid[p.y][p.x] = orig;
                }
                int type = safe.isEmpty()
                        ? random.nextInt(totalTypes)
                        : safe.get(random.nextInt(safe.size()));
                grid[p.y][p.x] = new NormalCandy(type, images[type]);
            }
            matches = findMatches();
            safety++;
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    private boolean wouldCreateMatch(int x, int y, int type) {
        int left = 0;
        while (x-left-1 >= 0 && grid[y][x-left-1] != null
               && grid[y][x-left-1].getType() == type) left++;
        int right = 0;
        while (x+right+1 < size && grid[y][x+right+1] != null
               && grid[y][x+right+1].getType() == type) right++;
        if (left + right >= 2) return true;

        int up = 0;
        while (y-up-1 >= 0 && grid[y-up-1][x] != null
               && grid[y-up-1][x].getType() == type) up++;
        int down = 0;
        while (y+down+1 < size && grid[y+down+1][x] != null
               && grid[y+down+1][x].getType() == type) down++;
        return up + down >= 2;
    }
}