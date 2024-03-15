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
import data_loader.Location;
import indexes.Distance;
import indexes.GridIndex;
import indexes.MyRectangle;
import indexes.QuadTree;

/**
 * QGP implementation, construct a quadtree for database, use gridindex for
 * query locations
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
	double scale;

	// grid cells that at least one patients in it
	public HashSet<Integer> areas = new HashSet<>();
	public GridIndex g;
	// the number of query grid cells of query locations
	public int totalQueryNB = 0;
	// the number of refine calculation
	public int totalCheckNB = 0;
	// the number of leaf nodes
	public int totalLeafNB = 0;

	public long cTime = 0;
	public long fTime = 0;
	public long sTime = 0;

	public QGP(double epsilon, int k, String cityname) {
		this.epsilon = epsilon;
		this.k = k;
		// this.scale = (epsilon / 10000 / Math.sqrt(2));
		this.scale = epsilon / 10000 * 2;
		g = new GridIndex(scale, cityname);
		// System.out.println(g.cellNB);
	}

	public void updateMBR(double[] initMBR, Location l) {
		initMBR[0] = initMBR[0] > l.lon ? l.lon : initMBR[0];
		initMBR[1] = initMBR[1] < l.lon ? l.lon : initMBR[1];
		initMBR[2] = initMBR[2] > l.lat ? l.lat : initMBR[2];
		initMBR[3] = initMBR[3] < l.lat ? l.lat : initMBR[3];
	}

	/**
	 * QGP implementation, quadtree for database, gridindex for query locations
	 * 
	 * @param batch
	 */
	public void constructIndex(ArrayList<Location> batch) {
		quadTree = new QuadTree(0, new MyRectangle(-1, Settings.lonRange[0], Settings.latRange[0],
				Settings.lonRange[1] - Settings.lonRange[0], Settings.latRange[1] - Settings.latRange[0]));
		// init infected grid cell set at current timestamp
		areas = new HashSet<Integer>();
		// each area and its covered non-query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		HashMap<Integer, double[]> patientAreasMBR = new HashMap<>();
		for (Location l : batch) {
			int areaID = g.getID(l.lon, l.lat);
			l.setAreaID(areaID);
			if (!patientAreasLocations.containsKey(areaID)) {
				patientAreasLocations.put(areaID, new ArrayList<Location>());
				patientAreasMBR.put(areaID, new double[] { 1000f, -1000f, 1000f, -1000f });
			}
			if (patientIDs.contains(l.id)) {
				// add the grid cell id to area set
				areas.add(areaID);
				patientAreasLocations.get(areaID).add(l);
				// get the MBR of all query locations in this grid cell before adding location
				double[] rawMBR = patientAreasMBR.get(areaID);
				// update the MBR if location l is outside MBR
				updateMBR(rawMBR, l);
			} else {
				quadTree.insert(l.infRec);
			}
		}
		// update grid index
		g.setPatientAreasLocations(patientAreasLocations);
		g.patientAreasMBR = patientAreasMBR;

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
		long t1 = System.currentTimeMillis();
		constructIndex(batch);
		// quadTree.dfs();
		// System.exit(0);
		totalQueryNB += areas.size();
		long t2 = System.currentTimeMillis();
		cTime += (t2 - t1);
		ArrayList<Integer> updateCE = new ArrayList<Integer>();
		// 2. for each leaf node of queryTree, find its intersection in dbTree
		for (Integer areaID : areas) {
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);

			t1 = System.currentTimeMillis();
			double[] patientMBR = g.patientAreasMBR.get(areaID);
			MyRectangle queryRec = new MyRectangle(-1, patientMBR[0], patientMBR[2], patientMBR[1] - patientMBR[0],
					patientMBR[3] - patientMBR[2]);
			HashSet<Integer> returnObjects = new HashSet<>();
			quadTree.retrieve(returnObjects, queryRec);
			t2 = System.currentTimeMillis();
			fTime += (t2 - t1);
			t1 = System.currentTimeMillis();
			// for (MyRectangle rec : returnObjects) {
			for (int id : returnObjects) {
				Location l1 = batch.get(id);
				if (l1.isContact)
					continue;
				for (Location l2 : patientLocations) {
					totalCheckNB += 1;
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
			}
			t2 = System.currentTimeMillis();
			sTime += (t2 - t1);

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