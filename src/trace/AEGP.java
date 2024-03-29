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
 * AEGP implementation
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import loader.Location;

/**
 * Approximate EGP implementation
 * we first check head and tail timestamp for each k timestamps to get
 * candidates, then we can only evaluate these candidates for other timestamps.
 */
public class AEGP extends EGP {

	public AEGP(double epsilon, int k) {
		super(epsilon, k);
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

	public ArrayList<Integer> getCandidate(ArrayList<Location> batch, boolean isHead,
			ArrayList<Integer> headCandidate) {
		HashSet<Integer> patientIDsCopy = null;
		if (!isHead) {
			patientIDsCopy = (HashSet<Integer>) patientIDs.clone();
			patientIDs.addAll(headCandidate);
		}
		constructIndex(batch);
		if (!isHead) {
			patientIDs = patientIDsCopy;
		}
		ArrayList<Integer> candidate = new ArrayList<>();
		long t1 = System.currentTimeMillis();
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
				for (Location ordinaryLocation : ordinaryLocations) {
					if (ordinaryLocation.isContact)
						continue;
					for (Location patientLocation : patientLocations) {
						double dis = D.distance(ordinaryLocation.lat, ordinaryLocation.lon, patientLocation.lat,
								patientLocation.lon);
						if (dis <= epsilon) {
							// mark this location as detected
							ordinaryLocation.isContact = true;
							candidate.add(ordinaryLocation.id);
							break;
						}
					}
				}
			}
		}
		fTime += (System.currentTimeMillis() - t1);
		return candidate;
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
	public ArrayList<Integer> trace(ArrayList<ArrayList<Location>> batches, int m, boolean isApproxiamte) {
		ArrayList<Integer> updateCE = new ArrayList<Integer>();
		assert batches.size() == this.k : "Wrong size of batches!";
		// 1. get candidate
		ArrayList<Location> head = batches.get(0);
		ArrayList<Location> tail = batches.get(this.k - 1);
		ArrayList<Integer> headCandidate = getCandidate(head, true, null);
		Iterator<Integer> headCandidateIter = headCandidate.iterator();
		while (headCandidateIter.hasNext()) {
			int id = headCandidateIter.next();
			if (objectMapDuration.get(id) == null || objectMapDuration.get(id) < 1) {
				headCandidateIter.remove();
			}
		}
		ArrayList<Integer> tailCandidate = getCandidate(tail, false, headCandidate);
		ArrayList<Integer> unionCandidate = new ArrayList<>(headCandidate);
		unionCandidate.removeAll(tailCandidate);
		unionCandidate.addAll(tailCandidate);
		// 2. for each ordinary location, find its intersection in query quadtree
		for (int i = 0; i < this.k; i++) {
			// for the timestamp among t, t+k (not include t and t+k)
			ArrayList<Location> batch = batches.get(i);
			// for (Location ordinaryLocation : ordinrayLocations) {
			Iterator<Integer> iterator = unionCandidate.iterator();
			// long t1 = System.currentTimeMillis();
			boolean isEarlyStop = (isApproxiamte && i != 0 && i % m != 0 && i != k - 1);
			if (!isEarlyStop) {
				constructIndex(batch);
			}
			long t1 = System.currentTimeMillis();
			while (iterator.hasNext()) {
				// for (int candidateId : unionCandidate) {
				int candidateId = iterator.next();
				Location ordinaryLocation = batch.get(candidateId);
				if (isEarlyStop) {
					ordinaryLocation.isContact = true;
				}
				if (!ordinaryLocation.isContact) {
					int[] nnIDs = g.getAffectAreas(ordinaryLocation.areaID);
					for (int nn : nnIDs) {
						if (nn == -1)
							continue;
						ArrayList<Location> patientLocations = g.patientAreasLocations.get(nn);
						// calculate pairwise minimal distance
						if (patientLocations == null || patientLocations.isEmpty())
							continue;
						for (Location patientLocation : patientLocations) {
							double dis = D.distance(ordinaryLocation.lat, ordinaryLocation.lon, patientLocation.lat,
									patientLocation.lon);
							if (dis <= epsilon) {
								// mark this location as detected
								ordinaryLocation.isContact = true;
								break;
							}
						}
					}
				}
				if (ordinaryLocation.isContact) {
					// mark this location as detected
					isContactMap.put(ordinaryLocation.id, true);
					objectMapDuration.putIfAbsent(ordinaryLocation.id, 0);
					int duration = objectMapDuration.compute(ordinaryLocation.id, (k, v) -> v + 1);
					// new updated case of exposure
					if (duration >= k) {
						patientIDs.add(ordinaryLocation.id);
						updateCE.add(ordinaryLocation.id);
						// remove from candidate
						iterator.remove();
					}
				}
			}
			// reset infected duration of specific objects
			for (Integer id : objectMapDuration.keySet()) {
				if (!isContactMap.containsKey(id))
					objectMapDuration.put(id, 0);
			}
			// reset contact information to process next timestamp
			isContactMap.clear();
			fTime += (System.currentTimeMillis() - t1);
		}
		return updateCE;
	}

}
