# encoding: utf-8
import sys
from pylab import *
import json
import numpy

LABELS = ['o', 'x', '^', 'v', '+']

def main(results_fp):
    results = json.load(open(results_fp))
    #print results
    points = {}
    for r in results:
        size, sum_length = r['size'], r['sum_length']
        f_cc, b_cc = r['forward_cc_persent'], r['backward_cc_persent']
        time, isomorph = r['time'], 1 if r['is_isomorph'] else 0
        complete = 1 #if r['is_complete'] else 0
        if size not in points:
            points[size] = {}
        if sum_length not in points[size]:
            points[size][sum_length] = ([], [], [], [], [])
        time_ls, iso_ls, f_cc_ls, b_cc_ls, compl_ls = points[size][sum_length]
        points[size][sum_length] = (time_ls + [time], 
                                    iso_ls + [isomorph], 
                                    f_cc_ls + [f_cc], 
                                    b_cc_ls + [b_cc],
                                    compl_ls + [complete])
    
    #print points

    subplot = figure().add_subplot(1, 1, 1)

    size_label = {}

    for size in sorted(points.keys()):
        lens, times = [], []
        for sum_len in sorted(points[size].keys()):
            time_ls, iso_ls, f_cc_ls, b_cc_ls, compl_ls = points[size][sum_len]

            time_avg = numpy.average(time_ls)
            time_std = numpy.std(time_ls)
            time_median = numpy.median(time_ls)

            iso_avg = 100. * numpy.average(iso_ls)
            compl_avg = 100. * numpy.average(compl_ls)

            f_avg = numpy.average(f_cc_ls)
            f_std = numpy.std(f_cc_ls)
            f_median = numpy.median(f_cc_ls)

            b_avg = numpy.average(b_cc_ls)
            b_std = numpy.std(b_cc_ls)
            b_median = numpy.median(b_cc_ls)

            #print '%3d & %5d & %4.1f (%.1f)' % (size, sum_len, time_avg, time_std),
            #print '& %4.1f & %5.1f (%.1f) & %4.1f (%.1f) \\\\' % (iso_avg, f_avg, f_std, b_avg, b_std)
            print '%3d & %5d & %4.1f' % (size, sum_len, time_median),
            print '& %4.1f & %5.1f & %4.1f \\\\' % (iso_avg, f_median, b_median)
            
            lens.append(sum_len)
            #times.append(time_avg)
            times.append(time_median)
            #times.append(f_median)

        print '\\hline'
        if size not in size_label:
            size_label[size] = LABELS[len(size_label)]
        
        subplot.plot(lens, times, size_label[size] + '-', color='black', label='%d states' % size)

    sum_lengths = set()
    for s in points:
        sum_lengths.update(points[s].keys())
    xlabels = sorted(list(sum_lengths))
    subplot.set_xticks(xlabels)

    xlabel('Summary scenarious size')
    #xlabel(u'Суммарная длина сценариев работы')
    ylabel('Median CryptoMiniSat execution time (s)')
    #ylabel('Median "forward check" (%)')
    
    image_fp = '.'.join(results_fp.split('.')[:-1]) + '.png'
    legend(loc=4)
    savefig(image_fp)
    show()

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: %s <fp>' % sys.argv[0]
    else:
        main(sys.argv[1])
