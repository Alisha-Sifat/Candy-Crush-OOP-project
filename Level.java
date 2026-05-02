/**
 * Represents a single game level with a unique objective.
 *
 * Goal types:
 *   COLLECT_CANDY – collect N candies of a specific color (by color index)
 *   MAKE_STRIPED  – create N striped candies via 4-in-a-row matches
 *   REACH_SCORE   – reach a target score within the move limit
 *   BURST_BOMBS   – create and burst N bomb candies
 *
 * Color index convention (must match your candyImages[] array order):
 *   0=Red  1=Blue  2=Green  3=Yellow  4=Orange  5=Purple
 */
public class Level {

    private int levelNumber;
    private int maxMoves;
    private LevelGoal goalType;
    private int targetAmount;       // N for all goal types; score for REACH_SCORE
    private int candyColorTarget;   // color index; -1 if not used

    // ── Constructor for COLLECT_CANDY (color matters) ────────────────────
    public Level(int levelNumber, LevelGoal goalType,
                 int targetAmount, int candyColorTarget, int maxMoves) {
        this.levelNumber      = levelNumber;
        this.goalType         = goalType;
        this.targetAmount     = targetAmount;
        this.candyColorTarget = candyColorTarget;
        this.maxMoves         = maxMoves;
    }

    // ── Constructor for MAKE_STRIPED, REACH_SCORE, BURST_BOMBS ───────────
    public Level(int levelNumber, LevelGoal goalType,
                 int targetAmount, int maxMoves) {
        this(levelNumber, goalType, targetAmount, -1, maxMoves);
    }

    // ── Getters ──────────────────────────────────────────────────────────
    public int       getLevelNumber()      { return levelNumber; }
    public int       getMaxMoves()         { return maxMoves; }
    public LevelGoal getGoalType()         { return goalType; }
    public int       getTargetAmount()     { return targetAmount; }
    public int       getCandyColorTarget() { return candyColorTarget; }

    /** Legacy alias so old callers of getTargetScore() still compile. */
    public int       getTargetScore()      { return targetAmount; }

    // ── Human-readable goal string (shown in TopPanel) ───────────────────
    public String getGoalDescription() {
        switch (goalType) {
            case COLLECT_CANDY:
                return "Collect " + targetAmount + " " + colorName(candyColorTarget) + " candies";
            case MAKE_STRIPED:
                return "Make " + targetAmount + " striped candies";
            case REACH_SCORE:
                return "Score " + targetAmount + " pts";
            case BURST_BOMBS:
                return "Burst " + targetAmount + " bombs";
            default:
                return "Complete the level";
        }
    }

    private String colorName(int index) {
        String[] names = { "Red", "Blue", "Green", "Yellow", "Orange", "Purple" };
        return (index >= 0 && index < names.length) ? names[index] : "Candy";
    }
}
