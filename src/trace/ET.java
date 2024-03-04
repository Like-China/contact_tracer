/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:08:22
 */
package trace;

// exact travesal algorithm (ET algorithm in the paper)
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;

public class ET {
	// the distance thupdatedCEhold
	public double epsilon;
	// the duration thupdatedCEhold
	public int k;
	// patient ids that consist of already infected objects and new discoverd cases
	// of exposure
	public HashSet<Integer> patientIDs;
	// grid cells that at least one patients in it
	public HashSet<Integer> areas;
	// the map from moving object to its conduct duration with query objects
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	// record each object contacts with at least one query object at current
	// timestamp or not
	public HashMap<Integer, Boolean> isContact = new HashMap<Integer, Boolean>();
	public Distance D = new Distance();

	public ET(double epsilon, int k, String cityname) {
		super();
		this.epsilon = epsilon;
		this.k = k;
	}

	/**
	 * continuously search updated cases of exposes with exact traversal algorithm
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch) {
		// record new cases of exposure
		ArrayList<Integer> updatedCE = new ArrayList<Integer>();
		// 1. divided all current locations into query locations and non-query locations
		ArrayList<Location> queryLocations = new ArrayList<Location>();
		ArrayList<Location> otherLocations = new ArrayList<Location>();
		for (Location l : batch) {
			if (patientIDs.contains(l.id)) {
				queryLocations.add(l);
			} else {
				otherLocations.add(l);
			}
		}

		// 2. calculate exact distance among each two query and non-query locations
		for (Location l1 : otherLocations) {
			for (Location l2 : queryLocations) {
				double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
				if (dis <= epsilon) {
					if (!objectMapDuration.containsKey(l1.id))
						objectMapDuration.put(l1.id, 0);
					int duration = objectMapDuration.get(l1.id) + 1;
					objectMapDuration.put(l1.id, duration);
					// duration exceeds thupdatedCEhold, add it to query object
					if (duration >= k) {
						patientIDs.add(l1.id);
						updatedCE.add(l1.id);
					}
					// mark this location as checked
					l1.isContact = true;
					isContact.put(l1.id, true);
					break;
				}
			}

			// set infected duration to 0 if objects are not infected at current timestamp
			if (!l1.isContact) {
				if (objectMapDuration.containsKey(l1.id)) {
					objectMapDuration.put(l1.id, 0);
				}
			}
		}
		// System.out.printf("New cases: %d ", updatedCE.size());
		// System.out.printf("Current cases: %d \n", patientIDs.size());
		return updatedCE;
	}
}
