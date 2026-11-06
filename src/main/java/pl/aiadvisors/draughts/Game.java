package pl.aiadvisors.draughts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game in progress: the current position, the moves played so far and the result. Moves are
 * validated against the {@link Rules}. Not thread-safe.
 */
public final class Game {
    private final Rules rules;
    private final List<Position> positions = new ArrayList<>();
    private final List<Move> moves = new ArrayList<>();

    /** A Polish (Brazilian) game from the starting position. */
    public Game() {
        this(Rules.POLISH);
    }

    public Game(Rules rules) {
        this(rules, Position.start());
    }

    public Game(Rules rules, Position start) {
        if (rules == null || start == null) throw new NullPointerException();
        this.rules = rules;
        positions.add(start);
    }

    public Rules rules() {
        return rules;
    }

    public Position position() {
        return positions.get(positions.size() - 1);
    }

    public Color sideToMove() {
        return position().sideToMove();
    }

    public List<Move> legalMoves() {
        return rules.legalMoves(position());
    }

    /** Moves played so far. */
    public List<Move> moves() {
        return Collections.unmodifiableList(moves);
    }

    /** Positions from the start up to and including the current one. */
    public List<Position> positions() {
        return Collections.unmodifiableList(positions);
    }

    /** Plays a legal move. @throws IllegalArgumentException if it is not legal or the game is over */
    public Position play(Move move) {
        if (result().isOver()) throw new IllegalStateException("game is over: " + result());
        if (!rules.isLegal(position(), move)) throw new IllegalArgumentException("illegal move " + move);
        moves.add(move);
        positions.add(position().play(move));
        return position();
    }

    /** Plays a move written as text; see {@link Rules#parseMove}. Returns the move played. */
    public Move play(String move) {
        if (result().isOver()) throw new IllegalStateException("game is over: " + result());
        Move m = rules.parseMove(position(), move);
        play(m);
        return m;
    }

    /** Takes back the last move. Returns false at the start of the game. */
    public boolean undo() {
        if (moves.isEmpty()) return false;
        moves.remove(moves.size() - 1);
        positions.remove(positions.size() - 1);
        return true;
    }

    /** Win when the side to move has no move, otherwise a draw by the king-move limit or by threefold repetition. */
    public GameResult result() {
        Position p = position();
        if (rules.hasNoMoves(p)) {
            Color winner = p.sideToMove().opposite();
            return GameResult.win(winner, p.count(p.sideToMove()) == 0 ? GameResult.Reason.NO_PIECES : GameResult.Reason.NO_MOVES);
        }
        if (p.quietPlies() >= 2 * rules.kingMovesDrawLimit()) return GameResult.draw(GameResult.Reason.KING_MOVES_LIMIT);
        int same = 0;
        for (Position q : positions) if (q.equals(p)) same++;
        return same >= 3 ? GameResult.draw(GameResult.Reason.THREEFOLD_REPETITION) : GameResult.ONGOING;
    }
}
