package data_loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;


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
	
	public Stream(String txt_path) {
		this.txt_path = txt_path;
		current_index = 0;
		location_totalLocNum = Integer.parseInt(txt_path.split("_")[1].replace(".txt", ""));
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
	 * @param txt_path  2_27886741.txt 后者记录了总的位置点数目
	 */
	public ArrayList<Location> read_batch()
	{	
		ArrayList<Location> batch = new ArrayList<Location>();
		if (first_loc != null)
		{	
			batch.add(first_loc);
			first_loc = null;
		}
		// 记录当前的时间戳
		int current_timestampe = -100;
		try {
			String lineString = null;
			int count = 0;
			while((lineString = reader.readLine()) != null)
			{	
				int id = Integer.parseInt(lineString.split(" ")[0]);
				String date = lineString.split(" ")[1];
				String time = lineString.split(" ")[2];
				float lon = Float.parseFloat(lineString.split(" ")[3]);
				float lat = Float.parseFloat(lineString.split(" ")[4]);
				int ts = Integer.parseInt(lineString.split(" ")[5]);
				if(count == 0) current_timestampe = ts;
				// 如果下一位置点读到的不再是当前时刻时间戳，直接跳出循环
				if(ts != current_timestampe)
				{
					first_loc = new Location(id, date, time, lon, lat, ts);
					break;
				}else {
					batch.add(new Location(id, date, time, lon, lat, ts));
					count++;
				}
			}
			current_index += count;
			if(batch.isEmpty())
			{
				current_index = 0;
				reader.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return batch;
	}
	
	
}



