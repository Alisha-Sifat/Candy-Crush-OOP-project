/**
 * Tracks the player's live progress toward the current level's goal.
 *
 * How to use from Game.java / Board:
 *   • When a candy is cleared        → progress.notifyCandyCleared(candyType)
 *   • When a striped candy is made   → progress.notifyStripedCreated()
 *   • After every score change       → progress.notifyScoreUpdated(totalScore)
 *   • When a bomb candy explodes     → progress.notifyBombBurst()
 *   Then call isGoalComplete() to check for a win.
 */
public class LevelProgress {

    private final Level level;

    private int candiesCollected = 0;
    private int stripedMade      = 0;
    private int currentScore     = 0;
    private int bombsBurst       = 0;

    public LevelProgress(Level level) {
        this.level = level;
    }

    // ── Notification methods (call from Game.java) ───────────────────────

    /** Call for every candy cleared from the board (normal or special). */
    public void notifyCandyCleared(int candyType) {
        if (level.getGoalType() == LevelGoal.COLLECT_CANDY
                && candyType == level.getCandyColorTarget()) {
            candiesCollected++;
        }
    }

    /** Call when a 4-in-a-row creates a StripedCandy on the board. */
    public void notifyStripedCreated() {
        if (level.getGoalType() == LevelGoal.MAKE_STRIPED) {
            stripedMade++;
        }
    }

    /** Call after any score update with the new total score. */
    public void notifyScoreUpdated(int totalScore) {
        this.currentScore = totalScore;
    }

    /** Call when a BombCandy is burst (normal bomb swap or bomb+stripe combo). */
    public void notifyBombBurst() {
        if (level.getGoalType() == LevelGoal.BURST_BOMBS) {
            bombsBurst++;
        }
    }

    // ── Progress query ───────────────────────────────────────────────────

    /** Current progress count (out of getTarget()). */
    public int getProgress() {
        switch (level.getGoalType()) {
            case COLLECT_CANDY: return candiesCollected;
            case MAKE_STRIPED:  return stripedMade;
            case REACH_SCORE:   return currentScore;
            case BURST_BOMBS:   return bombsBurst;
            default:            return 0;
        }
    }

    /** The number the player needs to reach. */
    public int getTarget() {
        return level.getTargetAmount();
    }

    /** Returns true when the player has fulfilled the level objective. */
    public boolean isGoalComplete() {
        return getProgress() >= getTarget();
    }

    /**
     * Short progress string for TopPanel, e.g. "Blue: 8/15" or "Bombs: 2/3".
     */
    public String getProgressText() {
        switch (level.getGoalType()) {
            case COLLECT_CANDY:
                String color = colorName(level.getCandyColorTarget());
                return color + ": " + Math.min(candiesCollected, getTarget())
                       + "/" + getTarget();
            case MAKE_STRIPED:
                return "Striped: " + Math.min(stripedMade, getTarget())
                       + "/" + getTarget();
            case REACH_SCORE:
                return "Score: " + currentScore + "/" + getTarget();
            case BURST_BOMBS:
                return "Bombs: " + Math.min(bombsBurst, getTarget())
                       + "/" + getTarget();
            default:
                return "";
        }
    }

    private String colorName(int index) {
        String[] names = { "Red", "Blue", "Green", "Yellow", "Orange", "Purple" };
        return (index >= 0 && index < names.length) ? names[index] : "Candy";
    }
}
