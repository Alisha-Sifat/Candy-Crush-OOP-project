public class Level {

    private int levelNumber;
    private int targetScore;
    private int maxMoves;

    public Level(int levelNumber, int targetScore, int maxMoves) {
        this.levelNumber = levelNumber;
        this.targetScore = targetScore;
        this.maxMoves = maxMoves;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public int getMaxMoves() {
        return maxMoves;
    }
}