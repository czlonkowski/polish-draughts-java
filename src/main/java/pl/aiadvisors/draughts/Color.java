package pl.aiadvisors.draughts;

/** The two sides. White starts and moves up the board (towards rank 8). */
public enum Color {
    WHITE, BLACK;

    /** The other side. */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** +1 for White, -1 for Black; used in the internal board encoding. */
    int sign() {
        return this == WHITE ? 1 : -1;
    }

    /** Row (0 = rank 1) where this side's men are promoted. */
    int promotionRow() {
        return this == WHITE ? 7 : 0;
    }

    static Color ofSign(int sign) {
        return sign > 0 ? WHITE : BLACK;
    }
}
