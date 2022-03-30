package test;
import java.io.File;
import java.util.ArrayList;

import trace.Settings;
import trace.Util;
import data_loader.Location;
import data_loader.Stream;

/*
验证位置流读取的准确性
 */

public class StreamTester {

	public static void main(String[] args) {
		//  获取该采样率下的所有轨迹点文件名
		File[] files = new File(Settings.dataPath).listFiles();
		if (files == null)
		{
			System.out.println("No valid files found!!");
			return;
		}
		for(File f:files)
		{	
			long t1 = System.currentTimeMillis();
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // 读取当天所有位置点
			System.out.println("First location of current day: "+batch.get(0));
			while(!batch.isEmpty())
			{
				System.out.println("Number of locations at current timestamp: "+batch.size());
				System.out.println("First location of current timestamp: \n"+batch.get(0));
				System.out.println("Last location of current timestamp: \n"+batch.get(batch.size()-1));
				System.out.println("******************");
				batch = stream.read_batch();
				Util.sleep(1);
			}
			long t2 = System.currentTimeMillis();
			System.out.println("用时: ");
			System.out.println(t2-t1);
			if(batch.isEmpty())
			{
				break;
			}
		}
		
	}
}
