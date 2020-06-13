import json
import sys
import numpy as np
import gensim
import re
from sklearn.metrics.pairwise import cosine_similarity


def sim(e1, e2, flist):  # F:selected features sgs:all the features
    min_sum = 0
    max_sum = 0
    for i in range(len(flist)):
        min_sum += min(e1[i], e2[i])
        max_sum += max(e1[i], e2[i])
    if max_sum == 0:
        return 1
    else:
        return min_sum / max_sum


field=sys.argv[1]
entity_file = open('./result/%s_entity_set_invocab.txt' % field)
seed_file = open('./%s_seed_concept_entity.json' % field, encoding='utf-8')
seed_info = json.load(seed_file)
matrix = np.zeros((0, 0))
file_name = './result/%s_corpus_together_processed.txt' % field  # corpus file
corpus = open(file_name, 'r', encoding='UTF-8')

sg_axis = []  # skip-grams axis
entity_axis = []  # entity axis

text = ''
for line in corpus:  # transfer corpus to a string
line = line.strip()
text = text + line + ' '
text.strip()

print("==========Finish load corpus==========")

# construct concurrence matrix
for concept in seed_info:
for seed in seed_info[concept]:
    pattern = '[^\s]+[^\s]+\s%s\s[^\s]+[^\s]+' % seed
    rule = re.compile(pattern)
    skip_grams = rule.findall(text)
    sgs = [(str.split(sg, " ")[0], str.split(sg, " ")[2]) for sg in skip_grams]  # patterns for seed

    if len(sgs) > 0:
        # if seed not in entity_axis:     #add seed
        entity_axis.append(seed)
        if len(entity_axis) > 1:
            matrix = np.row_stack((matrix, np.zeros((1, matrix.shape[1]))))

        for sg in sgs:  # for each pattern of seed
            if sg in sg_axis:  # pattern already exists
                matrix[matrix.shape[0] - 1][sg_axis.index(sg)] += 1
            else:
                if matrix.shape[1] == 0:  # if matrix is empty
                    matrix = np.zeros((1, 1))
                    matrix[0][0] = 1
                    sg_axis.append(sg)
                else:  # matrix is not empty
                    flag = 0
                    for s in sg_axis:  # traverse existing patterns
                        if sg[0] == s[0]:
                            matrix[matrix.shape[0] - 1][sg_axis.index(s)] += 1
                            flag = 1
                        elif len(sg[0]) < len(s[0]) and (  # sg[0] contained in s[0]
                                s[0].endswith(sg[0]) or s[0].startswith(sg[0])):
                            matrix[matrix.shape[0] - 1][sg_axis.index(s)] += 1
                            # x sg_axis[sg_axis.index(s)] = (sg[0], s[1])
                            # x s=(sg[0], s[1])
                            flag = 1
                        elif len(sg[0]) > len(s[0]) and (  # sg[0] contains s[0]
                                sg[0].endswith(s[0]) or sg[0].startswith(s[0])):
                            matrix[matrix.shape[0] - 1][sg_axis.index(s)] += 1
                            flag = 1

                        if sg[1] == s[1]:
                            matrix[matrix.shape[0] - 1][sg_axis.index(s)] += 1
                        elif len(sg[1]) < len(s[1]) and (  # sg[1] contained in s[1]
                                s[1].endswith(sg[1]) or s[1].startswith(sg[1])):
                            matrix[matrix.shape[0] - 1][sg_axis.index(s)] += 1
                        elif len(sg[1]) > len(s[1]) and (  # sg[1] contains s[1]
                                sg[1].endswith(s[1]) or sg[1].startswith(s[1])):
                            matrix[matrix.shape[0] - 1][sg_axis.index(s)] += 1
                            flag = 1

                        if flag == 1:
                            break

                    if flag == 0:  # no matching pattern in sg_axis for sg
                        matrix = np.column_stack((matrix, np.zeros((matrix.shape[0], 1))))
                        matrix[matrix.shape[0] - 1][matrix.shape[1] - 1] = 1
                        sg_axis.append(sg)

print("==========Finish construct concurrence matrix for seed entities==========")


concepts = {}
for concept in seed_info:  # seed_info: json of concepts
concepts[concept] = {}
m = np.zeros((0, matrix.shape[1]))
entities = []
for seed in entity_axis:
    if seed in seed_info[concept]:
        entities.append(seed)
        m = np.row_stack((m, matrix[entity_axis.index(seed)]))

concepts[concept]['m'] = m
concepts[concept]['entities'] = entities

