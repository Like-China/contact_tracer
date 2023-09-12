package data_loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import trace.Settings;

/*
Given a txt file with Line infromation: id date time lon lat timestamp
example: 00053 2008-02-08 00:00:00 116.410720 39.990820 0
 */

public class Stream {
	String txt_path;
	public int current_index;
	public int location_totalLocNum;
	public int totalLocNum;
	BufferedReader reader;
	Location first_loc = null;
	Random r = new Random(0);
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

	public ArrayList<Location> read_batch() {
		ArrayList<Location> location_batch = new ArrayList<>();
		if (first_loc != null) {
			location_batch.add(first_loc);
			first_loc = null;
		}
		int current_timestampe = -100;
		try {
			String lineString;
			int count = 0;
			while ((lineString = reader.readLine()) != null) {
				int id = Integer.parseInt(lineString.split(" ")[0]);
				maxID = maxID > id ? maxID : id;
				minID = minID < id ? minID : id;
				String date = lineString.split(" ")[1];
				String time = lineString.split(" ")[2];
				float lon = Float.parseFloat(lineString.split(" ")[3]);
				float lat = Float.parseFloat(lineString.split(" ")[4]);
				int ts = Integer.parseInt(lineString.split(" ")[5]);
				if (count == 0) {
					current_timestampe = ts;
				}
				if (ts != current_timestampe) {
					first_loc = new Location(id, date, time, lon, lat, ts);
					break;
				} else {
					location_batch.add(new Location(id, date, time, lon, lat, ts));
					if (Settings.expNum > 1) {
						for (int i = 1; i < Settings.expNum; i++) {
							location_batch.add(new Location(2000000 + id * Settings.expNum + i, date, time,
									(float) (lon + 0.0005), (float) (lat + 0.0005), ts));
						}
					}
					count++;
				}
			}
			current_index += count;
			if (location_batch.isEmpty()) {
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
