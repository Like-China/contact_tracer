package trace;

public class Settings {
	
	public static int maxProcessDays = 12; // 最大处理天数
	public static int sr = 50; // 读取的轨迹采样率
	public static int duration_threshold = 30; // 设定的感染时长
	public static float distance_threshold = 1f; // 设定的感染距离
	public static String city_name = "beijing";
	public static int objectNum = (city_name.equals("beijing")) ? 10000:1000000; // 初始化物体数目
	public static int initPatientNum = (city_name.equals("beijing")) ? 500:60000;// 初始化病人数目
	public static boolean isRandom = false; // 病人的生成是否随机
	public static String dataPath = String.format("/home/Like/data/contact_tracer/%s%s/", city_name, sr); // 存储位置流数据的目录

	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
