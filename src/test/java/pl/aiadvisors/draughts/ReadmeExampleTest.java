package pl.aiadvisors.draughts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The usage example from README.md, kept compiling and passing. */
class ReadmeExampleTest {
    @Test
    void example() {
        Game game = new Game(Rules.POLISH);              // or Rules.RUSSIAN
        game.play("c3-d4");                              // algebraic...
        game.play("23-19");                              // ...or numeric (1 = a1, 32 = h8)
        System.out.println(game.legalMoves());           // [d4:f6]: capturing is compulsory

        Engine engine = new Engine(Rules.POLISH).setTimeLimitMillis(1000);
        Move reply = engine.bestMove(game, Engine.Level.INTERMEDIATE);
        game.play(reply);

        GameResult result = game.result();               // ONGOING, or winner + reason
        System.out.println(game.position().toFen());     // W:W...:B... as in pydraughts / lidraughts
        Position p = Position.parse("W:WKa1:Bc3,f4");    // FEN or short "w:Ka1:c3,f4"
        System.out.println(Rules.POLISH.legalMoves(p));  // [a1:e5:g3, a1:e5:h2]

        assertEquals("[d4:f6]", new Game(Rules.POLISH, game.positions().get(2)).legalMoves().toString());
        assertTrue(reply.isCapture());
        assertFalse(result.isOver());
        assertEquals("[a1:e5:g3, a1:e5:h2]", Rules.POLISH.legalMoves(p).toString());
    }
}
