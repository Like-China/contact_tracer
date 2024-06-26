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
	public static String name = "porto";
	// sampling interval
	public static int sr = (name.equals("beijing")) ? 10 : 15;
	// data path
	public static String dataPath = "/home/like/data/contact_tracer/" + name + ".txt";
	// due to the rime-consuming of ET algorithm, we do not evalute all timestamps
	// for ET. Rather, we set a smaller value of evaluted timestamps.
	public static int maxTSNB = 20;
	// the contact duration threshold
	public static int k = 10;
	// distance threshold, default 2, ranges [2,4,6,8,10]
	public static float epsilon = 2f;
	// initial number of all objects
	public static int objectNum = (name.equals("beijing")) ? 200000 : 200000;
	// initial number of query objects
	public static int initPatientNum = (name.equals("beijing")) ? 2000 : 2000;
	public static boolean isRandom = true;
	// the hop interval in AGP algorithm
	public static int m = 2;
	// the expand times of trajectory location data
	public static int expNB = 4;
	public static double[] lonRange = name == "beijing" ? new double[] { 116.25f - 0.001f, 116.55f + 0.001f }
			: new double[] { -8.735f - 0.0015f, -8.156f + 0.0015f };
	public static double[] latRange = name == "beijing" ? new double[] { 39.83f - 0.001f, 40.03f + 0.001f }
			: new double[] { 40.953f - 0.0015f, 41.307f + 0.0015f };

	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
