/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 20:10:54
 */

package data_loader;

import java.util.ArrayList;
import java.util.Random;

import trace.Settings;

/*
Given a txt file with Line infromation: id date time lon lat timestamp
example: 00053 2008-02-08 00:00:00 116.410720 39.990820 0
 */

public class SimulatedStream {
	// the total number of locations, which is noted by the filename
	public int totalLocNB;
	// the file reader
	// random seed
	Random r = new Random(0);
	// current timestamp
	public int currentTS = 0;
	// continuously uodpdated locations
	ArrayList<Location> locBatch = new ArrayList<>();
	double[] lonRange = null;
	double[] latRange = null;

	/**
	 * Load locations in a stream manner
	 * 
	 * @param filePath the file path that records all location information
	 */
	public SimulatedStream(int totalLocNB) {
		this.totalLocNB = totalLocNB;
		init();
	}

	public void init() {

		if (Settings.name == "beijing") {
			lonRange = new double[] { 116.2501f - 0.0001f, 116.252f + 0.0001f };
			latRange = new double[] { 39.8301f - 0.0001f, 39.832f + 0.0001f };
		} else {
			lonRange = new double[] { -8.735f - 0.0015f, -8.156f + 0.0015f };
			latRange = new double[] { 40.953f - 0.0015f, 41.307f + 0.0015f };
		}

		for (int i = 0; i < totalLocNB; i++) {
			// int id, String date, String time, float lon, float lat, int ts
			double lon = lonRange[0] + r.nextDouble() * (lonRange[1] - lonRange[0]);
			double lat = latRange[0] + r.nextDouble() * (latRange[1] - latRange[0]);
			Location newL = new Location(i, "2020-20-20", "-1:-1:-1", lon, lat, currentTS);
			locBatch.add(newL);
		}
	}

	/**
	 * @name:
	 * @msg: load a collection of locations at the same timestamp in a stream manner
	 * @return {a list of locations at the same timestamp (curTS)*}
	 */

	public ArrayList<Location> batch() {
		// the current timestamp of loading locations
		for (Location l : locBatch) {
			l.ts += Settings.sr;
			double newLon = l.lon - 0.00002 + r.nextDouble() * 0.00004;
			while (newLon < lonRange[0] || newLon > lonRange[1]) {
				newLon = l.lon - 0.00002 + r.nextDouble() * 0.00004;
			}
			l.lon = newLon;
			double newLat = l.lat - 0.00001 + r.nextDouble() * 0.00002;
			while (newLat < latRange[0] || newLat > latRange[1]) {
				newLat = l.lat - 0.00001 + r.nextDouble() * 0.00002;
			}
			l.lat = newLat;
		}
		return locBatch;
	}

	public static void main(String[] args) {
		int locNB = 200;
		int tsNB = 100;
		SimulatedStream ss = new SimulatedStream(locNB);
		for (int i = 0; i < tsNB; i++) {
			ArrayList<Location> locs = ss.batch();
			// System.out.println(locs);
		}

	}
}
