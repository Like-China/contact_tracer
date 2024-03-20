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

import loader.Location;
import loader.Stream;
import trace.EGP;
import trace.Settings;
import trace.Util;

public class EGPTester {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. create a Tracer object
		EGP tracer = new EGP(Settings.epsilon, Settings.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"InitPatientSize: " + Settings.initPatientNum + " Random: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<Location> batch = stream.batch(Settings.objectNum);
		if (batch.size() < Settings.objectNum) {
			System.out.println("lacked data!!");
			return;
		}
		while (batch != null && !batch.isEmpty()) {
			locNum += batch.size();
			System.out.printf("Timestamp %3d return locations %d", batch.get(0).ts, batch.size());
			System.out.println("\t The first loc: " + batch.get(0));
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> EGPCases = new ArrayList<Integer>();
			EGPCases = tracer.trace(batch, Settings.prechecking);
			if (!EGPCases.isEmpty())
				EGPRes.put(tsNum, EGPCases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			if (tsNum >= Settings.maxTSNB) {
				break;
			}
			batch = stream.batch(Settings.objectNum);
		} // End 'While' Loop
		System.out.println("totalQueryNB/totalCheckNB");
		System.out.println(tracer.totalQueryNB + "/" + tracer.totalCheckNB);
		System.out.println("cTime/fTime/sTime");
		System.out.println(tracer.cTime + "/" + tracer.fTime + "/" + tracer.sTime);
		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> EGPCases = new HashSet<>();
		for (Integer key : EGPRes.keySet()) {
			EGPCases.addAll(EGPRes.get(key));
		}
		System.out.println("total cases of exposure: " + EGPCases.size());
		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		if (Settings.prechecking) {
			Util.writeFile("EGP", EGPCases.size(), setInfo, otherInfo);
		} else {
			Util.writeFile("EGP#", EGPCases.size(), setInfo, otherInfo);
		}
		System.out.printf("total number of pre-checking operations / the number of valid: %d / %d\n",
				tracer.totalCheckNums, tracer.validCheckNums);
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
