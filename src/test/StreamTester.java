package test;
import java.io.File;
import java.util.ArrayList;
import trace.Util;
import data_loader.Location;
import data_loader.Stream;


public class StreamTester {
	

	
	public static void main(String[] args) {
		
		
		String path = "D:/data/beijing50";
		//  获取该采样率下的所有轨迹点文件名
		File[] files = new File(path).listFiles();
		for(File f:files)
		{	
			long t1 = System.currentTimeMillis();
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // 读取当天所有位置点
			while(!batch.isEmpty())
			{
				System.out.println(batch.get(0));
				System.out.println(batch.get(batch.size()-1));
				System.out.println("******************");
				batch = stream.read_batch();
				Util.sleep(1000);
			}
			long t2 = System.currentTimeMillis();
			System.out.println("用时: ");
			System.out.println(t2-t1);
//			if(batch.isEmpty()) break;
		}
		
	}
}
