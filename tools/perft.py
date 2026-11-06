"""Perft (leaf node counts) from the starting position, computed by pydraughts, for PerftTest.

Usage: /tmp/pyd/bin/python -I tools/perft.py brazilian 7
Prints depth, number of legal moves as pydraughts lists them, and the count with moves that lead
to the same position merged (the library's convention).
"""
import sys
from draughts import Board

variant = sys.argv[1] if len(sys.argv) > 1 else 'brazilian'
max_depth = int(sys.argv[2]) if len(sys.argv) > 2 else 6

def perft(board, depth):
    moves = board.legal_moves()
    if depth == 1:
        succ = set()
        for m in moves:
            c = board.copy(); c.push(m); succ.add(c.fen)
        return len(moves), len(succ)
    raw = merged = 0
    seen = set()
    for m in moves:
        c = board.copy(); c.push(m)
        r, u = perft(c, depth - 1)
        raw += r
        if c.fen not in seen:
            seen.add(c.fen); merged += u
    return raw, merged

for d in range(1, max_depth + 1):
    raw, merged = perft(Board(variant), d)
    print(d, raw, merged, flush=True)
