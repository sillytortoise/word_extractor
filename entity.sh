#!/bin/sh

. /etc/profile
mv $1$3"_entity_set.txt" $2
mv $1$3"_seed_concept_entity.json" $2
cd /datamore/cc/entity_extraction/entity_classification
sh classification.sh $3 >test.log 2>&1
mv $2"result/"$3"_"*"_rank.txt" $1 >>test.log 2>&1
mv $2"result/"$3* $1 >>test.log 2>&1
mv $2"wordvector/"$3* $1 >>test.log 2>&1
mv $3"_entity_set.txt" $1 >>test.log 2>&1
mv $3"_seed_concept_entity.json" $1 >>test.log 2>&1
mv $3"_corpus_user_raw.txt" $1 >>test.log 2>&1
