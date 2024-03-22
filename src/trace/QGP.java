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
 * QGP implementation, quadtree for database, non-index for query locations
 */
public class QGP {
	// the distance threshold
	public double epsilon;
	// the duration threshold
	public int k;
	// patient ids that consist of already infected objects and new discoverd cases
	// of exposure
	public HashSet<Integer> patientIDs = new HashSet<>();
	// the map from moving object to its conduct duration with query objects
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	// record each object contacts with at least one query object at current
	// timestamp or not
	public HashMap<Integer, Boolean> isContactMap = new HashMap<Integer, Boolean>();
	public QuadTree quadTree;
	// the total pre-checking number / valid pre-checking number
	public Distance D = new Distance();
	// grid cells that at least one patients in it
	public HashSet<Integer> areas = new HashSet<>();
	// the number of query grid cells of query locations
	public ArrayList<Location> ordinrayLocations = new ArrayList<Location>();
	// index construction time/filtering time/ search time
	public long cTime = 0;
	public long fTime = 0;
	public long sTime = 0;
	// updated cases of exposure
	ArrayList<Integer> updateCE = new ArrayList<Integer>();
	public int totalCheckNB = 0;

	public QGP(double epsilon, int k) {
		this.epsilon = epsilon;
		this.k = k;
	}

	/**
	 * Given a set of locations, construct two quadtree indexes
	 * 
	 * @param batch
	 */
	public void constructIndex(ArrayList<Location> batch) {
		long t1 = System.currentTimeMillis();
		quadTree = new QuadTree(0, new MyRectangle(-1, Settings.lonRange[0], Settings.latRange[0],
				Settings.lonRange[1] - Settings.lonRange[0] + 2 * epsilon / 10000,
				Settings.latRange[1] - Settings.latRange[0] + 2 * epsilon / 10000), true);
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
		cTime += (System.currentTimeMillis() - t1);
	}

	/**
	 * continuously search updated cases of exposes with QGP algorithm
	 * update patientIDs and return updated cases of exposes
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch) {
		// 1. index construction
		constructIndex(batch);
		// 2. for each leaf node of queryTree, find its intersection in dbTree
		long t1 = System.currentTimeMillis();
		for (Location ordinaryLocation : ordinrayLocations) {
			ArrayList<MyRectangle> returnObjects = new ArrayList<>();
			quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
					this.epsilon);
			if (returnObjects.size() > 0) {
				// mark this location as detected
				ordinaryLocation.isContact = true;
				isContactMap.put(ordinaryLocation.id, true);
				objectMapDuration.putIfAbsent(ordinaryLocation.id, 0);
				int duration = objectMapDuration.compute(ordinaryLocation.id, (k, v) -> v + 1);
				// new updated case of exposure
				if (duration >= k) {
					patientIDs.add(ordinaryLocation.id);
					updateCE.add(ordinaryLocation.id);
				}
			}
		}

		// 3. reset infected duration of specific objects
		for (Integer id : objectMapDuration.keySet()) {
			if (!isContactMap.containsKey(id))
				objectMapDuration.put(id, 0);
		}
		// reset contact information to process next timestamp
		isContactMap.clear();
		totalCheckNB += quadTree.checkNB;
		fTime += (System.currentTimeMillis() - t1);
		return updateCE;
	}

}