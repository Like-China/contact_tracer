/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-06 12:41:58
 */
package trace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import data_loader.Location;

public class Util {

	public static void sleep(long duration) {
		try {
			Thread.sleep(duration);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}
	}

	public Util() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * get four vertexes of a batch of locations
	 * 
	 * @param locations
	 * @return
	 */
	public static float[][] getMBR(ArrayList<Location> locations) {
		float minLon = 1000f;
		float maxLon = -1000f;
		float minLat = 1000f;
		float maxLat = -1000f;
		float[][] res = new float[4][2];
		for (Location l : locations) {
			minLon = Math.min(minLon, l.lon);
			maxLon = Math.max(maxLon, l.lon);
			minLat = Math.min(minLat, l.lat);
			maxLat = Math.max(maxLat, l.lat);
		}
		res[0] = new float[] { minLon, minLat };
		res[1] = new float[] { minLon, maxLat };
		res[2] = new float[] { maxLon, minLat };
		res[3] = new float[] { maxLon, maxLat };
		return res;
	}

	// calculate delay
	public static double delay(HashMap<Integer, ArrayList<Integer>> accurate,
			HashMap<Integer, ArrayList<Integer>> Estimate) {

		HashMap<Integer, Integer> Delay_per_mp = new HashMap<Integer, Integer>();

		double delay_sum = 0;
		double delay_num = 0;

		Set<Integer> set = accurate.keySet();
		Object[] arr = set.toArray();
		Arrays.sort(arr);

		for (Object i : arr) { // all delayed ids
			ArrayList<Integer> accu_set = accurate.get(i);
			if (Estimate.containsKey(i)) {
				ArrayList<Integer> appro_set = Estimate.get(i);
				for (int j = 0; j < accu_set.size(); j++) {
					Integer id = accu_set.get(j);
					if (!appro_set.contains(id)) {
						Delay_per_mp.put(id, (Integer) i);
					}
				}
			} else {
				for (int j = 0; j < accu_set.size(); j++) {
					Integer id = accu_set.get(j);
					Delay_per_mp.put(id, (Integer) i);
				}
			}
		}

		for (Integer i : Estimate.keySet()) { // delay time
			ArrayList<Integer> appro_set = Estimate.get(i);
			for (int j = 0; j < appro_set.size(); j++) {
				Integer delay_id = appro_set.get(j);
				if (Delay_per_mp.containsKey(delay_id)) {
					Integer t = Delay_per_mp.get(delay_id);
					if (t < i) {
						delay_num += 1;
						delay_sum += (i - t);
						Delay_per_mp.remove(delay_id);
					}
				}
			}
		}
		if (delay_num == 0) {
			return 0;
		}
		return delay_sum / delay_num;
	}

	// calculate accuracy
	public static double accuracy(HashSet<Integer> accu_id, HashSet<Integer> appro_id) {
		Integer correct_num = 0;

		for (Integer id : accu_id) {
			if (appro_id.contains(id)) {
				correct_num += 1;
			}
		}
		return (double) correct_num * 100 / appro_id.size();
	}

	/**
	 * generate a set of patient ids with a fixed number
	 * 
	 * @param totalNum
	 * @param patientNum
	 * @return Id
	 */
	public static HashSet<Integer> initPatientIds(int totalNum, int patientNum, boolean isRandom) {
		HashSet<Integer> patientIDs = new HashSet<Integer>();
		if (!isRandom) {
			for (int i = 0; i < patientNum; i++) {
				patientIDs.add(i);
			}
			return patientIDs;
		}
		int[] num1 = new int[patientNum];
		int[] num2 = new int[totalNum];
		for (int i = 0; i < num2.length; i++) {
			num2[i] = i + 1;
		}
		Random r = new Random();
		int index = -1;
		for (int i = 0; i < num1.length; i++) {
			index = r.nextInt(num2.length - i);
			num1[i] = num2[index];
			int b = num2[index];
			num2[index] = num2[num2.length - 1 - i];
			num2[num2.length - 1 - i] = b;
		}
		Arrays.sort(num1);
		for (int i : num1) {
			patientIDs.add(i);
		}
		return patientIDs;
	}

	// sort files by name
	public static File[] orderByName(String filePath) {
		File file = new File(filePath);
		File[] files = file.listFiles();
		List<File> fileList = Arrays.asList(files);
		Collections.sort(fileList, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && o2.isFile())
					return -1;
				if (o1.isFile() && o2.isDirectory())
					return 1;
				return o1.getName().compareTo(o2.getName());
			}
		});
		return files;
	}

	// Write results log
	public static void writeFile(String algorithm, int caseNum, String setInfo, String otherInfo) {
		try {
			File writeName = new File("./out.txt");
			writeName.createNewFile();
			try (FileWriter writer = new FileWriter(writeName, true);
					BufferedWriter out = new BufferedWriter(writer)) {
				out.write("\nAlgorithm: " + algorithm);
				out.newLine();
				out.write(setInfo);
				out.newLine();
				out.write(otherInfo);
				out.write(", Cases of exposures: " + caseNum);
				out.newLine();
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// lonlat to coordinate
	public static double[] MillierConvertion(double lat, double lon) {
		double L = 6381372 * Math.PI * 2;
		double W = L;
		double H = L / 2;
		double mill = 2.3;
		double x = lon * Math.PI / 180;
		double y = lat * Math.PI / 180;
		y = 1.25 * Math.log(Math.tan(0.25 * Math.PI + 0.4 * y));
		x = (W / 2) + (W / (2 * Math.PI)) * x;
		y = (H / 2) - (H / (2 * mill)) * y;
		double[] result = new double[2];
		result[0] = x;
		result[1] = y;
		return result;
	}

}
