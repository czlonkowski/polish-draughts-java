package pl.aiadvisors.draughts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A small computer opponent: alpha-beta search with iterative deepening; forced captures extend
 * the search. Evaluation is simple (material, advancement of men, centre control); there is no
 * transposition table and no opening book. Good enough to beat beginners and to play quickly on
 * slow phones.
 *
 * <p>A search stops at the depth limit, the time limit ({@link #setTimeLimitMillis}), the node limit
 * ({@link #setNodeLimit}) or {@link #cancel()}, whichever comes first, and returns the best move of
 * the last finished iteration. One instance runs one search at a time; {@link #cancel()} may be
 * called from another thread.
 */
public final class Engine {
    /** Playing strength presets: search depth and a chance of a careless (random legal) move. */
    public enum Level {
        BEGINNER(1, .45f, 800_000),
        NOVICE(2, .18f, 800_000),
        INTERMEDIATE(4, 0, 800_000),
        ADVANCED(6, 0, 800_000),
        STRONG(9, 0, 3_000_000);

        final int depth;
        final float careless;
        final long nodes;

        Level(int depth, float careless, long nodes) {
            this.depth = depth;
            this.careless = careless;
            this.nodes = nodes;
        }

        /** Maximum search depth in plies (captures extend it). */
        public int depth() {
            return depth;
        }

        /** Probability of playing a random legal move instead of searching. */
        public float carelessness() {
            return careless;
        }
    }

    private static final int INF = 1_000_000, WIN = 100_000;

    private final Rules rules;
    private final Random random;
    private volatile boolean cancelled;
    private long timeLimitMillis = 2500;
    private long nodeLimit = -1;
    private long nodes, limit, deadline;
    private int lastDepth;

    public Engine(Rules rules) {
        this(rules, new Random());
    }

    /** With a seeded {@link Random}, the engine's choices are reproducible (as long as no time limit hits). */
    public Engine(Rules rules, Random random) {
        this.rules = rules;
        this.random = random;
    }

    /** Thinking time cap per move in milliseconds (default 2500). */
    public Engine setTimeLimitMillis(long millis) {
        timeLimitMillis = millis;
        return this;
    }

    /** Node cap per move; -1 (default) uses the level's cap, or no cap for {@link #bestMove(Position, int)}. */
    public Engine setNodeLimit(long nodes) {
        nodeLimit = nodes;
        return this;
    }

    /** Stops the running search and makes this engine return null from now on. Thread-safe. */
    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /** Nodes visited by the last search. */
    public long lastNodeCount() {
        return nodes;
    }

    /** Deepest fully searched iteration of the last search (0 if the move was forced or random). */
    public int lastDepth() {
        return lastDepth;
    }

    /** A move for the side to move at the given level, or null when there is none or the engine was cancelled. */
    public Move bestMove(Position p, Level level) {
        List<Move> moves = rules.legalMoves(p);
        lastDepth = 0;
        nodes = 0;
        if (moves.isEmpty() || cancelled) return null;
        if (moves.size() == 1) return moves.get(0);
        if (random.nextFloat() < level.careless) return moves.get(random.nextInt(moves.size()));
        return search(p, moves, level.depth, nodeLimit >= 0 ? nodeLimit : level.nodes);
    }

    public Move bestMove(Game game, Level level) {
        return bestMove(game.position(), level);
    }

    /** The best move found within {@code maxDepth} plies (plus capture extensions) and the time limit. */
    public Move bestMove(Position p, int maxDepth) {
        List<Move> moves = rules.legalMoves(p);
        lastDepth = 0;
        nodes = 0;
        if (moves.isEmpty() || cancelled) return null;
        if (moves.size() == 1) return moves.get(0);
        return search(p, moves, maxDepth, nodeLimit >= 0 ? nodeLimit : Long.MAX_VALUE);
    }

    private Move search(Position p, List<Move> legal, int maxDepth, long nodeCap) {
        List<Move> moves = new ArrayList<>(legal);
        limit = nodeCap;
        nodes = 0;
        deadline = System.nanoTime() + timeLimitMillis * 1_000_000L;
        Collections.shuffle(moves, random);
        Move best = moves.get(0);
        for (int depth = 1; depth <= maxDepth && !cancelled; depth++) {
            Move found = null;
            int alpha = -INF;
            for (Move m : moves) {
                int score = -negamax(p.play(m), depth - 1, -INF, -alpha, 1);
                if (stopped()) break;
                if (found == null || score > alpha) {
                    alpha = score;
                    found = m;
                }
            }
            if (stopped()) {
                if (found != null && depth > 1) best = found;
                break;
            }
            if (found != null) {
                best = found;
                lastDepth = depth;
                moves.remove(found);
                moves.add(0, found);
            }
        }
        return cancelled ? null : best;
    }

    private boolean stopped() {
        return cancelled || nodes > limit;
    }

    private int negamax(Position p, int depth, int alpha, int beta, int ply) {
        nodes++;
        if ((nodes & 1023) == 0 && System.nanoTime() > deadline) limit = 0;
        if (stopped()) return 0;
        List<Move> moves = rules.legalMoves(p);
        if (moves.isEmpty()) return -WIN + ply;
        if (p.quietPlies() >= 2 * rules.kingMovesDrawLimit()) return 0;
        boolean forced = moves.get(0).isCapture() || moves.size() == 1;
        if (depth <= 0 && (!forced || ply > 24)) return evaluate(p);
        int next = forced && depth <= 0 ? 0 : forced ? depth : depth - 1;
        for (Move m : moves) {
            int s = -negamax(p.play(m), next, -beta, -alpha, ply + 1);
            if (s > alpha) {
                alpha = s;
                if (alpha >= beta) break;
            }
        }
        return alpha;
    }

    /** Static score from the side to move's point of view (100 = one man). */
    public static int evaluate(Position p) {
        int score = 0;
        for (int sq = 0; sq < 64; sq++) {
            int v = p.board[sq];
            if (v == 0) continue;
            int color = v > 0 ? 1 : -1, own = color == 1 ? Square.row(sq) : 7 - Square.row(sq), c = Square.column(sq);
            int s = Math.abs(v) == 2 ? 320 : 100 + own * own;
            if (Math.abs(v) == 1 && own == 0) s += 8;
            if (c >= 2 && c <= 5 && own >= 2 && own <= 5) s += 4;
            score += color * s;
        }
        return score * p.side.sign();
    }
}
