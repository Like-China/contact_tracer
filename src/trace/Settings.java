/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-06 12:36:45
 */

package trace;

public class Settings {

	// default parameters settings
	public static String city_name = "porto";
	// sampling interval
	public static int sr = (city_name.equals("beijing")) ? 10 : 5;
	// data path
	public static String dataPath = String.format("/home/Like/data/contact_tracer/%s%s/", city_name, sr);
	public static int maxProcessDays = 20;
	// due to the rime-consuming of ET algorithm, we do not evalute all timestamps
	// for ET. Rather, we set a smaller value of evaluted timestamps.
	public static int maxETADays = 10;
	// the duration threshold
	// Beijing: default 15, [5,10,15,20,25]
	// Porto: [5,7,9,11], default 5
	public static int k = (city_name.equals("beijing")) ? 15 : 15;
	// distance threshold, default 2, ranges [2,4,6,8,10]
	public static float epsilon = 2f;
	// initial number of all objects
	public static int objectNum = (city_name.equals("beijing")) ? 10000 : 1000000;
	// initial number of query objects
	public static int initPatientNum = (city_name.equals("beijing")) ? 200 : 60000;
	public static boolean isRandom = false;
	// use pre-check or not
	public static boolean prechecking = true;
	// the hop interval in AGP algorithm
	public static int m = 2;
	// the minimal number of grid cells that enables efficient MBR pre-checking
	public static int minMBR = 20;
	// the evaluted number of experimental study
	public static int expNum = 1;

	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
