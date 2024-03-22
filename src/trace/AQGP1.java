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
import java.util.Iterator;

import indexes.Distance;
import indexes.MyRectangle;
import indexes.QuadTree;
import loader.Location;

/**
 * Approximate QGP implementation
 * we first check head and tail timestamp for each k timestamps to get
 * candidates, then we can only evaluate these candidates for other timestamps.
 */
public class AQGP1 {
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
	// the number of query grid cells of query locations
	public int totalQueryNB = 0;
	// the number of leaf nodes
	public int totalLeafNB = 0;
	public ArrayList<Location> ordinrayLocations = new ArrayList<Location>();
	// index construct time/filter time/final search time
	public long cTime = 0;
	public long fTime = 0;
	public long sTime = 0;
	// varied candidates at head timestamp and tail timestamp for each k-size
	// // sliding window
	// private ArrayList<Integer> headCandidate = null;
	// private ArrayList<Integer> tailCandidate = null;
	HashSet<Integer> updateCE = new HashSet<Integer>();

	public AQGP1(double epsilon, int k) {
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
		for (Location ordinaryLocation : ordinrayLocations) {
			long t1 = System.currentTimeMillis();
			totalQueryNB += 1;
			HashSet<MyRectangle> returnObjects = new HashSet<>();
			quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
					this.epsilon);
			if (returnObjects.size() > 0) {
				candidate.add(ordinaryLocation.id);
			}
			long t2 = System.currentTimeMillis();
			fTime += (t2 - t1);
		}
		return candidate;
	}

	/**
	 * continuously search updated cases of exposes with QAGP algorithm
	 * update patientIDs and return updated cases of exposes
	 * 
	 * @param batch a list of location at the same timesampe
	 * @return updated cases of exposes
	 */
	public HashSet<Integer> trace(ArrayList<ArrayList<Location>> batches, int ts, int m) {
		updateCE = new HashSet<Integer>();
		assert batches.size() == this.k : "Wrong size of batches!";
		long t1, t2;
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
		t1 = System.currentTimeMillis();
		ArrayList<Integer> tailCandidate = getCandidate(tail, false, headCandidate);
		ArrayList<Integer> unionCandidate = new ArrayList<>(headCandidate);
		t2 = System.currentTimeMillis();
		cTime += (t2 - t1);
		unionCandidate.removeAll(tailCandidate);
		unionCandidate.addAll(tailCandidate);
		System.out.println(
				"refinedHeadCandidate.size()/tailCandidate.size()/unionCandidate.size(): " + headCandidate.size()
						+ "/" + tailCandidate.size() + "/" + unionCandidate.size());
		// 2. for each ordinary location, find its intersection in query quadtree
		for (int i = 0; i < this.k; i++) {
			// for the timestamp among t, t+k (not include t and t+k)
			ArrayList<Location> batch = batches.get(i);
			constructIndex(batch);
			// for (Location ordinaryLocation : ordinrayLocations) {
			Iterator<Integer> iterator = unionCandidate.iterator();
			while (iterator.hasNext()) {
				// for (int candidateId : unionCandidate) {
				int candidateId = iterator.next();
				Location ordinaryLocation = batch.get(candidateId);
				t1 = System.currentTimeMillis();
				HashSet<MyRectangle> returnObjects = new HashSet<>();
				if (!ordinaryLocation.isContact) {
					// if (ts != 0 && ts % m == 0) {
					// quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon,
					// ordinaryLocation.lat, true,
					// this.epsilon);
					// } else {
					quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
							this.epsilon);
					// }
				}
				t2 = System.currentTimeMillis();
				fTime += (t2 - t1);
				t1 = System.currentTimeMillis();
				if (ordinaryLocation.isContact || returnObjects.size() > 0) {
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
						// remove from candidate
						iterator.remove();
					}
				}
				t2 = System.currentTimeMillis();
				sTime += (t2 - t1);
			}

			// reset infected duration of specific objects
			for (Integer id : objectMapDuration.keySet()) {
				if (!isContactMap.containsKey(id))
					objectMapDuration.put(id, 0);
			}
			// reset contact information to process next timestamp
			isContactMap.clear();
		}
		return updateCE;
	}

}