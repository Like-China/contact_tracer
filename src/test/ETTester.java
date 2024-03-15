/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:09:51
 */

package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.Stream;
import trace.ET;
import trace.Settings;
import trace.Util;

public class ETTester {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. create a Tracer object
		ET tracer = new ET(Settings.epsilon, Settings.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"InitPatientSize: " + Settings.initPatientNum + " Random: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> ETRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<Location> batch = stream.batch(Settings.objectNum);
		if (batch.size() < Settings.objectNum) {
			System.out.println("lacked data!!");
			return;
		}
		while (batch != null && !batch.isEmpty()) {
			locNum += batch.size();
			System.out.printf("Timestamp %d return locations %d", batch.get(0).ts, batch.size());
			System.out.println("\t The first loc: " + batch.get(0));
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> ETCases = new ArrayList<Integer>();
			ETCases = tracer.trace(batch);
			if (!ETCases.isEmpty())
				ETRes.put(tsNum, ETCases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			if (tsNum >= Settings.maxTSNB) {
				break;
			}
			batch = stream.batch(Settings.objectNum);
		} // End 'While' Loop
			// show results
		System.out.println("totalcheckNB: " + tracer.totalCheckNB);
		System.out.printf("total %3d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> ETCases = new HashSet<>();
		for (Integer key : ETRes.keySet()) {
			ETCases.addAll(ETRes.get(key));
		}
		System.out.println("total cases of exposure: " + ETCases.size());
		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		Util.writeFile("ET", ETCases.size(), setInfo, otherInfo);
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
