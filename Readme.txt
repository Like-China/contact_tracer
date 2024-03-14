# Contact tracer
This implementation follows the content of the article
"Towards Controlling the Transmission of Diseases: Continuous Exposure \\Discovery over Massive-Scale Moving Objects" Published in IJCAI2022


# data_loader
load data from specific filepath
construct classes of locations, infected areas, moving objects, and the data stream

# indexes
grid index

# trace
method implementation of ETA, EGP and AGP

# test
test of of ETA, EGP and AGP

# entrance

sequenceTester.java 
paramater settings are defined by Settings.java
run sequenceTester.main() for method evaluation

% 读取文件的速度较慢，所以应该统一读取 统一评测 beijing 全部读取都需6分钟 Porto (total 237683151 locations, 254299 timestamps) 全部读取都需5分钟
BJ  2 days
total 90382337 locations, 12414 timestamps
runtime: 40893,mean runtime:  3.2941034316094733
total cases of exposure: 418
total number of pre-checking operations / the number of valid: 76 / 76
total time consuming: 142356
4 days
total 208933353 locations, 29692 timestamps
runtime: 106924,mean runtime:  3.601104674659841
total cases of exposure: 1997
total number of pre-checking operations / the number of valid: 535 / 531
total time consuming: 371001
6 days pre-checking true
total 275745350 locations, 46971 timestamps
runtime: 145226,mean runtime:  3.0918226139532905
total cases of exposure: 2319
total number of pre-checking operations / the number of valid: 535 / 531
total time consuming: 484408

6 days pre-checking false
2008-02-07 21:18:10 return locations 3382
total 275745350 locations, 46971 timestamps
runtime: 146533,mean runtime:  3.119648293627983
total cases of exposure: 2319
total number of pre-checking operations / the number of valid: 0 / 0
total time consuming: 494911

296,364,033 and 23,767,470

2024/3/8 引入四叉树，查询结果与暴力查询匹配，虽然效率提升了，但构建索引的时间较长
Algorithm: EGP
name: beijing 	 days: 1 	 sr: 10 	 k: 5  	 epsilon: 2.000000  	 initPatientNum: 100 minMBR: 20
locations: 27886741 , timestamps 3775, runtime: 24443014, mean runtime: 6474.970596, checkNum: 0, validNum: 0, calcCount: 75829, Cases of exposures: 23

Algorithm: EGP#
name: beijing 	 days: 1 	 sr: 10 	 k: 5  	 epsilon: 2.000000  	 initPatientNum: 100 minMBR: 20
locations: 27886741 , timestamps 3775, runtime: 23862643, mean runtime: 6321.229934, checkNum: 0, validNum: 0, calcCount: 75829, Cases of exposures: 23

Algorithm: AGP
name: beijing 	 days: 1 	 sr: 10 	 k: 5  	 epsilon: 2.000000  	 initPatientNum: 100 minMBR: 20
locations: 27886741 , timestamps 3775, runtime: 16063076, mean runtime: 4255.119470, calcCount: 50687, Cases of exposures: 46

Algorithm: ETA
name: beijing 	 days: 1 	 sr: 10 	 k: 5  	 epsilon: 2.000000  	 initPatientNum: 200 minMBR: 20
locations: 27886741 , timestamps 3775, runtime: 1354129, mean runtime: 358.709669, Cases of exposures: 33

QGP scale*10 
tracer.totalCheckNB + "/" + tracer.totalQueryNB
filter后的check次数还是太多了，filter的力度不够，需要进一步调整dbTree的参数
904209162/562896 (把检查次数给降下来)
tracer.cTime + "/" + tracer.fTime + "/" + tracer.sTime
8159/17399/309872 (检查时间也就是refine耗时过长)
total 27886741 locations, 3775 timestamps
runtime: 335594,mean runtime:  88.89907284768212
total cases of exposure: 33
total time consuming: 370162

130398/570263
10984/29/2
total 27886741 locations, 3775 timestamps
runtime: 11394,mean runtime:  3.0182781456953642
total cases of exposure: 33
total time consuming: 45482

2024/3/10 检查beijng days=1, 2，4，6下的ET和EGP输出相同无误

// 模拟一大批轨迹,试验QGP和EGP的表现，再想如何更新数据集

2024/3/13 新增以矩形为输入单元的四叉树，修改该四叉树的retrieve函数
EGP的pre-checking可能会删除一些原本可能感染的样例

2024/3/14 EGP 50000-100000实验指标，QGP需要超过这个指标才行(QGP超不过，QGP2可以)
totalQueryNB/totalCheckNB
420070/1386054
cTime/fTime/sTime
425/24/1215
runtime: 1712,mean runtime:  171.2
      EGP 5w-100w
totalQueryNB/totalCheckNB
464498/35684522
cTime/fTime/sTime
6345/29/20916
total 10000000 locations, 10 timestamps
runtime: 27421,mean runtime:  2742.1
total cases of exposure: 78826
total number of pre-checking operations / the number of valid: 0 / 0
total time consuming: 37307
    EGP 5w-100w 1meters
totalQueryNB/totalCheckNB
483622/15335317
cTime/fTime/sTime
6637/24/9740
total 10000000 locations, 10 timestamps
runtime: 16491,mean runtime:  1649.1
total cases of exposure: 42759
total number of pre-checking operations / the number of valid: 0 / 0
total time consuming: 26774

// 需要注意划分多层后，划分四叉树不再有效果
// 四叉树可能适合更大的图

