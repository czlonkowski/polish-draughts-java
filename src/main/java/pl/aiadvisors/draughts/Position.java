package pl.aiadvisors.draughts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An immutable board position: pieces, side to move and the number of consecutive quiet plies
 * (king moves without a capture), which the draw rule needs.
 *
 * <p>Two text formats are read and written:
 * <ul>
 *   <li>FEN as used by pydraughts and lidraughts: {@code W:Wa1,Kc3:Bb6} (side, White pieces,
 *       Black pieces, K marks a king). Square numbers ({@code W:W1,2,K10:B21-24}) are accepted too.</li>
 *   <li>Short notation: {@code w:a1,Kc3:b6}.</li>
 * </ul>
 * A man standing on its promotion row is read as a king.
 *
 * <p>{@link #equals} compares pieces and side to move only, not the quiet-ply counter.
 */
public final class Position {
    /** The standard starting position in short notation. */
    public static final String START = "w:a1,c1,e1,g1,b2,d2,f2,h2,a3,c3,e3,g3:b6,d6,f6,h6,a7,c7,e7,g7,b8,d8,f8,h8";

    final int[] board;
    final Color side;
    final int quietPlies;

    Position(int[] board, Color side, int quietPlies) {
        this.board = board;
        this.side = side;
        this.quietPlies = quietPlies;
    }

    public static Position start() {
        return fromShort(START);
    }

    /** An empty board with the given pieces placed; see {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Reads either FEN ({@code W:Wa1:Bb6}) or short notation ({@code w:a1:b6}). */
    public static Position parse(String text) {
        String t = text.trim();
        if (t.startsWith("W") || t.startsWith("B")) return fromFen(t);
        return fromShort(t);
    }

    /** Reads pydraughts/lidraughts FEN, e.g. {@code W:Wa1,c1,Kd4:Bb6,d8}. Unknown extra fields are ignored. */
    public static Position fromFen(String fen) {
        String[] part = fen.trim().split(":", -1);
        if (part.length < 1 || !(part[0].equals("W") || part[0].equals("B"))) throw bad(fen, "side must be W or B");
        Builder b = new Builder().sideToMove(part[0].equals("W") ? Color.WHITE : Color.BLACK);
        for (int i = 1; i < part.length; i++) {
            String field = part[i].trim();
            if (field.isEmpty()) continue;
            char tag = field.charAt(0);
            if (tag != 'W' && tag != 'B') continue;
            addAll(b, tag == 'W' ? Color.WHITE : Color.BLACK, field.substring(1), fen);
        }
        return b.build();
    }

    /** Reads short notation, e.g. {@code w:a1,Kd4:b6,d8}. */
    public static Position fromShort(String text) {
        String[] part = text.trim().split(":", -1);
        if (part.length != 3 || !(part[0].equals("w") || part[0].equals("b"))) throw bad(text, "expected side:white:black");
        Builder b = new Builder().sideToMove(part[0].equals("w") ? Color.WHITE : Color.BLACK);
        addAll(b, Color.WHITE, part[1], text);
        addAll(b, Color.BLACK, part[2], text);
        return b.build();
    }

    private static void addAll(Builder b, Color color, String list, String source) {
        for (String token : list.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            boolean king = t.startsWith("K");
            if (king) t = t.substring(1);
            int dash = t.indexOf('-');
            if (dash > 0 && Character.isDigit(t.charAt(0))) {
                int from, to;
                try {
                    from = Integer.parseInt(t.substring(0, dash));
                    to = Integer.parseInt(t.substring(dash + 1));
                } catch (NumberFormatException e) {
                    throw bad(source, "bad range " + token);
                }
                if (from < 1 || to > 32 || from > to) throw bad(source, "bad range " + token);
                for (int n = from; n <= to; n++) b.put(Square.fromNumber(n), Piece.of(color, king), source);
            } else {
                int sq = Square.tryParse(t);
                if (sq < 0) throw bad(source, "bad square " + token);
                b.put(sq, Piece.of(color, king), source);
            }
        }
    }

    private static IllegalArgumentException bad(String text, String why) {
        return new IllegalArgumentException("bad position \"" + text + "\": " + why);
    }

    /** FEN with algebraic squares, e.g. {@code W:Wa1,c1:Bb6}; squares in index order (a1, c1, ..., h8). */
    public String toFen() {
        return (side == Color.WHITE ? "W" : "B") + ":W" + list(1, false) + ":B" + list(-1, false);
    }

    /** FEN with square numbers, e.g. {@code W:W1,2,K10:B21}. */
    public String toNumericFen() {
        return (side == Color.WHITE ? "W" : "B") + ":W" + list(1, true) + ":B" + list(-1, true);
    }

    /** Short notation, e.g. {@code w:a1,Kc3:b6}. */
    public String toShort() {
        return (side == Color.WHITE ? "w" : "b") + ":" + list(1, false) + ":" + list(-1, false);
    }

    private String list(int sign, boolean numeric) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (board[i] * sign <= 0) continue;
            if (s.length() > 0) s.append(',');
            if (Math.abs(board[i]) == 2) s.append('K');
            s.append(numeric ? String.valueOf(Square.number(i)) : Square.name(i));
        }
        return s.toString();
    }

    public Color sideToMove() {
        return side;
    }

    /** Consecutive plies in which only kings moved and nothing was captured. */
    public int quietPlies() {
        return quietPlies;
    }

    /** The same pieces and side to move with a different quiet-ply counter (for restoring saved games). */
    public Position withQuietPlies(int plies) {
        if (plies < 0) throw new IllegalArgumentException("negative quiet plies");
        return new Position(board, side, plies);
    }

    /** The piece on a square, or null when it is empty. */
    public Piece pieceAt(int square) {
        return Piece.ofCode(board[square]);
    }

    public Piece pieceAt(String square) {
        return pieceAt(Square.parse(square));
    }

    /** Number of pieces (men and kings) of one colour. */
    public int count(Color color) {
        int n = 0;
        for (int v : board) if (v * color.sign() > 0) n++;
        return n;
    }

    public int count(Piece piece) {
        int n = 0, code = piece.code();
        for (int v : board) if (v == code) n++;
        return n;
    }

    /** Squares holding a piece of the given colour, in index order. */
    public List<Integer> squares(Color color) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 64; i++) if (board[i] * color.sign() > 0) out.add(i);
        return out;
    }

    /**
     * Plays a move returned by {@link Rules#legalMoves(Position)} for this position and returns the
     * new position. The move is not re-validated; use {@link Rules#isLegal} or {@link Game} for that.
     */
    public Position play(Move m) {
        int[] b = board.clone();
        int v = b[m.from()];
        if (v * side.sign() <= 0) throw new IllegalArgumentException("no piece of the side to move on " + Square.name(m.from()));
        boolean man = Math.abs(v) == 1;
        b[m.from()] = 0;
        for (int c : m.captured()) b[c] = 0;
        b[m.to()] = man && m.promotes() ? v * 2 : v;
        int quiet = man || m.isCapture() ? 0 : quietPlies + 1;
        return new Position(b, side.opposite(), quiet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return side == p.side && Arrays.equals(board, p.board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(board) * 2 + side.ordinal();
    }

    @Override
    public String toString() {
        return toFen();
    }

    /** Diagram with White at the bottom: w/b men, W/B kings, '.' dark empty square. */
    public String toDiagram() {
        StringBuilder s = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            s.append(r + 1).append(' ');
            for (int c = 0; c < 8; c++) {
                int sq = r * 8 + c, v = board[sq];
                char ch = !Square.isDark(sq) ? ' ' : v == 0 ? '.' : v == 1 ? 'w' : v == 2 ? 'W' : v == -1 ? 'b' : 'B';
                s.append(ch).append(c < 7 ? " " : "");
            }
            s.append('\n');
        }
        return s.append("  a b c d e f g h\n").toString();
    }

    /** Builds a position piece by piece. */
    public static final class Builder {
        private final int[] board = new int[64];
        private Color side = Color.WHITE;
        private int quiet;

        public Builder sideToMove(Color color) {
            side = color;
            return this;
        }

        public Builder quietPlies(int plies) {
            quiet = plies;
            return this;
        }

        public Builder put(String square, Piece piece) {
            return put(Square.parse(square), piece, square);
        }

        public Builder put(int square, Piece piece) {
            return put(square, piece, String.valueOf(square));
        }

        Builder put(int square, Piece piece, String source) {
            if (!Square.isDark(square)) throw bad(source, Square.isValid(square) ? Square.name(square) + " is a light square" : "off board");
            if (board[square] != 0) throw bad(source, Square.name(square) + " is occupied twice");
            boolean king = piece.isKing() || Square.row(square) == piece.color().promotionRow();
            board[square] = Piece.of(piece.color(), king).code();
            return this;
        }

        public Position build() {
            return new Position(board.clone(), side, quiet);
        }
    }
}
