import os
import sys
import re
import json

GET_COMPLETENESS = True

def get_from_dir(dir_path):
    ans = {}
    
    ans['size'] = int(re.search(r'.*?(\d+)-states.*', dir_path).group(1))
    ans['sum_length'] = int(re.search(r'.*?(\d+)-sum.*', dir_path).group(1))

    log_path = os.path.join(dir_path, 'builder.log')
    log_str = open(log_path).read()
    ans['result'] = 'WAS FOUND' in log_str
    ans['tree_size'] = int(re.search(r'tree size: (\d+)', log_str).group(1))
    ans['time'] = float(re.search(r'execution time: (.+)', log_str).group(1))

    ic_result_path = os.path.join(dir_path, 'isomorphism-result')
    ans['is_isomorph'] = open(ic_result_path).readline().startswith('ISOMORPHIC')

    forward_cc_path = os.path.join(dir_path, 'forward-cc-result')
    result_str = open(forward_cc_path).read()
    ans['forward_cc_persent'] = float(re.search(r'Complies persent: (.+)', result_str).group(1))

    backward_cc_path = os.path.join(dir_path, 'backward-cc-result')
    result_str = open(backward_cc_path).read()
    ans['backward_cc_persent'] = float(re.search(r'Complies persent: (.+)', result_str).group(1))

    completeness_path = os.path.join(dir_path, 'completeness-result')
    ans['is_complete'] = open(completeness_path).readline().startswith('COMPLETE')
    
    if GET_COMPLETENESS:
        log_path = os.path.join(dir_path, 'c-builder.log')
        log_str = open(log_path).read()
        ans['c_result'] = 'WAS FOUND' in log_str
        ans['c_time'] = float(re.search(r'execution time: (.+)', log_str).group(1))

        ic_result_path = os.path.join(dir_path, 'c-isomorphism-result')
        ans['c_is_isomorph'] = open(ic_result_path).readline().startswith('ISOMORPHIC')

        forward_cc_path = os.path.join(dir_path, 'c-forward-cc-result')
        result_str = open(forward_cc_path).read()
        ans['c_forward_cc_persent'] = float(re.search(r'Complies persent: (.+)', result_str).group(1))

        backward_cc_path = os.path.join(dir_path, 'c-backward-cc-result')
        result_str = open(backward_cc_path).read()
        ans['c_backward_cc_persent'] = float(re.search(r'Complies persent: (.+)', result_str).group(1))

        completeness_path = os.path.join(dir_path, 'c-completeness-result')
        ans['c_is_complete'] = open(completeness_path).readline().startswith('COMPLETE')
        
    return ans

def main(results_dir):
    results = []
    
    for root, dirs, files in os.walk(results_dir):
        if os.path.exists(os.path.join(root, 'builder.log')):
            results.append(get_from_dir(root))
    
    results_json_str = '[' + ',\n'.join([json.dumps(r) for r in results]) + ']'
    print >>open(results_dir + '/results.json', 'w'), results_json_str
    
if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: %s <dir>' % sys.argv[0]
    else:
        main(sys.argv[1])
