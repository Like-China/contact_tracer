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
		File[] files = new File(Settings.dataPath).listFiles();
		// 2. 创建一个Tracer实例对象，处理实时读取到的轨迹位置点
		AGP_Tracer tracer = new AGP_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. 初始化一批query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. 记录统计数据
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> AGP_res = new HashMap<>();
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
				System.out.printf("\n%s %s return locations %d", batch.get(0).date, batch.get(0).time, batch.size());
				// AGP查询
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> AGP_cases = tracer.trace(batch, tsNum);
				// 添加新增病例
				if(!AGP_cases.isEmpty())
				{
					AGP_res.put(tsNum, AGP_cases);
				}
				long endTime = System.currentTimeMillis();
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


		// 展示实验结果
		System.out.printf("共处理%d个位置， %d个时刻 " , locNum, tsNum);
		System.out.println("用时:  " + runtime + " 平均用时:  " + (double)runtime/tsNum);
		HashSet<Integer>  AGP_cases = new HashSet<>();
		for(Integer key: AGP_res.keySet())
		{
			AGP_cases.addAll(AGP_res.get(key));
		}
		System.out.println("共发现cases of exposure: " + AGP_cases.size());
	}
	
}
