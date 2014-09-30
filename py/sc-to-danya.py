import sys

def main(inp, oup):
    lines = open(inp).readlines()
    lines = [l.replace(' ', '').strip() for l in lines if len(l.strip()) > 0]
    
    danya_sc = []
    for i in xrange(len(lines) / 2):
        events = lines[2 * i].split(';')
        actions = lines[2 * i + 1].split(';')
        sc = []
        for j in xrange(len(events)):
            sc += [events[j] + ('' if len(actions[j].strip()) == 0 else '/' + actions[j])]
        danya_sc += ['; '.join(sc)]

    print >>open(oup, 'w'), '\n'.join([str(len(danya_sc))] + danya_sc)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print 'Usage: %s <my-scenarios> <danya-scenarios>' % sys.argv[0]
    else:
        main(sys.argv[1], sys.argv[2])
