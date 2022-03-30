package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data_loader.Location;
import data_loader.Stream;
import trace.EGP_Tracer;
import trace.Settings;
import trace.Util;

public class EGPTester {
	
	public static void main(String[] args) {
		// 1. 获取该采样率下的所有轨迹点文件名
		File[] files = new File(Settings.dataPath).listFiles();
		// 2. 创建一个Tracer实例对象，处理实时读取到的轨迹位置点
		EGP_Tracer tracer = new EGP_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. 初始化一批query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. 记录一些统计数据
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGP_res = new HashMap<>();
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
				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				// EGP查询
				long startTime = System.currentTimeMillis();
//				ArrayList<Integer> EGP_cases = tracer.trace_no_checking(batch);
				ArrayList<Integer> EGP_cases = tracer.trace(batch);
				if(!EGP_cases.isEmpty()) EGP_res.put(tsNum, EGP_cases);
				long endTime = System.currentTimeMillis();
				System.out.println("用时: "+ (endTime-startTime));
				runtime += endTime-startTime;
				tsNum += 1;
				batch = stream.read_batch();
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays)
			{
				break;
			}
		} // End 'For' Loop


		// 展示一些结果
		System.out.printf("共处理%d个位置， %d个时刻 " , locNum, tsNum);
		System.out.println("用时:  " + runtime + " 平均用时:  " + (double)runtime/tsNum);
		HashSet<Integer>  EGP_cases = new HashSet<>();
		for(Integer key: EGP_res.keySet())
		{
			EGP_cases.addAll(EGP_res.get(key));
		}
		System.out.println("共发现cases of exposure: " + EGP_cases.size());
	}
	
}
