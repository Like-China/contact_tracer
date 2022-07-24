package trace;

/**
 * exact algorithm with grid index
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;
import indexes.GridIndex;

public class EGP_Tracer {
	public double distance_threshold;
	public int duration_threshold;
	public HashSet<Integer> patientIDs;
	public  HashSet<Integer> areas;
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	public HashMap<Integer, Boolean> isInfect = new HashMap<Integer, Boolean>();
	public GridIndex g;
	// the minimal objects within a MBR, or we do not apply MBR for approximation
	public int minMBR = Settings.minMBR;
	// 使用distance
	public Distance D = new Distance();
	
	public EGP_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		double scale = (distance_threshold / 10000 / Math.sqrt(2));
		this.duration_threshold = duration_threshold;
		g = new GridIndex(scale, cityname);
	}

	public void  findInfectedArea(ArrayList<Location> batch)
	{	
		// init infected areas at current timestamp
		areas = new HashSet<Integer>();
		// each area and its covered non-query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// each area and its covered query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		for(Location l:batch)
		{	
			int areaID = g.getID(l.lon, l.lat);
			l.setAreaID(areaID);
			if(!ordinaryAreasLocations.containsKey(areaID))
			{
				ordinaryAreasLocations.put(areaID, new ArrayList<Location>());
			}
			if(!patientAreasLocations.containsKey(areaID))
			{
				patientAreasLocations.put(areaID, new ArrayList<Location>());
			}
			if(patientIDs.contains(l.id))
			{	
				areas.add(l.areaID);
				patientAreasLocations.get(areaID).add(l);
			}else {
				ordinaryAreasLocations.get(areaID).add(l);
			}
		}
		// update grid index
		g.setOrdinaryAreasLocations(ordinaryAreasLocations);
		g.setPatientAreasLocations(patientAreasLocations);
	}


	/**
	 * without applying pre-checking with MBR partition
	 */
	public ArrayList<Integer> trace_no_checking(ArrayList<Location> batch)
	{
		// generate infected areas at current timestamp "areas"
		// get each patient area and its covered locations "g.patientAreasLocations"
		// get each ordinary area and its covered locations "g.ordinaryAreasLocations"
		findInfectedArea(batch);
		ArrayList<Integer> res = new ArrayList<Integer>();
		
		// for influenced areas of infected areas (included the infected area itself), we check objects within influenced areas are infected or not
		for(Integer areaID: areas)
		{
			int[] nnIDs = g.getAffectAreas(areaID);
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			for(int nn: nnIDs)
			{	
				if (nn == -1) continue;
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// calculate pairwise minimal distance 
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				for(Location l1:ordinaryLocations)
				{	
					if(l1.isContact) continue;
					for(Location l2: patientLocations)
					{	
						double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						if(dis <= distance_threshold)
						{	
							// mark this location as detected
							l1.isContact = true;
							isInfect.put(l1.id, true);
							// initial a key value in the dictionary 
							if(!objectMapDuration.containsKey(l1.id)) 
							{
								objectMapDuration.put(l1.id, 0);
							}
							int duration = objectMapDuration.get(l1.id)+1;
							objectMapDuration.put(l1.id, duration);
							// find new case of exposure
							if (duration >= duration_threshold)
							{	
								if(patientIDs.contains(l1.id))
								{
									System.out.println("EGP#计算已经包含该病人");
									break;
								}
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
		for(Integer id: objectMapDuration.keySet())
		{	
			if (!isInfect.containsKey(id)) objectMapDuration.put(id, 0);
		}
		isInfect.clear();
		return res;
	}
	
	
	/**
	 * apply pre-checking with MBR partition
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch)
	{
		findInfectedArea(batch);
		ArrayList<Integer> res = new ArrayList<Integer>();
		
		// 2. for influenced areas of infected areas (include infected area itself), we check objects within influenced areas are infected or not
		for(Integer areaID: areas)
		{
			// 2.1 find influenced areas' ids
			int[] nnIDs = g.getAffectAreas(areaID);
			// 2.2 find patients at infected area
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			// check objects within these influenced area
			for(int nn: nnIDs)
			{	
				// out of space range
				if (nn == -1) continue;
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				// start pre-checking if objects within MBR exceed specific number
				if (nn != areaID && patientLocations.size()>= minMBR)
				{	
					float[][] vertexs1 = Util.getMBR(patientLocations);
					float[][] vertexs2 = Util.getMBR(ordinaryLocations);
					double min_dist = 10000;
					double max_dist = -100;
					// 4 vertexes * 4 vertexes calculation 
					for(float[] lonlat:vertexs1)
					{
						for(float[] lonlat1:vertexs2)
						{	
							double dist = D.distance(lonlat[1], lonlat[0], lonlat1[1], lonlat1[0]);
							if(dist>max_dist) max_dist=dist;
							if(dist<min_dist) min_dist=dist;
						}
					}
					// no exposed cases at current timetamp, ignore
					// 检测是否真的所有距离都大于阈值(检测发现部分距离满足即小于阈值条件，但是之前被MBR错误过滤了)
					// 之前将所有未接触的普通人时长错误归零
					if(min_dist > distance_threshold)
					{	
						if(nn != areaID)
						{
						continue;
						}
					}
					// all objects within this influenced area are exposed at current timestamp
					if(max_dist <= distance_threshold)
					{
						for(Location l1:ordinaryLocations)
						{	
							if(l1.isContact) continue;
							if(!objectMapDuration.containsKey(l1.id)) objectMapDuration.put(l1.id, 0);
							int duration = objectMapDuration.get(l1.id)+1;
							objectMapDuration.put(l1.id, duration);
							if (duration >= duration_threshold)
							{	
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
							l1.isContact = true;
							isInfect.put(l1.id, true);
						}
						// check next influenced area
						continue;
					}
					
				}// finish pre-checking
				
				// after pre-checking, calculate remaining pairwise distance among each two ordinary lcation and patient location 
				for(Location l1: ordinaryLocations)
				{	
					if(l1.isContact) continue;
					for(Location l2: patientLocations)
					{	
						double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						if(dis <= distance_threshold)
						{
							if(!objectMapDuration.containsKey(l1.id)) objectMapDuration.put(l1.id, 0);
							int duration = objectMapDuration.get(l1.id)+1;
							objectMapDuration.put(l1.id, duration);
							// new case of exposure
							if (duration >= duration_threshold)
							{	
								if(patientIDs.contains(l1.id))
								{
									System.out.println("EGP已经包含该病人");
									break;
								}
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
							// mark this location as detected
							l1.isContact = true;
							isInfect.put(l1.id, true);
							break;
						}
					}
				}
			} 
		} // End 2
		
		// reset infected duration of specific objects
		for(Integer id: objectMapDuration.keySet())
		{	
			if (!isInfect.containsKey(id)) objectMapDuration.put(id, 0);
		}
		isInfect.clear();
		// no repeated cases in res
		return res;
	}
	
}
