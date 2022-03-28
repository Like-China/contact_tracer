package trace;

/**
 * ��ȷ�㷨-������ѯ
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;


public class ETA_Tracer {
	// ������ֵ
	public double distance_threshold;
	// ��Ⱦʱ����ֵ����������ֵ��Ϊ����Ⱦ����Ҫʵʱ����
	public int duration_threshold;
	// ��¼�ܸ�Ⱦ����������ʼ��Ϊquery patients
	public HashSet<Integer> patientIDs;
	// ��¼��ǰʱ�̵��ܸ�Ⱦ����
	public  HashSet<Integer> areas;
	// ά��һ��map��¼ÿ�������ڸ�Ⱦ�����ʱ�� (����ʱ��)
	public HashMap<Integer, Integer> records = new HashMap<Integer, Integer>();
	// ά��һ��map��¼ÿ���˵�ǰʱ���Ƿ񱻸�Ⱦ
	public HashMap<Integer, Boolean> isInfect = new HashMap<Integer, Boolean>();
	
	public ETA_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		this.duration_threshold = duration_threshold;
	}
	
	/**
	 * ��ȷ����������룬���ټ����������ֱ���ж�ÿ������λ�õ� �ڵ�ǰʱ���Ƿ��нӴ�����
	 * @param batch ��ǰʱ�̵Ĺ켣λ����
	 * @return
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch)
	{	
		// ��¼�����Ĳ���
		ArrayList<Integer> res = new ArrayList<Integer>();
		// 1. ����query��non-query��locations����
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
		
		// 2. �����������벢�ж��Ƿ�contact
		for(Location l1: ordinary_locations)
		{
			for(Location l2: query_locations)
			{
				// ��������λ�õ� l1 �� ��Ⱦ����λ�õ� l2  ��ȷ�ľ���
				double dis = Distance.getEuclideanDistance(l1, l2);
				if(dis <= distance_threshold)
				{	
//					System.out.println(l1.id+" "+l2.id+" "+dis);
					if(!records.containsKey(l1.id)) records.put(l1.id, 0);
					int period = records.get(l1.id)+1;
					records.put(l1.id, period);
					// ���ÿ��object�ڷ�������ʱ��������ֵ���������
					if (period >= duration_threshold)
					{
						patientIDs.add(l1.id);
						res.add(l1.id);
					}
					// ��Ǹ�λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬�����ظ��б�
					l1.isContact = true;
					isInfect.put(l1.id, true);
					break;
				}
			}
			// ��������û�б��ж�Ϊ���սӴ�����֮ǰʱ���з��ռ�¼�� ��Ⱦʱ���� 0
			// Ҫ��ʱ����������record��¼�� û�з����������սӴ��� object ��Ⱦʱ����0
			if(!l1.isContact)
			{
				if(records.containsKey(l1.id))
				{
					records.put(l1.id, 0);
				}
			}
		}
		System.out.printf("�·��ָ�Ⱦ������: %d ", res.size());
		System.out.printf("��ǰ��Ⱦ����: %d \n", patientIDs.size());
		return res;
	}
}
