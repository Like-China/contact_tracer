/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 20:22:17
 */
package indexes;

public class Distance {

	// once the function distance is triggered, the calcCount will be added by 1
	public long calcCount = 0;
	// the total runtime of multiple distance calculations
	public long runtime = 0;

	public Distance() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @name:
	 * @msg:
	 * @param {double} lat1 the latitude of source location
	 * @param {double} lon1 the longtitude of source location
	 * @param {double} lat2 the latitude of target location
	 * @param {double} lon2 the longtitude of target location
	 * @return {the tranfered distance between two locations (meters)*}
	 */
	public double distance(double lat1, double lon1, double lat2, double lon2) {
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
		runtime += (endTime - startTime);
		return miles * 1000;
	}

	public static double deg2rad(double degree) {
		return degree / 180 * Math.PI;
	}

	public static double rad2deg(double radian) {
		return radian * 180 / Math.PI;
	}

}
