/*
 * @Descripttion: 
 * @version: 
 * @Author: Paulzzzhang
 * @Date: 2022-03-30 12:02:25
 * @LastEditors: Paulzzzhang
 * @LastEditTime: 2022-03-30 21:12:54
 */
package data_loader;

public class Location {

	public int id = -1;
	public String date = "2020-20-20";
	public String time = "-1:-1:-1";
	public float lon = -1;
	public float lat = -1;
	public int ts = -1;

	// �Ƿ������нӴ���
	public boolean isContact = false;

	// ���ڵ�����ID
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
