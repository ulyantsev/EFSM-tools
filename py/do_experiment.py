# Comparision script for mzn-cpx and muaco
# Author: Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)

import datetime
import os
import sys
import re
import subprocess
import json
import random
import shutil

# reproducible results
random.seed(239239)

COMMAND_TEMPLATE = 'java -Xmx512M -Xss128M -jar %s %s'

RESULTS_ROOT = 'results'

AG_PATH = 'jars/automaton-generator.jar'
AG_PARAMS_TEMPLATE = '-s %(size)s -ac %(ac)d -ec %(ec)d -vc %(vc)d -o %(path)s -mina %(mina)d -maxa %(maxa)d -p %(persent)d -rs %(seed)d'

SG_PATH = 'jars/scenarios-generator.jar'
SG_PARAMS_TEMPLATE = '-a %(automaton_path)s -cnt %(count)d -o %(path)s -minl %(minl)d -maxl %(maxl)d -rs %(seed)d'
SG_PARAMS_TEMPLATE_COVER = '-a %(automaton_path)s -o %(path)s -suml %(suml)d -c -rs %(seed)d'

NOISER_TEMPLATE = 'python add_noise.py %(input)s %(percent)d %(seed)d > %(output)s'

BUILDER_TEMPLATE = 'python main.py %(scenarios)s %(size)d %(percent)d > %(result)s'
MUACO_TEMPLATE = 'python muaco.py %(scenarios)s %(size)d %(percent)d'

IC_PATH = 'jars/isomorphism-checker.jar'
IC_PARAMS_TEMPLATE = '%(first)s %(second)s > %(output)s'


TR_PERSENT, EVENTS_COUNT, ACTIONS_COUNT, VARS_COUNT, MIN_ACTIONS, MAX_ACTIONS = 100, 2, 3, 0, 1, 2
MIN_LEN_C, MAX_LEN_C = 1, 3

LAUNCHES_COUNT = 100
SIZES = [5, 6, 7, 8, 9]
ERR_PERCENTS = [1, 2]
SUM_LENGTHS = [1000, 1500, 2000]

def generate_automaton(automaton_path, size):
    ag_params = {'size' : size,
                 'ac' : ACTIONS_COUNT,
                 'ec' : EVENTS_COUNT,
                 'vc' : VARS_COUNT, 
                 'path' : automaton_path,
                 'mina' : MIN_ACTIONS,
                 'maxa' : MAX_ACTIONS,
                 'persent' : TR_PERSENT,
                 'seed' : random.randint(1, 100000)}
    ag_params_str = AG_PARAMS_TEMPLATE % ag_params
    ag_command = COMMAND_TEMPLATE % (AG_PATH, ag_params_str)
    os.system(ag_command)

def generate_scenarios(scenarios_path, automaton_path, size, sum_length):
    sg_params = {'automaton_path' : automaton_path,
                 'path' : scenarios_path,
                 'suml' : sum_length,
                 'seed' : random.randint(1, 100000)}
    sg_params_str = SG_PARAMS_TEMPLATE_COVER % sg_params
    sg_command = COMMAND_TEMPLATE % (SG_PATH, sg_params_str)

    os.system(sg_command)

def add_noise(scenarios_path, percent, noisy_path):
    noise_params = {'input' : scenarios_path,
                    'percent' : percent,
                    'output' : noisy_path,
                    'seed' : random.randint(1, 100000)}
    os.system(NOISER_TEMPLATE % noise_params)

def build_noisy_with_mzn(scenarios_path, size, percent, output_path):
    builder_params = {'scenarios' : scenarios_path,
                      'percent' : percent,
                      'size' : size,
                      'result' : output_path}
    os.system(BUILDER_TEMPLATE % builder_params)

    time = float(re.search(r'# Time = (.+)', open(output_path).read()).group(1))
    return time

def build_with_muaco(scenarios_path, size, percent, output_path):
    builder_params = {'scenarios' : scenarios_path,
                      'percent' : percent,
                      'size' : size}
    
    output = os.popen(MUACO_TEMPLATE % builder_params).read()

    print >>open(output_path, 'w'), '#', '#'.join(open('muaco_metadata').readlines()), open('muaco_result.gv').read()

    time = float(re.search(r'Time = (.+)', output).group(1))
    return time

def check_isomorphism(ic_result_path, automaton_path, result_path):
    ic_params = {'first' : automaton_path,
                 'second' : result_path,
                 'output' : ic_result_path}
    ic_params_str = IC_PARAMS_TEMPLATE % ic_params
    ic_command = COMMAND_TEMPLATE % (IC_PATH, ic_params_str)
    os.system(ic_command)

    is_isomorph = open(ic_result_path).readline().startswith('ISOMORPHIC')
    return is_isomorph

def launch_mzn(dir_path, size, sum_length, percent):
    automaton_path = os.path.join(dir_path, 'automaton.gv')
    generate_automaton(automaton_path, size)

    scenarios_path = os.path.join(dir_path, 'scenarios-correct')
    sc_count = generate_scenarios(scenarios_path, automaton_path, size, sum_length)
    
    noisy_path = os.path.join(dir_path, 'scenarios-noisy')
    add_noise(scenarios_path, percent, noisy_path)

    result_path = os.path.join(dir_path, 'automaton-res.gv')
    time = build_noisy_with_mzn(scenarios_path, size, percent, result_path)

    muaco_path = os.path.join(dir_path, 'automaton-muaco.gv')
    muaco_time = build_with_muaco(scenarios_path, size, percent, muaco_path)

    ic_result_path = os.path.join(dir_path, 'isomorphism-result')
    is_isomorph = check_isomorphism(ic_result_path, automaton_path, result_path)

    result = {'work_dir' : dir_path,
              'size' : size,
              'sum_length' : sum_length,
              'time' : time,
              'muaco_time' : muaco_time,
              'is_isomorph' : is_isomorph,
              'percent' : percent}
    return result

def main():
    if not os.path.exists(RESULTS_ROOT):
        os.mkdir(RESULTS_ROOT)

    dt = datetime.datetime.now()
    time_str = dt.strftime('%Y-%m-%d--%H-%M-%S')
    results_dir = os.path.join(RESULTS_ROOT, time_str)
    os.mkdir(results_dir)
    
    # print properties

    results = []
    for automaton_size in SIZES:
        cur_size_dir = "%s/%02d-states" % (results_dir, automaton_size)
        os.mkdir(cur_size_dir)
        for sum_length in SUM_LENGTHS:
            cur_length_dir = "%s/%04d-sum" % (cur_size_dir, sum_length)
            os.mkdir(cur_length_dir)

            for percent in ERR_PERCENTS:
                cur_percent_dir = "%s/%02d-err" % (cur_length_dir, percent)
                os.mkdir(cur_percent_dir)
                
                for i in xrange(LAUNCHES_COUNT):
                    launch_dir = "%s/%02d" % (cur_percent_dir, i)
                    os.mkdir(launch_dir)
                    res = launch_mzn(launch_dir, automaton_size, sum_length, percent)
                    results.append(res)
                    print res
    
    results_json_str = '[' + ',\n'.join([json.dumps(r) for r in results]) + ']'
    print >>open(results_dir + '/results.json', 'w'), results_json_str

if __name__ == '__main__':
    main()

