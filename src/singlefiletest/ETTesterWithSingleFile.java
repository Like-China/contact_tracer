/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:09:51
 */

package singlefiletest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.SingFileStream;
import trace.ET;
import trace.Settings;
import trace.Util;

public class ETTesterWithSingleFile {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. create a Tracer object
		ET tracer = new ET(Settings.epsilon, Settings.k,
				Settings.name);
		// 2. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"Init Patients finished, number: " + Settings.initPatientNum + " isRandom: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> ETRes = new HashMap<>();
		// 3. start query
		SingFileStream stream = new SingFileStream("/home/like/data/contact_tracer/beijing100_1000000.txt");
		ArrayList<Location> batch = stream.batch();
		if (batch.size() < Settings.objectNum) {
			System.out.println("lacked data!!");
			return;
		}
		batch = (ArrayList<Location>) batch.subList(0, Settings.objectNum);
		while (batch != null && !batch.isEmpty()) {
			System.out.printf("\nTimestamp %d return locations %d", batch.get(0).ts, batch.size());
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> ETCases = new ArrayList<Integer>();
			ETCases = tracer.trace(batch);
			if (!ETCases.isEmpty())
				ETRes.put(tsNum, ETCases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			if (tsNum > Settings.maxTSNB) {
				break;
			}
			locNum += batch.size();
			batch = stream.batch();
		} // End 'While' Loop

		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		// System.out.println(ETRes);
		HashSet<Integer> ETCases = new HashSet<>();
		for (Integer key : ETRes.keySet()) {
			ETCases.addAll(ETRes.get(key));
		}
		System.out.println("total cases of exposure: " + ETCases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(ETCases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name,
				Settings.maxProcessDays, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		if (Settings.prechecking) {
			Util.writeFile("EGP", ETCases.size(), setInfo, otherInfo);
		} else {
			Util.writeFile("EGP#", ETCases.size(), setInfo, otherInfo);
		}

		// the total number of pre-checking operations / the number of valid
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
