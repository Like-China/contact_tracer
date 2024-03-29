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
 * EGP implementation
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import indexes.Distance;
import indexes.GridIndex;
import loader.Location;

public class EGP {
	// the distance threshold
	public double epsilon;
	// the duration threshold
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
	public HashMap<Integer, Boolean> isContactMap = new HashMap<Integer, Boolean>();
	public GridIndex g;
	// the minimal objects within a MBR, or we do not apply MBR for pre-checking
	public int minMBR = Settings.minMBR;
	// the total pre-checking number / valid pre-checking number
	public int totalCheckNums = 0;
	public int validCheckNums = 0;
	public Distance D = new Distance();

	public long cTime = 0;
	public long fTime = 0;
	public long sTime = 0;

	public int totalQueryNB = 0;
	public int totalCheckNB = 0;

	public EGP(double epsilon, int k) {
		this.epsilon = epsilon;
		double scale = (epsilon / 10000 / Math.sqrt(2));
		this.k = k;
		g = new GridIndex(scale);
	}

	/**
	 * use a new location to update a MBR
	 * 
	 * @param initMBR the MBR of a set of locations before adding location l
	 * @param l       new location
	 */
	public void updateMBR(double[] initMBR, Location l) {
		initMBR[0] = initMBR[0] > l.lon ? l.lon : initMBR[0];
		initMBR[1] = initMBR[1] < l.lon ? l.lon : initMBR[1];
		initMBR[2] = initMBR[2] > l.lat ? l.lat : initMBR[2];
		initMBR[3] = initMBR[3] < l.lat ? l.lat : initMBR[3];
	}

