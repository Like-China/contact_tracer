package test;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.Stream;
import indexes.Distance;
import trace.ETA_Tracer;
import trace.Settings;
import trace.Util;
import trace.EGP_Tracer1;
import trace.AGP_Tracer;

// 多线程测试，只有共有参数来自Settings.java, 其他可变参数自定义
class Sequence
{
    public String threadName;
    public String city_name;
    public int duration_threshold;
    public float distance_threshold;
    public int initPatientNum;
    public int sr = 5;
    public int objectNum;

    public Sequence(String city_name, int duration_threshold, float distance_threshold, int sr, int initPatientNum, int objectNum)
    {
        this.city_name = city_name;
        this.duration_threshold = duration_threshold; 
        this.distance_threshold = distance_threshold;
        this.sr = sr;
        this.initPatientNum = initPatientNum;
        this.objectNum = objectNum;
    }


    public void eta(HashSet<Integer> patientIDs)
    {
        // 1. get all files and sort by days
		File[] files = Util.orderByName(String.format("/home/Like/data/contact_tracer/%s%s/", city_name, this.sr));
		// 2. create a Tracer object
		ETA_Tracer etaTracer = new ETA_Tracer(this.distance_threshold, this.duration_threshold,
				this.city_name);
		// 3. init a batch of patient ids
		etaTracer.patientIDs = (HashSet<Integer>)patientIDs.clone();
		long  locNum = 0;
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
				if (batch.get(0).ts % this.sr != 0) {
					continue;
				}
				locNum += batch.size();
				// System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				// ETA query
				// long startTime = System.currentTimeMillis();
				ArrayList<Integer> ETA_cases = etaTracer.trace(batch);
				if (!ETA_cases.isEmpty()) {
					ETA_res.put(tsNum, ETA_cases);
				}
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
		System.out.println("runtime:  " + etaTracer.D.runtime + " mean runtime:  " + (double) etaTracer.D.runtime / tsNum);
		HashSet<Integer> ETA_cases = new HashSet<>();
		for (Integer key : ETA_res.keySet()) {
			ETA_cases.addAll(ETA_res.get(key));
		}
		System.out.println("total cases of exposure: " + ETA_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(ETA_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f, calcCount: %d",
		 locNum, tsNum, etaTracer.D.runtime, (double) etaTracer.D.runtime / tsNum, etaTracer.D.calcCount);
         String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
		Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
		 Util.writeFile("ETA", ETA_cases.size(), setInfo, otherInfo);
    }


    public void egp(HashSet<Integer> patientIDs, boolean prechecking)
    {
        // 1. get all files and sort by days
		File[] files = Util.orderByName(String.format("/home/Like/data/contact_tracer/%s%s/", city_name, this.sr));
		// 2. create a Tracer object
		EGP_Tracer1 tracer = new EGP_Tracer1(this.distance_threshold, this.duration_threshold,
				this.city_name);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>)patientIDs.clone();
		long  locNum = 0;
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
				if (batch.get(0).ts % this.sr != 0) {
					continue; // If not sampled location, ignore
				}
				locNum += batch.size();
				// System.out.printf("\n%s %s return locations %d\n", batch.get(0).date,
				// batch.get(0).time, batch.size());
				ArrayList<Integer> EGP_cases = new ArrayList<Integer>();
				EGP_cases = tracer.trace(batch, prechecking);
				
				if (!EGP_cases.isEmpty())
					EGP_res.put(tsNum, EGP_cases);
				// long endTime = System.currentTimeMillis();
				// runtime += endTime - startTime;
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
		System.out.println("runtime:  " + tracer.D.runtime + " mean runtime:  " + (double) tracer.D.runtime / tsNum);
		HashSet<Integer> EGP_cases = new HashSet<>();
		for (Integer key : EGP_res.keySet()) {
			EGP_cases.addAll(EGP_res.get(key));
		}
		System.out.println("total cases of exposure: " + EGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(EGP_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f, checkNum: %d, validNum: %d, calcCount: %d",
		 locNum, tsNum, tracer.D.runtime, (double) tracer.D.runtime / tsNum, tracer.totalCheckNums, tracer.validCheckNums, tracer.D.calcCount);
         String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
		Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
		 if (prechecking)
		 {
			Util.writeFile("EGP", EGP_cases.size(), setInfo, otherInfo);
		 }else
		 {
			Util.writeFile("EGP#", EGP_cases.size(), setInfo, otherInfo);
		 }
    }
    

    public void agp(HashSet<Integer> patientIDs)
    {
        // 1. get all files and sort by days
		File[] files = Util.orderByName(String.format("/home/Like/data/contact_tracer/%s%s/", city_name, this.sr));
		// 2. create a Tracer object
		AGP_Tracer tracer = new AGP_Tracer(this.distance_threshold, this.duration_threshold, this.city_name);
		// 3. init a batch of patient ids
		tracer.patientIDs = (HashSet<Integer>)patientIDs.clone();
		long  locNum = 0;
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
				if (batch.get(0).ts % this.sr != 0) {
					continue;
				}
				locNum += batch.size();
				ArrayList<Integer> AGP_cases = tracer.trace(batch, tsNum);
				// add new cases of exposure
				if (!AGP_cases.isEmpty()) {
					AGP_res.put(tsNum, AGP_cases);
				}
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
		System.out.println("runtime:  " + tracer.D.runtime + " mean runtime:  " + (double) tracer.D.runtime / tsNum);
		HashSet<Integer> AGP_cases = new HashSet<>();
		for (Integer key : AGP_res.keySet()) {
			AGP_cases.addAll(AGP_res.get(key));
		}
		System.out.println("Total cases of exposure: " + AGP_cases.size());
		// System.out.println("cases of exposure:");
		// System.out.println(AGP_cases);

		String otherInfo = String.format("locations: %d , timestamps %d, runtime: %d, mean runtime: %f, calcCount: %d",
		 locNum, tsNum, tracer.D.runtime, (double) tracer.D.runtime / tsNum, tracer.D.calcCount);
         String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
		Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
		Util.writeFile("AGP", AGP_cases.size(), setInfo, otherInfo);
    }

	
    public void run() {
        System.out.println(String.format("Running: city_name: %s \t duration_threshold: %d  \t distance_threshold: %f", this.city_name,
        this.duration_threshold, this.distance_threshold));
        HashSet<Integer> patientIDs = Util.initPatientIds(this.objectNum, this.initPatientNum, Settings.isRandom);
        long t1 = System.currentTimeMillis();
        long t2 = System.currentTimeMillis();
		String info = null;

        t1 = System.currentTimeMillis();
        this.egp(patientIDs, true);
        t2 = System.currentTimeMillis();
        info = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
		Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
        System.out.println(info);
        System.out.println("egp time_consuming: "+ (t2-t1));
		System.out.println();
		t1 = System.currentTimeMillis();
        this.egp(patientIDs, false);
        t2 = System.currentTimeMillis();
        info = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
		Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
        System.out.println(info);
        System.out.println("egp* time_consuming: "+ (t2-t1));
		System.out.println();

        t1 = System.currentTimeMillis();
        this.agp(patientIDs);
        t2 = System.currentTimeMillis();
        info = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
		Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
        System.out.println(info);
        System.out.println("agp time_consuming: "+ (t2-t1));
		System.out.println();
		
		if (!this.city_name.equals("beijing")) 
		{
			t1 = System.currentTimeMillis();
			this.eta(patientIDs);
			t2 = System.currentTimeMillis();
			info = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", this.city_name,
			Settings.maxProcessDays, this.sr, this.duration_threshold, this.distance_threshold, this.initPatientNum, Settings.minMBR);
			System.out.println(info);
			System.out.println("eta time_consuming: "+ (t2-t1));
		}
        
    }

}


