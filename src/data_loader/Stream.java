package data_loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import trace.Settings;

/*
给定某一天的时间有序的位置点文本路径
该文本每一行记录信息为 id date time lon lat timestamp, 以空格间隔
如 00053 2008-02-08 00:00:00 116.410720 39.990820 0
 */

public class Stream {
	// 当前读取的txt_path
	String txt_path;
	// 存储当前时刻读取到的位置点索引, 用于获取批量数据
	public int current_index;
	// 文本中总的位置点个数
	public int location_totalLocNum;
	// 位置点个数
	public int totalLocNum;
	// 维护一个reader
	BufferedReader reader;
	// 由于顺序读取会少读每个时刻的第一个位置点，因此维护下一个第一个位置点
	Location first_loc = null;
	// 维护一个随机数
	Random r = new Random(0);
	// 记录最大ID，minID
	public int minID = 10000;
	public int maxID = -1;
	
	public Stream(String txt_path) {
		this.txt_path = txt_path;
		current_index = 0;
		location_totalLocNum = Integer.parseInt(txt_path.split("_")[2].replace(".txt", ""));
		// TODO Auto-generated constructor stub
		try {
			File file = new File(txt_path);
			reader = new BufferedReader(new FileReader(file));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	
	/**
	 *   读取一个时刻的位置点, 记录到ArrayList<Location> batch中并返回
	 */
	public ArrayList<Location> read_batch()
	{	
		ArrayList<Location> location_batch = new ArrayList<>();
		if (first_loc != null)
		{	
			location_batch.add(first_loc);
			first_loc = null;
		}
		// 记录当前的时间戳
		int current_timestampe = -100;
		try {
			String lineString;
			int count = 0;
			while((lineString = reader.readLine()) != null)
			{	
				int id = Integer.parseInt(lineString.split(" ")[0]);
				maxID = maxID>id?maxID:id;
				minID = minID<id?minID:id;
				String date = lineString.split(" ")[1];
				String time = lineString.split(" ")[2];
				float lon = Float.parseFloat(lineString.split(" ")[3]);
				float lat = Float.parseFloat(lineString.split(" ")[4]);
				int ts = Integer.parseInt(lineString.split(" ")[5]);
				if(count == 0) {
					current_timestampe = ts;
				}
				// 如果下一位置点读到的不再是当前时刻时间戳，直接跳出循环
				if(ts != current_timestampe)
				{
					first_loc = new Location(id, date, time, lon, lat, ts);
					break;
				}else {
					// 扩张数据位置点规模 （2022/5/13）
					// 添加原位置点
					location_batch.add(new Location(id, date, time, lon, lat, ts));
					// 添加扩张数据点
					if(Settings.expNum>1)
					{
						for(int i=1;i<Settings.expNum;i++)
						{
							location_batch.add(new Location(2000000+id*Settings.expNum+i, date, time, (float)(lon+0.0005), (float)(lat+0.0005), ts));
						}
					}
					count++;
				}
			}
			current_index += count;
			if(location_batch.isEmpty())
			{
				current_index = 0;
				reader.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return location_batch;
	}
}



