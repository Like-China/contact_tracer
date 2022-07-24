/*
 * @Author: your name
 * @Date: 2022-03-30 17:24:57
 * @LastEditTime: 2022-06-13 15:19:01
 * @LastEditors: Like likemelikeyou@126.com
 * @Description: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 * @FilePath: /contact_tracer/src/trace/ETA_Tracer.java
 */
package trace;

/**
 * exact travesal algorithm
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;


public class ETA_Tracer {
	public double distance_threshold;
	public int duration_threshold;
	// patient ids includes already infected objects and new discoverd cases of exposure
	public HashSet<Integer> patientIDs;
	// infected areas at current timestamp (e.g., at least one patients in this area)
	public  HashSet<Integer> areas;
	// record each object's infected duration
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	// record each object is infected at current timestamp or not
	public HashMap<Integer, Boolean> isInfect = new HashMap<Integer, Boolean>();
	//  π”√distance
	public Distance D = new Distance();
	
	public ETA_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		this.duration_threshold = duration_threshold;
	}
	
	/**
	 * calculate exact distance from each non-query location to each query location
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch)
	{	
		// record new cases of exposure
		ArrayList<Integer> res = new ArrayList<Integer>();
		// 1. divided all current locations into query locations and non-query locations
		ArrayList<Location> query_locations = new ArrayList<Location>();
		ArrayList<Location> ordinary_locations = new ArrayList<Location>();
		for(Location l:batch)
		{
			if(patientIDs.contains(l.id))
			{
				query_locations.add(l);
			}else {
				ordinary_locations.add(l);
			}
		}
		
		// 2. calculate exact distance among each two non-query locations and query location
		for(Location l1: ordinary_locations)
		{
			for(Location l2: query_locations)
			{
				double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
				if(dis <= distance_threshold)
				{	
					if(!objectMapDuration.containsKey(l1.id)) objectMapDuration.put(l1.id, 0);
					int duration = objectMapDuration.get(l1.id)+1;
					objectMapDuration.put(l1.id, duration);
					// duration exceeds threshold, add it to query object
					if (duration >= duration_threshold)
					{
						patientIDs.add(l1.id);
						res.add(l1.id);
					}
					// mark this location as checked
					l1.isContact = true;
					isInfect.put(l1.id, true);
					break;
				}
			}
			
			// set infected duration to 0 if objects are not infected at current timestamp
			if(!l1.isContact)
			{
				if(objectMapDuration.containsKey(l1.id))
				{
					objectMapDuration.put(l1.id, 0);
				}
			}
		}
		// System.out.printf("New cases: %d ", res.size());
		// System.out.printf("Current cases: %d \n", patientIDs.size());
		return res;
	}
}
