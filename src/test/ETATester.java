package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data_loader.Location;
import data_loader.Stream;
import trace.ETA_Tracer;
import trace.Settings;
import trace.Util;


public class ETATester {
	
	public static void main(String[] args) {
		// 1. 获取该采样率下的所有轨迹点文件名
		File[] files = new File(Settings.dataPath).listFiles();
		// 2. 创建一个Tracer实例对象，处理实时读取到的轨迹位置点
		ETA_Tracer etaTracer = new ETA_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. 初始化一批query 
		etaTracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. 记录一些统计数据
		long etaRuntime = 0; // 运行时间
		int locNum = 0; // 处理的位置点数目
		int dayNum = 0;  // 处理的天数
		int currentTs = 0; // 当前时刻处理的时间戳
		HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<>();
		// 4. 开始流形式查询
		if (files == null)
		{
			System.out.println("No valid files found!!");
			return;
		}
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
				locNum += batch.size();
//				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				// ETA查询
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> ETA_cases = etaTracer.trace(batch);
				if(!ETA_cases.isEmpty()) ETA_res.put(currentTs, ETA_cases);
				long endTime = System.currentTimeMillis();
				etaRuntime += endTime-startTime;
				currentTs += 1;
				batch = stream.read_batch();
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays)
			{
				break;
			}
		} // End 'For' Loop
		
		// 展示一些结果
		System.out.println("共处理位置点数目: " + locNum);
		System.out.println("处理用时: "+ etaRuntime);
		HashSet<Integer> ETA_cases = new HashSet<>();
		for(Integer key: ETA_res.keySet())
		{
			ETA_cases.addAll(ETA_res.get(key));
		}
		System.out.println("共发现cases of exposure: " + ETA_cases.size());

	}
	
}
