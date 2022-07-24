/*
 * @Author: your name
 * @Date: 2022-03-30 12:02:25
 * @LastEditTime: 2022-06-13 15:14:50
 * @LastEditors: Like likemelikeyou@126.com
 * @Description:  https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 * @FilePath: /java_workplace/contact_tracer/src/indexes/Distance.java
 */
package indexes;


public class Distance {

	// 使用distance的次数
	public  long calcCount= 0;
	// distance用时
	public long runtime = 0;

	public Distance() {
		// TODO Auto-generated constructor stub
	}
	
	
	// distance between two locations (meters)
	public double distance(double lat1, double lon1, double lat2, double lon2) 
	{
		calcCount += 1;
		long startTime = System.nanoTime();
	    double theta = lon1 - lon2;
		double deg2rad_lat1 = deg2rad(lat1);
		double deg2rad_lat2 = deg2rad(lat2);
	    double dist = Math.sin(deg2rad_lat1) * Math.sin(deg2rad_lat2)
	                + Math.cos(deg2rad_lat1) * Math.cos(deg2rad_lat2)
	                * Math.cos(deg2rad(theta));
	    dist = Math.acos(dist);
	    dist = rad2deg(dist);
	    double miles = dist * 60 * 1.1515;
		long endTime = System.nanoTime();
		runtime += (endTime-startTime);
	    return miles*1000;
	}
	
	public static  double deg2rad(double degree) 
	{
	    return degree / 180 * Math.PI;
	}
	public  static double rad2deg(double radian) 
	{
	    return radian * 180 / Math.PI;
	}

}
