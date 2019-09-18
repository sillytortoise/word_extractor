#!/bin/sh

. /etc/profile
cd /datamore/cc/entity_extraction/entity_extraction
cp $1"processed.txt" $2
echo "Amax1979!" | sudo -S sh test.sh $3
rm "context_"$3".txt"
cd results/
mv "AutoPhrase_"*$3".txt" $1"mission/"$4
