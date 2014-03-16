# Author: Vladimir Ulyantsev
# Script for testing SAT-based EFSM builder with and without BFS-constraints

import datetime
import os
import sys
import re
import subprocess
import json

CHECK_COMPLETE = True
COVER_TRANSITIONS = True

COMMAND_TEMPLATE = 'java -Xmx512M -Xss128M -jar %s %s'
#COMMAND_TEMPLATE = 'java -Xmx256M -Xss128M -jar %s %s'

RESULTS_ROOT = 'results'

AG_PATH = 'jars/automaton-generator.jar'
AG_PARAMS_TEMPLATE = '-s %(size)s -ac %(ac)d -ec %(ec)d -vc %(vc)d -o %(path)s -mina %(mina)d -maxa %(maxa)d -p %(persent)d'

SG_PATH = 'jars/scenarios-generator.jar'
SG_PARAMS_TEMPLATE = '-a %(automaton_path)s -cnt %(count)d -o %(path)s -minl %(minl)d -maxl %(maxl)d'
SG_PARAMS_TEMPLATE_COVER = '-a %(automaton_path)s -o %(path)s -suml %(suml)d -c'

BUILDER_PARAMS_TEMPLATE = '%(sc_path)s -s %(size)d -l %(log_path)s -r %(result_path)s -t %(tree_path)s -m %(model_path)s'

IC_PATH = 'jars/isomorphism-checker.jar'
IC_PARAMS_TEMPLATE = '%(first)s %(second)s > %(output)s'

COMPL_PATH = 'jars/completeness-checker.jar'
COMPL_PARAMS_TEMPLATE = '%(automaton)s > %(output)s'

CC_PATH = 'jars/consistency-checker.jar'
CC_PARAMS_TEMPLATE = '%(automaton)s %(scenarios)s > %(output)s'
SC_COUNT_C, SC_LEN_C = 1000, 4

TR_PERSENT, EVENTS_COUNT, ACTIONS_COUNT, VARS_COUNT, MIN_ACTIONS, MAX_ACTIONS = 50, 2, 2, 3, 1, 2
MIN_LEN_C, MAX_LEN_C = 1, 3

LAUNCHES_COUNT = 30
#SIZES = [5, 10, 15, 20]
#SUM_LENGTHS = [250, 500, 750, 1000, 1250, 1500]
#SUM_LENGTHS = [500, 1000, 1500]
SIZES = [4, 6, 8, 10]
SUM_LENGTHS = [800, 900, 1000, 1100, 1200]

FORBIDDEN = [(15, 250), (20, 250), (20, 500)]

def get_properties_str(builder_path):
    vals = ['Used builder: ' + builder_path,
            'Transition persent: %d' % TR_PERSENT,
            'Events count: %d' % EVENTS_COUNT,
            'Actions count: %d' % ACTIONS_COUNT,
            'Variables count: %d' % VARS_COUNT,
            'Minimum actions count in transition: %d' % MIN_ACTIONS,
            'Maximum actions count in transition: %d' % MAX_ACTIONS,
            'Minimum scenario length constant (min_len = C * size): %d' % MIN_LEN_C,
            'Maximum scenario length constant (max_len = C * size): %d' % MAX_LEN_C,
            'Launches count per experiment: %d' % LAUNCHES_COUNT,
            'Used EFSM sizes: ' + str(SIZES),
            'Used summary scenarios lengths: ' + str(SUM_LENGTHS),
            'Forbidden combinations: ' + str(FORBIDDEN),
            'JAR execution template: ' + COMMAND_TEMPLATE,
            'Compare with "-c" option: ' + str(CHECK_COMPLETE)]
    return '\n'.join(vals)

def generate_automaton(automaton_path, size):
    ag_params = {'size' : size,
                 'ac' : ACTIONS_COUNT,
                 'ec' : EVENTS_COUNT,
                 'vc' : VARS_COUNT, 
                 'path' : automaton_path,
                 'mina' : MIN_ACTIONS,
                 'maxa' : MAX_ACTIONS,
                 'persent' : TR_PERSENT}
    ag_params_str = AG_PARAMS_TEMPLATE % ag_params
    ag_command = COMMAND_TEMPLATE % (AG_PATH, ag_params_str)
    os.system(ag_command)