# ====sort skip-grams for each concept
sg_list = sg_axis  # copy of sg_axis
pattern_list = []  # selected patterns
max_min_dic = {}
original_score = 0
while True:
max_sg = ('', '')
max_score_f = 0
for sg in sg_list:  # find the best sg making the score_f largest
    total1 = 0
    total2 = 0
    index = sg_list.index(sg)

    if len(pattern_list) == 0:  # pattern list is empty
        for c in concepts:  # for each concept
            score = 0
            for i in range(concepts[c]['m'].shape[0] - 1):
                for j in range(i + 1, concepts[c]['m'].shape[0]):
                    if max(concepts[c]['m'][i][index], concepts[c]['m'][j][index]) != 0:
                        score += min(concepts[c]['m'][i][index], concepts[c]['m'][j][index]) \
                                 / max(concepts[c]['m'][i][index], concepts[c]['m'][j][index])
            if concepts[c]['m'].shape[0] != 0 and concepts[c]['m'].shape[0] != 1:
                score /= (concepts[c]['m'].shape[0] * (concepts[c]['m'].shape[0] - 1)) / 2
            total1 += score

        total1 /= len(concepts)

        for c1 in concepts:
            for c2 in concepts:
                if c1 == c2:
                    continue

                score = 0
                for i in range(concepts[c1]['m'].shape[0]):
                    for j in range(concepts[c2]['m'].shape[0]):
                        if max(concepts[c1]['m'][i][index], concepts[c2]['m'][j][index]) != 0:
                            score += min(concepts[c1]['m'][i][index], concepts[c2]['m'][j][index]) / max(
                                concepts[c1]['m'][i][index], concepts[c2]['m'][j][index])

                if concepts[c1]['m'].shape[0]!=0 and concepts[c2]['m'].shape[0]!=0:
                    score /= concepts[c1]['m'].shape[0] * concepts[c2]['m'].shape[0]
                total2 += score
        if len(concepts) == 1:
            total2 = 0
        else:
            total2 /= (len(concepts) * (len(concepts) - 1)) / 2

    else:  # pattern exists
        for c in concepts:
            score = 0
            for i in range(concepts[c]['m'].shape[0] - 1):
                for j in range(i + 1, concepts[c]['m'].shape[0]):
                    max_num = max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])][
                              'max'] + max(concepts[c]['m'][i][index], concepts[c]['m'][j][index])
                    min_num = max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])][
                              'min'] + min(concepts[c]['m'][i][index], concepts[c]['m'][j][index])
                    if max_num != 0:
                        score += min_num / max_num
            if concepts[c]['m'].shape[0]!=0 and concepts[c]['m'].shape[0]!=1:
                score /= (concepts[c]['m'].shape[0] * (concepts[c]['m'].shape[0] - 1)) / 2
            total1 += score

        total1 /= len(concepts)

        for c1 in concepts:
            for c2 in concepts:
                if c1 == c2:
                    continue

                score = 0
                for i in range(concepts[c1]['m'].shape[0]):
                    for j in range(concepts[c2]['m'].shape[0]):
                        max_num = max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])][
                                  'max'] + max(concepts[c1]['m'][i][index], concepts[c2]['m'][j][index])
                        min_num = max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])][
                                  'min'] + min(concepts[c1]['m'][i][index], concepts[c2]['m'][j][index])
                        if max_num != 0:
                            score += min_num / max_num

                if concepts[c1]['m'].shape[0] != 0 and concepts[c2]['m'].shape[0] != 0:
                    score /= concepts[c1]['m'].shape[0] * concepts[c2]['m'].shape[0]
                total2 += score

        if len(concepts) == 1:
            total2 = 0
        else:
            total2 /= (len(concepts) * (len(concepts) - 1)) / 2

    score_f = total1 - total2
    if score_f > max_score_f:
        max_score_f = score_f
        max_sg = sg

print(max_score_f)
if max_score_f <= original_score:
    break
else:
    pattern_list.append(max_sg)
    original_score = max_score_f
    for c in concepts:
        for i in range(concepts[c]['m'].shape[0] - 1):
            for j in range(i + 1, concepts[c]['m'].shape[0]):
                if (c, concepts[c]['entities'][i], c, concepts[c]['entities'][j]) not in max_min_dic:
                    max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])]={}
                    max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])]['max'] = max(
                        concepts[c]['m'][i][sg_list.index(max_sg)], concepts[c]['m'][j][sg_list.index(max_sg)])
                    max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])]['min'] = min(
                        concepts[c]['m'][i][sg_list.index(max_sg)], concepts[c]['m'][j][sg_list.index(max_sg)])
                else:
                    max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])]['max'] += max(
                        concepts[c]['m'][i][sg_list.index(max_sg)], concepts[c]['m'][j][sg_list.index(max_sg)])
                    max_min_dic[(c, concepts[c]['entities'][i], c, concepts[c]['entities'][j])]['min'] += min(
                        concepts[c]['m'][i][sg_list.index(max_sg)], concepts[c]['m'][j][sg_list.index(max_sg)])

    for c1 in concepts:
        for c2 in concepts:
            if c1 == c2:
                continue
            else:
                for i in range(concepts[c1]['m'].shape[0]):
                    for j in range(concepts[c2]['m'].shape[0]):
                        if (c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j]) not in max_min_dic:
                            max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])]={}
                            max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])]['max']\
                                = max(concepts[c1]['m'][i][sg_list.index(max_sg)],concepts[c2]['m'][j][sg_list.index(max_sg)])
                            max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])]['min']\
                                = min(concepts[c1]['m'][i][sg_list.index(max_sg)],concepts[c2]['m'][j][sg_list.index(max_sg)])
                        else:
                            max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])]['max']\
                                += max(concepts[c1]['m'][i][sg_list.index(max_sg)],concepts[c2]['m'][j][sg_list.index(max_sg)])
                            max_min_dic[(c1, concepts[c1]['entities'][i], c2, concepts[c2]['entities'][j])]['min']\
                                += min(concepts[c1]['m'][i][sg_list.index(max_sg)],concepts[c2]['m'][j][sg_list.index(max_sg)])

    del sg_list[sg_list.index(sg)]

