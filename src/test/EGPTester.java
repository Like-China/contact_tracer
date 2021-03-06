/*
 * @Author: Like likemelikeyou@126.com
 * @Date: 2022-03-30 12:02:25
 * @LastEditors: Like likemelikeyou@126.com
 * @LastEditTime: 2022-06-13 15:11:50
 * @FilePath: /contact_tracer/src/test/EGPTester.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
/*EGP_Tracer1
 * @Author: your name
 * @Date: 2022-03-30 12:02:25
 * @LastEditTime: 2022-05-26 09:45:23
 * @LastEditors: Like likemelikeyou@126.com
 * @Description: open koroFileHeader for settings: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 * @FilePath: /contact_tracer/src/test/EGPTester.java
 */
package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import data_loader.Stream;
import trace.EGP_Tracer1;
import trace.Settings;
import trace.Util;

public class EGPTester {

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		// 1. get all files and sort by days
		File[] files = Util.orderByName(Settings.dataPath);
		// 2. create a Tracer object
		EGP_Tracer1 tracer = new EGP_Tracer1(Settings.distance_threshold, Settings.duration_threshold,
				Settings.city_name);
		// 3. init a batch of patient ids
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		long runtime = 0;
		long locNum = 0;
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
				EGP_cases = tracer.trace(batch,Settings.prechecking);
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
		 String setInfo = String.format("city_name: %s \t days: %d \t sr: %d \t duration_threshold: %d  \t distance_threshold: %f  \t initPatientNum: %d minMBR: %d", Settings.city_name,
		Settings.maxProcessDays, Settings.sr, Settings.duration_threshold, Settings.distance_threshold, Settings.initPatientNum, Settings.minMBR);
		 if (Settings.prechecking)
		 {
			Util.writeFile("EGP", EGP_cases.size(), setInfo, otherInfo);
		 }else
		 {
			Util.writeFile("EGP#", EGP_cases.size(), setInfo, otherInfo);
		 }

		 // 输出总的precheck次数和有效的prechecking次数
		 System.out.printf("%d / %d",tracer.totalCheckNums,tracer.validCheckNums);
		 System.out.println("time_consuming: "+(System.currentTimeMillis()-start_time));
	}

}
