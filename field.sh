#!/bin/sh

. /etc/profile
cd /datamore/cc/entity_extraction/entity_extraction
cp $1"processed.txt" $2 >>/datamore/cc/knowledge/test.log 2>&1
echo "Amax1979!" | sudo -S sh test.sh $3 $2 >>/datamore/cc/knowledge/test.log 2>&1
rm "context_"$3".txt" >>/datamore/cc/knowledge/test.log 2>&1
cd results/
mv "AutoPhrase_"*$3".txt" $1"mission/"$4 >>/datamore/cc/knowledge/test.log 2>&1
