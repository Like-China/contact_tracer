/*
 * @Author: your name
 * @Date: 2022-03-30 12:02:24
 * @LastEditTime: 2022-03-30 13:41:26
 * @LastEditors: your name
 * @FilePath: /contact_tracer/src/data_loader/InfectedArea.java
 */
package data_loader;


public class InfectedArea {
	public int[] areaIDs;
	public int currentTs;
	public String time;
	
	public InfectedArea(int[] areaIDs, int currentTs, String time) {
		// TODO Auto-generated constructor stub
		this.areaIDs = areaIDs;
		this.currentTs = currentTs;
		this.time = time;
	}
	

	
}
