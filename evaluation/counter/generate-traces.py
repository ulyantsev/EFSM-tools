#!/usr/bin/python

import random

h = 0
m = 0
s = 0
ho = False
mo = False
so = False

max_d = 3

def tick():
    global s, m, h, mo, ho, so, max_d
    
    s += 1
    so = s == max_d
    if s == max_d:
        s = 0

    if so: 
        m += 1
    mo = m == max_d
    if m == max_d:
        m = 0
    
    if mo: 
        h += 1
    ho = h == max_d
    if h == max_d:
        h = 0
    
def print_values(step, f):
    f.write("    " + str(step) + " " + str(h) + " " + str(m) + " " + str(s) + " " + str(int(ho)) + " " + str(int(mo)) + " " + str(int(so)) + "\n")
 
def print_trace(h_, m_, s_, num, length):
    global h, m, s
    h = h_
    m = m_
    s = s_
    f = open("traces/COUNTER_TRACE_" + str(num) + ".txt", 'w')
    f.write("7\n")
    f.write(" SIMULATION TIME\n")
    f.write(" H VALUE\n")
    f.write(" M VALUE\n")
    f.write(" S VALUE\n")
    f.write(" H OVERFLOW\n")
    f.write(" M OVERFLOW\n")
    f.write(" S OVERFLOW\n")
    for i in range(0, length):
        tick()
        print_values(i, f)
    f.close()

def main():
    global max_d
    for i in range(0, 100):
        print_trace(random.randint(0, max_d - 1), random.randint(0, max_d - 1), random.randint(0, max_d - 1), i, 50)

main()
