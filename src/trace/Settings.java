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
	public static String name = "beijing";
	// sampling interval
	public static int sr = (name.equals("beijing")) ? 10 : 5;
	// data path
	public static String dataPath = String.format("/home/like/data/contact_tracer/%s%s/", name, sr);
	public static int maxProcessDays = 1;
	// due to the rime-consuming of ET algorithm, we do not evalute all timestamps
	// for ET. Rather, we set a smaller value of evaluted timestamps.
	public static int maxETADays = 10;
	public static int maxTSNB = 10;

	// the duration threshold
	// Beijing: default 15, [5,10,15,20,25]
	// Porto: [5,7,9,11], default 5
	public static int k = (name.equals("beijing")) ? 5 : 5;
	// distance threshold, default 2, ranges [2,4,6,8,10]
	public static float epsilon = 2f;
	// initial number of all objects
	public static int objectNum = (name.equals("beijing")) ? 100000 : 1000000;
	// initial number of query objects
	public static int initPatientNum = (name.equals("beijing")) ? 20000 : 60000;
	public static boolean isRandom = false;
	// use pre-check or not
	public static boolean prechecking = true;
	// the hop interval in AGP algorithm
	public static int m = 2;
	// the minimal number of grid cells that enables efficient MBR pre-checking
	public static int minMBR = 20;
	// the expand times of trajectory location data
	public static int expTimes = 1;

	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
