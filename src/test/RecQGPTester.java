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
import trace.RectangleQGP;
import trace.Settings;
import trace.Util;

public class RecQGPTester {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. create a Tracer object
		RectangleQGP tracer = new RectangleQGP(Settings.epsilon, Settings.k,
				Settings.name);
		// 2. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"Init Patients size: " + Settings.initPatientNum + "Random: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> QGPRes = new HashMap<>();
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
			ArrayList<Integer> QGPCases = new ArrayList<Integer>();
			QGPCases = tracer.trace(batch);
			if (!QGPCases.isEmpty())
				QGPRes.put(tsNum, QGPCases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			if (tsNum >= Settings.maxTSNB) {
				break;
			}
			batch = stream.batch(Settings.objectNum);
		} // End 'While' Loop
		System.out.println("totalQueryNB/totalCheckNB/leafNB");
		System.out.println(tracer.totalQueryNB + "/" + tracer.totalCheckNB + "/" + tracer.totalLeafNB);
		System.out.println("cTime/fTime/sTime");
		System.out.println(tracer.cTime + "/" + tracer.fTime + "/" + tracer.sTime);
		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> QGPCases = new HashSet<>();
		for (Integer key : QGPRes.keySet()) {
			QGPCases.addAll(QGPRes.get(key));
		}
		System.out.println("total cases of exposure: " + QGPCases.size());
		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		// Util.writeFile("QGP", QGPCases.size(), setInfo, otherInfo);
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