public class SequenceTester {
 
    public static void main(String args[]) {
        // Beijing: default 15, [5,10,15,20,25]; Porto: [5,7,9,11], default 5
        // Beijing: default 2, [2,4,6,8,10]; Porto: 2, [2,4,6,8,10];
        //Beijing
		long t1 = System.currentTimeMillis();
		long t2 = System.currentTimeMillis();
        for(Integer patientNum:new int[]{1000})
        {
            new Sequence("beijing", 15, 2f, 10, patientNum, 10000).run();
        }
	    t2 = System.currentTimeMillis();
	    System.out.println(t2-t1);

    //    for(Integer duration:new int[]{5,10,20,25})
    //    {
    //         new Sequence("beijing", duration, 2f, 10, 600, 10000).run();
    //    }
	//    t2 = System.currentTimeMillis();
	//    System.out.println(t2-t1);
	   
    //    for(Float distance:new float[]{4f,6f,8f,10f})
    //    {
    //         new Sequence("beijing", 15, distance, 10, 600, 10000).run();
    //    }
	//    t2 = System.currentTimeMillis();
	//    System.out.println(t2-t1);
       
    //    Porto
       for(Integer patientNum:new int[]{20000,40000,60000,80000,100000}) 
       {
            new Sequence("porto", 15, 2f, 5, patientNum, 1000000).run();
       }
       t2 = System.currentTimeMillis();
	   System.out.println(t2-t1);
       for(Integer duration:new int[]{10,15,20,25})
       {
            new Sequence("porto", duration, 2f, 5, 60000, 1000000).run();
       }

       for(Float distance:new float[]{4f,6f,8f,10f})
       {
            new Sequence("porto", 5, distance, 5, 60000, 1000000).run(); 
       }
	   t2 = System.currentTimeMillis();
	   System.out.println(t2-t1);

    }   
 }