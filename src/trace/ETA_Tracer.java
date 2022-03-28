package trace;

/**
 * 精确算法-遍历查询
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;


public class ETA_Tracer {
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
	
	public ETA_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		this.duration_threshold = duration_threshold;
	}
	
	/**
	 * 精确两两计算距离，不再计算风险区域，直接判断每个正常位置点 在当前时刻是否有接触风险
	 * @param batch 当前时刻的轨迹位置流
	 * @return
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch)
	{	
		// 记录新增的病例
		ArrayList<Integer> res = new ArrayList<Integer>();
		// 1. 划分query和non-query的locations集合
		ArrayList<Location> query_locations = new ArrayList<Location>();
		ArrayList<Location> ordinary_locations = new ArrayList<Location>();
		for(Location l:batch)
		{
			if(patientIDs.contains(l.id))
			{
				query_locations.add(l);
			}else {
				ordinary_locations.add(l);
			}
		}
		
		// 2. 计算两两距离并判断是否contact
		for(Location l1: ordinary_locations)
		{
			for(Location l2: query_locations)
			{
				// 计算正常位置点 l1 与 感染病患位置点 l2  精确的距离
				double dis = Distance.getEuclideanDistance(l1, l2);
				if(dis <= distance_threshold)
				{	
//					System.out.println(l1.id+" "+l2.id+" "+dis);
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
			if(!l1.isContact)
			{
				if(records.containsKey(l1.id))
				{
					records.put(l1.id, 0);
				}
			}
		}
		System.out.printf("新发现感染病例数: %d ", res.size());
		System.out.printf("当前感染人数: %d \n", patientIDs.size());
		return res;
	}
}
