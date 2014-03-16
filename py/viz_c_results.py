# encoding: utf-8
import sys
from pylab import *
import json
import numpy

LABELS = ['o', 'x', '^', 'v', '+']

def main(results_fp):
    results = json.load(open(results_fp))

    hist_data = {}

    points = {}
    for r in results:
        size, sum_length = r['size'], r['sum_length']
        is_complete, is_isomorph = r['is_complete'], r['is_isomorph']
        c_is_isomorph = r['c_is_isomorph']
        
        if size not in points:
            points[size] = {}
            hist_data[size] = []
        if sum_length not in points[size]:
            points[size][sum_length] = ([], [], [])
        
        complete_ls, isomorph_ls, c_isomorph_ls = points[size][sum_length]
        points[size][sum_length] = (complete_ls + [1 if is_complete else 0],
                                    isomorph_ls + [1 if is_isomorph else 0],
                                    c_isomorph_ls + [1 if c_is_isomorph else 0])
        if is_complete:
            hist_data[size] += [sum_length]

    for size in sorted(points.keys()):
        for sum_length in sorted(points[size].keys()):
            print size, sum_length, sum(points[size][sum_length][0])

    subplot = figure().add_subplot(1, 1, 1)
    size_label = {}

    for size in sorted(points.keys()):
        lens, compl = [], []
        for sum_len in sorted(points[size].keys()):
            compl_ls, iso_ls, c_iso_ls = points[size][sum_len]

            compl_avg = 100. * numpy.average(compl_ls)
            iso_avg = 100. * numpy.average(iso_ls)
            c_iso_avg = 100. * numpy.average(c_iso_ls)

            lens.append(sum_len)
            compl.append(compl_avg)

        if size not in size_label:
            size_label[size] = LABELS[len(size_label)]
        
        #subplot.plot(lens, compl, size_label[size] + '-', color='black', label='%d states' % size)

    subplot.hist(hist_data, bins = [4, 6, 8, 10])

    sum_lengths = set()
    for s in points:
        sum_lengths.update(points[s].keys())
    xlabels = sorted(list(sum_lengths))
    subplot.set_xticks(xlabels)

    xlabel('Summary scenarious size')
    #xlabel(u'Суммарная длина сценариев работы')
    #ylabel(u'Среднее число полных автоматов')
    ylabel('Average full automatons (%)')
    
    image_fp = '.'.join(results_fp.split('.')[:-1]) + '.png'
    legend(loc=4)
    savefig(image_fp)
    show()            

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: %s <fp>' % sys.argv[0]
    else:
        main(sys.argv[1])
