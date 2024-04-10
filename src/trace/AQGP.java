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
import java.util.HashSet;
import java.util.Iterator;

import indexes.MyRectangle;
import loader.Location;

/**
 * Approximate QGP implementation
 * we first check head and tail timestamp for each k timestamps to get
 * candidates, then we can only evaluate these candidates for other timestamps.
 */
public class AQGP extends QGP {

	public AQGP(double epsilon, int k) {
		super(epsilon, k);
	}

	/**
	 * Given a set of locations, construct two quadtree indexes
	 * 
	 * @param batch
	 */

	// return a set of candidate of a batch but do not record them
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
		for (Location ordinaryLocation : ordinrayLocations) {
			ArrayList<MyRectangle> returnObjects = new ArrayList<>();
			quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, this.epsilon);
			if (returnObjects.size() > 0) {
				candidate.add(ordinaryLocation.id);
			}
		}
		fTime += (System.currentTimeMillis() - t1);
		return candidate;
	}

	/**
	 * continuously search updated cases of exposes with QAGP algorithm
	 * update patientIDs and return updated cases of exposes
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public ArrayList<Integer> trace(ArrayList<ArrayList<Location>> batches, int m, boolean isApproxiamte) {
		updateCE = new ArrayList<Integer>();
		int curBatchSize = batches.size();
		assert curBatchSize != 0 : "Wrong size of batches!";
		// 1. get candidate
		ArrayList<Location> head = batches.get(0);
		ArrayList<Integer> headCandidate = getCandidate(head, true, null);
		ArrayList<Integer> tailCandidate = new ArrayList<>();
		if (curBatchSize == this.k) {
			ArrayList<Location> tail = batches.get(curBatchSize - 1);
			Iterator<Integer> headCandidateIter = headCandidate.iterator();
			while (headCandidateIter.hasNext()) {
				int id = headCandidateIter.next();
				if (objectMapDuration.get(id) == null || objectMapDuration.get(id) < 1) {
					headCandidateIter.remove();
				}
			}
			tailCandidate = getCandidate(tail, false, headCandidate);
		}
		ArrayList<Integer> unionCandidate = new ArrayList<>(headCandidate);
		unionCandidate.removeAll(tailCandidate);
		unionCandidate.addAll(tailCandidate);
		// 2. for each ordinary location, find its intersection in query quadtree
		for (int i = 0; i < curBatchSize; i++) {
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
				ArrayList<MyRectangle> returnObjects = new ArrayList<>();
				if (isEarlyStop) {
					ordinaryLocation.isContact = true;
				}
				if (!ordinaryLocation.isContact) {
					quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon,
							ordinaryLocation.lat, this.epsilon);
				}
				if (ordinaryLocation.isContact || returnObjects.size() > 0) {
					// mark this location as detected
					ordinaryLocation.isContact = true;
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