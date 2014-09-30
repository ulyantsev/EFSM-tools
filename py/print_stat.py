# encoding: utf-8
import sys
from pylab import *
import json
import numpy

LABELS = ['o', 'x', '^', 'v', '+']

def main(results_fp):
    results = json.load(open(results_fp))

    points = {}
    for r in results:
        size, sum_length, percent = r['size'], r['sum_length'], r['percent']
        
        if size not in points:
            points[size] = {}
        if sum_length not in points[size]:
            points[size][sum_length] = {}
        if percent not in points[size][sum_length]:
            points[size][sum_length][percent] = ([], [], [])
        
        muaco_time, csp_time, is_isomorph = r['muaco_time'], r['time'], r['is_isomorph']

        l1, l2, l3 = points[size][sum_length][percent]
        points[size][sum_length][percent] = (l1 + [muaco_time], l2 + [csp_time], l3 + [1 if is_isomorph else 0])

    for size in sorted(points.keys()):
        for sum_length in sorted(points[size].keys()):
            for percent in sorted(points[size][sum_length].keys()):
                muaco_avg = numpy.average(points[size][sum_length][percent][0])
                csp_avg = numpy.average(points[size][sum_length][percent][1])
                
                muaco_med = numpy.median(points[size][sum_length][percent][0])
                csp_med = numpy.median(points[size][sum_length][percent][1])

                #print size, sum_length, percent, muaco_avg, csp_avg, sum(points[size][sum_length][percent][2])
                print size, sum_length, percent, muaco_med, csp_med, sum(points[size][sum_length][percent][2])


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: %s <fp>' % sys.argv[0]
    else:
        main(sys.argv[1])
