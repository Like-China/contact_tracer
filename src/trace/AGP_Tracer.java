package trace;

/**
 * ��ȷ�㷨-����MBR
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import data_loader.Location;
import indexes.Distance;
import indexes.GridIndex;

public class AGP_Tracer {
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
	// ά��һ��ʵʱ���µĿռ�����
	public GridIndex g;
	// MBR��С����
	public int minMBR = 100;
	
	public AGP_Tracer(double distance_threshold, int duration_threshold, String cityname) {
		super();
		this.distance_threshold = distance_threshold;
		double scale = (distance_threshold/1000/Math.sqrt(2));
		this.duration_threshold = duration_threshold;
		g = new GridIndex(scale, cityname);
	}
	
	
	/**
	 * ���ҵ�ǰʱ�̵�Σ������
	 * @param batch ��ǰʱ�̵Ĺ켣λ����
	 * @return ��ǰʱ�̵ķ����������б�
	 */
	public void  findInfectedArea(ArrayList<Location> batch)
	{	
		// ��ʼ����Ⱦ��������
		areas = new HashSet<Integer>();
		// ÿ��ʱ�̣���¼ÿ�������а�����Location���� (������)
		HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// ÿ��ʱ�̣���¼ÿ�������а�����Location���� (ȷ�ﲡ�˺��Ѿ����ֵ����нӴ���)
		HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
		// 1. ����Grid_Index, �����е�λ�õ���������ڵĿռ�ID
		// 2. ��grid Index�ϼ�¼��ǰʱ��ÿ��areaID�ϵ�λ�õ�
		for(Location l:batch)
		{	
			int areaID = g.getID(l.lon, l.lat);
//			System.out.printf("%d<-%d \n", areaID, l.id);
			l.setAreaID(areaID);
			// ��� ordinaryAreasLocations �в�������areaID�ļ�¼���½�map
			if(!ordinaryAreasLocations.containsKey(areaID))
			{
				ordinaryAreasLocations.put(areaID, new ArrayList<Location>());
			}
			if(!patientAreasLocations.containsKey(areaID))
			{
				patientAreasLocations.put(areaID, new ArrayList<Location>());
			}
			// �����Ƿ���ȷ�ﲡ�� �ֵ���ͬ��¼��
			// �ǣ� �ֵ�patientAreasLocations
			// �񣺷��䵽ordinaryAreasLocations
			if(patientIDs.contains(l.id))
			{	
				areas.add(l.areaID);
				patientAreasLocations.get(areaID).add(l);
			}else {
				ordinaryAreasLocations.get(areaID).add(l);
			}
		}
		// 3. ����Grid Index�е� ÿ�������¼��locations
		g.setOrdinaryAreasLocations(ordinaryAreasLocations);
		g.setPatientAreasLocations(patientAreasLocations);
	}

	/**
	 * ��ȡÿ��ʱ�̵ķ���������ܱ�����
	 * ����ÿ��λ�õ㵽 �Ѽ�¼patients�켣��ľ��룬�����ݾ�����ֵ �ж��䵱ǰ�Ƿ���Σ������
	 * @param batch ��ǰʱ�̵Ĺ켣λ����
	 */
	public ArrayList<Integer> EGP_trace(ArrayList<Location> batch)
	{
		// ��¼ÿ��λ�õ��areaID, ��¼ÿ��area�е�locations, ��¼��ǰʱ�̵�Σ������
		findInfectedArea(batch);
		// ��¼�����Ĳ���
		ArrayList<Integer> res = new ArrayList<Integer>();
		// 1. ����ÿ����������, �ڸ÷������ڵ������� ��Ⱦʱ��ֱ�Ӽ�1, �����Ϊ��ʱ�������ܵ���Ⱦ
		for(Integer areaID: areas)
		{
			for(Location l: g.ordinaryAreasLocations.get(areaID))
			{	
				// ��λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ�
				if(l.isContact) continue;
				if(!records.containsKey(l.id)) records.put(l.id, 0);
				int period = records.get(l.id)+1;
				records.put(l.id, period);
				// ���ÿ��object�ڷ�������ʱ��������ֵ�����������
				if (period >= this.duration_threshold)
				{
					patientIDs.add(l.id);
					res.add(l.id);
				}
				// ��Ǹ�λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬�����ظ��б�
				l.isContact = true;
				isInfect.put(l.id, true);
			}
		}
		
		// 2. ����ÿ����������, ��Ѱ�䵱ǰʱ�� ���Ը�Ⱦ������λ�õ㣬��������Щ��������ܵ���Ⱦ��Location
		for(Integer areaID: areas)
		{
			// 2.1 ͨ�������ҵ� ��������Χ�� Areas
			int[] nnIDs = g.getAffectAreas(areaID);
			// 2.2 ��ȡ ��ǰ�������� ÿ�����˵�Locations
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			for(int nn: nnIDs)
			{	
				// ���ھӲ��ڿռ䷶Χ�ڣ�ֱ���Թ�
				if (nn == -1) continue;
				// 2.3 ����ÿ�� nn area ������locations
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// 2.4 ���� ordinaryLocations �� patientLocations ����֮�����С����
				// �������ڸ�ʱ�̲�������Ա��ֱ���ӹ�
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				for(Location l1:ordinaryLocations)
				{	
					// ��λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬 ֱ���ӹ�
					if(l1.isContact) continue;
					for(Location l2: patientLocations)
					{	
						// �������С���޶��ľ��뷶Χ����Ϊһ��contact, ��object ��Ⱦʱ��+1
						// +1������ѭ�������ٺ����� patientLocations ���������
						double dis = Distance.distance(l1.lat, l1.lon, l2.lat, l2.lon);
						if(dis <= distance_threshold)
						{	
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
//						if(!l1.isContact && records.containsKey(l1.id)) records.put(l1.id, 0);
				}
			} 
		} // End 2
		
		// 3. Ҫ��ʱ����������record��¼�� û�з����������սӴ��� object ��Ⱦʱ����0
		for(Integer id: records.keySet())
		{	
			if (!isInfect.containsKey(id)) records.put(id, 0);
		}
		isInfect.clear();
		return res;
	}
	
	
	
	/**
	 * ��ȡÿ��ʱ�̵ķ���������ܱ�����
	 * ����MBR���н�������ѯ���ܴ��ڵĲ���������һ���ԽǾ���������
	 * @param batch ��ǰʱ�̵Ĺ켣λ����
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch, int ts)
	{	
		
		
		ArrayList<Integer> res = new ArrayList<Integer>();
		// �����������ʱ�䣬ֱ���������������+1
		if(ts % 2 == 0)
		{
//			for(Integer id: records.keySet())
//			{	
//				int period = records.get(id)+1;
//				records.put(id, period);
//				// ���ÿ��object�ڷ�������ʱ��������ֵ�����������
//				if (period >= this.infectPeriod)
//				{
//					patientIDs.add(id);
//					res.add(id);
//				}
//			}
			return EGP_trace(batch);
		}
		
		
		// ��¼ÿ��λ�õ��areaID, ��¼ÿ��area�е�locations, ��¼��ǰʱ�̵�Σ������
		findInfectedArea(batch);
		// ��¼�����Ĳ���
		// 1. ����ÿ����������, �ڸ÷������ڵ������� ��Ⱦʱ��ֱ�Ӽ�1, �����Ϊ��ʱ�������ܵ���Ⱦ
//		for(Integer areaID: areas)
//		{
//			for(Location l: g.ordinaryAreasLocations.get(areaID))
//			{	
//				if(l.isContact) continue;
//				if(!records.containsKey(l.id)) {
//					records.put(l.id, 1);
//				}else {
//					int period = records.get(l.id)+1;
//					records.put(l.id, period);
//					// ���ÿ��object�ڷ�������ʱ��������ֵ�����������
//					if (period >= this.infectPeriod)
//					{
//						patientIDs.add(l.id);
//						res.add(l.id);
//					}
//				}
//				// ��Ǹ�λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬�����ظ��б�
//				l.isContact = true;
//				isInfect.put(l.id, true);
//			}
//		}
		
		// 2. ����ÿ����������, ��Ѱ�䵱ǰʱ�� ���Ը�Ⱦ������λ�õ㣬��������Щ��������ܵ���Ⱦ��Location
		for(Integer areaID: areas)
		{	
			// 2.1 ͨ�������ҵ� ��������Χ�� Areas
			int[] nnIDs = g.getAffectAreas(areaID);
			// ��affect area����̽��
			for(int nn: nnIDs)
			{	
				// ���ھӲ��ڿռ䷶Χ�ڣ�ֱ���Թ�
				if (nn == -1) continue;
				// 2.4 ����ÿ�� nn area ������locations
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// �������ڸ�ʱ�̲�������Ա��ֱ���ӹ�
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				for(Location l:ordinaryLocations)
				{	
					if(l.isContact) continue;
					if(!records.containsKey(l.id)) {
						records.put(l.id, 1);
					}else {
						int period = records.get(l.id)+1;
						records.put(l.id, period);
						// ���ÿ��object�ڷ�������ʱ��������ֵ�����������
						if (period >= this.duration_threshold)
						{
							patientIDs.add(l.id);
							res.add(l.id);
						}
					}
					// ��Ǹ�λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬�����ظ��б�
					l.isContact = true;
					isInfect.put(l.id, true);
				}
			}
		}// End 2
				
		
		// 3. Ҫ��ʱ����������record��¼�� û�з����������սӴ��� object ��Ⱦʱ����0
		for(Integer id: records.keySet())
		{	
			if (!isInfect.containsKey(id)) records.put(id, 0);
		}
		isInfect.clear();
		
		
		// ����֤res�в�����
		return res;
	}

	
	/**
	 * ��ȡÿ��ʱ�̵ķ���������ܱ�����
	 * ����MBR���н�������ѯ���ܴ��ڵĲ���������һ���ԽǾ���������
	 * @param batch ��ǰʱ�̵Ĺ켣λ����
	 */
	public ArrayList<Integer> approximate(ArrayList<Location> batch, boolean getAll)
	{
		// ��¼ÿ��λ�õ��areaID, ��¼ÿ��area�е�locations, ��¼��ǰʱ�̵�Σ������
		findInfectedArea(batch);
		// ��¼ȷ����Ⱦ������, ��getAll���¼���п��ܸ�Ⱦ����
		ArrayList<Integer> infectObjects = new ArrayList<Integer>();
		// 1. ����ÿ����������, �ڸ÷������ڵ������� ��Ⱦʱ��ֱ�Ӽ�1, �����Ϊ��ʱ�������ܵ���Ⱦ
		for(Integer areaID: areas)
		{	
			for(Location l:g.ordinaryAreasLocations.get(areaID))
			{
				infectObjects.add(l.id);
			}
		}
		
		// 2. ����ÿ����������, ��Ѱ�䵱ǰʱ�� ���Ը�Ⱦ������λ�õ㣬��������Щ��������ܵ���Ⱦ��Location
		for(Integer areaID: areas)
		{
			// 2.1 ͨ�������ҵ� ��������Χ�� Areas
			int[] nnIDs = g.getAffectAreas(areaID);
			// 2.2 ��ȡ ��ǰ�������� ÿ�����˵�Locations
			ArrayList<Location> patientLocations = g.patientAreasLocations.get(areaID);
			for(int nn: nnIDs)
			{	
				if (nn == -1) continue; // ���ھӲ��ڿռ䷶Χ�ڣ�ֱ���Թ�
				// 2.3 ����ÿ�� nn area ������locations
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// 2.4 ���� ordinaryLocations �� patientLocations ����֮�����С����
				if(ordinaryLocations != null && !ordinaryLocations.isEmpty())
				{
					for(Location l1:ordinaryLocations)
					{	
						if(getAll)
						{
							infectObjects.add(l1.id);
							continue;
						}
						for(Location l2: patientLocations)
						{	
							// �������С���޶��ľ��뷶Χ����Ϊһ��contact, ��object ��Ⱦʱ��+1
							// +1������ѭ�������ٺ����� patientLocations ���������
							double dis = Distance.distance(l1.lat, l1.lon, l2.lat, l2.lon);
							if(dis <= distance_threshold)
							{	
								infectObjects.add(l1.id);
								break;
							}
						}
					}
				}
			} 
		} // End 2
		// ���ܻ����ظ��ģ���Ҫ���˵�
		HashSet<Integer> set = new HashSet<Integer>(infectObjects);
		infectObjects = new ArrayList<Integer>(set);
		return infectObjects;
	}

	
	
	
}
