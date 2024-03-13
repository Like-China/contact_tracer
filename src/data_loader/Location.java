/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 20:15:41
 */

package data_loader;

import indexes.MyRectangle;
import trace.Settings;

public class Location {
	// basic location info
	public int id = -1;
	public double lon = -1;
	public double lat = -1;
	public int ts = -1;
	// contacted timestamped location or not
	public boolean isContact = false;
	// the belonging cell id
	public int areaID = -1;
	// influenced rectangle
	public MyRectangle infRec;

	public Location() {
		// TODO Auto-generated constructor stub
	}

	public Location(int id, double lon, double lat, int ts) {
		this.id = id;
		this.lon = lon;
		this.lat = lat;
		this.ts = ts;
		double expandScale = Settings.epsilon / 10000;
		this.infRec = new MyRectangle(this,  lon - expandScale, lat - expandScale, 2 * expandScale, 2 * expandScale);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String res = this.id + " " + this.lon + " " + this.lat + " " + this.ts;
		return res;
	}

	public int getAreaID() {
		return areaID;
	}

	public void setAreaID(int areaID) {
		this.areaID = areaID;
	}

}