def generate_scenarios(scenarios_path, automaton_path, size, sum_length):
    if COVER_TRANSITIONS:
        sg_params = {'automaton_path' : automaton_path,
                     'path' : scenarios_path,
                     'suml' : sum_length}
        sg_params_str = SG_PARAMS_TEMPLATE_COVER % sg_params
        sc_count = 'UNKNOWN'
    else:
        minl, maxl = MIN_LEN_C * size, MAX_LEN_C * size
        sc_count = sum_length * 2 / (minl + maxl)    
        sg_params = {'automaton_path' : automaton_path,
                     'count' : sc_count,
                     'path' : scenarios_path,
                     'minl' : minl,
                     'maxl' : maxl}
        sg_params_str = SG_PARAMS_TEMPLATE % sg_params
    sg_command = COMMAND_TEMPLATE % (SG_PATH, sg_params_str)
    os.system(sg_command)
    return sc_count

def build_automaton(builder_path, size, dir_path, result_path, scenarios_path, build_complete = False):
    log_filename, model_filename = 'builder.log', 'model'
    if build_complete:
        log_filename, model_filename = 'c-builder.log', 'c-model'
    
    log_path = os.path.join(dir_path, log_filename)

    builder_params = {'sc_path' : scenarios_path,
                      'size' : size,
                      'log_path' : log_path,
                      'result_path' : result_path,
                      'tree_path' : os.path.join(dir_path, 'tree.gv'),
                      'model_path' : os.path.join(dir_path, model_filename)}
    builder_params_str = BUILDER_PARAMS_TEMPLATE % builder_params
    if build_complete:
        builder_params_str += ' -c'
    builder_command = COMMAND_TEMPLATE % (builder_path, builder_params_str)
    # start time
    #start_time = datetime.datetime.now()
    os.system(builder_command)
    # end time
    #end_time = datetime.datetime.now() 

    log_str = open(log_path).read()
    was_found = 'WAS FOUND' in log_str
    tree_size = int(re.search(r'tree size: (\d+)', log_str).group(1))
    function_ex_time = float(re.search(r'execution time: (.+)', log_str).group(1))
    return was_found, tree_size, function_ex_time

def check_isomorphism(ic_result_path, automaton_path, result_path):
    ic_params = {'first' : automaton_path,
                 'second' : result_path,
                 'output' : ic_result_path}
    ic_params_str = IC_PARAMS_TEMPLATE % ic_params
    ic_command = COMMAND_TEMPLATE % (IC_PATH, ic_params_str)
    os.system(ic_command)

    is_isomorph = open(ic_result_path).readline().startswith('ISOMORPHIC')
    return is_isomorph

def check_on_scenarios(result_path, first_automaton, second_automaton, size):
    sc_count, sc_len = size * SC_COUNT_C, size * SC_LEN_C
    scenarios_path = 'temp_scenarios'
    sg_params = {'automaton_path' : first_automaton,
                 'path' : scenarios_path,
                 'count' : sc_count,
                 'minl' : sc_len,
                 'maxl' : sc_len}
    sg_params_str = SG_PARAMS_TEMPLATE % sg_params
    sg_command = COMMAND_TEMPLATE % (SG_PATH, sg_params_str)
    os.system(sg_command)

    cc_params = {'automaton' : second_automaton,
                 'scenarios' : scenarios_path,
                 'output' : result_path}
    cc_params_str = CC_PARAMS_TEMPLATE % cc_params
    cc_command = COMMAND_TEMPLATE % (CC_PATH, cc_params_str)
    os.system(cc_command)
    os.remove(scenarios_path)

    result_str = open(result_path).read()
    persent = float(re.search(r'Complies persent: (.+)', result_str).group(1))
    return persent

def check_completeness(compl_result_path, automaton_path):
    compl_params = {'automaton' : automaton_path,
                    'output' : compl_result_path}
    compl_params_str = COMPL_PARAMS_TEMPLATE % compl_params
    compl_command = COMMAND_TEMPLATE % (COMPL_PATH, compl_params_str)
    os.system(compl_command)

    is_complete = open(compl_result_path).readline().startswith('COMPLETE')
    return is_complete

