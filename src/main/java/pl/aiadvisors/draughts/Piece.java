package pl.aiadvisors.draughts;

/** A piece on the board: a man or a king of either colour. */
public enum Piece {
    WHITE_MAN(Color.WHITE, false), WHITE_KING(Color.WHITE, true),
    BLACK_MAN(Color.BLACK, false), BLACK_KING(Color.BLACK, true);

    private final Color color;
    private final boolean king;

    Piece(Color color, boolean king) {
        this.color = color;
        this.king = king;
    }

    public Color color() {
        return color;
    }

    public boolean isKing() {
        return king;
    }

    public static Piece of(Color color, boolean king) {
        if (color == Color.WHITE) return king ? WHITE_KING : WHITE_MAN;
        return king ? BLACK_KING : BLACK_MAN;
    }

    /** Internal code: +1 man, +2 king for White; negative for Black. */
    int code() {
        return color.sign() * (king ? 2 : 1);
    }

    static Piece ofCode(int code) {
        if (code == 0) return null;
        return of(Color.ofSign(code), Math.abs(code) == 2);
    }
}
