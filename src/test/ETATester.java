package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import data_loader.Location;
import data_loader.Stream;
import trace.ETA_Tracer;
import trace.Settings;
import trace.Util;

public class ETATester {
	
	public static void main(String[] args) {
		// 1. 获取该采样率下的所有轨迹点文件名
		File[] files = new File("D:/data/"+"beijing"+Settings.sr).listFiles();
		// 2. 创建一个Tracer实例对象，处理实时读取到的轨迹位置点
		ETA_Tracer tracer = new ETA_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. 初始化一批query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. 记录一些统计数据
		long ETA_time = 0;
		int location_num = 0;
		int current_day = 0;
		int current_ts = 0;
		HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<Integer, ArrayList<Integer>>();
		// 4. 开始流形式查询
		for(File f:files)
		{	
			Stream stream  = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // 流式读取当天一个时刻的位置点
			while (batch != null && !batch.isEmpty())
			{  
				if(batch == null || batch.isEmpty()) break; // 如果当天时间流已经结束，则跳出循环进行下一天的查询，感染时长仍然会累计
				if (batch.get(0).ts % Settings.sr != 0) continue; // 如果不在采样时间点上，不处理
				location_num += batch.size();
				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				
				// ETA查询
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> ETA_cases = tracer.trace(batch);
				if(!ETA_cases.isEmpty()) ETA_res.put(current_ts, ETA_cases);
				long endTime = System.currentTimeMillis();
				System.out.println("用时: "+ (endTime-startTime));
				ETA_time += endTime-startTime;
				current_ts += 1;
				batch = stream.read_batch();
			} // End While Loop
			current_day += 1;
			if (current_day == 2) break;
		} // End For Loop
		
		// 展示一些结果
		System.out.println("共处理位置点数目: " + location_num);
		System.out.println("处理用时: "+ ETA_time);
	}
	
}
