import json
import numpy
import sys

def main(results_fp):
    results = json.load(open(results_fp))
    
    for r in results:
        size = r['size']
        sum_length = r['sum_length']
        tree_size = r['tree_size']
        time = r['time']
        c_time = r['c_time']
        is_complete = r['is_complete']
        c_forward_cc_persent = r['c_forward_cc_persent']
        forward_cc_persent = r['forward_cc_persent']
        is_isomorph = r['is_isomorph']
        c_is_isomorph = r['c_is_isomorph']

        print '%d, %d, %d, %d' % (size, sum_length, 1 if is_isomorph else 0, 1 if c_is_isomorph else 0)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: %s <fp>' % sys.argv[0]
    else:
        main(sys.argv[1])
