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
import data_loader.SimulatedStream;
import trace.EGP;
import trace.Settings;
import trace.Util;

public class EGPTesterWithSimulatedData {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		int objNB = 1000000;
		int tsNB = 100;
		// 1. get all files and sort by days
		SimulatedStream stream = new SimulatedStream(objNB);
		// 2. create a Tracer object
		EGP tracer = new EGP(Settings.epsilon, Settings.k, Settings.name);
		// 3. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(objNB, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"Init Patients finished, number: " + Settings.initPatientNum + " isRandom: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGPRes = new HashMap<>();
		// 4. start query
		for (int i = 0; i < tsNB; i++) {
			ArrayList<Location> batch = stream.batch();
			locNum += batch.size();
			if (tsNum % 100 == 0) {
				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date,
						batch.get(0).time, batch.size());
			}
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> EGPCases = new ArrayList<Integer>();
			EGPCases = tracer.trace(batch, Settings.prechecking);
			if (!EGPCases.isEmpty())
				EGPRes.put(tsNum, EGPCases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			batch = stream.batch();
		} // End 'For' Loop

		System.out.println("totalQueryNB/totalCheckNB/totalLeafNB:");
		System.out.println(tracer.totalQueryNB + "/" + tracer.totalCheckNB);
		System.out.println("cTime/fTime/sTime");
		System.out.println(tracer.cTime + "/" + tracer.fTime + "/" + tracer.sTime);
		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		// System.out.println(EGPRes);
		HashSet<Integer> EGPCases = new HashSet<>();
		for (Integer key : EGPRes.keySet()) {
			EGPCases.addAll(EGPRes.get(key));
		}
		System.out.println("total cases of exposure: " + EGPCases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(EGPCases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name,
				Settings.maxProcessDays, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		if (Settings.prechecking) {
			Util.writeFile("EGP", EGPCases.size(), setInfo, otherInfo);
		} else {
			Util.writeFile("EGP#", EGPCases.size(), setInfo, otherInfo);
		}

		// the total number of pre-checking operations / the number of valid
		// pre-checking operations
		System.out.printf("total number of pre-checking operations / the number of valid: %d / %d\n",
				tracer.totalCheckNums, tracer.validCheckNums);
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
