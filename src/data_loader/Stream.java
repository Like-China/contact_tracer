/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 20:10:54
 */

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
	// the file path that records all location information
	String filePath;
	// the current already loaded number of lines of the file
	public int loadLineNB;
	// the total number of locations, which is noted by the filename
	public int totalLocNB;
	// the file reader
	BufferedReader reader;
	// the first line location that begins from
	Location firstLocation = null;
	// random seed
	Random r = new Random(0);
	// the min/max id among all moving objects
	public int minID = 10000;
	public int maxID = -1;

	/**
	 * Load locations in a stream manner
	 * 
	 * @param filePath the file path that records all location information
	 */
	public Stream(String filePath) {
		this.filePath = filePath;
		loadLineNB = 0;
		totalLocNB = Integer.parseInt(filePath.split("_")[2].replace(".txt", ""));
		// TODO Auto-generated constructor stub
		try {
			File file = new File(filePath);
			reader = new BufferedReader(new FileReader(file));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/**
	 * @name:
	 * @msg: load a collection of locations at the same timestamp in a stream manner
	 * @return {a list of locations at the same timestamp (curTS)*}
	 */

	public ArrayList<Location> batch() {
		ArrayList<Location> locBatch = new ArrayList<>();
		// the first location of each timestamp may be loaded at last timestamp, thus we
		// need to add it
		if (firstLocation != null) {
			locBatch.add(firstLocation);
			firstLocation = null;
		}
		// the current timestamp of loading locations
		int curTS = -100;
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
					curTS = ts;
				}
				// if two adjacent lines record two different timestamps, then
				if (ts != curTS) {
					firstLocation = new Location(id, date, time, lon, lat, ts);
					break;
				} else {
					locBatch.add(new Location(id, date, time, lon, lat, ts));
					// manually add some virtual locations to increase data volume
					if (Settings.expNum > 1) {
						for (int i = 1; i < Settings.expNum; i++) {
							locBatch.add(new Location(2000000 + id * Settings.expNum + i, date, time,
									(float) (lon + 0.0005), (float) (lat + 0.0005), ts));
						}
					}
					count++;
				}
			}
			loadLineNB += count;
			// close reader if all locations are loaded
			if (locBatch.isEmpty()) {
				loadLineNB = 0;
				reader.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return locBatch;
	}
}
