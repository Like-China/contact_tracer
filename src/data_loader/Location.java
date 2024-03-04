/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 20:15:41
 */

package data_loader;

public class Location {
	// basic location info
	public int id = -1;
	public String date = "2020-20-20";
	public String time = "-1:-1:-1";
	public float lon = -1;
	public float lat = -1;
	public int ts = -1;
	// contacted timestamped location or not
	public boolean isContact = false;
	// the belonging cell id
	public int areaID = -1;

	public Location() {
		// TODO Auto-generated constructor stub
	}

	public Location(int id, String date, String time, float lon, float lat, int ts) {
		this.id = id;
		this.date = date;
		this.time = time;
		this.lon = lon;
		this.lat = lat;
		this.ts = ts;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String res = this.id + " " + this.date + " " + this.time + " " + this.lon + " " + this.lat + " " + " " + this.ts
				+ "\n";
		return res;
	}

	public int getAreaID() {
		return areaID;
	}

	public void setAreaID(int areaID) {
		this.areaID = areaID;
	}

}
