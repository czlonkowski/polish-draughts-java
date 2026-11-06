"""Reference move lists computed by pydraughts, for OracleTest.

Dev-time only; pydraughts is not a dependency of the library. Usage:
  python3 -m venv /tmp/pyd && /tmp/pyd/bin/pip install pydraughts==0.6.7
  /tmp/pyd/bin/python -I tools/oracle.py brazilian 8 3 > src/test/resources/oracle-brazilian.txt
Arguments: variant (brazilian | russian), number of random games (random-piece positions = 30x that), seed.
Each output line: FEN|successor FEN;successor FEN;... with the squares of each colour sorted.
"""
import random, sys
from draughts import Board

def norm(fen):
    side, w, b = fen.split(':')[:3]
    def pieces(s): return ','.join(sorted(p for p in s[1:].split(',') if p))
    return side + ':W' + pieces(w) + ':B' + pieces(b)

def line(board):
    succ = set()
    for m in board.legal_moves():
        c = board.copy(); c.push(m); succ.add(norm(c.fen))
    return norm(board.fen) + '|' + ';'.join(sorted(succ))

variant = sys.argv[1] if len(sys.argv) > 1 else 'brazilian'
games = int(sys.argv[2]) if len(sys.argv) > 2 else 60
rng = random.Random(int(sys.argv[3]) if len(sys.argv) > 3 else 7)
out = []
for g in range(games):
    b = Board(variant)
    for ply in range(150):
        moves = b.legal_moves()
        if not moves: break
        out.append(line(b))
        b.push(rng.choice(moves))
dark = [c + r for c in 'abcdefgh' for r in '12345678' if (ord(c) - 97 + int(r) - 1) % 2 == 0]
for i in range(games * 30):
    sq = rng.sample(dark, rng.randint(3, 12)); w = []; bl = []
    for s in sq:
        white = rng.random() < .5; king = rng.random() < .35 or s[1] == ('8' if white else '1')
        (w if white else bl).append(('K' if king else '') + s)
    if not w or not bl: continue
    fen = rng.choice('WB') + ':W' + ','.join(w) + ':B' + ','.join(bl)
    b = Board(variant, fen)
    if b.legal_moves(): out.append(line(b))
print('\n'.join(out))
