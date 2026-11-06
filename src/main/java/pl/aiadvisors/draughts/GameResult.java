package pl.aiadvisors.draughts;

/** State of a game: still going, won (with the reason) or drawn (with the reason). */
public final class GameResult {
    public enum Reason {
        /** The game is not over. */
        NONE,
        /** The loser has no pieces left. */
        NO_PIECES,
        /** The loser has pieces but no legal move. */
        NO_MOVES,
        /** The same position occurred three times with the same side to move. */
        THREEFOLD_REPETITION,
        /** Only kings moved without a capture for {@link Rules#kingMovesDrawLimit()} moves each. */
        KING_MOVES_LIMIT
    }

    public static final GameResult ONGOING = new GameResult(null, Reason.NONE);

    private final Color winner;
    private final Reason reason;

    private GameResult(Color winner, Reason reason) {
        this.winner = winner;
        this.reason = reason;
    }

    static GameResult win(Color winner, Reason reason) {
        return new GameResult(winner, reason);
    }

    static GameResult draw(Reason reason) {
        return new GameResult(null, reason);
    }

    public boolean isOver() {
        return reason != Reason.NONE;
    }

    public boolean isDraw() {
        return isOver() && winner == null;
    }

    /** The winning side, or null for a draw or an unfinished game. */
    public Color winner() {
        return winner;
    }

    public Reason reason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GameResult && ((GameResult) o).winner == winner && ((GameResult) o).reason == reason;
    }

    @Override
    public int hashCode() {
        return reason.hashCode() * 3 + (winner == null ? 0 : winner.ordinal() + 1);
    }

    @Override
    public String toString() {
        if (!isOver()) return "ongoing";
        return (winner == null ? "draw" : winner.name().toLowerCase() + " wins") + " (" + reason.name().toLowerCase() + ")";
    }
}
