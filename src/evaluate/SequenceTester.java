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

class Sequence {
	// the distance threshold
	public double epsilon;
	// the duration threshold
	public int k;
	// the number of init query objects
	public int initPatientNum;
	// the number of all moving objects
	public int objectNum;
	// mean runtime at each timestamp of each round
	public long egpTime = 0, qgpTime = 0, aqgpExactTime = 0, aegpExactTime = 0, aqgpAppTime = 0, etTime = 0;
	// mean construction time/ filtering time/ search time
	public long egpcTime = 0, qgpcTime = 0, aqgpExactcTime = 0, aegpExactcTime = 0, aqgpAppcTime = 0, etcTime = 0;
	public long egpfTime = 0, qgpfTime = 0, aqgpExactfTime = 0, aegpExactfTime = 0, aqgpAppfTime = 0;
	public long egpsTime = 0, qgpsTime = 0, aqgpExactsTime = 0, aegpExactsTime = 0, aqgpAppsTime = 0;
	// mean number of cases
	public int exactCases = 0;
	public int appCases = 0;

	public void initTime() {
		egpTime = 0;
		qgpTime = 0;
		aqgpExactTime = 0;
		aqgpAppTime = 0;
		etTime = 0;
		egpcTime = 0;
		qgpcTime = 0;
		aqgpExactcTime = 0;
		aqgpAppcTime = 0;
		egpfTime = 0;
		qgpfTime = 0;
		aqgpExactfTime = 0;
		aqgpAppfTime = 0;
		egpsTime = 0;
		qgpsTime = 0;
		aqgpExactsTime = 0;
		aqgpAppsTime = 0;
		exactCases = 0;
		appCases = 0;
	}

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

		String otherInfo = String.format("locations: %d, timestamps %d, runtime: %d, mean runtime: %3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum);
		etTime += runtime / tsNum;
		System.out.println(otherInfo + " total cases of exposure: " + ETCases.size());
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
				"locations: %d, timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);

		egpTime += runtime / tsNum;
		egpcTime += tracer.cTime / tsNum;
		egpfTime += tracer.fTime / tsNum;
		egpsTime += tracer.sTime / tsNum;

		System.out.println(otherInfo + " total cases of exposure: " + EGPCases.size());
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
				"locations: %d, timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);

		qgpTime += runtime / tsNum;
		qgpcTime += tracer.cTime / tsNum;
		qgpfTime += tracer.fTime / tsNum;
		qgpsTime += tracer.sTime / tsNum;

		System.out.println(otherInfo + " total cases of exposure: " + QGPCases.size());
		exactCases += QGPCases.size();
	}

