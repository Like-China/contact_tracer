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

public class EGP_Tracer1 {
	public double distance_threshold;
	public int duration_threshold;
	public HashSet<Integer> patientIDs;
	public  HashSet<Integer> areas;
	public HashMap<Integer, Integer> objectMapDuration = new HashMap<Integer, Integer>();
	public HashMap<Integer, Boolean> isInfect = new HashMap<Integer, Boolean>();
	public GridIndex g;
	// the minimal objects within a MBR, or we do not apply MBR for approximation
	public int minMBR = Settings.minMBR;
	// 统计 总的pre-checking次数 和 成功的prechecking次数
	public int totalCheckNums = 0;
	public int validCheckNums = 0;
	// 使用distance
	public Distance D = new Distance();

	public EGP_Tracer1(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		double scale = (distance_threshold / 10000 / Math.sqrt(2));
		this.duration_threshold = duration_threshold;
		g = new GridIndex(scale, cityname);
	}
	
	// 用新的位置点更新一个区域的MBR
	public void updateMBR(Float[] initMBR, Location l) {
		// Float[] newMBR = new Float[4];
		initMBR[0] = initMBR[0]>l.lon?l.lon:initMBR[0];
		initMBR[1] = initMBR[1]<l.lon?l.lon:initMBR[1];
		initMBR[2] = initMBR[2]>l.lat?l.lat:initMBR[2];
		initMBR[3] = initMBR[3]<l.lat?l.lat:initMBR[3];
		// return newMBR;
	}


	public void  findInfectedArea(ArrayList<Location> batch)
	{	
		// init infected areas at current timestamp
		areas = new HashSet<Integer>();
		// each area and its covered non-query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// each area and its covered query locations at current timestamp
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// 记录每个区域病人行程的MBR, Float[]分别记录minLon, maxLon, minLat, maxLat
		HashMap<Integer, Float[]> ordinaryAreasMBR = new HashMap<>();
		// 记录每个区域正常人形成的MBR
		HashMap<Integer, Float[]> patientAreasMBR = new HashMap<>();
		for(Location l:batch)
		{	
			int areaID = g.getID(l.lon, l.lat);
			l.setAreaID(areaID);
			if(!ordinaryAreasLocations.containsKey(areaID))
			{
				ordinaryAreasLocations.put(areaID, new ArrayList<Location>());
				ordinaryAreasMBR.put(areaID, new Float[]{1000f,-1000f,1000f,-1000f});
			}
			if(!patientAreasLocations.containsKey(areaID))
			{
				patientAreasLocations.put(areaID, new ArrayList<Location>());
				patientAreasMBR.put(areaID, new Float[]{1000f,-1000f,1000f,-1000f});
			}
			if(patientIDs.contains(l.id))
			{	
				areas.add(areaID);
				patientAreasLocations.get(areaID).add(l);
				Float[] rawMBR = patientAreasMBR.get(areaID);
				updateMBR(rawMBR, l);
				// Float[] newMBR = updateMBR(rawMBR, l);
				// patientAreasMBR.put(areaID, newMBR);
			}else {
				ordinaryAreasLocations.get(areaID).add(l);
				Float[] rawMBR = ordinaryAreasMBR.get(areaID);
				updateMBR(rawMBR, l);
				// Float[] newMBR = updateMBR(rawMBR, l);
				// ordinaryAreasMBR.put(areaID, newMBR);
			}
		}
		// update grid index
		g.setOrdinaryAreasLocations(ordinaryAreasLocations);
		g.setPatientAreasLocations(patientAreasLocations);
		g.ordinaryAreasMBR = ordinaryAreasMBR;
		g.patientAreasMBR = patientAreasMBR;
	}

