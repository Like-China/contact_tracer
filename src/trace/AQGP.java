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
 * Approximate QGP implementation
 */
public class AQGP {
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

	public AQGP(double epsilon, int k) {
		this.epsilon = epsilon;
		this.k = k;
	}

	/**
	 * Given a set of locations, construct two quadtree indexes
	 * two-phase update: 1. get new cases of exposure based on candidates on ts 1and
	 * ts k
	 * 2. check if any objs can be influenced by updated cases
	 * 
	 * which is time-consuming compared to
	 * AGP1
	 * 
	 * @param batch
	 */
	public void constructIndex(ArrayList<Location> batch) {
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
	}

	// return a set of candidate of a batch but do not record them
	public ArrayList<Integer> getCandidate(ArrayList<Location> batch) {
		long t1 = System.currentTimeMillis();
		constructIndex(batch);
		long t2 = System.currentTimeMillis();
		cTime += (t2 - t1);
		ArrayList<Integer> candidate = new ArrayList<>();
		for (Location ordinaryLocation : ordinrayLocations) {
			t1 = System.currentTimeMillis();
			totalQueryNB += 1;
			HashSet<MyRectangle> returnObjects = new HashSet<>();
			quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
					this.epsilon);
			t2 = System.currentTimeMillis();
			fTime += (t2 - t1);
			t1 = System.currentTimeMillis();
			if (returnObjects.size() > 0) {
				candidate.add(ordinaryLocation.id);
			}
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
		// the copy of current objectMapDuration
		HashMap<Integer, Integer> objectMapDurationCopy = (HashMap<Integer, Integer>) objectMapDuration.clone();
		HashSet<Integer> patientIDsCopy = (HashSet<Integer>) patientIDs.clone();
		assert batches.size() == this.k : "Wrong size of batches!";
		ArrayList<Location> head = batches.get(0);
		ArrayList<Location> tail = batches.get(this.k - 1);
		ArrayList<Integer> headCandidate = getCandidate(head);
		ArrayList<Integer> tailCandidate = getCandidate(tail);
		ArrayList<Integer> unionCandidate = new ArrayList<>(headCandidate);
		unionCandidate.removeAll(tailCandidate);
		unionCandidate.addAll(tailCandidate);

		// 1. index construction
		HashSet<Integer> updateCE = new HashSet<Integer>();
		// 2. for each ordinary location, find its intersection in query quadtree
		for (int i = 0; i < this.k; i++) {
			if (i == 0) {
				for (int objId : headCandidate) {
					batches.get(i).get(objId).isContact = true;
					isContactMap.put(objId, true);
					if (!objectMapDuration.containsKey(objId))
						objectMapDuration.put(objId, 0);
					int duration = objectMapDuration.get(objId) + 1;
					objectMapDuration.put(objId, duration);
					// new updated case of exposure
					if (duration >= k) {
						patientIDs.add(objId);
						updateCE.add(objId);
					}
				}
			} else if (i == this.k - 1) {
				for (int objId : tailCandidate) {
					batches.get(i).get(objId).isContact = true;
					if (updateCE.contains(objId))
						continue;
					isContactMap.put(objId, true);
					if (!objectMapDuration.containsKey(objId))
						objectMapDuration.put(objId, 0);
					int duration = objectMapDuration.get(objId) + 1;
					objectMapDuration.put(objId, duration);
					// new updated case of exposure
					if (duration >= this.k) {
						patientIDs.add(objId);
						updateCE.add(objId);
					}
				}
			} else {
				// for the timestamp among t, t+k (not include t and t+k)
				ArrayList<Location> batch = batches.get(i);
				long t1 = System.currentTimeMillis();
				constructIndex(batch);
				long t2 = System.currentTimeMillis();
				cTime += (t2 - t1);
				// for (Location ordinaryLocation : ordinrayLocations) {
				for (int candidateId : unionCandidate) {
					if (updateCE.contains(candidateId))
						continue;
					Location ordinaryLocation = batch.get(candidateId);
					t1 = System.currentTimeMillis();
					HashSet<MyRectangle> returnObjects = new HashSet<>();
					// if (ts != 0 && ts % m == 0) {
					// quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon,
					// ordinaryLocation.lat, true,
					// this.epsilon);
					// } else {
					quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
							this.epsilon);
					// }
					t2 = System.currentTimeMillis();
					fTime += (t2 - t1);
					t1 = System.currentTimeMillis();
					if (returnObjects.size() > 0) {
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

						}
					}
					t2 = System.currentTimeMillis();
					sTime += (t2 - t1);
				}
			}
			// reset infected duration of specific objects
			for (Integer id : objectMapDuration.keySet()) {
				if (!isContactMap.containsKey(id))
					objectMapDuration.put(id, 0);
			}
			// reset contact information to process next timestamp
			isContactMap.clear();
		}
		// 3. check if new updated cases can affect some ordinary locations
		// ArrayList<Integer> updatedTailCandidate = new ArrayList<>();
		ArrayList<Integer> updatedTailCandidate = getCandidate(tail);
		updatedTailCandidate.removeAll(headCandidate);
		updatedTailCandidate.addAll(headCandidate);
		patientIDs = patientIDsCopy;
		objectMapDuration = objectMapDurationCopy;
		HashSet<Integer> newupdateCE = new HashSet<Integer>();
		if (updatedTailCandidate.size() > 0) {
			for (int i = 0; i < this.k; i++) {
				// for the timestamp among t, t+k (not include t and t+k)
				ArrayList<Location> batch = batches.get(i);
				long t1 = System.currentTimeMillis();
				constructIndex(batch);
				long t2 = System.currentTimeMillis();
				cTime += (t2 - t1);
				// for (Location ordinaryLocation : ordinrayLocations) {
				for (int candidateId : updatedTailCandidate) {
					if (newupdateCE.contains(candidateId))
						continue;
					Location ordinaryLocation = batch.get(candidateId);
					HashSet<MyRectangle> returnObjects = new HashSet<>();
					if (!ordinaryLocation.isContact) {
						t1 = System.currentTimeMillis();
						// if (ts != 0 && ts % m == 0) {
						// quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon,
						// ordinaryLocation.lat, true,
						// this.epsilon);
						// } else {
						quadTree.retrieveByLocation(returnObjects, ordinaryLocation.lon, ordinaryLocation.lat, false,
								this.epsilon);
						// }
						t2 = System.currentTimeMillis();
						fTime += (t2 - t1);
					}
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
							newupdateCE.add(ordinaryLocation.id);
							// remove from candidate
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
		}
		updateCE.addAll(newupdateCE);
		return updateCE;
	}

}