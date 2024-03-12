/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:09:51
 */

package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.Stream;
import trace.QGP;
import trace.Settings;
import trace.Util;

public class QGPTester {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		System.out.println("The total number of days: " + files.length);
		// 2. create a Tracer object
		QGP tracer = new QGP(Settings.epsilon, Settings.k, Settings.name);
		// 3. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		System.out.println(
				"Init Patients finished, number: " + Settings.initPatientNum + " isRandom: " + Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> QGPRes = new HashMap<>();
		// 4. start query
		for (File f : files) {
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.batch();
			while (batch != null && !batch.isEmpty()) {
				// If not sampled location, ignore
				if (batch.get(0).ts % Settings.sr != 0) {
					continue;
				}
				locNum += batch.size();
				if (tsNum % 1000 == 0) {
					System.out.printf("\n%s %s return locations %d\n", batch.get(0).date,
							batch.get(0).time, batch.size());
				}

				long startTime = System.currentTimeMillis();
				ArrayList<Integer> QGPCases = new ArrayList<Integer>();
				QGPCases = tracer.trace(batch);
				if (!QGPCases.isEmpty())
					QGPRes.put(tsNum, QGPCases);
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

		System.out.println("totalQueryNB/totalCheckNB/totalLeafNB:");
		System.out.println(tracer.totalQueryNB + "/" + tracer.totalCheckNB + "/" + tracer.totalLeafNB);
		System.out.println("cTime/fTime/sTime");
		System.out.println(tracer.cTime + "/" + tracer.fTime + "/" + tracer.sTime);
		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime: " + runtime + ",mean runtime:  " + (double) runtime / tsNum);
		// System.out.println(QGPRes);
		HashSet<Integer> QGPCases = new HashSet<>();
		for (Integer key : QGPRes.keySet()) {
			QGPCases.addAll(QGPRes.get(key));
		}
		System.out.println("total cases of exposure: " + QGPCases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(QGPCases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				Settings.name,
				Settings.maxProcessDays, Settings.sr, Settings.k, Settings.epsilon,
				Settings.initPatientNum, Settings.minMBR);
		Util.writeFile("QGP", QGPCases.size(), setInfo, otherInfo);

		// the total number of pre-checking operations / the number of valid
		// pre-checking operations
		System.out.println("total time consuming: " + (System.currentTimeMillis() - start_time));
	}

}
