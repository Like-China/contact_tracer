package indexes;

import data_loader.Location;

public class Distance {

	public Distance() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * ��������Location�����ľ��룬���Ϊ�ױ�ʾ
	 * @param startLocation
	 * @param endLocation
	 * @return
	 */
	public static double getEuclideanDistance(Location startLocation, Location endLocation)
	{
		double r = 3959; // ƽ���뾶
		double startRadianLat = Math.toRadians((double)(startLocation.lat));
		double endRadianLat = Math.toRadians((double)(endLocation.lat));
		double diff = endRadianLat-startRadianLat;
		double lambda = Math.toRadians((double)(endLocation.lon-startLocation.lon));
		double a = Math.sin(diff/2)*Math.sin(diff/2)+Math.cos(startRadianLat)*
				Math.cos(endRadianLat)*Math.sin(lambda/2)*Math.sin(lambda/2);
		double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = r*c;
		return d*1000;
	}
	
	// ��������λ�õ�֮��ľ��룬���Ϊǧ�ױ�ʾ
	public static double distance(double lat1, double lon1, double lat2, double lon2) 
	{
	    double theta = lon1 - lon2;
	    double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
	                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
	                * Math.cos(deg2rad(theta));
	    dist = Math.acos(dist);
	    dist = rad2deg(dist);
	    double miles = dist * 60 * 1.1515;
	    return miles*1000;
	}
	//���Ƕ�ת��Ϊ����
	public static  double deg2rad(double degree) 
	{
	    return degree / 180 * Math.PI;
	}
	//������ת��Ϊ�Ƕ�
	public  static double rad2deg(double radian) 
	{
	    return radian * 180 / Math.PI;
	}

}
