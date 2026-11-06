package pl.aiadvisors.draughts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Rule sets for 8 × 8 draughts. Shared by both variants: White starts, play on the dark squares
 * (a1 is dark), men step one square forward and capture forwards and backwards, capturing is
 * compulsory and a capture sequence must be completed, kings fly along diagonals and may land on
 * any free square behind the captured piece, and captured pieces are removed only after the whole
 * sequence (Turkish strike: a piece cannot be jumped twice and still blocks the path).
 *
 * <ul>
 *   <li>{@link #BRAZILIAN} (also {@link #POLISH}): the sequence capturing the most pieces is
 *       compulsory (kings count like men); a man that only passes the last row during a capture
 *       stays a man.</li>
 *   <li>{@link #RUSSIAN}: any capture sequence may be chosen; a man reaching the last row during a
 *       capture becomes a king at once and continues capturing as a king.</li>
 * </ul>
 *
 * <p>Draw rules here are a simplification of the federations' rules: threefold repetition, or
 * {@link #kingMovesDrawLimit()} moves by each side with only kings moving and nothing captured.
 */
public enum Rules {
    /** Brazilian draughts = Polish draughts on 8 × 8: international rules on the small board. */
    BRAZILIAN(true, false, 25),
    /** Russian draughts (shashki). */
    RUSSIAN(false, true, 15);

    /** Polish 8 × 8 draughts ("warcaby") use exactly the Brazilian rules. */
    public static final Rules POLISH = BRAZILIAN;

    private static final int[][] DIRS = {{1, -1}, {1, 1}, {-1, -1}, {-1, 1}};
    private static final int[] NONE = new int[0];

    private final boolean maximumCapture;
    private final boolean promoteDuringCapture;
    private final int kingMovesDrawLimit;

    Rules(boolean maximumCapture, boolean promoteDuringCapture, int kingMovesDrawLimit) {
        this.maximumCapture = maximumCapture;
        this.promoteDuringCapture = promoteDuringCapture;
        this.kingMovesDrawLimit = kingMovesDrawLimit;
    }

    /** True when only the sequences capturing the most pieces are legal. */
    public boolean maximumCapture() {
        return maximumCapture;
    }

    /** True when a man reaching the last row in the middle of a capture is crowned at once. */
    public boolean promoteDuringCapture() {
        return promoteDuringCapture;
    }

    /** Moves per side with only kings moving and no capture after which the game is drawn. */
    public int kingMovesDrawLimit() {
        return kingMovesDrawLimit;
    }

    // ---- move generation ----

    /** Every legal move for the side to move. If any capture exists, only captures are returned. */
    public List<Move> legalMoves(Position p) {
        List<Move> caps = captures(p.board.clone(), p.side.sign());
        return caps.isEmpty() ? steps(p.board, p.side.sign()) : caps;
    }

    /** True when the side to move has no legal move (and so has lost). */
    public boolean hasNoMoves(Position p) {
        return legalMoves(p).isEmpty();
    }

    public boolean isLegal(Position p, Move m) {
        return legalMoves(p).contains(m);
    }

    private static List<Move> steps(int[] b, int color) {
        List<Move> out = new ArrayList<>();
        for (int sq = 0; sq < 64; sq++) {
            int v = b[sq];
            if (v * color <= 0) continue;
            boolean man = Math.abs(v) == 1;
            int promotionRow = color > 0 ? 7 : 0;
            for (int[] d : DIRS) {
                if (man && d[0] != color) continue;
                for (int to = Square.step(sq, d[0], d[1]); to >= 0 && b[to] == 0; to = Square.step(to, d[0], d[1])) {
                    out.add(new Move(new int[]{sq, to}, NONE, man && Square.row(to) == promotionRow));
                    if (man) break;
                }
            }
        }
        return out;
    }

    /** Capture sequences for {@code color}; the board is modified temporarily and restored. */
    private List<Move> captures(int[] b, int color) {
        List<Move> all = new ArrayList<>();
        int[] path = new int[16], caps = new int[12];
        for (int sq = 0; sq < 64; sq++) {
            int v = b[sq];
            if (v * color <= 0) continue;
            b[sq] = 0;
            path[0] = sq;
            boolean king = Math.abs(v) == 2;
            jump(b, sq, king, !king, false, color, path, 1, caps, 0, all);
            b[sq] = v;
        }
        if (all.isEmpty()) return all;
        int best = 0;
        for (Move m : all) best = Math.max(best, m.captureCount());
        LinkedHashSet<Move> unique = new LinkedHashSet<>();
        for (Move m : all) if (!maximumCapture || m.captureCount() == best) unique.add(m);
        return new ArrayList<>(unique);
    }

    private static boolean taken(int[] caps, int n, int sq) {
        for (int i = 0; i < n; i++) if (caps[i] == sq) return true;
        return false;
    }

    /** Square of an enemy piece this piece can capture in direction d, or -1. */
    private static int victim(int[] b, int at, boolean king, int color, int[] d, int[] caps, int n) {
        int over = Square.step(at, d[0], d[1]);
        if (king) while (over >= 0 && b[over] == 0) over = Square.step(over, d[0], d[1]);
        if (over < 0 || b[over] * color >= 0 || taken(caps, n, over)) return -1;
        int land = Square.step(over, d[0], d[1]);
        return land >= 0 && b[land] == 0 ? over : -1;
    }

    private static boolean canCapture(int[] b, int at, boolean king, int color, int[] caps, int n) {
        for (int[] d : DIRS) if (victim(b, at, king, color, d, caps, n) >= 0) return true;
        return false;
    }

    /**
     * Extends a capture sequence. A sequence ends only when no further capture is possible; when a
     * king has a choice of landing squares and some of them allow the capture to continue, it must
     * land on one of those.
     */
    private void jump(int[] b, int at, boolean king, boolean wasMan, boolean crowned, int color,
                      int[] path, int depth, int[] caps, int n, List<Move> out) {
        boolean extended = false;
        int promotionRow = color > 0 ? 7 : 0;
        int[] lands = new int[7];
        boolean[] continues = new boolean[7];
        for (int[] d : DIRS) {
            int over = victim(b, at, king, color, d, caps, n);
            if (over < 0) continue;
            caps[n] = over;
            int count = 0;
            boolean anyContinues = false;
            for (int land = Square.step(over, d[0], d[1]); land >= 0 && b[land] == 0; land = Square.step(land, d[0], d[1])) {
                boolean k = king || (promoteDuringCapture && Square.row(land) == promotionRow);
                lands[count] = land;
                continues[count] = canCapture(b, land, k, color, caps, n + 1);
                anyContinues |= continues[count];
                count++;
                if (!king) break;
            }
            for (int i = 0; i < count; i++) {
                if (anyContinues && !continues[i]) continue;
                int land = lands[i];
                boolean nowKing = king || (promoteDuringCapture && Square.row(land) == promotionRow);
                path[depth] = land;
                caps[n] = over;
                extended = true;
                jump(b, land, nowKing, wasMan, crowned || (nowKing && !king), color, path, depth + 1, caps, n + 1, out);
            }
        }
        if (!extended && n > 0) {
            boolean promotes = wasMan && (crowned || Square.row(at) == promotionRow);
            out.add(new Move(Arrays.copyOf(path, depth), Arrays.copyOf(caps, n), promotes));
        }
    }

    // ---- reading moves ----

    /**
     * Finds the legal move written as text. Accepts algebraic ({@code c3-d4}, {@code c3:e5:g7},
     * {@code c3xe5}) and numeric ({@code 10-14}, {@code 10x19x28}) notation. A capture may be given
     * by its start and end square only ({@code c3:g7}) when that is unambiguous.
     *
     * @throws IllegalArgumentException when the text is not a legal move or is ambiguous
     */
    public Move parseMove(Position p, String text) {
        String[] tokens = text.trim().split("[-:xX]");
        int[] squares = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            squares[i] = Square.tryParse(tokens[i]);
            if (squares[i] < 0) throw new IllegalArgumentException("not a move: " + text);
        }
        if (squares.length < 2) throw new IllegalArgumentException("not a move: " + text);
        List<Move> legal = legalMoves(p);
        for (Move m : legal) if (Arrays.equals(m.path(), squares)) return m;
        Move found = null;
        for (Move m : legal) {
            if (m.from() != squares[0] || m.to() != squares[squares.length - 1] || !contains(m.path(), squares)) continue;
            if (found != null && !found.equals(m)) throw new IllegalArgumentException("ambiguous move " + text + ": " + found + " or " + m);
            found = m;
        }
        if (found == null) throw new IllegalArgumentException("illegal move " + text + " in " + p.toFen());
        return found;
    }

    /** True when {@code part} is a subsequence of {@code path}. */
    private static boolean contains(int[] path, int[] part) {
        int j = 0;
        for (int i = 0; i < path.length && j < part.length; i++) if (path[i] == part[j]) j++;
        return j == part.length;
    }
}
