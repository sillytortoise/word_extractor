#!/bin/sh
cd /datamore/cc/entity_extraction/entity_extraction
rm $2
cp $1 $2
echo "Amax1979!" | sudo -S sh test.sh $3
cd result/
rm AutoPhrase_*_$3
