/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2024-03-04 19:00:49
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-04 21:09:38
 */
package trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;
import indexes.GridIndex;

public class AGP_Tracer {
	public double distance_threshold;
	public int duration_threshold;
	// record all cases of exposures, init == query patients
	public HashSet<Integer> patientIDs;
	// infected area ids of current timestamp
	public HashSet<Integer> areas;
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	public HashMap<Integer, Boolean> isInfect = new HashMap<Integer, Boolean>();
	public GridIndex g;
	public int minMBR = Settings.minMBR;
	public Distance D = new Distance();

	public AGP_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		double scale = (distance_threshold / 10000 / Math.sqrt(2));
		this.duration_threshold = duration_threshold;
		// this.duration_threshold = duration_threshold/ Settings.m;
		g = new GridIndex(scale, cityname);
	}

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
		ArrayList<Integer> res = new ArrayList<Integer>();

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
						if (dis <= distance_threshold) {
							// mark this location as detected
							l1.isContact = true;
							isInfect.put(l1.id, true);
							// initial a key value in the dictionary
							if (!objectMapDuration.containsKey(l1.id)) {
								objectMapDuration.put(l1.id, 0);
							}
							int duration = objectMapDuration.get(l1.id) + 1;
							objectMapDuration.put(l1.id, duration);
							// find new case of exposure
							if (duration >= duration_threshold) {
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
							break;
						}
					}
				}
			}
		} // End 2

		// reset infected duration for these objects not infected at current timestamp
		for (Integer id : objectMapDuration.keySet()) {
			if (!isInfect.containsKey(id))
				objectMapDuration.put(id, 0);
		}
		isInfect.clear();
		return res;
	}

	// 1. for specific timestampe we run EGP
	// 2. otherwise, add the infected duration of all moving objects by 1
	public ArrayList<Integer> trace(ArrayList<Location> batch, int ts) {

		ArrayList<Integer> res = new ArrayList<Integer>();
		// periodically apply EGP algorithm for exact detection
		if (ts % Settings.m == 0 || ts % duration_threshold == 0 || ts % (duration_threshold / 2) == 0) {
			// || ts % duration_threshold == 0 || ts % (duration_threshold / 2) == 0) {
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
						if (period >= this.duration_threshold) {
							patientIDs.add(l.id);
							res.add(l.id);
						}
					}
					// mark this location as detected
					l.isContact = true;
					isInfect.put(l.id, true);
				}
			}
		} // End 2

		// reset infected duration of specific objects
		for (Integer id : objectMapDuration.keySet()) {
			if (!isInfect.containsKey(id))
				objectMapDuration.put(id, 0);
		}
		isInfect.clear();
		return res;
	}

}
