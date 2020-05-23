#!/bin/sh

#arg1:field name arg2:user

alias python="/home/amax/anaconda3/bin/python3.6"
workdir='/datamore/cc/entity_extraction/baike_extraction/'
corpusdir='/datamore/cc/corpus/'
python $workdir'get_seed_type.py' $1
mv $workdir'result/'$1'_seed_type.txt' $corpusdir$2'/'$1'/mission/'
rm $workdir$1'_seed_entity.txt'