sgs_record = open('./sgs.txt', 'w')
sgs_record.write(str(pattern_list[0]))
for i in range(1,len(pattern_list)):
sgs_record.write('\n')
sgs_record.write(str(pattern_list[i]))

sgs_record.close()

print("==========Finish construct pattern list==========")

items = entity_file.readlines()  # the list of candidates
for i in range(len(items)):
items[i] = items[i].rstrip('\n')

corpus_items = text.split(" ")  # the list of corpus after split

# traverse the corpus to get the concurrence matrix
can_matrix = np.zeros((len(items), len(pattern_list)))  # candidate matrix
for i in range(1, len(corpus_items) - 1):
try:
    index = items.index(corpus_items[i])  # position of the candidate word
except ValueError as e:
    index = -1

if index != -1:
    sg = (corpus_items[i - 1], corpus_items[i + 1])
    matches = list(
        filter(lambda x: sg[0] in x[0] or x[0] in sg[0] or sg[1] in x[1] or x[1] in sg[1], pattern_list))
    for s in matches:
        j = matches.index(s)
        can_matrix[index][j] += 1

print("==========Finish construct concurrence matrix for candidates==========")

# classification
results = {}
for c in seed_info:
results[c] = []

model = gensim.models.KeyedVectors.load_word2vec_format('./wordvector/%s_100.vector' % field, binary=False)
print("==========Finish load model==========")

for c in seed_info:  # for each concept
# 导入word2vec模型
total_dic = {}
# sg_dic = {}
# vec_dic = {}

vector_centroid = np.zeros(100)
for e in concepts[c]['entities']:
    vector_centroid += model[e]
if len(concepts[c]['entities'])!=0:
    vector_centroid /= len(concepts[c]['entities'])

for i in range(len(items)):  # for each candidate
    score1 = 0
    sim1 = cosine_similarity([model[items[i]], vector_centroid])[0, 1]
    sim2 = 0
    for j in range(len(concepts[c]['entities'])):
        score1 += sim(can_matrix[i], concepts[c]['m'][j], pattern_list)
        sim2 += cosine_similarity([model[items[i]], model[concepts[c]['entities'][j]]])[0, 1]

    if len(concepts[c]['entities'])!=0:
        score1 /= len(concepts[c]['entities'])
    if len(concepts[c]['entities']) !=0:
        score2 = (sim1 + sim2 / len(concepts[c]['entities'])) / 2
    else:
        score2 = sim1

    # sg_dic[items[i]] = score1
    # vec_dic[items[i]] = score2
    total_dic[items[i]] = (score1 + score2) / 2

# sort according to score1 and score2

# sg_sorted = sorted(sg_dic.items(), key=lambda d: d[1], reverse=True)
# vec_sorted = sorted(vec_dic.items(), key=lambda d: d[1], reverse=True)

# for it in items:
#     index1 = sg_sorted.index((it, sg_dic[it])) + 1  # the rank in sg_sorted list
#     index2 = vec_sorted.index((it, vec_dic[it])) + 1  # the rank in vec_sorted list
#     score = 1 / index1 + 1 / index2
#     total_dic[it] = score
total_sorted = sorted(total_dic.items(), key=lambda d: d[1], reverse=True)
rankfile = open('./%s_%s_rank.txt' % (field, c), 'w', encoding='utf-8')
rankfile.write(total_sorted[0][0])
rankfile.write('  ')
rankfile.write(str(total_sorted[0][1]))
for i in range(1, len(total_sorted)):
    rankfile.write('\n')
    rankfile.write(total_sorted[i][0])
    rankfile.write('  ')
    rankfile.write(str(total_sorted[i][1]))

rankfile.close()
print("==========Finish rank %s==========" % c)

