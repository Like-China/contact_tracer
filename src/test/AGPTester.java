package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data_loader.Location;
import data_loader.Stream;
import trace.AGP_Tracer;
import trace.Settings;
import trace.Util;

public class AGPTester {
	
	public static void main(String[] args) {
		// 1. 获取该采样率下的所有轨迹点文件名
		File[] files = new File("E:/data/contact_tracer/" + Settings.city_name + Settings.sr).listFiles();
		// 2. 创建一个Tracer实例对象，处理实时读取到的轨迹位置点
		AGP_Tracer tracer = new AGP_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. 初始化一批query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. 记录统计数据
		long AGP_time = 0;
		int location_num = 0;
		int dayNum = 0;
		int current_ts = 0;
		HashMap<Integer, ArrayList<Integer>> AGP_res = new HashMap<Integer, ArrayList<Integer>>();
		// 4. 开始流形式查询
		for(File f:files)
		{	
			Stream stream  = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // 流式读取当天一个时刻的位置点
			while (batch != null && !batch.isEmpty())
			{
				if (batch.get(0).ts % Settings.sr != 0)
				{
					continue; // 如果不在采样时间点上，不处理
				}
				location_num += batch.size();
				System.out.printf("\n%s %s return locations %d", batch.get(0).date, batch.get(0).time, batch.size());
				// AGP查询
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> AGP_cases = tracer.trace(batch, current_ts);
				// 添加新增病例
				if(!AGP_cases.isEmpty())
				{
					AGP_res.put(current_ts, AGP_cases);
				}
				long endTime = System.currentTimeMillis();
				AGP_time += endTime-startTime;
				current_ts += 1;
				batch = stream.read_batch();
			} // End While Loop
			dayNum += 1;
			if (dayNum >= Settings.days) {
				break;
			}
		} // End For Loop


		// 展示实验结果
		System.out.printf("共处理%d个位置， %d个时刻 " , location_num, current_ts);
		System.out.println("用时:  " + AGP_time + " 平均用时:  " + (double)AGP_time/current_ts);
		HashSet<Integer>  AGP_cases = new HashSet<Integer>();
		for(Integer key: AGP_res.keySet())
		{
			AGP_cases.addAll(AGP_res.get(key));
		}
		System.out.println("共发现cases of exposure: " + AGP_cases.size());
	}
	
}