	/**
	 * apply pre-checking with MBR partition
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch, boolean isCheck)
	{
		// generate infected areas at current timestamp "areas"
		// get each patient area and its covered locations "g.patientAreasLocations"
		// get each ordinary area and its covered locations "g.ordinaryAreasLocations"
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
				// 只检查还未contact的普通人
				ArrayList<Location> unchecked_ordinaryLocations = new ArrayList<>();
				for(Location l1: ordinaryLocations)
				{
					if (!l1.isContact)
					{
						unchecked_ordinaryLocations.add(l1);
					}
				}
				ordinaryLocations = unchecked_ordinaryLocations;
				// 获得病人中心区域的MBR， 构造patient区域的四个MBR顶点
				Float[] patientMBR = g.patientAreasMBR.get(areaID);
				Float[][] vertexs1 = new Float[4][2];
				vertexs1[0] = new Float[] { patientMBR[0], patientMBR[2] };
				vertexs1[1] = new Float[] { patientMBR[0], patientMBR[3] };
				vertexs1[2] = new Float[] { patientMBR[1], patientMBR[2] };
				vertexs1[3] = new Float[] { patientMBR[1], patientMBR[3] };
				// start pre-checking if objects within MBR exceed specific number
				if (isCheck && nn != areaID && patientLocations.size()*ordinaryLocations.size() >= minMBR)
				{	
					totalCheckNums += 1;
					Float[] ordinaryMBR = g.ordinaryAreasMBR.get(nn);
					// 构造ordinary的四个MBR顶点
					Float[][] vertexs2 = new Float[4][2];
					vertexs2[0] = new Float[] { ordinaryMBR[0], ordinaryMBR[2] };
					vertexs2[1] = new Float[] { ordinaryMBR[0], ordinaryMBR[3] };
					vertexs2[2] = new Float[] { ordinaryMBR[1], ordinaryMBR[2] };
					vertexs2[3] = new Float[] { ordinaryMBR[1], ordinaryMBR[3] };
					// 计算两个MBR的最小距离和最大距离
					double min_dist = 10000;
					// double max_dist = -100;
					// 4 vertexes * 4 vertexes calculation 
					if(ordinaryMBR[0]>patientMBR[1] && ordinaryMBR[3]<patientMBR[2])
					{	
						// case 1: 左上角
						min_dist = D.distance(vertexs1[2][1], vertexs1[2][0], vertexs2[1][1], vertexs2[1][0]);
						// max_dist = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[2][1], vertexs2[2][0]);
					}else if(ordinaryMBR[0]>patientMBR[1] && ordinaryMBR[2]>patientMBR[3])
					{
						// case 2: 左下角
						min_dist = D.distance(vertexs1[3][1], vertexs1[3][0], vertexs2[0][1], vertexs2[0][0]);
						// max_dist = D.distance(vertexs1[0][1], vertexs1[0][0], vertexs2[3][1], vertexs2[3][0]);
					}else if(ordinaryMBR[1]<patientMBR[0] && ordinaryMBR[3]<patientMBR[2])
					{
						// case 3: 右上角
						min_dist = D.distance(vertexs1[0][1], vertexs1[0][0], vertexs2[3][1], vertexs2[3][0]);
						// max_dist = D.distance(vertexs1[3][1], vertexs1[3][0], vertexs2[0][1], vertexs2[0][0]);
					}else if(ordinaryMBR[1]<patientMBR[0] && ordinaryMBR[2]>patientMBR[3])
					{
						// case 4: 右下角
						min_dist = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[2][1], vertexs2[2][0]);
						// max_dist = D.distance(vertexs1[2][1], vertexs1[2][0], vertexs2[1][1], vertexs2[1][0]);
					}else if(ordinaryMBR[3]<patientMBR[2])
					{
						// case 5:上面
						double min_dist1 = D.distance(vertexs1[0][1], vertexs1[0][0], vertexs2[1][1], vertexs2[1][0]);
						double min_dist2 = D.distance(vertexs1[2][1], vertexs1[2][0], vertexs2[3][1], vertexs2[3][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[2][1], vertexs2[2][0]);
						// double max_dist2 = D.distance(vertexs1[3][1], vertexs1[3][0], vertexs2[0][1], vertexs2[0][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					}else if(ordinaryMBR[2]>patientMBR[3])
					{
						// case 6: 下面
						double min_dist1 = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[0][1], vertexs2[0][0]);
						double min_dist2 = D.distance(vertexs1[3][1], vertexs1[3][0], vertexs2[2][1], vertexs2[2][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(vertexs1[2][1], vertexs1[2][0], vertexs2[1][1], vertexs2[1][0]);
						// double max_dist2 = D.distance(vertexs1[0][1], vertexs1[0][0], vertexs2[3][1], vertexs2[3][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					}else if(ordinaryMBR[0]>patientMBR[1])
					{
						// case 7: 左面
						double min_dist1 = D.distance(vertexs1[3][1], vertexs1[3][0], vertexs2[1][1], vertexs2[1][0]);
						double min_dist2 = D.distance(vertexs1[2][1], vertexs1[2][0], vertexs2[0][1], vertexs2[0][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[2][1], vertexs2[2][0]);
						// double max_dist2 = D.distance(vertexs1[0][1], vertexs1[0][0], vertexs2[3][1], vertexs2[3][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					}else if(ordinaryMBR[1]<patientMBR[0])
					{
						// case 8: 右面
						double min_dist1 = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[3][1], vertexs2[3][0]);
						double min_dist2 = D.distance(vertexs1[0][1], vertexs1[0][0], vertexs2[2][1], vertexs2[2][0]);
						min_dist = Math.min(min_dist1, min_dist2);
						// double max_dist1 = D.distance(vertexs1[2][1], vertexs1[2][0], vertexs2[1][1], vertexs2[1][0]);
						// double max_dist2 = D.distance(vertexs1[1][1], vertexs1[1][0], vertexs2[0][1], vertexs2[0][0]);
						// max_dist = Math.min(max_dist1, max_dist2);
					}
					else
					{
						// 这种情况不会出现，均被上面几种情况包含了
						for(Float[] lonlat:vertexs1)
						{
							for(Float[] lonlat1:vertexs2)
							{	
								double dist = D.distance(lonlat[1], lonlat[0], lonlat1[1], lonlat1[0]);
								// if(dist>max_dist) max_dist=dist;
								if(dist<min_dist) min_dist=dist;
							}
						}
					}
					

					// no exposed cases at current timetamp, ignore
					// 这里可能不对，该区域普通人不被当前感染区与感染，有可能被其他感染区感染！！！！！
					// 解决方案：注释掉归0
				    // 对于两个MBR可能重叠，最小最大距离可能不是四个角的最大最小距离
					if(min_dist > distance_threshold)
					{	
						validCheckNums += 1;
						// for(Location l1: ordinaryLocations)
						// {	
						// 	if(l1.isContact) continue;
						// 	for(Location l2: patientLocations)
						// 	{	
						// 		double dis = D.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						// 		if(dis <= distance_threshold)
						// 		{	
						// 			System.out.println(dis);
						// 			System.out.println(min_dist);
						// 			System.out.println(max_dist);
						// 			System.out.println(l1.areaID);
						// 			System.out.println(l2.areaID);
						// 			System.out.println();
						// 		}
						// 	}
						// }
						// 在同一个区域，MBR就会有重叠
						continue;
					}
					// all objects within this influenced area are exposed at current timestamp
					// else if(max_dist <= distance_threshold)
					// {
					// 	// validCheckNums += 1;
					// 	// System.out.println("pre-checking successful 2");
					// 	for(Location l1:ordinaryLocations)
					// 	{	
					// 		if(l1.isContact) continue;
					// 		if(!objectMapDuration.containsKey(l1.id)) objectMapDuration.put(l1.id, 0);
					// 		int duration = objectMapDuration.get(l1.id)+1;
					// 		objectMapDuration.put(l1.id, duration);
					// 		if (duration >= duration_threshold)
					// 		{	
					// 			// if(patientIDs.contains(l1.id))
					// 			// {
					// 			// 	System.out.println("EGP已经包含该病人");
					// 			// 	continue;
					// 			// }
					// 			patientIDs.add(l1.id);
					// 			res.add(l1.id);
					// 		}
					// 		l1.isContact = true;
					// 		isInfect.put(l1.id, true);
					// 	}
					// 	// check next influenced area
					// 	continue;
					// }
					
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
							// mark this location as detected
							l1.isContact = true;
							isInfect.put(l1.id, true);
							if(!objectMapDuration.containsKey(l1.id)) objectMapDuration.put(l1.id, 0);
							int duration = objectMapDuration.get(l1.id)+1;
							objectMapDuration.put(l1.id, duration);
							// new case of exposure
							if (duration >= duration_threshold)
							{	
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
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