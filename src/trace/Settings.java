/*
 * @Descripttion: 
 * @version: 
 * @Author: Paulzzzhang
 * @Date: 2022-03-30 12:02:25
 * @LastEditors: Like likemelikeyou@126.com
 * @LastEditTime: 2022-06-13 11:41:47
 */

package trace;
public class Settings {

	// default parameters settings
	public static String city_name = "porto"; 
	public static int maxProcessDays = 20; 
	public static int maxETADays = 10; // to get eta mean runtime, 不测试所有的天数,beijing ETA不再实验
	public static int sr = (city_name.equals("beijing")) ? 10:5; // sampling rate
	public static int duration_threshold = (city_name.equals("beijing")) ? 15:15; // Beijing: default 15, [5,10,15,20,25]; Porto: [5,7,9,11], default 5
	public static float distance_threshold = 2f; // Beijing, Porto: default 2, ranges [2,4,6,8,10]
	public static int objectNum = (city_name.equals("beijing")) ? 10000 : 1000000; // initial number of all objects
	public static int initPatientNum = (city_name.equals("beijing")) ? 200 : 60000;// initial number of query objects
	public static boolean isRandom = false; 
	public static String dataPath = String.format("/home/Like/data/contact_tracer/%s%s/", city_name, sr); // data path
	public static boolean prechecking = true;
	public static int m = 2; // how often use EGP for AGP algorithm
	public static int minMBR = 20;
	public static int expNum = 1;

	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
