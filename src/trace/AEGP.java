/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-05 13:53:12
 */
package trace;

/**
 * AGP implementation
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import indexes.Distance;
import indexes.GridIndex;
import loader.Location;

public class AEGP {
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
	public int totalQueryNB = 0;
	public int totalCheckNB = 0;

	public Distance D = new Distance();

	/*
	 * the value of k in AGP is k//m exactly
	 */
	public AEGP(double epsilon, int k, String cityname) {
		this.epsilon = epsilon;
		double scale = (epsilon / 10000 / Math.sqrt(2));
		this.k = k;
		g = new GridIndex(scale);
	}

	/**
	 * Given a set of locations, mark grid cells that contains cases of exposure
	 * Meanwhile, we establish non-query location group/query location group for
	 * each grid cell
	 * 
	 * @param batch
	 */
	public void findInfectedArea(ArrayList<Location> batch) {
		areas = new HashSet<Integer>();
		// record ordinary locations of each grid cell
		HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// record patient locations of each grid cell
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		for (Location l : batch) {
			int areaID = g.getID(l.lon, l.lat);
			l.setAreaID(areaID);
			if (!ordinaryAreasLocations.containsKey(areaID)) {
				ordinaryAreasLocations.put(areaID, new ArrayList<Location>());
			}
			if (!patientAreasLocations.containsKey(areaID)) {
				patientAreasLocations.put(areaID, new ArrayList<Location>());
			}
			// record object id -> area accroding to it is a patient or not
			if (patientIDs.contains(l.id)) {
				areas.add(l.areaID);
				patientAreasLocations.get(areaID).add(l);
			} else {
				ordinaryAreasLocations.get(areaID).add(l);
			}
		}
		// update the record in grid index
		g.setOrdinaryAreasLocations(ordinaryAreasLocations);
		g.setPatientAreasLocations(patientAreasLocations);
	}

	/**
	 * without applying pre-checking with MBR partition
	 */
	public ArrayList<Integer> EGP_trace(ArrayList<Location> batch) {
		// generate infected areas at current timestamp "areas"
		// get each patient area and its covered locations "g.patientAreasLocations"
		// get each ordinary area and its covered locations "g.ordinaryAreasLocations"
		findInfectedArea(batch);
		ArrayList<Integer> updateCE = new ArrayList<Integer>();

		// for influenced areas of infected areas (included the infected area itself),
		// we check objects within influenced areas are infected or not
		for (Integer areaID : areas) {
			int[] nnIDs = g.getAffectAreas(areaID);
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			for (int nn : nnIDs) {
				if (nn == -1)
					continue;
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// calculate pairwise minimal distance
				if (ordinaryLocations == null || ordinaryLocations.isEmpty())
					continue;
				for (Location l1 : ordinaryLocations) {
					if (l1.isContact)
						continue;
					for (Location l2 : patientLocations) {
						double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						if (dis <= epsilon) {
							// mark this location as detected
							l1.isContact = true;
							isContactMap.put(l1.id, true);
							// initial a key value in the dictionary
							if (!objectMapDuration.containsKey(l1.id)) {
								objectMapDuration.put(l1.id, 0);
							}
							int duration = objectMapDuration.get(l1.id) + 1;
							objectMapDuration.put(l1.id, duration);
							// find new case of exposure
							if (duration >= k) {
								patientIDs.add(l1.id);
								updateCE.add(l1.id);
							}
							break;
						}
					}
				}
			}
		} // End 2

		// reset infected duration for these objects not infected at current timestamp
		for (Integer id : objectMapDuration.keySet()) {
			if (!isContactMap.containsKey(id))
				objectMapDuration.put(id, 0);
		}
		isContactMap.clear();
		return updateCE;
	}

	/**
	 * continuously search updated cases of exposes with ET algorithm
	 * update patientIDs and return updated cases of exposes
	 * for specific timestampe we run EGP
	 * otherwise, add the infected duration of all moving objects by 1
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch, int ts) {

		ArrayList<Integer> updateCE = new ArrayList<Integer>();
		// periodically apply EGP algorithm for exact detection
		if (ts % Settings.m == 0 || ts % k == 0 || ts % (k / 2) == 0) {
			// || ts % k == 0 || ts % (k / 2) == 0) {
			// System.out.println("ts->EGP: "+ts);
			return EGP_trace(batch);
		}

		findInfectedArea(batch);
		// System.out.println("ts->AGP: "+ ts);
		// for influenced areas of infected areas (include infected area itself), we
		// check objects within influenced areas are infected or not
		for (Integer areaID : areas) {
			// find influenced areas (include the infected area itself)
			int[] nnIDs = g.getAffectAreas(areaID);
			// detect influenced areas
			for (int nn : nnIDs) {
				if (nn == -1)
					continue;
				// get ordinary objects in these influenced areas
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				if (ordinaryLocations == null || ordinaryLocations.isEmpty())
					continue;
				for (Location l : ordinaryLocations) {
					if (l.isContact)
						continue;
					if (!objectMapDuration.containsKey(l.id)) {
						objectMapDuration.put(l.id, 1);
					} else {
						int period = objectMapDuration.get(l.id) + 1;
						objectMapDuration.put(l.id, period);
						// find new cases of exposure
						if (period >= this.k) {
							patientIDs.add(l.id);
							updateCE.add(l.id);
						}
					}
					// mark this location as detected
					l.isContact = true;
					isContactMap.put(l.id, true);
				}
			}
		} // End 2

		// reset infected duration of specific objects
		for (Integer id : objectMapDuration.keySet()) {
			if (!isContactMap.containsKey(id))
				objectMapDuration.put(id, 0);
		}
		isContactMap.clear();
		return updateCE;
	}

}
