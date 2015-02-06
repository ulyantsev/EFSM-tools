#!/bin/bash
for ((size = 10; size <= 10; size++)); do
    for ((events = 2; events <= 5; events++)); do
        for ((actions = 2; actions <= 5; actions++)); do
            if [ ! -f testing/fsm_${size}s${events}e${actions}a.done ]; then
                echo "NOT DONE s=$size e=$events a=$actions"
                continue
            fi
            for cnt in 5 10 20; do
                for l in 20 30 40 60 80 120 160 240 320 480 640; do
                    rm -f generated-fsm.gv
                    timeout -s TERM 300s java -jar ../jars/sat-builder.jar testing/fsm_${size}s${events}e${actions}a_${cnt}_$l.sc --result generated-fsm.gv --size $size 2>/dev/null
                    if [ -f generated-fsm.gv ]; then
                        fsm_complete=$(java -jar ../jars/completeness-checker.jar generated-fsm.gv)
                    fi
                    for ((formula_num = 1; formula_num <= 5; formula_num++)); do
                        report_str="s=$size e=$events a=$actions cnt=$cnt l=$l f=$formula_num"
                        if [ -f generated-fsm.gv ]; then
                            if [[ $fsm_complete != "COMPLETE" ]]; then
                                echo "GOOD[NOT COMPLETE] $report_str"
                                continue
                            fi
                            verified=$(java -jar verifier.jar generated-fsm.gv $size testing/fsm_${size}s${events}e${actions}a-$formula_num-true.ltl | wc -l)
                            if ((verified == formula_num)); then
                                echo "BAD [  VERIFIED  ] $report_str"
                            else
                                echo "GOOD[NOT VERIFIED] $report_str"
                            fi
                        else 
                            echo "GOOD[ TIME LIMIT ] $report_str"
                        fi
                    done
                done
            done
        done
    done
done
