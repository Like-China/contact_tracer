# 读取porto数据集文件
import pandas
import datetime
from beijing_interpolate import time2ts, ts2time
from tqdm import tqdm
import os

# df.columns= ['TRIP_ID', 'CALL_TYPE', 'ORIGIN_CALL', 'ORIGIN_STAND', 'TRIP_ID',
# 'TIMESTAMP', 'DAY_TYPE', 'MISSING_DATA', 'POLYLINE']
"""
将porto.csv文件转化为以出租车id为例排序的txt文本文件
porto中的每条轨迹是带有起始位置点时间戳的
按照beijing_taxi_by_id里面的txt文本格式
csv文件中初始1710670条轨迹， 经过处理得到1704695条轨迹
处理需要用时两小时左右， 未限制经纬度范围

2022/4/12生成 将所有时间戳变为一天以内
"""
def process_porto():
    # 每一行就是一个轨迹
    df = pandas.read_csv('/home/Like/data/porto.csv')  
    df = df[['TIMESTAMP', 'POLYLINE']]
    df = df.sort_values(by=['TIMESTAMP'], ascending=True)
    df = df.reset_index(drop=True)
    # df['TRIP_ID'] = df['TRIP_ID'] - 1372636853620000379
    return df


def write_to_txt_by_id(df):
    dir_path = "/home/Like/data/contact_tracer/taxi_porto_by_id"
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)
    for ii in tqdm(range(len(df.POLYLINE))):
        trip = df.POLYLINE[ii]
        if len(trip) < 5:
            continue
        lon_lat_str = trip.replace("[", "").replace("]", "").split(",")
        lons = [float(lon_lat_str[j]) for j in range(len(lon_lat_str)) if j % 2 == 0]
        lats = [float(lon_lat_str[j]) for j in range(len(lon_lat_str)) if j % 2 != 0]
        date = datetime.datetime.fromtimestamp(df.TIMESTAMP[ii])
        data_path = "%s/%d.txt" % (dir_path, ii)
        f = open(data_path, "a+")
        ts = time2ts(str(date.time()))
        for n in range(len(lons)):
            real_ts = ts + 15*n
            real_date = str(date)[:11]+ts2time(real_ts)
            f.writelines("%s,%s,%f,%f\n" % (ii, real_date, lons[n], lats[n]))
        f.close()


if __name__ == "__main__":
    df = process_porto()
    date = datetime.datetime.fromtimestamp(df.TIMESTAMP[0])
    print(df.POLYLINE[0])
    # write_to_txt_by_id(df)