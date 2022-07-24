/*
 * @Author: your name
 * @Date: 2022-04-06 12:00:33
 * @LastEditTime: 2022-04-08 11:52:15
 * @LastEditors: Please set LastEditors
 * @Description: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 * @FilePath: /contact_tracer/src/test/Evaluate.java
 */
package test;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.Stream;
import trace.ETA_Tracer;
import trace.Settings;
import trace.Util;
import trace.EGP_Tracer;
import trace.AGP_Tracer;

// evalute different methods based on settings.class
public class Evaluate {
    
    public void eta(HashSet<Integer> patientIDs)
    {
        // 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		ETA_Tracer etaTracer = new ETA_Tracer(Settings.distance_threshold, Settings.duration_threshold,
				Settings.city_name);
		// 3. init a batch of patient ids
		etaTracer.patientIDs = (HashSet<Integer>)patientIDs.clone();
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<>();
		// 4. start query
		if (files == null) {
			System.out.println("No valid files found!!");
			return;
		}
		for (File f : files) {
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch();
			while (batch != null && !batch.isEmpty()) {
				if (batch.get(0).ts % Settings.sr != 0) {
					continue;
				}
				locNum += batch.size();
				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				// ETA query
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> ETA_cases = etaTracer.trace(batch);
				if (!ETA_cases.isEmpty()) {
					ETA_res.put(tsNum, ETA_cases);
				}
				long endTime = System.currentTimeMillis();
				runtime += endTime - startTime;
				tsNum += 1;
				batch = stream.read_batch();
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxETADays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("%d locations, %d timestamps ", locNum, tsNum);
		System.out.println("runtime:  " + runtime + " mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> ETA_cases = new HashSet<>();
		for (Integer key : ETA_res.keySet()) {
			ETA_cases.addAll(ETA_res.get(key));
		}
		System.out.println("total cases of exposure: " + ETA_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(ETA_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
		 locNum, tsNum, runtime, (double) runtime / tsNum);
		String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", Settings.city_name,
		Settings.maxProcessDays, Settings.sr, Settings.duration_threshold, Settings.distance_threshold, Settings.initPatientNum, Settings.minMBR);
		 Util.writeFile("ETA", ETA_cases.size(), setInfo, otherInfo);
    }


    public void egp(HashSet<Integer> patientIDs, boolean prechecking)
    {
        // 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		EGP_Tracer tracer = new EGP_Tracer(Settings.distance_threshold, Settings.duration_threshold,
				Settings.city_name);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>)patientIDs.clone();
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGP_res = new HashMap<>();
		// 4. start query
		if (files == null) {
			System.out.println("No valid files found!!");
			return;
		}
		for (File f : files) {
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch();
			while (batch != null && !batch.isEmpty()) {
				if (batch.get(0).ts % Settings.sr != 0) {
					continue; // If not sampled location, ignore
				}
				locNum += batch.size();
				// System.out.printf("\n%s %s return locations %d\n", batch.get(0).date,
				// batch.get(0).time, batch.size());
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> EGP_cases = new ArrayList<Integer>();
				if (!prechecking){
					EGP_cases = tracer.trace_no_checking(batch);
				}else
				{
					EGP_cases = tracer.trace(batch);
				}
				
				// ArrayList<Integer> EGP_cases = tracer.trace(batch);
				if (!EGP_cases.isEmpty())
					EGP_res.put(tsNum, EGP_cases);
				long endTime = System.currentTimeMillis();
				
				runtime += endTime - startTime;
				tsNum += 1;
				batch = stream.read_batch();
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("%d locations, %d timestamps ", locNum, tsNum);
		System.out.println("runtime:  " + runtime + " mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> EGP_cases = new HashSet<>();
		for (Integer key : EGP_res.keySet()) {
			EGP_cases.addAll(EGP_res.get(key));
		}
		System.out.println("total cases of exposure: " + EGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(EGP_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
		 locNum, tsNum, runtime, (double) runtime / tsNum);
		 String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", Settings.city_name,
		Settings.maxProcessDays, Settings.sr, Settings.duration_threshold, Settings.distance_threshold, Settings.initPatientNum, Settings.minMBR);
		 if (prechecking)
		 {
			Util.writeFile("EGP", EGP_cases.size(), setInfo, otherInfo);
		 }else
		 {
			Util.writeFile("EGP#", EGP_cases.size(),setInfo, otherInfo);
		 }
    }

    
    public void agp(HashSet<Integer> patientIDs)
    {
        // 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		AGP_Tracer tracer = new AGP_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>)patientIDs.clone();
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> AGP_res = new HashMap<>();
		// 4. start query
		if (files == null) {
			System.out.println("No valid files found!!");
			return;
		}
		for (File f : files) {
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch();
			while (batch != null && !batch.isEmpty()) {
				if (batch.get(0).ts % Settings.sr != 0) {
					continue;
				}
				locNum += batch.size();
				// System.out.printf("\n%s %s return locations %d", batch.get(0).date,
				// batch.get(0).time, batch.size());
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> AGP_cases = tracer.trace(batch, tsNum);
				// add new cases of exposure
				if (!AGP_cases.isEmpty()) {
					AGP_res.put(tsNum, AGP_cases);
				}
				long endTime = System.currentTimeMillis();
				runtime += endTime - startTime;
				tsNum += 1;
				batch = stream.read_batch();
			} // End 'While' Loop

			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays) {
				break;
			}
		} // End 'For' Loop

		// show results
		System.out.printf("%d locations, %d timestamps ", locNum, tsNum);
		System.out.println("runtime:  " + runtime + " mean runtime:  " + (double) runtime / tsNum);
		HashSet<Integer> AGP_cases = new HashSet<>();
		for (Integer key : AGP_res.keySet()) {
			AGP_cases.addAll(AGP_res.get(key));
		}
		System.out.println("Total cases of exposure: " + AGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(AGP_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f",
		 locNum, tsNum, runtime, (double) runtime / tsNum);
		 String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", Settings.city_name,
		Settings.maxProcessDays, Settings.sr, Settings.duration_threshold, Settings.distance_threshold, Settings.initPatientNum, Settings.minMBR);
		Util.writeFile("AGP", AGP_cases.size(),setInfo, otherInfo);
    }


    public static void main(String[] args) {
        
        HashSet<Integer> patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
        Evaluate e = new Evaluate();
        

        long t1 = System.currentTimeMillis();
        e.egp(patientIDs, false);
        long t2 = System.currentTimeMillis();
        System.out.println("egp* time_consuming: "+ (t2-t1));
		System.out.println();

        t1 = System.currentTimeMillis();
        e.egp(patientIDs, true);
        t2 = System.currentTimeMillis();
        System.out.println("egp time_consuming: "+ (t2-t1));
		System.out.println();

        t1 = System.currentTimeMillis();
        e.agp(patientIDs);
        t2 = System.currentTimeMillis();
        System.out.println("agp time_consuming: "+ (t2-t1));
		System.out.println();

        t1 = System.currentTimeMillis();
        e.eta(patientIDs);
        t2 = System.currentTimeMillis();
        System.out.println("eta time_consuming: "+ (t2-t1));
    }

	
}
