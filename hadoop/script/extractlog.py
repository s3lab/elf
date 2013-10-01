import argparse

parser = argparse.ArgumentParser(description='Process some integers.')
parser.add_argument('fn', metavar='N', type=str, nargs='+',
                   help='file name')
args = parser.parse_args()

f = open(args.fn[0], "r")
g = open("loginfo", "w")

for line in f:
    if line.strip():
        g.write("\t".join(line.split()[5:]) + "\n")

f.close()
g.close()
