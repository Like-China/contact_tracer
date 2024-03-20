import numpy as np
import pandas as pd
from tqdm import tqdm
import os
import gc, random
"""
For the txt files in taxi_porto, interpolate each trj to unified sampling rate
1. get all trajectories of all moving objects by days
2. pad all trajectories to unfied sampling rates, split trajectories to the same length (same number of timestamps)
re-number these fixed-length trajectories
3. write all locations with name ts_objNB
beijing-100w moving objects, 100 timestamps, time cost 502s
beijing-100w moving objects, 20 timestamps, time cost 1200s
[obj_id lon lat relative_ts]
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



#  [[1, '2008-02-02 15:36:08', 116.51172, 39.92123, ts] ,]
class Interpolation:

    def __init__(self, name, load_path, save_path, read_txt_size, sr):
        self.save_path = save_path
        self.name = name
        if name == 'beijing':
            self.lon_range = [116.25, 116.55]
            self.lat_range = [39.83, 40.03]
        else:
            self.lon_range = [-8.735, -8.156]
            self.lat_range = [40.953, 41.307]
        self.read_txt_size = read_txt_size
        self.file_dir = load_path
        self.raw_trips = []
        # all padding trips
        self.all_pad_trips = []
        self.sr = sr

    def read_files(self):
        """
        load all raw trips, store in self.raw_trips
        each trip in self.raw_trips is a trajectory of a moving object in the same day
        """
        all_file_list = os.listdir(self.file_dir)
        print("total number of files/raw moving objects: ", len(all_file_list))
        all_file_list.sort(key=lambda x: int(x[:-4]))
        read_size = min(self.read_txt_size, len(all_file_list))
        all_file_list = all_file_list[:read_size]
        
        for ii in tqdm(range(len(all_file_list)), desc='read and store raw trajectories in memory...'):
            file = all_file_list[ii]
            # txt to DataFrame
            single_data = pd.read_csv(os.path.join(self.file_dir, file), names=['id', 'days', 'longitude', 'latitude'],
                                      header=None)
            single_data = single_data[self.lon_range[0] <= single_data.longitude]
            single_data = single_data[single_data.longitude <= self.lon_range[1]]
            single_data = single_data[self.lat_range[0] <= single_data.latitude]
            single_data = single_data[single_data.latitude <= self.lat_range[1]]
            days = []
            ts = []  
            for cur_time in single_data['days']:
                day = 400*int(cur_time[2:4])+30*int(cur_time[5:7])+int(cur_time[8:10])
                cur_time = cur_time[11:]
                day_ts = time2ts(cur_time)
                ts.append(day_ts)
                days.append(day)
            single_data['timestamp'] = ts
            single_data['days'] = days
            day_groups = single_data.groupby('days')
            for key, df in day_groups:
                self.raw_trips.append(np.array(df).tolist())
        print("The number of raw trajectories: ", len(self.raw_trips))
        # for trip in self.raw_trips:
        #     print(trip, len(trip))
        # print(self.raw_trips[0])
        # self.raw_trips[0] a MO's trip in the same day

    def padding_one(self, trip):
        # trip [[1.0, 2.0, 116.51172, 39.92123, 56168.0], ...]
        for ii in range(len(trip)):
            point = trip[ii]
            point[-1] = (point[-1]//self.sr)*self.sr
        if len(trip) <= 5:
            return trip
        trip_id = trip[0][0]
        no_duplicate_trip = []
        for ii, point in enumerate(trip):
            if ii == 0:
                no_duplicate_trip.append(point)
            if ii > 0 and point[-1] != no_duplicate_trip[-1][-1]:
                no_duplicate_trip.append(point)
        pad_trip = []
        for ii in range(len(no_duplicate_trip)-1):
            cur_point = no_duplicate_trip[ii]
            next_point = no_duplicate_trip[ii+1]
            pad_trip.append(cur_point)
            if cur_point[1] == next_point[1]:
                lon_diff = next_point[2] - cur_point[2]
                lat_diff = next_point[3] - cur_point[3]
                ts_diff = next_point[-1] - cur_point[-1]
                insert_nums = int(ts_diff//self.sr-1)
                for j in range(insert_nums):
                    insert_ts = int(cur_point[-1]) + (j+1)*self.sr
                    insert_lon = cur_point[2] + (j+1)/(insert_nums+1)*lon_diff
                    insert_lat = cur_point[3] + (j+1)/(insert_nums+1)*lat_diff
                    pad_trip.append([trip_id, cur_point[1], insert_lon, insert_lat, insert_ts])
        return pad_trip

    def padding_all(self, cut_length, max_obj_num):
        self.read_files()
        self.all_pad_trips = []
        obj_id = 0
        random.shuffle(self.raw_trips)
        trj_num = 0
        for trip in tqdm(self.raw_trips, desc='trajectory padding...'):
            trj_num += 1
            # the original sr of Porto is 15s, unchanged
            if self.name == "porto" and self.sr == 15:
                pad_trip = trip
            else:
                pad_trip = self.padding_one(trip)
            cut_trip = []
            index = 0
            # point [1.0, 2.0, 116.5220985, 39.91579383333333, 75020]
            # reorganize
            for point in pad_trip:
                cut_trip.append([obj_id, point[2],point[3], index*self.sr])
                index += 1
                if index == cut_length:
                    self.all_pad_trips.extend(cut_trip)
                    cut_trip = []
                    index = 0
                    obj_id += 1
                    if obj_id >= max_obj_num:
                        break
            if obj_id >= max_obj_num:
                    break
        del self.raw_trips
        gc.collect()
        print("The number of split trajectories by days: ", trj_num)
        print("Generated moving object number: ", obj_id)
        # for point in self.all_pad_trips:
        #     print(point)

    def write(self, cut_length, max_obj_num):
        # all_pad_trips[0]  == 1032, 116.41758766119221, 40.021166475060824, 290
        self.all_pad_trips.sort(key=lambda point: point[-1])
        dir_path = self.save_path
        if not os.path.exists(dir_path):
            os.mkdir(dir_path)
        f = open(dir_path+"/%s%d_%d.txt" % (self.name,cut_length, max_obj_num), "w")
        for t_id, lon, lat, ts in tqdm(self.all_pad_trips, desc='Padded trajectory writing...'):
            f.writelines("%05d %f %f %d" % (t_id, lon, lat, ts))
            f.writelines("\n")
        f.close()


if __name__ == "__main__":
    name = "porto"
    load_path = "/home/like/data/taxi_porto_by_id/"
    # the  sampling interval
    sr = 15  
    # the number of reading trj-file (Porto has 1704695 raw MOs Beijing is 10347)
    max_read_num = 500000 #1704695
    save_path = "/home/like/data/contact_tracer/"
    if name == "beijing":
        load_path = "/home/like/data/taxi_log_2008_by_id/"
        sr = 10
        save_path = "/home/like/data/contact_tracer/"
        max_read_num = 10357
    import time
    start_time = time.time()
    P = Interpolation(name, load_path, save_path, max_read_num, sr)
    
    cut_length = 20
    max_obj_num = 1000000
    P.padding_all(cut_length, max_obj_num)
    P.write(cut_length, max_obj_num)
    print("time consuming: ", time.time()-start_time)