def launch(dir_path, builder_path, size, sum_length):
    automaton_path = os.path.join(dir_path, 'automaton.gv')
    generate_automaton(automaton_path, size)

    scenarios_path = os.path.join(dir_path, 'scenarios')
    sc_count = generate_scenarios(scenarios_path, automaton_path, size, sum_length)
    
    result_path = os.path.join(dir_path, 'result.gv')
    was_found, tree_size, function_ex_time = build_automaton(builder_path, size, dir_path, result_path, scenarios_path)

    ic_result_path = os.path.join(dir_path, 'isomorphism-result')
    is_isomorph = check_isomorphism(ic_result_path, automaton_path, result_path)

    forward_cc_path = os.path.join(dir_path, 'forward-cc-result')
    forward_cc_persent = check_on_scenarios(forward_cc_path, automaton_path, result_path, size)

    backward_cc_path = os.path.join(dir_path, 'backward-cc-result')
    backward_cc_persent = check_on_scenarios(backward_cc_path, result_path, automaton_path, size)    
    
    compl_path = os.path.join(dir_path, 'completeness-result')
    is_complete = check_completeness(compl_path, result_path)

    result = {'size' : size,
              'sc_count' : sc_count,
              'sum_length' : sum_length,
              'tree_size' : tree_size,
              'function_ex_time' : function_ex_time,
              'result' : was_found,
              'time' : function_ex_time,
              'is_complete' : is_complete,
              'is_isomorph' : is_isomorph,
              'forward_cc_persent' : forward_cc_persent,
              'backward_cc_persent': backward_cc_persent}
    
    if CHECK_COMPLETE:
        complete_result_path = os.path.join(dir_path, 'c-result.gv')
        was_found, tree_size, function_ex_time = build_automaton(builder_path, size, dir_path, 
                                                                 complete_result_path, scenarios_path, build_complete = True)

        ic_result_path = os.path.join(dir_path, 'c-isomorphism-result')
        is_isomorph = check_isomorphism(ic_result_path, automaton_path, complete_result_path)

        forward_cc_path = os.path.join(dir_path, 'c-forward-cc-result')
        forward_cc_persent = check_on_scenarios(forward_cc_path, automaton_path, complete_result_path, size)

        backward_cc_path = os.path.join(dir_path, 'c-backward-cc-result')
        backward_cc_persent = check_on_scenarios(backward_cc_path, complete_result_path, automaton_path, size)

        compl_path = os.path.join(dir_path, 'c-completeness-result')
        is_complete = check_completeness(compl_path, complete_result_path)
        
        result.update({'c_time': function_ex_time,
                       'c_result' : was_found, 
                       'c_is_isomorph' : is_isomorph,
                       'c_forward_cc_persent' : forward_cc_persent,
                       'c_backward_cc_persent' : backward_cc_persent,
                       'c_is_complete' : is_complete})

    return result

def main(builder_path):
    if not os.path.exists(RESULTS_ROOT):
        os.mkdir(RESULTS_ROOT)

    dt = datetime.datetime.now()
    time_str = dt.strftime('%Y-%m-%d--%H-%M-%S')
    results_dir = os.path.join(RESULTS_ROOT, time_str)
    os.mkdir(results_dir)

    print >>open(results_dir + '/experiment-properties', 'w'), get_properties_str(builder_path)
    
    results = []
    for automaton_size in SIZES:
        cur_size_dir = "%s/%02d-states" % (results_dir, automaton_size)
        os.mkdir(cur_size_dir)
        for sum_length in SUM_LENGTHS:
            if (automaton_size, sum_length) in FORBIDDEN:
                continue
            cur_length_dir = "%s/%04d-sum" % (cur_size_dir, sum_length)
            os.mkdir(cur_length_dir)
            for i in xrange(LAUNCHES_COUNT):
                launch_dir = "%s/%02d" % (cur_length_dir, i)
                os.mkdir(launch_dir)
                res = launch(launch_dir, builder_path, automaton_size, sum_length)
                results.append(res)
                print res
    
    results_json_str = '[' + ',\n'.join([json.dumps(r) for r in results]) + ']'
    print >>open(results_dir + '/results.json', 'w'), results_json_str

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: %s <builder.jar>'
    else:
        main(sys.argv[1])
