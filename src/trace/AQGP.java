/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-05 13:50:51
 */
package trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import indexes.Distance;
import indexes.MyRectangle;
import indexes.QuadTree;
import loader.Location;

/**
 * Approximate QGP implementation
 */
public class AQGP {
	// the distance threshold
	public double epsilon;
	// the duration threshold
	public int k;
	// patient ids that consist of already infected objects and new discoverd cases
	// of exposure
	public HashSet<Integer> patientIDs = new HashSet<>();
	// the map from moving object to its conduct duration with query objects
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	// record if each object contacts with at least one query at current ts or not
	public HashMap<Integer, Boolean> isContactMap = new HashMap<Integer, Boolean>();
	public QuadTree quadTree;
	public Distance D = new Distance();
	// grid cells that at least one patients in it
	public HashSet<Integer> areas = new HashSet<>();
	// the number of query grid cells of query locations
	public int totalQueryNB = 0;
	// the number of leaf nodes
	public int totalLeafNB = 0;
	public ArrayList<Location> ordinrayLocations = new ArrayList<Location>();
	// index construct time/filter time/final search time
	public long cTime = 0;
	public long fTime = 0;
	public long sTime = 0;

	public AQGP(double epsilon, int k) {
		this.epsilon = epsilon;
		this.k = k;
	}

	/**
	 * Given a set of locations, construct two quadtree indexes
	 * 
	 * @param batch
	 */
	public void constructIndex(ArrayList<Location> batch) {
		quadTree = new QuadTree(0, new MyRectangle(-1, Settings.lonRange[0], Settings.latRange[0],
				Settings.lonRange[1] - Settings.lonRange[0] + 2 * epsilon / 10000,
				Settings.latRange[1] - Settings.latRange[0] + 2 * epsilon / 10000));
		// init infected grid cell set at current timestamp
		areas = new HashSet<Integer>();
		// each area and its covered non-query locations at current timestamp
		ordinrayLocations = new ArrayList<Location>();
		for (Location l : batch) {
			if (!patientIDs.contains(l.id)) {
				// add the grid cell id to area set
				ordinrayLocations.add(l);
			} else {
				quadTree.insert(l.infRec);
			}
		}
	}

	/**
	 * continuously search updated cases of exposes with QAGP algorithm
	 * update patientIDs and return updated cases of exposes
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch, int ts, int m) {
		// 1. index construction
		long t1 = System.currentTimeMillis();
		constructIndex(batch);
		long t2 = System.currentTimeMillis();
		cTime += (t2 - t1);
		ArrayList<Integer> updateCE = new ArrayList<Integer>();
		// 2. for each ordinary location, find its intersection in query quadtree
		for (Location ordinaryLocation : ordinrayLocations) {
			t1 = System.currentTimeMillis();
			totalQueryNB += 1;
			HashSet<MyRectangle> returnObjects = new HashSet<>();
			if (ts != 0 && ts % m == 0) {
				quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, true,
						this.epsilon);
			} else {
				quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
						this.epsilon);
			}
			t2 = System.currentTimeMillis();
			fTime += (t2 - t1);
			t1 = System.currentTimeMillis();
			if (returnObjects.size() > 0) {
				totalLeafNB += 1;
				// mark this location as detected
				ordinaryLocation.isContact = true;
				isContactMap.put(ordinaryLocation.id, true);
				if (!objectMapDuration.containsKey(ordinaryLocation.id))
					objectMapDuration.put(ordinaryLocation.id, 0);
				int duration = objectMapDuration.get(ordinaryLocation.id) + 1;
				objectMapDuration.put(ordinaryLocation.id, duration);
				// new updated case of exposure
				if (duration >= k) {
					patientIDs.add(ordinaryLocation.id);
					updateCE.add(ordinaryLocation.id);
				}
			}
			t2 = System.currentTimeMillis();
			sTime += (t2 - t1);
		}
		System.out.println(totalLeafNB);
		totalLeafNB = 0;
		// 3. reset infected duration of specific objects
		for (Integer id : objectMapDuration.keySet()) {
			if (!isContactMap.containsKey(id))
				objectMapDuration.put(id, 0);
		}
		// reset contact information to process next timestamp
		isContactMap.clear();
		return updateCE;
	}

}