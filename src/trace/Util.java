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

import loader.Location;

public class Util {

	public static double distance(double[] mbr1, double[] mbr2) {
		double shortestDistance = Double.MAX_VALUE;

		// 计算每个矩形的4个顶点到另一个矩形的距离
		for (int i = 0; i < 4; i++) {
			double x1 = i % 2 == 0 ? mbr1[0] : mbr1[2];
			double y1 = i < 2 ? mbr1[1] : mbr1[3];

			for (int j = 0; j < 4; j++) {
				double x2 = j % 2 == 0 ? mbr2[0] : mbr2[2];
				double y2 = j < 2 ? mbr2[1] : mbr2[3];

				double dist = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
				shortestDistance = Math.min(shortestDistance, dist);
			}
		}

		// 计算两个矩形之间有交集的边的距离
		for (int i = 0; i < 4; i++) {
			double x1 = i % 2 == 0 ? mbr1[0] : mbr1[2];
			double y1 = i < 2 ? mbr1[1] : mbr1[3];
			double x2 = (i + 1) % 2 == 0 ? mbr1[0] : mbr1[2];
			double y2 = (i + 1) % 2 < 2 ? mbr1[1] : mbr1[3];

			for (int j = 0; j < 4; j++) {
				double x3 = j % 2 == 0 ? mbr2[0] : mbr2[2];
				double y3 = j < 2 ? mbr2[1] : mbr2[3];
				double x4 = (j + 1) % 2 == 0 ? mbr2[0] : mbr2[2];
				double y4 = (j + 1) % 2 < 2 ? mbr2[1] : mbr2[3];

				double dist = distancePointToSegment(x1, y1, x3, y3, x4, y4);
				shortestDistance = Math.min(shortestDistance, dist);
			}
		}

		return shortestDistance;
	}

	private static double distancePointToSegment(double x, double y, double x1, double y1, double x2, double y2) {
		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;

		double dot = A * C + B * D;
		double len_sq = C * C + D * D;
		double param = -1;
		if (len_sq != 0) // in case of 0 length line
			param = dot / len_sq;

		double xx, yy;

		if (param < 0) {
			xx = x1;
			yy = y1;
		} else if (param > 1) {
			xx = x2;
			yy = y2;
		} else {
			xx = x1 + param * C;
			yy = y1 + param * D;
		}

		return Math.sqrt((x - xx) * (x - xx) + (y - yy) * (y - yy));
	}

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
	public static double[][] getMBR(ArrayList<Location> locations) {
		double minLon = 1000f;
		double maxLon = -1000f;
		double minLat = 1000f;
		double maxLat = -1000f;
		double[][] res = new double[4][2];
		for (Location l : locations) {
			minLon = Math.min(minLon, l.lon);
			maxLon = Math.max(maxLon, l.lon);
			minLat = Math.min(minLat, l.lat);
			maxLat = Math.max(maxLat, l.lat);
		}
		res[0] = new double[] { minLon, minLat };
		res[1] = new double[] { minLon, maxLat };
		res[2] = new double[] { maxLon, minLat };
		res[3] = new double[] { maxLon, maxLat };
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
	public static double precision(HashSet<Integer> exactRes, HashSet<Integer> appRes) {
		double TP = 0, TN = 0, FP = 0, FN = 0;
		for (int appId : appRes) {
			if (exactRes.contains(appId)) {
				TP += 1;
			} else {
				FP += 1;
			}
		}
		return TP * 100 / (TP + FP);
	}

	/**
	 * generate a set of patient ids with a fixed number
	 * 
	 * @param totalNum
	 * @param patientNum
	 * @return Id
	 */
	public static HashSet<Integer> initPatientIds(int totalNum, int patientNum, boolean isRandom, int seed) {
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
		Random r = new Random(seed);
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

}
