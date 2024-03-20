/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 20:12:49
 */

package loader;

// the cell object that contains cases of exposure/queries
public class InfectedArea {
	// the id of the grid cell
	public int[] areaIDs;
	// current time stamp
	public int currentTs;
	// time
	public String time;

	/**
	 * @name:
	 * @msg:
	 * @return {*}
	 */
	public InfectedArea(int[] areaIDs, int currentTs, String time) {
		// TODO Auto-generated constructor stub
		this.areaIDs = areaIDs;
		this.currentTs = currentTs;
		this.time = time;
	}

}
