import com.candycrush.Candy;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Game {
    final int SIZE = 8;
    final int PIXEL_SIZE = 600;
    final int SQUARE_SIZE = PIXEL_SIZE / SIZE;

    Board board = new Board(SIZE);
    Image[] candyImages = new Image[6];
    Image BG, SELECTOR;
    Point selected = null;
    int score = 0;
    JFrame frame = new JFrame("Score: 0");
    JPanel panel;

    // --- Animation state ---
    // Maps each (x, y) grid cell to its current visual Y offset in pixels
    // e.g. offset of -75 means the candy is 75px above its resting position
    private final Map<String, Float> candyOffsets = new HashMap<>();
    private boolean isAnimating = false;
    private javax.swing.Timer animTimer;
    final float FALL_SPEED = 18f; // pixels per frame — tune this for speed

    public void run() throws IOException {
        String basePath = System.getProperty("user.dir") + "/src/images/";
        BG = ImageIO.read(new File(basePath + "b.png"));
        SELECTOR = ImageIO.read(new File(basePath + "s.png"));
        for (int i = 0; i < 6; i++) {
            candyImages[i] = ImageIO.read(new File(basePath + (i + 1) + ".png"));
        }

        initBoard();

        panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                render(g);
            }
        };
        panel.setPreferredSize(new Dimension(PIXEL_SIZE, PIXEL_SIZE));
        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!isAnimating) {       // block input while falling
                    handleClick(e);
                    panel.repaint();
                }
            }
        });

        // ~60 fps animation timer — always running, only moves things when needed
        animTimer = new javax.swing.Timer(16, e -> {
            if (isAnimating) {
                stepAnimation();
                panel.repaint();
            }
        });
        animTimer.start();

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initBoard() {
        board.initBoard(candyImages);
    }

    // Called after gravity+refill to set up the starting offsets for every
    // candy that needs to fall. Candies that are already in place get offset 0.
    private void startFallAnimation(boolean[][] wasEmpty) {
    candyOffsets.clear();
    boolean anyFalling = false;

    for (int x = 0; x < SIZE; x++) {
        // Count how many empty cells were in this column
        int emptyCount = 0;
        for (int y = 0; y < SIZE; y++) {
            if (wasEmpty[y][x]) emptyCount++;
        }
        if (emptyCount == 0) continue;

        // Only the top (emptyCount) rows of this column shifted down
        for (int y = 0; y < SIZE; y++) {
            Candy c = board.get(x, y);
            if (c == null) continue;

            // How many empty slots are below this candy in the column?
            int emptiesBelow = 0;
            for (int yy = y + 1; yy < SIZE; yy++) {
                if (wasEmpty[yy][x]) emptiesBelow++;
            }

            if (emptiesBelow > 0) {
                // This candy fell down by (emptiesBelow) cells
                String key = x + "," + y;
                candyOffsets.put(key, (float) -(emptiesBelow * SQUARE_SIZE));
                anyFalling = true;
            }
        }
    }
    isAnimating = anyFalling;
}

    // Moves every offset one step closer to 0 each frame
    private void stepAnimation() {
        boolean stillMoving = false;
        for (String key : new ArrayList<>(candyOffsets.keySet())) {
            float offset = candyOffsets.get(key);
            if (offset < 0) {
                offset = Math.min(0f, offset + FALL_SPEED);
                candyOffsets.put(key, offset);
                if (offset < 0) stillMoving = true;
            }
        }

        if (!stillMoving) {
            // All candies have landed — check for cascade matches
            isAnimating = false;
            candyOffsets.clear();
            processCascade();
        }
    }

    // Handles one round of match → remove → gravity → refill, then
    // triggers another animation if new matches appeared.
   private void processCascade() {
    ArrayList<Point> matches = board.findMatches();
    if (!matches.isEmpty()) {
        score += matches.size();
        frame.setTitle("Score: " + score);
        board.removeMatches(matches);
        board.applyGravity();

        // Snapshot which cells are empty BEFORE refill
        boolean[][] wasEmpty = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                wasEmpty[y][x] = (board.get(x, y) == null);

        board.refill(candyImages);
        startFallAnimation(wasEmpty);
    }
}

private void handleClick(MouseEvent e) {
    int x = e.getX() / SQUARE_SIZE;
    int y = e.getY() / SQUARE_SIZE;

    if (selected == null) {
        selected = new Point(x, y);
    } else {
        Point second = new Point(x, y);
        if (isNeighbor(selected, second)) {
            board.swap(selected, second);
            ArrayList<Point> matches = board.findMatches();
            if (matches.isEmpty()) {
                board.swap(selected, second);
            } else {
                score += matches.size();
                frame.setTitle("Score: " + score);
                board.removeMatches(matches);
                board.applyGravity();

                // Snapshot empty cells BEFORE refill
                boolean[][] wasEmpty = new boolean[SIZE][SIZE];
                for (int y2 = 0; y2 < SIZE; y2++)
                    for (int x2 = 0; x2 < SIZE; x2++)
                        wasEmpty[y2][x2] = (board.get(x2, y2) == null);

                board.refill(candyImages);
                startFallAnimation(wasEmpty);
            }
        }
        selected = null;
    }
}
private void render(Graphics g) {
    g.drawImage(BG, 0, 0, PIXEL_SIZE, PIXEL_SIZE, null);

    for (int y = 0; y < SIZE; y++) {
        for (int x = 0; x < SIZE; x++) {
            Candy c = board.get(x, y);
            if (c == null) continue;

            int drawX = x * SQUARE_SIZE;
            int drawY = y * SQUARE_SIZE;

            String key = x + "," + y;
            if (candyOffsets.containsKey(key)) {
                drawY += (int) candyOffsets.get(key).floatValue();
            }

            g.drawImage(c.getImage(), drawX, drawY, SQUARE_SIZE, SQUARE_SIZE, null);
        }
    }

    if (selected != null) {
        g.drawImage(SELECTOR,
            selected.x * SQUARE_SIZE,
            selected.y * SQUARE_SIZE,
            SQUARE_SIZE, SQUARE_SIZE, null);
    }
}

    private boolean isNeighbor(Point a, Point b) {
        return (Math.abs(a.x - b.x) == 1 && a.y == b.y) ||
               (Math.abs(a.y - b.y) == 1 && a.x == b.x);
    }
}