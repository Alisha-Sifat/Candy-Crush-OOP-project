import java.util.ArrayList;

/**
 * All six levels — each with a completely different objective.
 *
 * Color index: 0=Red | 1=Blue | 2=Green | 3=Yellow | 4=Orange | 5=Purple
 */
public class LevelManager {

    public static ArrayList<Level> getLevels() {
        ArrayList<Level> levels = new ArrayList<>();

        // Level 1 — Collect 15 Blue candies in 25 moves
        levels.add(new Level(1, LevelGoal.COLLECT_CANDY,
                /*target=*/ 15, /*color=*/ 1, /*moves=*/ 25));

        // Level 2 — Make 5 Striped candies in 28 moves
        levels.add(new Level(2, LevelGoal.MAKE_STRIPED,
                /*target=*/ 5, /*moves=*/ 28));

        // Level 3 — Reach score of 600 in 16 moves
        levels.add(new Level(3, LevelGoal.REACH_SCORE,
                /*target=*/ 600, /*moves=*/ 16));

        // Level 4 — Collect 20 Red candies in 20 moves
        levels.add(new Level(4, LevelGoal.COLLECT_CANDY,
                /*target=*/ 20, /*color=*/ 0, /*moves=*/ 20));

        // Level 5 — Burst 3 Bomb candies in 22 moves
        levels.add(new Level(5, LevelGoal.BURST_BOMBS,
                /*target=*/ 3, /*moves=*/ 22));

        // Level 6 — Reach score of 1500 in 12 moves (hardest!)
        levels.add(new Level(6, LevelGoal.REACH_SCORE,
                /*target=*/ 1500, /*moves=*/ 12));

        return levels;
    }
}
