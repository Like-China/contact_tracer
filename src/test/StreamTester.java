/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:09:07
 */

package test;

import java.io.File;
import java.util.ArrayList;

import trace.Settings;
import trace.Util;
import data_loader.Location;
import data_loader.Stream;

public class StreamTester {

	public static void main(String[] args) {
		File[] files = Util.orderByName(Settings.dataPath);
		if (files == null) {
			System.out.println("No valid files found!!");
			return;
		}
		for (File f : files) {
			long t1 = System.currentTimeMillis();
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.batch();
			double lon1 = batch.get(0).lon;
			double lat1 = batch.get(0).lat;
			while (!batch.isEmpty()) {
				if (batch.size() > 3000) {
					// batch = stream.batch();
					// continue;
					// }
					// System.out.println("Number of locations at current timestamp: " +
					// batch.size());
					// System.out.println("First location of current timestamp: \n" + batch.get(0));
					// System.out.println("Last location of current timestamp: \n" +
					// batch.get(batch.size() - 1));
					// System.out.println("******************");
				}
				batch = stream.batch();
				System.out.println("First location of current timestamp: " + batch.get(0));
				System.out.println(batch.get(0).lon - lon1);
				System.out.println(batch.get(0).lat - lat1);
				// Util.sleep(5000);
			}
			long t2 = System.currentTimeMillis();
			System.out.println("ID ranges: " + stream.minID + "---" + stream.maxID);
			System.out.println("time cost: ");
			System.out.println(t2 - t1);
			if (batch.isEmpty()) {
				break;
			}
		}

	}
}
