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
    Image stripedImg, bombImg;

    Point selected = null;
    int score = 0;

    JFrame frame = new JFrame("Score: 0");
    JPanel panel;

    private final Map<String, Float> candyOffsets = new HashMap<>();
    private boolean isAnimating = false;
    private javax.swing.Timer animTimer;
    final float FALL_SPEED = 18f;

    public void run() throws IOException {

        String basePath = System.getProperty("user.dir") + "/src/images/";

        BG = ImageIO.read(new File(basePath + "b.png"));
        SELECTOR = ImageIO.read(new File(basePath + "s.png"));

        for (int i = 0; i < 6; i++) {
            candyImages[i] = ImageIO.read(new File(basePath + (i + 1) + ".png"));
        }

        stripedImg = ImageIO.read(new File(basePath + "striped.png"));
        bombImg = ImageIO.read(new File(basePath + "bomb.png"));

        initBoard();

        panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                render(g);
            }
        };

        panel.setPreferredSize(new Dimension(PIXEL_SIZE, PIXEL_SIZE));

        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!isAnimating) {
                    handleClick(e);
                    panel.repaint();
                }
            }
        });

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

                    Point specialPoint = null;

                    // ⭐ CREATE SPECIAL CANDIES
                    if (matches.size() == 4) {
                        Point p = matches.get(0);
                        int type = board.get(p.x, p.y).getType();

                        board.set(p.x, p.y,
                                new StripedCandy(type, stripedImg, true)
                        );

                        specialPoint = p;
                    }
                    else if (matches.size() >= 5) {
                        Point p = matches.get(0);

                        board.set(p.x, p.y,
                                new BombCandy(bombImg)
                        );

                        specialPoint = p;
                    }

                    // ⭐ REMOVE special candy from matches (NO LAMBDA)
                    if (specialPoint != null) {
                        for (int i = 0; i < matches.size(); i++) {
                            Point pt = matches.get(i);
                            if (pt.x == specialPoint.x && pt.y == specialPoint.y) {
                                matches.remove(i);
                                break;
                            }
                        }
                    }

                    // ⭐ NORMAL FLOW
                    score += matches.size();
                    frame.setTitle("Score: " + score);

                    board.removeMatches(matches);
                    board.applyGravity();

                    boolean[][] wasEmpty = new boolean[SIZE][SIZE];

                    for (int y2 = 0; y2 < SIZE; y2++) {
                        for (int x2 = 0; x2 < SIZE; x2++) {
                            wasEmpty[y2][x2] = (board.get(x2, y2) == null);
                        }
                    }

                    board.refill(candyImages);
                    startFallAnimation(wasEmpty);
                }
            }

            selected = null;
        }
    }

    private void startFallAnimation(boolean[][] wasEmpty) {

        candyOffsets.clear();
        boolean anyFalling = false;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {

                Candy c = board.get(x, y);
                if (c == null) continue;

                int emptiesBelow = 0;

                for (int yy = y + 1; yy < SIZE; yy++) {
                    if (wasEmpty[yy][x]) emptiesBelow++;
                }

                if (emptiesBelow > 0) {
                    String key = x + "," + y;
                    candyOffsets.put(key, (float) -(emptiesBelow * SQUARE_SIZE));
                    anyFalling = true;
                }
            }
        }

        isAnimating = anyFalling;
    }

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
            isAnimating = false;
            candyOffsets.clear();
            processCascade();
        }
    }

    private void processCascade() {

        ArrayList<Point> matches = board.findMatches();

        if (!matches.isEmpty()) {

            score += matches.size();
            frame.setTitle("Score: " + score);

            board.removeMatches(matches);
            board.applyGravity();

            boolean[][] wasEmpty = new boolean[SIZE][SIZE];

            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    wasEmpty[y][x] = (board.get(x, y) == null);
                }
            }

            board.refill(candyImages);
            startFallAnimation(wasEmpty);
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
                    drawY += candyOffsets.get(key).intValue(); // ✅ FIXED
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