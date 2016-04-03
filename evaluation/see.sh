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
            sattimes[$instance]=$(grep "execution time" < $satname | sed -e "s/^.*time: //g")
            if [ -f $unsatname ]; then
                unsattimes[$instance]=$(grep "execution time" < $unsatname | sed -e "s/^.*time: //g")
            else
                unsattimes[$instance]=0.0
            fi
        else
            sattimes[$instance]=300.0
            unsattimes[$instance]=300.0
        fi
    done
    if [[ $count == 0 ]]; then
        count=1
    fi
    str=${sattimes[@]}
    str=$(echo $(echo -e ${str// /\\n} | sort -n))
    IFS=' ' read -a sattimes_sorted <<< $str
    str=${unsattimes[@]}
    str=$(echo $(echo -e ${str// /\\n} | sort -n))
    IFS=' ' read -a unsattimes_sorted <<< $str
    printf "%9s found %2d, unknown %2d, total %2d, frac %5s%%, realstates %4s, medsat %5s, medunsat %5s\n" $prefix $found $unknown $total $(python -c "print(round(float($found) / $total * 1000) / 10)") $(python -c "print(round(float($sumstates) / $count * 10)) / 10") $(python -c "print(round(${sattimes_sorted[25]} * 10) / 10)") $(python -c "print(round(${unsattimes_sorted[25]} * 10) / 10)")

}

for compdir in "complete" "incomplete"; do
    echo ">>> $compdir"
    for prefix in FAST* EXP* QSAT* COUN* BACK*; do
        echo_str=
        for ((s = 3; s <= 12; s++)); do
            ls "eval"/$compdir/$prefix-$s-*/"done" 1>/dev/null 2>/dev/null
            if [[ $? != 0 ]]; then
                continue
            fi
            print_found_by_prefix "eval/$compdir" "$prefix-$s-"
            echo_str='\n'
        done
        echo -en "$echo_str"
    done
done
