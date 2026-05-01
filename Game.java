
import java.io.File;
import com.candycrush.Candy;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.ArrayList;

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
                handleClick(e);
                panel.repaint();
            }
        });

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initBoard() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int type = (int) (Math.random() * 6);
                board.set(x, y, new NormalCandy(type, candyImages[type]));
            }
        }
    }

    private void render(Graphics g) {
        g.drawImage(BG, 0, 0, PIXEL_SIZE, PIXEL_SIZE, null);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Candy c = board.get(x, y);
                if (c != null) {
                    g.drawImage(c.getImage(), x * SQUARE_SIZE, y * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE, null);
                }
            }
        }

        if (selected != null) {
            g.drawImage(SELECTOR, selected.x * SQUARE_SIZE, selected.y * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE, null);
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
                   while (!matches.isEmpty()) {
    board.removeMatches(matches);
    board.applyGravity();
    board.refill(candyImages);   // 🔥 ADD THIS

    matches = board.findMatches();

    score += matches.size();
    frame.setTitle("Score: " + score);
}
                }
            }
            selected = null;
        }
    }

    private boolean isNeighbor(Point a, Point b) {
        return (Math.abs(a.x - b.x) == 1 && a.y == b.y) ||
               (Math.abs(a.y - b.y) == 1 && a.x == b.x);
    }
}