#!/bin/sh
mv $1$3"_seed_entity.txt" $2
cd $2
sh baike.sh $3
mv $2"result/"$3"_rank_baike.txt" $1"result_temp.txt"
mv $2$3"_seed_entity.txt" $1
