import sys
for l in open(sys.argv[1]):
    if l.startswith('true'):
        print l[4:],
    elif l.startswith('false'):
        pass
    else:
        print l,