/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:08:54
 */
package singlefiletest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.SingFileStream;
import trace.ET;
import trace.Settings;
import trace.Util;
import trace.EGP;
import trace.AGP;

class Sequence {
	public String name;
	// the distance threshold
	public double epsilon;
	// the duration threshold
	public int k;
	public int initPatientNum;
	public int sr = Settings.sr;
	public int objectNum;

	public Sequence(String name, int k, float epsilon, int sr, int initPatientNum,
			int objectNum) {
		this.name = name;
		this.k = k;
		this.epsilon = epsilon;
		this.sr = sr;
		this.initPatientNum = initPatientNum;
		this.objectNum = objectNum;
	}

	public void eta(HashSet<Integer> patientIDs) {
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		ET etaTracer = new ET(this.epsilon, this.k,
				this.name);
		// 3. init a batch of patient ids
		etaTracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<>();
		// 4. start query
		for (File f : files) {
			SingFileStream stream = new SingFileStream(Settings.dataPath);
			ArrayList<Location> batch = stream.batch(Settings.objectNum);
			while (batch != null && !batch.isEmpty()) {
				if (batch.get(0).ts % this.sr != 0) {
					continue;
				}
				locNum += batch.size();
				// if (tsNum % 10000 == 0) {
				// System.out.printf("\n%s %s return locations %d\n", batch.get(0).date,
				// batch.get(0).time, batch.size());
				// }
				// ETA query
				ArrayList<Integer> ETA_cases = etaTracer.trace(batch);
				if (!ETA_cases.isEmpty()) {
					ETA_res.put(tsNum, ETA_cases);
				}
				tsNum += 1;
				batch = stream.batch(Settings.objectNum);
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxETADays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println(
				"runtime:  " + etaTracer.D.runtime + " mean runtime:  " + (double) etaTracer.D.runtime / tsNum);
		HashSet<Integer> ETA_cases = new HashSet<>();
		for (Integer key : ETA_res.keySet()) {
			ETA_cases.addAll(ETA_res.get(key));
		}
		System.out.println("total cases of exposure: " + ETA_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(ETA_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f, calcCount: %d",
				locNum, tsNum, etaTracer.D.runtime, (double) etaTracer.D.runtime / tsNum, etaTracer.D.calcCount);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon, this.initPatientNum,
				Settings.minMBR);
		Util.writeFile("ETA", ETA_cases.size(), setInfo, otherInfo);
	}

	public void egp(HashSet<Integer> patientIDs, boolean prechecking) {
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		EGP tracer = new EGP(this.epsilon, this.k,
				this.name);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGP_res = new HashMap<>();
		// 4. start query
		for (File f : files) {
			SingFileStream stream = new SingFileStream(Settings.dataPath);
			ArrayList<Location> batch = stream.batch(Settings.objectNum);
			while (batch != null && !batch.isEmpty()) {
				if (batch.get(0).ts % this.sr != 0) {
					continue; // If not sampled location, ignore
				}
				locNum += batch.size();
				// if (tsNum % 1000 == 0) {
				// System.out.printf("\n%s %s return locations %d\n", batch.get(0).date,
				// batch.get(0).time, batch.size());
				// }
				ArrayList<Integer> EGP_cases = new ArrayList<Integer>();
				EGP_cases = tracer.trace(batch, prechecking);

				if (!EGP_cases.isEmpty())
					EGP_res.put(tsNum, EGP_cases);
				tsNum += 1;
				batch = stream.batch(Settings.objectNum);
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("total %d locations, %d timestamps\n", locNum, tsNum);
		System.out.println("runtime:  " + tracer.D.runtime + " mean runtime:  " + (double) tracer.D.runtime / tsNum);
		HashSet<Integer> EGP_cases = new HashSet<>();
		for (Integer key : EGP_res.keySet()) {
			EGP_cases.addAll(EGP_res.get(key));
		}
		System.out.println("total cases of exposure: " + EGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(EGP_cases);

		String otherInfo = String.format(
				"locations: %d , timestamps %d, runtime: %d, mean runtime: %f, checkNum: %d, validNum: %d, calcCount: %d",
				locNum, tsNum, tracer.D.runtime, (double) tracer.D.runtime / tsNum, tracer.totalCheckNums,
				tracer.validCheckNums, tracer.D.calcCount);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon, this.initPatientNum,
				Settings.minMBR);
		if (prechecking) {
			Util.writeFile("EGP", EGP_cases.size(), setInfo, otherInfo);
		} else {
			Util.writeFile("EGP#", EGP_cases.size(), setInfo, otherInfo);
		}
	}

	public void agp(HashSet<Integer> patientIDs) {
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		AGP tracer = new AGP(this.epsilon, this.k, this.name);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> AGP_res = new HashMap<>();
		// 4. start query
		for (File f : files) {
			SingFileStream stream = new SingFileStream(Settings.dataPath);
			ArrayList<Location> batch = stream.batch(Settings.objectNum);
			while (batch != null && !batch.isEmpty()) {
				if (batch.get(0).ts % this.sr != 0) {
					continue;
				}
				locNum += batch.size();
				ArrayList<Integer> AGPCases = tracer.trace(batch, tsNum);
				// add new cases of exposure
				if (!AGPCases.isEmpty()) {
					AGP_res.put(tsNum, AGPCases);
				}
				tsNum += 1;
				batch = stream.batch(Settings.objectNum);
			} // End 'While' Loop

			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("%d locations, %d timestamps ", locNum, tsNum);
		System.out.println("runtime:  " + tracer.D.runtime + " mean runtime:  " + (double) tracer.D.runtime / tsNum);
		HashSet<Integer> AGPCases = new HashSet<>();
		for (Integer key : AGP_res.keySet()) {
			AGPCases.addAll(AGP_res.get(key));
		}
		System.out.println("Total cases of exposure: " + AGPCases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(AGPCases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f, calcCount: %d",
				locNum, tsNum, tracer.D.runtime, (double) tracer.D.runtime / tsNum, tracer.D.calcCount);
		String setInfo = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon, this.initPatientNum,
				Settings.minMBR);
		Util.writeFile("AGP", AGPCases.size(), setInfo, otherInfo);
	}

	public void run() {
		System.out.println(String.format("Running: name: %s \t k: %d  \t epsilon: %f",
				this.name,
				this.k, this.epsilon));
		HashSet<Integer> patientIDs = Util.initPatientIds(this.objectNum, this.initPatientNum, Settings.isRandom);
		long t1, t2;
		String info = null;
		/**
		 * EGP
		 */
		t1 = System.currentTimeMillis();
		this.egp(patientIDs, true);
		t2 = System.currentTimeMillis();
		info = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon, this.initPatientNum,
				Settings.minMBR);
		System.out.println(info);
		System.out.println("egp time consuming: " + (t2 - t1));
		System.out.println();
		/**
		 * EGP#
		 */
		t1 = System.currentTimeMillis();
		this.egp(patientIDs, false);
		t2 = System.currentTimeMillis();
		info = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon, this.initPatientNum,
				Settings.minMBR);
		System.out.println(info);
		System.out.println("egp* time consuming: " + (t2 - t1));
		System.out.println();
		/**
		 * AGP
		 */
		t1 = System.currentTimeMillis();
		this.agp(patientIDs);
		t2 = System.currentTimeMillis();
		info = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon, this.initPatientNum,
				Settings.minMBR);
		System.out.println(info);
		System.out.println("agp time_consuming: " + (t2 - t1));
		System.out.println();
		/**
		 * ET
		 */
		t1 = System.currentTimeMillis();
		this.eta(patientIDs);
		t2 = System.currentTimeMillis();
		info = String.format(
				"name: %s \t days: %d \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d minMBR: %d",
				this.name,
				Settings.maxProcessDays, this.sr, this.k, this.epsilon,
				this.initPatientNum, Settings.minMBR);
		System.out.println(info);
		System.out.println("eta time_consuming: " + (t2 - t1));

	}

}

public class SequenceTester {

	public static void main(String args[]) {
		// Beijing: default 15, [5,10,15,20,25]; Porto: [5,7,9,11], default 5
		// Beijing: default 2, [2,4,6,8,10]; Porto: 2, [2,4,6,8,10];
		// Beijing
		long t1 = System.currentTimeMillis();
		long t2 = System.currentTimeMillis();
		for (Integer patientNum : new int[] { 100, 200, 300, 400, 500 }) {
			new Sequence("beijing", Settings.k, Settings.epsilon, Settings.sr, patientNum,
					Settings.objectNum).run();
		}
		t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);

		// for(Integer duration:new int[]{5,10,20,25})
		// {
		// new Sequence("beijing", duration, 2f, 10, 600, 10000).run();
		// }
		// t2 = System.currentTimeMillis();
		// System.out.println(t2-t1);

		// for(Float distance:new float[]{4f,6f,8f,10f})
		// {
		// new Sequence("beijing", 15, distance, 10, 600, 10000).run();
		// }
		// t2 = System.currentTimeMillis();
		// System.out.println(t2-t1);

		// Porto
		for (Integer patientNum : new int[] { 20000, 40000, 60000, 80000, 100000 }) {
			new Sequence("porto", Settings.k, Settings.epsilon, Settings.sr, Settings.initPatientNum,
					Settings.objectNum).run();
		}
		t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);
		for (Integer duration : new int[] { 10, 15, 20, 25 }) {
			new Sequence("porto", duration, Settings.epsilon, Settings.sr, Settings.initPatientNum, Settings.objectNum)
					.run();
		}

		for (Float distance : new float[] { 4f, 6f, 8f, 10f }) {
			new Sequence("porto", Settings.k, distance, Settings.sr, Settings.initPatientNum, Settings.objectNum).run();
		}
		t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);

	}
}