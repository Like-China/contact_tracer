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
import trace.EGP;
import trace.Settings;
import trace.Util;

public class EGPTesterWithSingleFile {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. create a Tracer object
		EGP tracer = new EGP(Settings.epsilon, Settings.k,
				Settings.name);
		// 2. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"Init Patients finished, number: " + Settings.initPatientNum + " isRandom: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGP_res = new HashMap<>();
		// 3. start query
		SingFileStream stream = new SingFileStream("/home/like/data/contact_tracer/beijing100_1000000.txt");
		ArrayList<Location> batch = stream.batch(Settings.objectNum);
		if (batch.size() < Settings.objectNum) {
			System.out.println("lacked data!!");
			return;
		}
		System.out.println(batch.size());
		while (batch != null && !batch.isEmpty()) {
			// If not sampled location, ignore
			System.out.printf("\nTimestamp %d return locations %d", batch.get(0).ts, batch.size());
			System.out.println("\t The first loc: " + batch.get(0));
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> EGP_cases = new ArrayList<Integer>();
			EGP_cases = tracer.trace(batch, Settings.prechecking);
			if (!EGP_cases.isEmpty())
				EGP_res.put(tsNum, EGP_cases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			if (tsNum >= Settings.maxTSNB) {
				break;
			}
			locNum += batch.size();
			batch = stream.batch(Settings.objectNum);
		} // End 'While' Loop

		System.out.println("totalQueryNB/totalCheckNB");
		System.out.println(tracer.totalQueryNB + "/" + tracer.totalCheckNB);
		System.out.println("cTime/fTime/sTime");
		System.out.println(tracer.cTime + "/" + tracer.fTime + "/" + tracer.sTime);
		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		// System.out.println(EGP_res);
		HashSet<Integer> EGP_cases = new HashSet<>();
		for (Integer key : EGP_res.keySet()) {
			EGP_cases.addAll(EGP_res.get(key));
		}
		System.out.println("total cases of exposure: " + EGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(EGP_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name,
				Settings.maxProcessDays, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		if (Settings.prechecking) {
			Util.writeFile("EGP", EGP_cases.size(), setInfo, otherInfo);
		} else {
			Util.writeFile("EGP#", EGP_cases.size(), setInfo, otherInfo);
		}

		// the total number of pre-checking operations / the number of valid
		// pre-checking operations
		System.out.printf("total number of pre-checking operations / the number of valid: %d / %d\n",
				tracer.totalCheckNums, tracer.validCheckNums);
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
