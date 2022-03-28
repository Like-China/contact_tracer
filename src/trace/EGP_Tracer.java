package trace;

/**
 * 精确算法-运用MBR
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;
import indexes.GridIndex;

public class EGP_Tracer {
	// 距离阈值
	public double distance_threshold;
	// 感染时长阈值，超过该阈值认为被感染，需要实时报告
	public int duration_threshold;
	// 记录受感染的人数，初始化为query patients
	public HashSet<Integer> patientIDs;
	// 记录当前时刻的受感染区域
	public  HashSet<Integer> areas;
	// 维护一个map记录每个人置于感染区域的时长 (连续时长)
	public HashMap<Integer, Integer> records = new HashMap<Integer, Integer>();
	// 维护一个map记录每个人当前时刻是否被感染
	public HashMap<Integer, Boolean> isInfect = new HashMap<Integer, Boolean>();
	// 维护一个实时更新的空间索引
	public GridIndex g;
	// MBR最小人数
	public int minMBR = 100;
	
	public EGP_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		double scale = (distance_threshold/1000/Math.sqrt(2));
		this.duration_threshold = duration_threshold;
		g = new GridIndex(scale, cityname);
	}
	
	
	/**
	 * 查找当前时刻的危险区域
	 * @param batch 当前时刻的轨迹位置流
	 * @return 当前时刻的风险区域编号列表
	 */
	public void  findInfectedArea(ArrayList<Location> batch)
	{	
		// 初始化感染风险区域
		areas = new HashSet<Integer>();
		// 每个时刻，记录每个区域中包含的Location对象 (正常人)
		HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// 每个时刻，记录每个区域中包含的Location对象 (确诊病人和已经发现的密切接触者)
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// 1. 利用Grid_Index, 对所有的位置点查找其所在的空间ID
		// 2. 在grid Index上记录当前时刻每个areaID上的位置点
		for(Location l:batch)
		{	
			int areaID = g.getID(l.lon, l.lat);
//			System.out.printf("%d<-%d \n", areaID, l.id);
			l.setAreaID(areaID);
			// 如果 ordinaryAreasLocations 中不包含该areaID的记录，新建map
			if(!ordinaryAreasLocations.containsKey(areaID))
			{
				ordinaryAreasLocations.put(areaID, new ArrayList<Location>());
			}
			if(!patientAreasLocations.containsKey(areaID))
			{
				patientAreasLocations.put(areaID, new ArrayList<Location>());
			}
			// 根据是否是确诊病人 分到不同记录中
			// 是： 分到patientAreasLocations
			// 否：分配到ordinaryAreasLocations
			if(patientIDs.contains(l.id))
			{	
				areas.add(l.areaID);
				patientAreasLocations.get(areaID).add(l);
			}else {
				ordinaryAreasLocations.get(areaID).add(l);
			}
		}
		// 3. 更新Grid Index中的 每个区域记录的locations
		g.setOrdinaryAreasLocations(ordinaryAreasLocations);
		g.setPatientAreasLocations(patientAreasLocations);
	}

	
	
	/**
	 * 获取每个时刻的风险区域和周边区域
	 * 计算每个位置点到 已记录patients轨迹点的距离，并根据距离阈值 判定其当前是否处于危险区域
	 * @param batch 当前时刻的轨迹位置流
	 */
	public ArrayList<Integer> trace_no_checking(ArrayList<Location> batch)
	{
		// 记录每个位置点的areaID, 记录每个area中的locations, 记录当前时刻的危险区域
		findInfectedArea(batch);
		// 记录新增的病例
		ArrayList<Integer> res = new ArrayList<Integer>();
		// 1. 对于每个风险区域, 在该风险区内的正常人 感染时长直接加1, 并标记为该时刻连续受到感染
		for(Integer areaID: areas)
		{
			for(Location l: g.ordinaryAreasLocations.get(areaID))
			{	
				// 该位置点为已经探测 且属于 密切接触的位置点
				if(l.isContact) continue;
				if(!records.containsKey(l.id)) records.put(l.id, 0);
				int period = records.get(l.id)+1;
				records.put(l.id, period);
				// 如果每个object在风险区域时长超过阈值，将其加入结果
				if (period >= this.duration_threshold)
				{
					patientIDs.add(l.id);
					res.add(l.id);
				}
				// 标记该位置点为已经探测 且属于 密切接触的位置点，避免重复判别
				l.isContact = true;
				isInfect.put(l.id, true);
			}
		}
		
		// 2. 对于每个风险区域, 找寻其当前时刻 可以感染的正常位置点，并计算这些区域可能受到感染的Location
		for(Integer areaID: areas)
		{
			// 2.1 通过索引找到 风险区周围的 Areas
			int[] nnIDs = g.getAffectAreas(areaID);
			// 2.2 获取 当前风险区内 每个病人的Locations
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			for(int nn: nnIDs)
			{	
				// 该邻居不在空间范围内，直接略过
				if (nn == -1) continue;
				// 2.3 查找每个 nn area 包含的locations
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// 2.4 计算 ordinaryLocations 与 patientLocations 两两之间的最小距离
				// 该区域在该时刻不存在人员，直接掠过
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				for(Location l1:ordinaryLocations)
				{	
					// 该位置点为已经探测 且属于 密切接触的位置点， 直接掠过
					if(l1.isContact) continue;
					for(Location l2: patientLocations)
					{	
						// 如果距离小于限定的距离范围，认为一次contact, 该object 感染时长+1
						// +1后跳出循环，不再和其他 patientLocations 做距离计算
						double dis = Distance.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						if(dis <= distance_threshold)
						{	
							if(!records.containsKey(l1.id)) records.put(l1.id, 0);
							int period = records.get(l1.id)+1;
							records.put(l1.id, period);
							// 如果每个object在风险区域时长超过阈值，将其加入
							if (period >= duration_threshold)
							{
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
							// 标记该位置点为已经探测 且属于 密切接触的位置点，避免重复判别
							l1.isContact = true;
							isInfect.put(l1.id, true);
							break;
						}
					}
					// 如果到最后都没有被判定为风险接触，且之前时刻有风险记录， 感染时长归 0
					// 要求时间连续，将record记录中 没有发生连续风险接触的 object 感染时长归0
//						if(!l1.isContact && records.containsKey(l1.id)) records.put(l1.id, 0);
				}
			} 
		} // End 2
		
		// 3. 要求时间连续，将record记录中 没有发生连续风险接触的 object 感染时长归0
		for(Integer id: records.keySet())
		{	
			if (!isInfect.containsKey(id)) records.put(id, 0);
		}
		isInfect.clear();
		return res;
	}
	
	
	/**
	 * 获取每个时刻的风险区域和周边区域
	 * 计算每个位置点到 已记录patients轨迹点的距离，并根据距离阈值 判定其当前是否处于危险区域
	 * 利用MBR进行pre-checking
	 * @param batch 当前时刻的轨迹位置流
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch)
	{
		// 记录每个位置点的areaID, 记录每个area中的locations, 记录当前时刻的危险区域
		findInfectedArea(batch);
		// 记录新增的病例
		ArrayList<Integer> res = new ArrayList<Integer>();
		
		// 1. 对于每个风险区域, 在该风险区内的正常人 感染时长直接加1, 并标记为该时刻连续受到感染
		for(Integer areaID: areas)
		{
			for(Location l: g.ordinaryAreasLocations.get(areaID))
			{	
				// 该位置点为已经探测 且属于 密切接触的位置点
				if(l.isContact) continue;
				if(!records.containsKey(l.id)) records.put(l.id, 0);
				int period = records.get(l.id)+1;
				records.put(l.id, period);
				// 如果每个object在风险区域时长超过阈值，将其加入结果
				if (period >= this.duration_threshold)
				{
					patientIDs.add(l.id);
					res.add(l.id);
				}
				// 标记该位置点为已经探测 且属于 密切接触的位置点，避免重复判别
				l.isContact = true;
				isInfect.put(l.id, true);
			}
		}
		
		// 2. 对于每个风险区域, 找寻其当前时刻 可以感染的正常位置点，并计算这些区域可能受到感染的Location
		for(Integer areaID: areas)
		{
			// 2.1 通过索引找到 风险区周围的 Areas
			int[] nnIDs = g.getAffectAreas(areaID);
			// 2.2 获取 当前风险区内 每个病人的Locations
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			// 利用affect area进行探测
			for(int nn: nnIDs)
			{	
				// 该邻居不在空间范围内，直接略过
				if (nn == -1) continue;
				// 2.4 查找每个 nn area 包含的locations
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// 该区域在该时刻不存在人员，直接掠过
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				
				// 2.5 如果有病人存在的感染区和受影响的普通区人数均大于4，才考虑使用prechecking，
				// 检查MBR 四个角 的最大最小值
				if (patientLocations.size()>= minMBR)
				{	
					// 计算该区域普通人位置的MBR，已经确定处于风险的不用再次计算
					// 2.3 计算该区域patient位置的MBR
					float[][] vertexs1 = Util.getMBR(patientLocations);
					float[][] vertexs2 = Util.getMBR(ordinaryLocations);
					double min_dist = 10000;
					double max_dist = -100;
					for(float[] lonlat:vertexs1)
					{
						for(float[] lonlat1:vertexs2)
						{	
							double dist = Distance.distance(lonlat[1], lonlat[0], lonlat1[1], lonlat1[0]);
							if(dist>max_dist) max_dist=dist;
							if(dist<min_dist) min_dist=dist;
						}
					}
					
					// 最小距离都大于限度，该区域所有object当前时刻设置为未感染，感染时长归零，直接掠过
					if(min_dist > distance_threshold)
					{	
						for(Location l:ordinaryLocations)
						{	
							// 该位置点为已经探测 且属于 密切接触的 位置点， 直接掠过
							if(!l.isContact && records.containsKey(l.id)) records.put(l.id, 0);
						}
						continue;
					}
					// 最大距离都小于限度，该区域全部objects的感染时长+1
					else if(max_dist <= distance_threshold)
					{
						for(Location l1:ordinaryLocations)
						{	
							// 该位置点为已经探测 且属于 密切接触的位置点， 直接掠过
							if(l1.isContact) continue;
							if(!records.containsKey(l1.id)) records.put(l1.id, 0);
							int period = records.get(l1.id)+1;
							records.put(l1.id, period);
							// 如果每个object在风险区域时长超过阈值，将其加入
							if (period >= duration_threshold)
							{
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
							// 标记该位置点为已经探测 且属于 密切接触的位置点，避免重复判别
							l1.isContact = true;
							isInfect.put(l1.id, true);
						}
						continue;
					}
					
				}// 结束预检查
				
				// 预检查后，剩下的才计算 ordinaryLocations 与 patientLocations 两两之间的最小距离
				for(Location l1: ordinaryLocations)
				{	
					// 该位置点为已经探测 且属于 密切接触的位置点， 直接掠过
					if(l1.isContact) continue;
					for(Location l2: patientLocations)
					{	
						// 如果距离小于限定的距离范围，认为一次contact, 该object 感染时长+1
						// +1后跳出循环，不再和其他 patientLocations 做距离计算
						double dis = Distance.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						if(dis <= distance_threshold)
						{
							if(!records.containsKey(l1.id)) records.put(l1.id, 0);
							int period = records.get(l1.id)+1;
							records.put(l1.id, period);
							// 如果每个object在风险区域时长超过阈值，将其加入
							if (period >= duration_threshold)
							{	
								patientIDs.add(l1.id);
								res.add(l1.id);
							}
							// 标记该位置点为已经探测 且属于 密切接触的位置点，避免重复判别
							l1.isContact = true;
							isInfect.put(l1.id, true);
							break;
						}
					}
					// 如果到最后都没有被判定为风险接触，且之前时刻有风险记录， 感染时长归 0
					// 要求时间连续，将record记录中 没有发生连续风险接触的 object 感染时长归0
//					if(!l1.isContact && records.containsKey(l1.id)) records.put(l1.id, 0);
				}
			} 
		} // End 2
		
		// 3. 要求时间连续，将record记录中 没有发生连续风险接触的 object 感染时长归0
		for(Integer id: records.keySet())
		{	
			if (!isInfect.containsKey(id)) records.put(id, 0);
		}
		isInfect.clear();
		// 经验证res中不含重
		return res;
	}
	
	
	
	
}
