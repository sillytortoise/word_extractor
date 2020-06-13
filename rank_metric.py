import gensim
import sys
import numpy as np
import json
from metric_learn import LMNN
from sklearn.metrics.pairwise import cosine_similarity


field=sys.argv[1]
json_file = open('/datamore/cc/entity_extraction/entity_classification/%s_seed_concept_entity.json' %field, encoding='utf-8')
concept_json = json.load(json_file)
concept_info = {}

# 导入word2vec模型
model = gensim.models.KeyedVectors.load_word2vec_format('./wordvector/%s_100.vector' %field, binary=False)

data = {}
data['data'] = np.zeros((0, 100))
data['target'] = np.zeros((0))
data['target_names'] = []
seeds_invocab = []
vocab = open('./result/%s_entity_set_invocab.txt' %field)
content = vocab.readlines()
worddict = [line.rstrip('\n') for line in content]

for c in concept_json:
    data['target_names'].append(c)
    concept_info[c] = {}
    concept_info[c]['rankdict'] = {}
    concept_info[c]['rankfile'] = open('./%s_%s_rank.txt' %(field, c), 'w+', encoding='utf-8')
    concept_info[c]['seeds_invocab'] = []
    concept_info[c]['vector_centroid'] = np.zeros(100)
    for item in concept_json[c]:
        if item in model.vocab:
            tar = list(concept_json.keys()).index(c)
            data['data'] = np.row_stack((data['data'], model[item]))
            data['target'] = np.append(data['target'], tar)
            concept_info[c]['seeds_invocab'].append(item)
            concept_info[c]['vector_centroid'] += model[item]
    concept_info[c]['vector_centroid'] /= len(concept_info[c]['seeds_invocab'])

X = data['data']
Y = data['target']

print("first training")
print("================================================")
lmnn = LMNN(None, k=5, learn_rate=1e-6)
lmnn.fit(X, Y)
print("train finish")
print("================================================")

# for word in worddict:
#     score = {}
#     for c in concept_info:
#         flag = False
#         sim1 = cosine_similarity([model[word], concept_info[c]['vector_centroid']])[0, 1]
#         sim2 = 0
#         for i in range(len(concept_info[c]['seeds_invocab'])):
#             if word == concept_info[c]['seeds_invocab'][i]:
#                 flag = True
#                 continue
#             dis = lmnn.score_pairs([[model[word], model[concept_info[c]['seeds_invocab'][i]]]])[0]
#             sim2 += 1 / dis
#         if flag:
#             score[c] = (sim1 + sim2 / (len(concept_info[c]['seeds_invocab']) - 1)) / 2
#         else:
#             score[c] = (sim1 + sim2 / len(concept_info[c]['seeds_invocab'])) / 2
#     sorted_score = sorted(score.items(), key=lambda d: d[1], reverse=True)
#     if sorted_score[0][1] >= 0.95:
#         concept_info[sorted_score[0][0]]['seeds_invocab'].append(word)
#         data['data'] = np.row_stack((data['data'], model[word]))
#         data['target'] = np.append(data['target'], list(concept_json.keys()).index(sorted_score[0][0]))
#
# X = data['data']
# Y = data['target']
#
# print("second training")
# print("================================================")
# lmnn = LMNN(None, k=10, learn_rate=1e-6)
# lmnn.fit(X, Y)
# print("train finish")
# print("================================================")
#
# for c in concept_info:
#     concept_info[c]["vector_centroid"] = np.zeros(100)
#     for e in concept_info[c]['seeds_invocab']:
#         concept_info[c]["vector_centroid"] += model[e]
#     concept_info[c]["vector_centroid"] /= len(concept_info[c]['seeds_invocab'])

##根据score降序排序
for word in worddict:
    score = {}
    for c in concept_info:
        flag = False
        sim1 = cosine_similarity([model[word], concept_info[c]['vector_centroid']])[0, 1]
        sim2 = 0
        for i in range(len(concept_info[c]['seeds_invocab'])):
            if word == concept_info[c]['seeds_invocab'][i]:
                flag = True
                continue
            dis = lmnn.score_pairs([[model[word], model[concept_info[c]['seeds_invocab'][i]]]])[0]
            sim2 += 1 / dis
        if flag:
            score[c] = (sim1 + sim2 / (len(concept_info[c]['seeds_invocab']) - 1)) / 2
        else:
            score[c] = (sim1 + sim2 / len(concept_info[c]['seeds_invocab'])) / 2
    sorted_score = sorted(score.items(), key=lambda d: d[1], reverse=True)
    concept_info[sorted_score[0][0]]['rankdict'][word] = sorted_score[0][1]

for c in concept_info:
    rankfile = concept_info[c]['rankfile']
    sortedword = sorted(concept_info[c]['rankdict'].items(), key=lambda d: d[1], reverse=True)
    for i in range(len(sortedword)):
        #        print(sortedword[i][0])
        #        print(sortedword[i][1])
        rankfile.write(sortedword[i][0])
        rankfile.write('  ')
        rankfile.write(str(sortedword[i][1]))
        rankfile.write('\n')
    rankfile.close
    print("排序完成，新文件名为：%s_%s_rank.txt！" %(field, c))
    print("================================================")
