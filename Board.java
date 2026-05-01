package com.candycrush;

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

    public ArrayList<Point> findMatches() {
        ArrayList<Point> result = new ArrayList<>();

        // horizontal
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
        }

        // vertical
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
        }

        return result;
    }

    public void removeMatches(ArrayList<Point> matches) {
        for (Point p : matches) {
            if (grid[p.y][p.x] != null) {
                grid[p.y][p.x].onMatch(); // polymorphism
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
}