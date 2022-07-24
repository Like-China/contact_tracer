/*
 * @Author: your name
 * @Date: 2022-03-30 12:02:25
 * @LastEditTime: 2022-03-30 13:41:55
 * @LastEditors: your name
 * @Description: 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 * @FilePath: /contact_tracer/src/data_loader/Object.java
 */
package data_loader;

import java.util.ArrayList;

public class Object {
	
	ArrayList<Location> locations = new ArrayList<Location>();
	

	public Object(Location l) {
		// TODO Auto-generated constructor stub
		locations.add(l);
	}
	

}
