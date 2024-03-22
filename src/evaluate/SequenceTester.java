/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:08:54
 */
package evaluate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import loader.Location;
import loader.Stream;
import trace.ET;
import trace.QGP;
import trace.Settings;
import trace.Util;
import trace.EGP;
import trace.AEGP;
import trace.AQGP;
import trace.AQGP1;

class Sequence {
	// the distance threshold
	public double epsilon;
	// the duration threshold
	public int k;
	public int initPatientNum;
	public int objectNum;

	public Sequence(int k, float epsilon, int initPatientNum,
			int objectNum) {
		this.k = k;
		this.epsilon = epsilon;
		this.initPatientNum = initPatientNum;
		this.objectNum = objectNum;
	}

	public void et(HashSet<Integer> patientIDs) {
		// 1. create a Tracer object
		ET tracer = new ET(this.epsilon, this.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> ETRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<Location> batch = stream.batch(this.objectNum);
		while (batch != null && !batch.isEmpty()) {
			locNum += batch.size();
			if (tsNum % 4 == 0) {
				System.out.printf("Timestamp %d return locations %d", batch.get(0).ts, batch.size());
				System.out.println("\t The first loc: " + batch.get(0));
			}
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
			batch = stream.batch(this.objectNum);
		} // End 'While' Loop
			// show results
		HashSet<Integer> ETCases = new HashSet<>();
		for (Integer key : ETRes.keySet()) {
			ETCases.addAll(ETRes.get(key));
		}

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		System.out.println(otherInfo + " total cases of exposure: " + ETCases.size());
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d \t objectNB: %d",
				Settings.name, Settings.sr, this.k, this.epsilon,
				this.initPatientNum, this.objectNum);
		Util.writeFile("ET", ETCases.size(), setInfo, otherInfo);
	}

	public void egp(HashSet<Integer> patientIDs, boolean prechecking) {
		// 1. create a Tracer object
		EGP tracer = new EGP(this.epsilon, this.k);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<Location> batch = stream.batch(this.objectNum);
		while (batch != null && !batch.isEmpty()) {
			locNum += batch.size();
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
			batch = stream.batch(this.objectNum);
		} // End 'While' Loop
			// show results
		HashSet<Integer> EGPCases = new HashSet<>();
		for (Integer key : EGPRes.keySet()) {
			EGPCases.addAll(EGPRes.get(key));
		}
		String otherInfo = String.format(
				"locations: %d , timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);
		System.out.println(otherInfo + " total cases of exposure: " + EGPCases.size());
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d \t objectNum:%d \t minMBR: %d ",
				Settings.name, Settings.sr, this.k, this.epsilon,
				this.initPatientNum, this.objectNum, Settings.minMBR);
		if (Settings.prechecking) {
			Util.writeFile("EGP", EGPCases.size(), setInfo, otherInfo);
			System.out.printf("total number of pre-checking operations / the number of valid: %d / %d\n",
					tracer.totalCheckNums, tracer.validCheckNums);
		} else {
			Util.writeFile("EGP#", EGPCases.size(), setInfo, otherInfo);
		}
	}

