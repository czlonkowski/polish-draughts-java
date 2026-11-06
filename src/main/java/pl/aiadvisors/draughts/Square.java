package pl.aiadvisors.draughts;

/**
 * Square helpers. A square is an index 0..63: {@code index = row * 8 + column}, row 0 is rank 1
 * (White's side), column 0 is file a. Only dark squares are used; a1 is dark.
 *
 * <p>Square numbers 1..32 follow the PDN numbering for Russian and Brazilian draughts used by
 * pydraughts: 1 = a1, 2 = c1, 3 = e1, 4 = g1, 5 = b2, ..., 32 = h8.
 */
public final class Square {
    private Square() {
    }

    public static int row(int square) {
        return square >> 3;
    }

    public static int column(int square) {
        return square & 7;
    }

    public static int of(int column, int row) {
        if (column < 0 || column > 7 || row < 0 || row > 7) throw new IllegalArgumentException("off board: " + column + "," + row);
        return row * 8 + column;
    }

    public static boolean isValid(int square) {
        return square >= 0 && square < 64;
    }

    public static boolean isDark(int square) {
        return isValid(square) && (row(square) + column(square)) % 2 == 0;
    }

    /** "a1".."h8". */
    public static String name(int square) {
        if (!isValid(square)) throw new IllegalArgumentException("no such square: " + square);
        return "" + (char) ('a' + column(square)) + (char) ('1' + row(square));
    }

    /** Parses "a1".."h8" or a square number "1".."32". Throws on anything else. */
    public static int parse(String text) {
        int sq = tryParse(text);
        if (sq < 0) throw new IllegalArgumentException("not a square: " + text);
        return sq;
    }

    static int tryParse(String text) {
        if (text == null) return -1;
        text = text.trim();
        if (text.isEmpty()) return -1;
        if (Character.isDigit(text.charAt(0))) {
            try {
                int n = Integer.parseInt(text);
                return n >= 1 && n <= 32 ? fromNumber(n) : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (text.length() != 2) return -1;
        int c = Character.toLowerCase(text.charAt(0)) - 'a', r = text.charAt(1) - '1';
        return c < 0 || c > 7 || r < 0 || r > 7 ? -1 : r * 8 + c;
    }

    /** Square number 1..32 of a dark square. */
    public static int number(int square) {
        if (!isDark(square)) throw new IllegalArgumentException("not a dark square: " + square);
        return row(square) * 4 + column(square) / 2 + 1;
    }

    /** Square index of the dark square numbered 1..32. */
    public static int fromNumber(int number) {
        if (number < 1 || number > 32) throw new IllegalArgumentException("square number out of range: " + number);
        int row = (number - 1) / 4, column = (number - 1) % 4 * 2 + (row & 1);
        return row * 8 + column;
    }

    /** One diagonal step away, or -1 off the board. */
    static int step(int square, int dRow, int dColumn) {
        int r = row(square) + dRow, c = column(square) + dColumn;
        return r < 0 || r > 7 || c < 0 || c > 7 ? -1 : r * 8 + c;
    }
}
