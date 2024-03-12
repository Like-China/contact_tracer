/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-05 13:50:51
 */
package trace;

/**
 * QGP implementation, qiery?db????????
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import data_loader.Location;
import indexes.Distance;
import indexes.QuadTree;
import indexes.QuadTree.CoordHolder;
import indexes.QuadTree.Quad;

public class QGP2 {
	// the distance threshold
	public double epsilon;
	// the duration threshold
	public int k;
	// patient ids that consist of already infected objects and new discoverd cases
	// of exposure
	public HashSet<Integer> patientIDs;
	// the map from moving object to its conduct duration with query objects
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	// record each object contacts with at least one query object at current
	// timestamp or not
	public HashMap<Integer, Boolean> isContactMap = new HashMap<Integer, Boolean>();
	public QuadTree qQuadTree, dbQuadTree;
	// the total pre-checking number / valid pre-checking number
	public Distance D = new Distance();
	double scale;

	public QGP2(double epsilon, int k, String cityname) {
		this.epsilon = epsilon;
		this.k = k;
		this.scale = (epsilon / 10000 / Math.sqrt(2));
		qQuadTree = new QuadTree();
		dbQuadTree = new QuadTree();
		qQuadTree.DYNAMIC_MAX_OBJECTS = true;
		qQuadTree.MAX_OBJ_TARGET_EXPONENT = 0.5;
		dbQuadTree.DYNAMIC_MAX_OBJECTS = true;
		dbQuadTree.MAX_OBJ_TARGET_EXPONENT = 0.5;
	}

	/**
	 * Given a set of locations, construct two quadtree indexes
	 * 
	 * @param batch
	 */
	public void constructIndex(ArrayList<Location> batch) {
		qQuadTree = new QuadTree();
		dbQuadTree = new QuadTree();
		qQuadTree.DYNAMIC_MAX_OBJECTS = true;
		qQuadTree.MAX_OBJ_TARGET_EXPONENT = 0.5;
		dbQuadTree.DYNAMIC_MAX_OBJECTS = true;
		dbQuadTree.MAX_OBJ_TARGET_EXPONENT = 0.5;
		for (Location l : batch) {
			if (patientIDs.contains(l.id)) {
				qQuadTree.place(l.lat, l.lon, l);
			} else {
				dbQuadTree.place(l.lat, l.lon, l);
			}
		}

	}

	/**
	 * continuously search updated cases of exposes with QGP2 algorithm
	 * update patientIDs and return updated cases of exposes
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch) {
		// 1. index construction
		constructIndex(batch);
		ArrayList<Integer> updateCE = new ArrayList<Integer>();
		// 2. for each leaf node of queryTree, find its intersection in dbTree
		List<Quad> leafs = qQuadTree.getAllLeafs();
		for (Quad leaf : leafs) {
			double minX = 1000;
			double maxX = -1000;
			double minY = 1000;
			double maxY = -1000;
			for (CoordHolder ch : leaf.items) {
				minX = minX > ch.x ? ch.x : minX;
				minY = minY > ch.y ? ch.y : minY;
				maxX = maxX > ch.x ? maxX : ch.x;
				maxY = maxY > ch.y ? maxY : ch.y;
			}

			List<CoordHolder> influenced = dbQuadTree.findAll(minX - epsilon / 10000,
					maxX + epsilon / 10000,
					minY - epsilon / 10000, maxY + epsilon / 10000);
			for (CoordHolder db_ch : influenced) {
				Location l1 = db_ch.o;
				if (l1.isContact)
					continue;
				for (CoordHolder q_ch : leaf.items) {
					Location l2 = q_ch.o;
					double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
					if (dis <= epsilon) {
						// mark this location as detected
						l1.isContact = true;
						isContactMap.put(l1.id, true);
						if (!objectMapDuration.containsKey(l1.id))
							objectMapDuration.put(l1.id, 0);
						int duration = objectMapDuration.get(l1.id) + 1;
						objectMapDuration.put(l1.id, duration);
						// new updated case of exposure
						if (duration >= k) {
							patientIDs.add(l1.id);
							updateCE.add(l1.id);
						}
						break;
					}
				}
			} // End 2
		}

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