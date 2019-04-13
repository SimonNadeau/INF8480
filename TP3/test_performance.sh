#!/bin/bash
start=$(date +%s%N)
for i in {1..50}
do
    wget -O ./results/index$i $1 &
done
wait
stop=$(date +%s%N)
mean=$((($stop - $start)/50000000))
echo "Temps moyen des requetes: $mean ms"