package trace;

public class Settings {
	
	public static int days = 12;
	public static int sr = 50; // ��ȡ�Ĺ켣������
	public static int duration_threshold = 30; // �趨�ĸ�Ⱦʱ��
	public static float distance_threshold = 1f; // �趨�ĸ�Ⱦ����
	public static String city_name = "beijing";
	public static int objectNum = (city_name == "beijing") ? 10000:1000000; // ��ʼ��������Ŀ
	public static int initPatientNum = (city_name == "beijing") ? 500:60000;// ��ʼ��������Ŀ
	public static boolean isRandom = false; // ���˵������Ƿ����
	
	public Settings() {
		// TODO Auto-generated constructor stub
	}

}
