package pl.aiadvisors.draughts;

import java.util.Arrays;

/**
 * A legal move produced by {@link Rules#legalMoves(Position)}: the path of the moving piece and the
 * pieces it captures. Two moves are equal when they start and end on the same squares, capture the
 * same set of pieces and agree on promotion; such moves lead to the same position.
 */
public final class Move {
    private final int[] path;
    private final int[] captured;
    private final boolean promotes;

    Move(int[] path, int[] captured, boolean promotes) {
        this.path = path;
        this.captured = captured;
        this.promotes = promotes;
    }

    public int from() {
        return path[0];
    }

    public int to() {
        return path[path.length - 1];
    }

    /** Every square the piece stands on: start, each landing square of a capture, end. */
    public int[] path() {
        return path.clone();
    }

    /** Squares of the captured pieces, in the order they were jumped. */
    public int[] captured() {
        return captured.clone();
    }

    public boolean isCapture() {
        return captured.length > 0;
    }

    public int captureCount() {
        return captured.length;
    }

    /** True when the moving man becomes a king. */
    public boolean promotes() {
        return promotes;
    }

    /** Algebraic notation: "c3-d4" for a step, "c3:e5:g7" for a capture (every landing square). */
    @Override
    public String toString() {
        return join(false);
    }

    /** Numeric notation (1 = a1 ... 32 = h8): "10-14" for a step, "10x19x28" for a capture. */
    public String toNumeric() {
        return join(true);
    }

    private String join(boolean numeric) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < path.length; i++) {
            if (i > 0) s.append(isCapture() ? (numeric ? 'x' : ':') : '-');
            s.append(numeric ? String.valueOf(Square.number(path[i])) : Square.name(path[i]));
        }
        return s.toString();
    }

    private int[] sortedCaptures() {
        int[] c = captured.clone();
        Arrays.sort(c);
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return from() == m.from() && to() == m.to() && promotes == m.promotes
                && Arrays.equals(sortedCaptures(), m.sortedCaptures());
    }

    @Override
    public int hashCode() {
        return ((from() * 64 + to()) * 31 + Arrays.hashCode(sortedCaptures())) * 2 + (promotes ? 1 : 0);
    }
}
