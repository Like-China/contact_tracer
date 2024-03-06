/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:10:01
 */
package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.Stream;
import trace.AGP;
import trace.Settings;
import trace.Util;

public class AGPTester {

	public static void main(String[] args) {
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		AGP tracer = new AGP(Settings.epsilon, Settings.k,
				Settings.city_name);
		// 3. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> AGP_res = new HashMap<>();
		// 4. start query
		if (files == null) {
			System.out.println("No valid files found!!");
			return;
		}
		for (File f : files) {
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.batch();
			while (batch != null && !batch.isEmpty()) {
				// aviod abnormal locations
				if (batch.get(0).ts % Settings.sr != 0) {
					continue;
				}
				locNum += batch.size();
				// System.out.printf("\n%s %s return locations %d", batch.get(0).date,
				// batch.get(0).time, batch.size());
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> AGPCases = tracer.trace(batch, tsNum);
				// add new cases of exposure
				if (!AGPCases.isEmpty()) {
					AGP_res.put(tsNum, AGPCases);
				}
				long endTime = System.currentTimeMillis();
				runtime += endTime - startTime;
				tsNum += 1;
				// get next-timestamped locations of all moving objects
				batch = stream.batch();
			} // End 'While' Loop

			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("%d locations, %d timestamps ", locNum, tsNum);
		System.out.println("runtime:  " + runtime + " mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> AGPCases = new HashSet<>();
		for (Integer key : AGP_res.keySet()) {
			AGPCases.addAll(AGP_res.get(key));
		}
		System.out.println("Total cases of exposure: " + AGPCases.size());
		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);

		String setInfo = String.format(
				"city_name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.city_name,
				Settings.maxProcessDays, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		Util.writeFile("AGP", AGPCases.size(), setInfo, otherInfo);

	}

}
