#!/bin/sh
mv $1$3"_entity_set.txt" $2
mv $1$3"_seed_concept_entity.txt" $2
cd /datamore/cc/entity_extraction/entity_classification
sh classification.sh $3
mv $2"result/"$3"_*_rank.txt" $1
rm $2"result/"$3"*"
