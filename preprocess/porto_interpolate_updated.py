import numpy as np
import pandas as pd
from tqdm import tqdm
import os
import gc
import datetime
"""
对轨迹按照给定的采样率进行插值 (分为每一天进行输出，节省内存)%
对所有轨迹点按照时间进行排序并写出到location的txt文件,每个轨迹点在taxi_by_id中记录为
[trj_id date lon lat timestamp1 timestamp2]
taxi_porto_by_id中共有1704695条不同的轨迹,单核处理需用时两小时
读取taxi_porto_by_id文件夹,里面每个txt是一个taxi的轨迹
生成porto_%d 指定采样率的轨迹文件,每个txt为一个月的轨迹


2022/4/12生成 将所有时间戳变为一天以内
"""


def time2ts(cur_time):
    """
    时间格式转换为时间戳
    :param cur_time: 13:33:52
    :return:
    """
    h, m, s = [int(i) for i in cur_time.split(":")]
    timestamp = h * 3600 + m * 60 + s
    return timestamp



def ts2time(timestamp):
    """
    时间戳转换为具体的时间格式字符串
    :param timestamp:
    :return:
    """
    h = timestamp // 3600
    m = (timestamp - 3600*h) // 60
    s = timestamp - 3600*h - 60*m
    return "%02d:%02d:%02d" % (h, m, s)


