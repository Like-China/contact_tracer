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
import trace.AGP_Tracer;
import trace.Settings;
import trace.Util;

public class AGPTester {

	public static void main(String[] args) {
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		AGP_Tracer tracer = new AGP_Tracer(Settings.distance_threshold, Settings.duration_threshold,
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
				if (batch.get(0).ts % Settings.sr != 0) {
					continue;
				}
				locNum += batch.size();
				// System.out.printf("\n%s %s return locations %d", batch.get(0).date,
				// batch.get(0).time, batch.size());
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> AGP_cases = tracer.trace(batch, tsNum);
				// add new cases of exposure
				if (!AGP_cases.isEmpty()) {
					AGP_res.put(tsNum, AGP_cases);
				}
				long endTime = System.currentTimeMillis();
				runtime += endTime - startTime;
				tsNum += 1;
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
		HashSet<Integer> AGP_cases = new HashSet<>();
		for (Integer key : AGP_res.keySet()) {
			AGP_cases.addAll(AGP_res.get(key));
		}
		System.out.println("Total cases of exposure: " + AGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(AGP_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);

		String setInfo = String.format(
				"city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d",
				Settings.city_name,
				Settings.maxProcessDays, Settings.sr, Settings.duration_threshold, Settings.distance_threshold,
				Settings.initPatientNum, Settings.minMBR);
		Util.writeFile("AGP", AGP_cases.size(), setInfo, otherInfo);

	}

}