	public void qgp(HashSet<Integer> patientIDs) {
		// 1. create a Tracer object
		QGP tracer = new QGP(this.epsilon, this.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> QGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<Location> batch = stream.batch(this.objectNum);
		while (batch != null && !batch.isEmpty()) {
			locNum += batch.size();
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
			batch = stream.batch(this.objectNum);
		} // End 'While' Loop
			// show results
		HashSet<Integer> QGPCases = new HashSet<>();
		for (Integer key : QGPRes.keySet()) {
			QGPCases.addAll(QGPRes.get(key));
		}
		String otherInfo = String.format(
				"locations: %d , timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);
		System.out.println(otherInfo + " total cases of exposure: " + QGPCases.size());
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d \t this.objectNum: %d",
				Settings.name, Settings.sr, this.k, this.epsilon,
				this.initPatientNum, this.objectNum);
		Util.writeFile("QGP", QGPCases.size(), setInfo, otherInfo);
	}

	public void aqgp(HashSet<Integer> patientIDs, int m) {
		// 1. create a Tracer object
		AQGP tracer = new AQGP(this.epsilon, this.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, HashSet<Integer>> QGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<ArrayList<Location>> batches = stream.multibBatch(this.objectNum, this.k);
		while (batches != null && !batches.isEmpty()) {
			long startTime = System.currentTimeMillis();
			HashSet<Integer> QGPCases = tracer.trace(batches, tsNum, m);
			if (!QGPCases.isEmpty())
				QGPRes.put(tsNum * this.k, QGPCases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			locNum += this.k * batches.get(0).size();
			if (tsNum * this.k >= Settings.maxTSNB) {
				break;
			}
			batches = stream.multibBatch(this.objectNum, this.k);
		} // End 'While' Loop
			// show results
		HashSet<Integer> QGPCases = new HashSet<>();
		for (Integer key : QGPRes.keySet()) {
			QGPCases.addAll(QGPRes.get(key));
		}
		tsNum = tsNum * this.k;
		String otherInfo = String.format(
				"locations: %d , timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);
		System.out.println(otherInfo + " total cases of exposure: " + QGPCases.size());
		String setInfo = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t initPatientNum: %d \t this.objectNum: %d",
				Settings.name, Settings.sr, this.k, this.epsilon,
				this.initPatientNum, this.objectNum);
		Util.writeFile("AQGP", QGPCases.size(), setInfo, otherInfo);
	}

	public void run() {
		System.out.println(String.format("\n******Name: %s \t k: %d  \t epsilon: %f******",
				Settings.name,
				this.k, this.epsilon));
		// init a set of patients
		HashSet<Integer> patientIDs = Util.initPatientIds(this.objectNum, this.initPatientNum, Settings.isRandom);
		System.out.println(
				"InitPatientSize: " + this.initPatientNum + " Random: " + Settings.isRandom + "\n");
		long t1, t2;
		String info = String.format(
				"name: %s \t sr: %d \t k: %d  \t epsilon: %f  \t init/objectNB: %d/%d",
				Settings.name,
				Settings.sr, this.k, this.epsilon, this.initPatientNum,
				this.objectNum);
		System.out.println(info);
		/**
		 * EGP
		 */
		// t1 = System.currentTimeMillis();
		// this.egp(patientIDs, true);
		// t2 = System.currentTimeMillis();
		// System.out.println("EGP time consuming: " + (t2 - t1));
		// System.out.println();
		/**
		 * QGP
		 */
		t1 = System.currentTimeMillis();
		this.qgp(patientIDs);
		t2 = System.currentTimeMillis();
		System.out.println("QGP time_consuming: " + (t2 - t1));
		System.out.println();
		/**
		 * AQGP
		 */
		t1 = System.currentTimeMillis();
		this.aqgp(patientIDs, Settings.m);
		t2 = System.currentTimeMillis();
		System.out.println("AQGP time_consuming: " + (t2 - t1));
		System.out.println();
		/**
		 * ET
		 */
		if (initPatientNum < 500) {
			t1 = System.currentTimeMillis();
			this.et(patientIDs);
			t2 = System.currentTimeMillis();
			System.out.println("eta time_consuming: " + (t2 - t1));
		}

	}

}

public class SequenceTester {

	public static void main(String args[]) {
		// Beijing
		long t1 = System.currentTimeMillis();
		long t2 = System.currentTimeMillis();
		for (Integer patientNum : new int[] { 5000 }) {
			new Sequence(Settings.k, Settings.epsilon, patientNum,
					Settings.objectNum).run();
		}
		t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);

		// for (Integer objectNum : new int[] { 200000, 400000, 600000, 800000, 1000000
		// }) {
		// new Sequence(Settings.k, Settings.epsilon, Settings.initPatientNum,
		// objectNum).run();
		// }
		// t2 = System.currentTimeMillis();
		// System.out.println(t2 - t1);

		// for (Integer k : new int[] { 5, 10, 15, 20, 25 }) {
		// new Sequence(k, Settings.epsilon, Settings.initPatientNum,
		// Settings.objectNum).run();
		// }
		// t2 = System.currentTimeMillis();
		// System.out.println(t2 - t1);

		// for (Float epsilon : new float[] { 2f, 4f, 6f, 8f, 10f }) {
		// new Sequence(Settings.k, epsilon, Settings.initPatientNum,
		// Settings.objectNum).run();
		// }
		// t2 = System.currentTimeMillis();
		// System.out.println("Total time cost: " + (t2 - t1));
	}
}