# P.raw_trips[0] = [[1, '2008-02-02 15:36:08', 116.51172, 39.92123, 221553, 56168] ,]
class Interpolation:

    def __init__(self, read_txt_size, sr):
        # 经度范围
        self.lon_range = [-8.735, -8.156]
        # 维度范围
        self.lat_range = [40.953, 41.307]
        # 总共读取的taxi txt文本数
        self.read_txt_size = read_txt_size
        # taxi 文本文件路径
        self.file_dir = os.path.join("/home/like/data/taxi_porto_by_id1")
        # [经纬度坐标, 转化的时间戳]
        self.raw_trips = []
        # 插值后的所有轨迹
        self.all_pad_trips = []
        # 插值采样率
        self.sr = sr
        

    def read_files(self):
        """
        从一定数目的taxi txt轨迹文本中读取一批轨迹信息, 存储到 raw_trjs 中并返回

        :return: trjs_raw 为固定数目的一批txt文件，在经纬度范围内，每条记录为同一天之内的轨迹，且满足轨迹最小长度限制
        Porto: 将同一个月的轨迹放在同一个txt文件
        """
        # 获取所有taxi轨迹文本, 并根据文件名进行排序， 按批次读取一批固定数目文件夹下的轨迹txt文本
        all_file_list = os.listdir(self.file_dir)
        all_file_list.sort(key=lambda x: int(x[:-4]))
        read_size = min(self.read_txt_size, len(all_file_list))
        all_file_list = all_file_list[:read_size]
        print("Raw trajecto")
        for ii in tqdm(range(len(all_file_list)), desc='读取轨迹并存储原始轨迹'):
            if ii % 10000 == 0: print("进度: %d / %d" % (ii, len(all_file_list)))
            file = all_file_list[ii]
            # 每个sing_data为一个txt文本转化的DataFrame结构
            single_data = pd.read_csv(os.path.join(self.file_dir, file), names=['id', 'times', 'longitude', 'latitude'],
                                      header=None)
            single_data = single_data[self.lon_range[0] <= single_data.longitude]
            single_data = single_data[single_data.longitude <= self.lon_range[1]]
            single_data = single_data[self.lat_range[0] <= single_data.latitude]
            single_data = single_data[single_data.latitude <= self.lat_range[1]]
            # 将每个txt转化的DataFrame结构全部添加到all_data中
            ts = []  # 每天相对时间
            for cur_time in single_data['times']:
                cur_time = cur_time[11:]
                day_ts = time2ts(cur_time)
                ts.append(day_ts)
            single_data['timestamp'] = ts
            single_data['id'] = ii
            # 排除超过24小时的轨迹点
            self.raw_trips.append(np.array(single_data).tolist())
        # raw_trips[0] = [[1, '2008-02-02 15:36:08', 116.51172, 39.92123, 221553, 56168] ,]
        trj_size = len(self.raw_trips)
        print("共有轨迹%d"%len(self.raw_trips))
        return self.raw_trips

    def padding_one(self, trip):
        """
        输入一条轨迹， 输出固定采样率 线性插值补全的 的轨迹
        :param trip: 轨迹
        :param sr: 采样率 (s)
        :return:
        """
        # 1. 将原轨迹的时间变为最接近的采样点上的时间
        for ii in range(len(trip)):
            point = trip[ii]
            # 改变时间戳
            point[-1] = (point[-1]//self.sr)*self.sr
            # 改变时间点
            point[1] = point[1][:11]+ts2time(point[-1])
        if len(trip) <= 10:
            # 轨迹长度太短, 不进行插值, 但是要把时间线对其到最近的采样点
            # 放到处理函数之后
            return trip
        trip_id = trip[0][0]
        # 2. 删除重复采样的时间点，防止重复插值
        no_duplicate_trip = []
        for ii, point in enumerate(trip):
            if ii == 0:
                no_duplicate_trip.append(point)
            if ii > 0 and point[-1] != no_duplicate_trip[-1][-1]:
                no_duplicate_trip.append(point)
        # 3. 对轨迹进行插值
        pad_trip = []
        for ii in range(len(no_duplicate_trip)-1):
            cur_point = no_duplicate_trip[ii]
            next_point = no_duplicate_trip[ii+1]
            pad_trip.append(cur_point)
            # 如果是跨两天的轨迹，不进行插值
            if cur_point[1][:11] == next_point[1][:11]:
                lon_diff = next_point[2] - cur_point[2]
                lat_diff = next_point[3] - cur_point[3]
                ts_diff = next_point[-1] - cur_point[-1]
                # 需要插值的个数
                insert_nums = ts_diff//self.sr-1

                for j in range(insert_nums):
                    insert_ts = int(cur_point[-1]) + (j+1)*self.sr
                    insert_time = cur_point[1][:11]+ts2time(insert_ts)
                    insert_lon = cur_point[2] + (j+1)/(insert_nums+1)*lon_diff
                    insert_lat = cur_point[3] + (j+1)/(insert_nums+1)*lat_diff
                    pad_trip.append([trip_id, insert_time, insert_lon, insert_lat, insert_ts])
            # pad_trip.append(next_point) 下一个点不用再添加，否则会重复添加
        return pad_trip

    def padding_all(self):
        """
        以一定的采样率补全所有轨迹
        :param sr: 采样率
        :return: 以一定采样率补全的所有轨迹
        """
        # 读取原始轨迹
        days_trips = self.read_files()
        self.all_pad_trips = []
        # for trip in tqdm(days_trips, desc='轨迹插值至统一采样率'):
        print("轨迹插值至统一采样率")
        ii = 0
        for trip in days_trips:
            self.all_pad_trips.extend(self.padding_one(trip))
            ii += 1
            if ii % 100000 == 0:
                print("进度: %d / %d"%(ii, len(days_trips)))
        # 按照时间戳进行排序
        self.write_day_stream()

    def write_day_stream(self):
        """
        一次写出一天的排序轨迹点
        :param day:
        :return:
        """
        # self.all_pad_trips中存储的是排序后的location
        self.all_pad_trips.sort(key=lambda point: point[-1])
        # 对排序后的轨迹点写入txt文件，10K条Beijing轨迹需要10 min 左右（采样率为100）
        dir_path = "/home/like/data/contact_tracer/porto11_%d" % self.sr
        if not os.path.exists(dir_path):
            os.mkdir(dir_path)
        point_size = len(self.all_pad_trips)
        for part in range(0, 12):
            f = open(dir_path + "/%d_%d.txt" % (part, len(self.all_pad_trips)), "w")
            # for t_id, date, lon, lat, ts in tqdm(self.all_pad_trips[part*(point_size//12):(part+1)*(point_size//12)], desc='正在写出轨迹流文件'):
            print("正在写出轨迹流文件%d"%part)
            for t_id, date, lon, lat, ts in self.all_pad_trips[part * (point_size // 12):(part + 1) * (point_size // 12)]:
                f.writelines("%08d %s %f %f %d" % (t_id, date, lon, lat, ts))
                f.writelines("\n")
            f.close()


if __name__ == "__main__":
    P = Interpolation(20000000, 5)