	/**
	 * Given a set of locations, mark grid cells that contains cases of exposure
	 * Meanwhile, we establish non-query location group/query location group for
	 * each grid cell
	 * 
	 * @param batch
	 */
	public void findInfectedArea(ArrayList<Location> batch) {
		long t1 = System.currentTimeMillis();
		// init infected grid cell set at current timestamp
		areas = new HashSet<Integer>();
		// each area and its covered non-query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// each area and its covered query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		HashMap<Integer, double[]> ordinaryAreasMBR = new HashMap<>();
		HashMap<Integer, double[]> patientAreasMBR = new HashMap<>();
		for (Location l : batch) {
			int areaID = g.getID(l.lon, l.lat);
			l.setAreaID(areaID);
			if (!ordinaryAreasLocations.containsKey(areaID)) {
				ordinaryAreasLocations.put(areaID, new ArrayList<Location>());
				ordinaryAreasMBR.put(areaID, new double[] { 1000f, -1000f, 1000f, -1000f });
			}
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
				// get the MBR of all query locations in this grid cell before adding location
				ordinaryAreasLocations.get(areaID).add(l);
				double[] rawMBR = ordinaryAreasMBR.get(areaID);
				updateMBR(rawMBR, l);
			}
		}
		// update grid index
		g.setOrdinaryAreasLocations(ordinaryAreasLocations);
		g.setPatientAreasLocations(patientAreasLocations);
		g.ordinaryAreasMBR = ordinaryAreasMBR;
		g.patientAreasMBR = patientAreasMBR;
		long t2 = System.currentTimeMillis();
		cTime += (t2 - t1);
	}

	/**
	 * continuously search updated cases of exposes with EGP algorithm
	 * update patientIDs and return updated cases of exposes
	 * apply pre-checking with MBR partition
	 * 
	 * @param batch      a list of location at the same timesampe
	 * @param isPreCheck use prechecking or not
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch, boolean isPreCheck) {
		// 1. get infected "areas"
		// get each patient area and its covered locations "g.patientAreasLocations"
		// get each ordinary area and its covered locations "g.ordinaryAreasLocations"
		findInfectedArea(batch);
		totalQueryNB += areas.size();
		ArrayList<Integer> updateCE = new ArrayList<Integer>();
		// 2. for influenced grid cells of infected areas (include infected area
		// itself), we check objects within them are infected or not

		for (Integer areaID : areas) {
			// 2.1 find influenced areas' ids
			int[] nnIDs = g.getAffectAreas(areaID);
			long t1 = System.currentTimeMillis();
			// 2.2 find query locations at infected area, and get its MBR
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			// 2.3 check objects within these influenced area
			for (int nn : nnIDs) {
				// out of space range (four corner grid cells)
				if (nn == -1)
					continue;
				// get the location of non-query objects within this grid cell nn
				// any logical error in this line??
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				if (ordinaryLocations == null || ordinaryLocations.isEmpty())
					continue;
				// get query locations within this grid cell at curent timestamp
				double[] patientMBR = g.patientAreasMBR.get(areaID);
				// get four MBR vertexes
				double[][] patientMBRVertexs = new double[4][2];
				patientMBRVertexs[0] = new double[] { patientMBR[0], patientMBR[2] };
				patientMBRVertexs[1] = new double[] { patientMBR[0], patientMBR[3] };
				patientMBRVertexs[2] = new double[] { patientMBR[1], patientMBR[2] };
				patientMBRVertexs[3] = new double[] { patientMBR[1], patientMBR[3] };
				// start pre-checking if objects within MBR exceed specific number
				// if the number of locations within this grid cell is small, then no
				// pre-checking
				if (isPreCheck && nn != areaID && patientLocations.size() * ordinaryLocations.size() >= minMBR) {
					totalCheckNums += 1;
					double[] ordinaryMBR = g.ordinaryAreasMBR.get(nn);
					double[][] ordinaryMBRVertexs = new double[4][2];
					ordinaryMBRVertexs[0] = new double[] { ordinaryMBR[0], ordinaryMBR[2] };
					ordinaryMBRVertexs[1] = new double[] { ordinaryMBR[0], ordinaryMBR[3] };
					ordinaryMBRVertexs[2] = new double[] { ordinaryMBR[1], ordinaryMBR[2] };
					ordinaryMBRVertexs[3] = new double[] { ordinaryMBR[1], ordinaryMBR[3] };
					double min_dist = 10000;
					// double max_dist = -100;
					// 4 vertexes * 4 vertexes calculation to get min distance between two MBRs\
					// double m1 = Math.abs(ordinaryMBR[0]-patientMBR[0]);
					// double m2 = Math.abs(ordinaryMBR[0]-patientMBR[1]);
					// double m3 = Math.abs(ordinaryMBR[0]-patientMBR[0]);
					// double m4 = Math.abs(ordinaryMBR[0]-patientMBR[0]);
					if (ordinaryMBR[0] > patientMBR[1] && ordinaryMBR[3] < patientMBR[2]) {
						// case 1: left-top
						min_dist = D.distance(patientMBRVertexs[2][1], patientMBRVertexs[2][0],
								ordinaryMBRVertexs[1][1],
								ordinaryMBRVertexs[1][0]);
						// max_dist = D.distance(patientMBRVertexs[1][1], patientMBRVertexs[1][0],
						// ordinaryMBRVertexs[2][1],
						// ordinaryMBRVertexs[2][0]);
					} else if (ordinaryMBR[0] > patientMBR[1] && ordinaryMBR[2] > patientMBR[3]) {
						// case 2: left-bottle
						min_dist = D.distance(patientMBRVertexs[3][1], patientMBRVertexs[3][0],
								ordinaryMBRVertexs[0][1],
								ordinaryMBRVertexs[0][0]);
						// max_dist = D.distance(patientMBRVertexs[0][1], patientMBRVertexs[0][0],
						// ordinaryMBRVertexs[3][1],
						// ordinaryMBRVertexs[3][0]);
					} else if (ordinaryMBR[1] < patientMBR[0] && ordinaryMBR[3] < patientMBR[2]) {
						// case 3: right-top
						min_dist = D.distance(patientMBRVertexs[0][1], patientMBRVertexs[0][0],
								ordinaryMBRVertexs[3][1],
								ordinaryMBRVertexs[3][0]);
						// max_dist = D.distance(patientMBRVertexs[3][1], patientMBRVertexs[3][0],
						// ordinaryMBRVertexs[0][1],
						// ordinaryMBRVertexs[0][0]);
					} else if (ordinaryMBR[1] < patientMBR[0] && ordinaryMBR[2] > patientMBR[3]) {
						// case 4: right-bottle
						min_dist = D.distance(patientMBRVertexs[1][1], patientMBRVertexs[1][0],
								ordinaryMBRVertexs[2][1],
								ordinaryMBRVertexs[2][0]);
						// max_dist = D.distance(patientMBRVertexs[2][1], patientMBRVertexs[2][0],
						// ordinaryMBRVertexs[1][1],
						// ordinaryMBRVertexs[1][0]);
					} else if (ordinaryMBR[3] < patientMBR[2]) {
						// case 5:top
						double min_dist1 = D.distance(patientMBRVertexs[0][1], patientMBRVertexs[0][0],
								ordinaryMBRVertexs[1][1],
								ordinaryMBRVertexs[1][0]);
						double min_dist2 = D.distance(patientMBRVertexs[2][1], patientMBRVertexs[2][0],
								ordinaryMBRVertexs[3][1],
								ordinaryMBRVertexs[3][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(patientMBRVertexs[1][1],
						// patientMBRVertexs[1][0], ordinaryMBRVertexs[2][1],
						// ordinaryMBRVertexs[2][0]);
						// double max_dist2 = D.distance(patientMBRVertexs[3][1],
						// patientMBRVertexs[3][0], ordinaryMBRVertexs[0][1],
						// ordinaryMBRVertexs[0][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					} else if (ordinaryMBR[2] > patientMBR[3]) {
						// case 6: bottle
						double min_dist1 = D.distance(patientMBRVertexs[1][1], patientMBRVertexs[1][0],
								ordinaryMBRVertexs[0][1],
								ordinaryMBRVertexs[0][0]);
						double min_dist2 = D.distance(patientMBRVertexs[3][1], patientMBRVertexs[3][0],
								ordinaryMBRVertexs[2][1],
								ordinaryMBRVertexs[2][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(patientMBRVertexs[2][1],
						// patientMBRVertexs[2][0], ordinaryMBRVertexs[1][1],
						// ordinaryMBRVertexs[1][0]);
						// double max_dist2 = D.distance(patientMBRVertexs[0][1],
						// patientMBRVertexs[0][0], ordinaryMBRVertexs[3][1],
						// ordinaryMBRVertexs[3][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					} else if (ordinaryMBR[0] > patientMBR[1]) {
						// case 7: ����
						double min_dist1 = D.distance(patientMBRVertexs[3][1], patientMBRVertexs[3][0],
								ordinaryMBRVertexs[1][1],
								ordinaryMBRVertexs[1][0]);
						double min_dist2 = D.distance(patientMBRVertexs[2][1], patientMBRVertexs[2][0],
								ordinaryMBRVertexs[0][1],
								ordinaryMBRVertexs[0][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(patientMBRVertexs[1][1],
						// patientMBRVertexs[1][0], ordinaryMBRVertexs[2][1],
						// ordinaryMBRVertexs[2][0]);
						// double max_dist2 = D.distance(patientMBRVertexs[0][1],
						// patientMBRVertexs[0][0], ordinaryMBRVertexs[3][1],
						// ordinaryMBRVertexs[3][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					} else if (ordinaryMBR[1] < patientMBR[0]) {
						// case 8: right
						double min_dist1 = D.distance(patientMBRVertexs[1][1], patientMBRVertexs[1][0],
								ordinaryMBRVertexs[3][1],
								ordinaryMBRVertexs[3][0]);
						double min_dist2 = D.distance(patientMBRVertexs[0][1], patientMBRVertexs[0][0],
								ordinaryMBRVertexs[2][1],
								ordinaryMBRVertexs[2][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(patientMBRVertexs[2][1],
						// patientMBRVertexs[2][0], ordinaryMBRVertexs[1][1],
						// ordinaryMBRVertexs[1][0]);
						// double max_dist2 = D.distance(patientMBRVertexs[1][1],
						// patientMBRVertexs[1][0], ordinaryMBRVertexs[0][1],
						// ordinaryMBRVertexs[0][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					} else {
						for (double[] lonlat : patientMBRVertexs) {
							for (double[] lonlat1 : ordinaryMBRVertexs) {
								double dist = D.distance(lonlat[1], lonlat[0], lonlat1[1], lonlat1[0]);
								// if(dist>max_dist) max_dist=dist;
								if (dist < min_dist)
									min_dist = dist;
							}
						}
					}

					// prune
					if (min_dist > epsilon) {
						validCheckNums += 1;
						continue;
					}
				} // finish pre-checking

				// after pre-checking, calculate remaining pairwise exact-distance among each
				// two
				// ordinary lcation and patient location
				for (Location ordinaryLocation : ordinaryLocations) {
					if (ordinaryLocation.isContact)
						continue;
					for (Location patientLocation : patientLocations) {
						totalCheckNB += 1;
						double dis = D.distance(ordinaryLocation.lat, ordinaryLocation.lon, patientLocation.lat,
								patientLocation.lon);
						if (dis <= epsilon) {
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
							break;
						}
					}
				}
			}
			fTime += (System.currentTimeMillis() - t1);
		} // End 2

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