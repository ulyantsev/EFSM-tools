#!/bin/bash

print_found_by_prefix() {
    local dir="$1"
    local prefix=$2
    local total=$(ls $dir/$prefix*/done 2>/dev/null | wc -l)
    local found=$(ls $dir/$prefix*/found 2>/dev/null | wc -l)
    local unknown=$(ls $dir/$prefix*/unknown 2>/dev/null | wc -l)
    sumstates=0
    count=0
    sattimes=()
    unsattimes=()
    for ((instance = 0; instance < 50; instance++)); do
        if [ -f $dir/$prefix$instance/found ]; then
            satsize=$(cat $dir/$prefix$instance/size)
            (( sumstates += satsize ))
            (( count++ ))
            unsatsize=$(($satsize - 1))
            satname=$dir/$prefix$instance/$satsize.log 
            unsatname=$dir/$prefix$instance/$unsatsize.log
            #echo $satname $unsatname
            sattimes[${#sattimes[@]}]=$(grep "execution time" < $satname | sed -e "s/^.*time: //g")
            if ((unsatsize == 0)); then
                unsattimes[${#unsattimes[@]}]=0
            else
                unsattimes[${#unsattimes[@]}]=$(grep "execution time" < $unsatname | sed -e "s/^.*time: //g")
            fi
        else
            sattimes[${#sattimes[@]}]=300
            unsattimes[${#unsattimes[@]}]=300
        fi
    done
    if [[ $count == 0 ]]; then
        count=1
    fi
    printf "%9s found %2d, unknown %2d, total %2d, frac %5s%%, realstates %4s, medsat %5s, medunsat %5s\n" $prefix $found $unknown $total $(python -c "print(round(float($found) / $total * 1000) / 10)") $(python -c "print(round(float($sumstates) / $count * 10)) / 10") $(python -c "print(round(${sattimes[25]} * 10) / 10)") $(python -c "print(round(${unsattimes[25]} * 10) / 10)")

}

for compdir in "complete" "incomplete"; do
    echo ">>> $compdir"
    for prefix in EXP* QSAT* COUN* BACK*; do
        echo_str=
        for ((s = 3; s <= 10; s++)); do
            ls evaluation/$compdir/$prefix-$s-*/"done" 1>/dev/null 2>/dev/null
            if [[ $? != 0 ]]; then
                continue
            fi
            print_found_by_prefix evaluation/$compdir "$prefix-$s-"
            echo_str='\n'
        done
        echo -en "$echo_str"
    done
done
