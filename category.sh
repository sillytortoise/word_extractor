#!/bin/sh

#arg1:field name arg2:user arg3:num

alias python="/home/amax/anaconda3/bin/python3.6"
workdir='/datamore/cc/entity_extraction/baike_extraction/'
corpusdir='/datamore/cc/corpus/'

python $workdir'get_seed_type.py' $1
mkdir -p $corpusdir$2'/'$1'/mission/基于百科的抽取'$3'
mv $workdir'result/'$1'_seed_type.txt' $corpusdir$2'/'$1'/mission/基于百科的抽取'$3'/'
