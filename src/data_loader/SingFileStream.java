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

public class SingFileStream {
	// the file path that records all location information
	String filePath;
	// the current already loaded number of lines of the file
	public int loadLineNB;
	// the total number of locations, which is noted by the filename
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
	public SingFileStream(String filePath) {
		this.filePath = filePath;
		loadLineNB = 0;
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

	public ArrayList<Location> batch(int readObjNB) {
		ArrayList<Location> locBatch = new ArrayList<>();
		// the current timestamp of loading locations
		int curTS = -100;
		try {
			String lineString;
			int count = 0;
			while ((lineString = reader.readLine()) != null) {
				int id = Integer.parseInt(lineString.split(" ")[0]);
				minID = minID < id ? minID : id;
				maxID = maxID < id ? id : maxID;
				float lon = Float.parseFloat(lineString.split(" ")[1]);
				float lat = Float.parseFloat(lineString.split(" ")[2]);
				int ts = Integer.parseInt(lineString.split(" ")[3]);
				if (count == 0) {
					curTS = ts;
				}
				// if two adjacent lines record two different timestamps, then
				if (ts != curTS) {
					break;
				} else {
					if (count++ < readObjNB) {
						locBatch.add(new Location(id, " ", " ", lon, lat, ts));
					}
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