	public void aqgpApp(HashSet<Integer> patientIDs, int m) {
		// 1. create a Tracer object
		AQGP tracer = new AQGP(this.epsilon, this.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> QGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<ArrayList<Location>> batches = stream.multibBatch(this.objectNum, this.k);
		while (batches != null && !batches.isEmpty()) {
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> QGPCases = tracer.trace(batches, m, true);
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
				"locations: %d, timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);

		aqgpAppTime += runtime / tsNum;
		aqgpAppcTime += tracer.cTime / tsNum;
		aqgpAppfTime += tracer.fTime / tsNum;
		aqgpAppsTime += tracer.sTime / tsNum;

		System.out.println(otherInfo + " total cases of exposure: " + QGPCases.size());
		appCases += QGPCases.size();
	}

	public void aqgpExact(HashSet<Integer> patientIDs, int m) {
		// 1. create a Tracer object
		AQGP tracer = new AQGP(this.epsilon, this.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> QGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<ArrayList<Location>> batches = stream.multibBatch(this.objectNum, this.k);
		while (batches != null && !batches.isEmpty()) {
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> QGPCases = tracer.trace(batches, m, false);
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
		HashSet<Integer> QGPCases = new HashSet<>();
		for (Integer key : QGPRes.keySet()) {
			QGPCases.addAll(QGPRes.get(key));
		}
		tsNum = tsNum * this.k;
		String otherInfo = String.format(
				"locations: %d, timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);

		aqgpExactTime += runtime / tsNum;
		aqgpExactcTime += tracer.cTime / tsNum;
		aqgpExactfTime += tracer.fTime / tsNum;
		aqgpExactsTime += tracer.sTime / tsNum;

		System.out.println(otherInfo + " total cases of exposure: " + QGPCases.size());
	}

	public void aegpExact(HashSet<Integer> patientIDs, int m) {
		// 1. create a Tracer object
		AEGP tracer = new AEGP(this.epsilon, this.k);
		// 2. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>) patientIDs.clone();
		long runtime = 0;
		long locNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> AEGPRes = new HashMap<>();
		// 3. start query
		Stream stream = new Stream(Settings.dataPath);
		ArrayList<ArrayList<Location>> batches = stream.multibBatch(this.objectNum, this.k);
		while (batches != null && !batches.isEmpty()) {
			long startTime = System.currentTimeMillis();
			ArrayList<Integer> cases = tracer.trace(batches, m, false);
			if (!cases.isEmpty())
				AEGPRes.put(tsNum * this.k, cases);
			long endTime = System.currentTimeMillis();
			runtime += endTime - startTime;
			tsNum += 1;
			locNum += this.k * batches.get(0).size();
			if (tsNum * this.k >= Settings.maxTSNB) {
				break;
			}
			batches = stream.multibBatch(this.objectNum, this.k);
		} // End 'While' Loop
		HashSet<Integer> AEGPCases = new HashSet<>();
		for (Integer key : AEGPRes.keySet()) {
			AEGPCases.addAll(AEGPRes.get(key));
		}
		tsNum = tsNum * this.k;
		String otherInfo = String.format(
				"locations: %d, timestamps %d, runtime: %d, mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
				locNum, tsNum, runtime, (double) runtime / tsNum, (double) tracer.cTime / tsNum,
				(double) tracer.fTime / tsNum, (double) tracer.sTime / tsNum);

		aegpExactTime += runtime / tsNum;
		aegpExactcTime += tracer.cTime / tsNum;
		aegpExactfTime += tracer.fTime / tsNum;
		aegpExactsTime += tracer.sTime / tsNum;

		System.out.println(otherInfo + " total cases of exposure: " + AEGPCases.size());
	}

	public void run(int evaluateNB) {

		String setInfo = String.format(
				"**name: %s \t sr: %d \t k: %d  \t epsilon: %2.1f  \t init/objectNB: %d/%d \t isRandom queries: %s**",
				Settings.name,
				Settings.sr, this.k, this.epsilon, this.initPatientNum,
				this.objectNum, Settings.isRandom);
		System.out.println(setInfo);
		initTime();
		for (int n = 0; n < evaluateNB; n++) {
			// init a set of patients
			HashSet<Integer> patientIDs = Util.initPatientIds(this.objectNum, this.initPatientNum, Settings.isRandom,
					n);
			long t1, t2;
			System.out.println("*******Round " + (n + 1) + "********");
			/**
			 * EGP
			 */
			t1 = System.currentTimeMillis();
			this.egp(patientIDs, true);
			t2 = System.currentTimeMillis();
			System.out.println("EGP time consuming: " + (t2 - t1));
			System.out.println();
			/**
			 * QGP
			 */
			t1 = System.currentTimeMillis();
			this.qgp(patientIDs);
			t2 = System.currentTimeMillis();
			System.out.println("QGP time consuming: " + (t2 - t1));
			System.out.println();
			/**
			 * AEGP-Exact
			 */
			t1 = System.currentTimeMillis();
			this.aegpExact(patientIDs, Settings.m);
			t2 = System.currentTimeMillis();
			System.out.println("AEGP-Exa time consuming: " + (t2 - t1));
			System.out.println();
			/**
			 * AQGP-Exact
			 */
			// t1 = System.currentTimeMillis();
			// this.aqgpExact(patientIDs, Settings.m);
			// t2 = System.currentTimeMillis();
			// System.out.println("AQGP-Exa time consuming: " + (t2 - t1));
			// System.out.println();
			/**
			 * AQGP-App
			 */
			// t1 = System.currentTimeMillis();
			// this.aqgpApp(patientIDs, Settings.m);
			// t2 = System.currentTimeMillis();
			// System.out.println("AQGP-App time consuming: " + (t2 - t1));
			// System.out.println();
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
		// output evaluation results
		// String otherInfo = String.format(
		// "mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
		// (double) egpTime / evaluateNB, (double) egpcTime / evaluateNB,
		// (double) egpfTime / evaluateNB, (double) egpsTime / evaluateNB);
		// Util.writeFile("EGP" + " individual experimetal number: " + Settings.expNB,
		// exactCases / evaluateNB, setInfo,
		// otherInfo);
		// otherInfo = String.format(
		// "mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
		// (double) qgpTime / evaluateNB, (double) qgpcTime / evaluateNB,
		// (double) qgpfTime / evaluateNB, (double) qgpsTime / evaluateNB);
		// Util.writeFile("QGP" + " individual experimetal number: " + Settings.expNB,
		// exactCases / evaluateNB, setInfo,
		// otherInfo);
		// otherInfo = String.format(
		// "mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
		// (double) aqgpExactTime / evaluateNB, (double) aqgpExactcTime / evaluateNB,
		// (double) aqgpExactfTime / evaluateNB, (double) aqgpExactsTime / evaluateNB);
		// Util.writeFile("AQGP-Exact" + " individual experimetal number: " +
		// Settings.expNB, exactCases / evaluateNB,
		// setInfo, otherInfo);
		// otherInfo = String.format(
		// "mean runtime: %3.1f, cTime/fTime/sTime: %3.1f/%3.1f/%3.1f",
		// (double) aqgpAppTime / evaluateNB, (double) aqgpAppcTime / evaluateNB,
		// (double) aqgpAppfTime / evaluateNB, (double) aqgpAppsTime / evaluateNB);
		// Util.writeFile("AQGP-App" + " individual experimetal number: " +
		// Settings.expNB, appCases / evaluateNB, setInfo,
		// otherInfo);
		// System.out.println();

	}
}

public class SequenceTester {

	public static void main(String args[]) {
		// Beijing
		long t1 = System.currentTimeMillis();
		for (Integer patientNum : new int[] { 1000, 2000, 3000, 4000, 5000 }) {
			new Sequence(Settings.k, Settings.epsilon, patientNum,
					Settings.objectNum).run(Settings.expNB);
		}
		System.out.println("Total time cost:" + (System.currentTimeMillis() - t1));

		for (Integer objectNum : new int[] { 200000, 400000, 600000, 800000, 1000000
		}) {
			new Sequence(Settings.k, Settings.epsilon, Settings.initPatientNum,
					objectNum).run(Settings.expNB);
		}
		System.out.println("Total time cost:" + (System.currentTimeMillis() - t1));

		for (Integer k : new int[] { 5, 10, 15, 20, 25 }) {
			new Sequence(k, Settings.epsilon, Settings.initPatientNum,
					Settings.objectNum).run(Settings.expNB);
		}
		System.out.println("Total time cost:" + (System.currentTimeMillis() - t1));

		for (Float epsilon : new float[] { 2f, 4f, 6f, 8f, 10f }) {
			new Sequence(Settings.k, epsilon, Settings.initPatientNum,
					Settings.objectNum).run(Settings.expNB);
		}
		System.out.println("Total time cost:" + (System.currentTimeMillis() - t1));
	}

}