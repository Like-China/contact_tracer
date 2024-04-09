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
	// the total pre-checking number / valid pre-checking number
	public int totalCheckNums = 0;
	public int validCheckNums = 0;
	public Distance D = new Distance();
	// index construction time / filter & search time
	public long cTime = 0;
	public long fTime = 0;
	public int totalQueryNB = 0;
	public int totalCheckNB = 0;

	public EGP(double epsilon, int k) {
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
	public void constructIndex(ArrayList<Location> batch) {
		long t1 = System.currentTimeMillis();
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
	public ArrayList<Integer> trace(ArrayList<Location> batch) {
		// 1. get infected "areas"
		// get each patient area and its covered locations "g.patientAreasLocations"
		// get each ordinary area and its covered locations "g.ordinaryAreasLocations"
		constructIndex(batch);
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