package trace;

public class Settings {
	
	public static int days = 12;
	public static int sr = 50; // 读取的轨迹采样率
	public static int duration_threshold = 30; // 设定的感染时长
	public static float distance_threshold = 1f; // 设定的感染距离
	public static String city_name = "beijing";
	public static int objectNum = (city_name == "beijing") ? 10000:1000000; // 初始化物体数目
	public static int initPatientNum = (city_name == "beijing") ? 500:60000;// 初始化病人数目
	public static boolean isRandom = false; // 病人的生成是否随机
	
	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
