import numpy as np
import pandas as pd
from tqdm import tqdm
import os
import gc
"""
操作taxi_log_2008_by_id中的txt文件，对北京数据集进行插值
对轨迹按照给定的采样率进行插值 (分为每一天进行输出，节省内存)%
对所有轨迹点按照时间进行排序并写出到txt文件，每个轨迹点记录为
[trj_id date lon lat timestamp1 timestamp2]
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

    def __init__(self, load_path, save_path, read_txt_size, sr):
        self.save_path = save_path
        # 经度范围
        self.lon_range = [116.25, 116.55]
        # 维度范围
        self.lat_range = [39.83, 40.03]
        # 总共读取的taxi txt文本数
        self.read_txt_size = read_txt_size
        # taxi 文本文件路径
        self.file_dir = load_path
        # [经纬度坐标, 转化的时间戳]
        self.raw_trips = []
        # 插值后的所有轨迹
        self.all_pad_trips = []
        # 插值采样率
        self.sr = sr
        # 按照采样率插值所有轨迹
        self.padding_all()

    def read_files(self):
        """
        从一定数目的taxi txt轨迹文本中读取一批轨迹信息, 存储到 raw_trjs 中并返回

        :return: trjs_raw 为固定数目的一批txt文件，在经纬度范围内，每条记录为同一天之内的轨迹，且满足轨迹最小长度限制
        """
        # 获取所有taxi轨迹文本, 并根据文件名进行排序， 按批次读取一批固定数目文件夹下的轨迹txt文本
        all_file_list = os.listdir(self.file_dir)
        print("total number of files: ", len(all_file_list))
        all_file_list.sort(key=lambda x: int(x[:-4]))
        read_size = min(self.read_txt_size, len(all_file_list))
        all_file_list = all_file_list[:read_size]
        for ii in tqdm(range(len(all_file_list)), desc='read and store raw trajectories in memory...'):
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
                day = int(cur_time[8:10])
                cur_time = cur_time[11:]
                day_ts = time2ts(cur_time)
                ts.append(day_ts)
            single_data['timestamp'] = ts
            # 排除超过24小时的轨迹点
            self.raw_trips.append(np.array(single_data).tolist())
        #  对于所有的轨迹，按天分为几个列表 (beijing路网共有七天)
        all_days_trip = {}
        for ii in range(2, 9):
            all_days_trip[ii] = []
        for trip in self.raw_trips:
            head, tail = 0, 0
            for ii in range(len(trip)-1):
                cur_point = trip[ii]
                next_point = trip[ii + 1]
                day = int(cur_point[1][8:10])
                if cur_point[1][:11] != next_point[1][:11]:
                    tail = ii+1
                    all_days_trip[day].append(trip[head:tail])
                    head = tail
                # 记录最后一天，因为最后一天不能有区分
                if ii == len(trip)-2:
                    tail = ii+1
                    all_days_trip[day].append(trip[head:tail])
        del self.raw_trips
        gc.collect()
        return all_days_trip

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
        # load trajectories
        days_trips = self.read_files()
        for ii in range(2, 9):
            day_trips = days_trips[ii]
            self.all_pad_trips = []
            for trip in tqdm(day_trips, desc='trajectory padding...'):
                self.all_pad_trips.extend(self.padding_one(trip))
            self.write_day_stream(ii)

    def write_day_stream(self, day):
        """
        一次写出一天的排序轨迹点
        :param day:
        :return:
        """
        self.all_pad_trips.sort(key=lambda point: point[-1])
        # 对排序后的轨迹点写入txt文件，10K条Beijing轨迹需要10 min 左右（采样率为100）
        dir_path = self.save_path
        if not os.path.exists(dir_path):
            os.mkdir(dir_path)
        f = open(dir_path+"/%d_%d.txt" % (day, len(self.all_pad_trips)), "w")
        for t_id, date, lon, lat, ts in tqdm(self.all_pad_trips, desc='Padded trajectory writing...'):
            f.writelines("%05d %s %f %f %d" % (t_id, date, lon, lat, ts))
            f.writelines("\n")
        f.close()


if __name__ == "__main__":
    load_path = "/home/like/data/taxi_log_2008_by_id/"
    max_read_num = 20000 #读取的最大txt文本数目
    sr = 10  # 平衡的采样率
    save_path = "/home/like/data/contact_tracer/beijing55%d" % sr
    import time
    start_time = time.time()
    P = Interpolation(load_path, save_path, max_read_num, sr)
    print("time consuming: ", time.time()-start_time)





