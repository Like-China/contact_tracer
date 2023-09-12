package indexes;

public class Distance {

	public long calcCount = 0;
	public long runtime = 0;

	public Distance() {
		// TODO Auto-generated constructor stub
	}

	// distance between two locations (meters)
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
