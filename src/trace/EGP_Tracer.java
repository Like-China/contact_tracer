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

public class EGP_Tracer {
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
	
	public EGP_Tracer(double distance_threshold, int duration_threshold, String cityname) {
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
	public ArrayList<Integer> trace_no_checking(ArrayList<Location> batch)
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
	 * ����ÿ��λ�õ㵽 �Ѽ�¼patients�켣��ľ��룬�����ݾ�����ֵ �ж��䵱ǰ�Ƿ���Σ������
	 * ����MBR����pre-checking
	 * @param batch ��ǰʱ�̵Ĺ켣λ����
	 */
	public ArrayList<Integer> trace(ArrayList<Location> batch)
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
			// ����affect area����̽��
			for(int nn: nnIDs)
			{	
				// ���ھӲ��ڿռ䷶Χ�ڣ�ֱ���Թ�
				if (nn == -1) continue;
				// 2.4 ����ÿ�� nn area ������locations
				ArrayList<Location> ordinaryLocations = g.ordinaryAreasLocations.get(nn);
				// �������ڸ�ʱ�̲�������Ա��ֱ���ӹ�
				if(ordinaryLocations == null || ordinaryLocations.isEmpty()) continue;
				
				// 2.5 ����в��˴��ڵĸ�Ⱦ������Ӱ�����ͨ������������4���ſ���ʹ��prechecking��
				// ���MBR �ĸ��� �������Сֵ
				if (patientLocations.size()>= minMBR)
				{	
					// �����������ͨ��λ�õ�MBR���Ѿ�ȷ�����ڷ��յĲ����ٴμ���
					// 2.3 ���������patientλ�õ�MBR
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
					
					// ��С���붼�����޶ȣ�����������object��ǰʱ������Ϊδ��Ⱦ����Ⱦʱ�����㣬ֱ���ӹ�
					if(min_dist > distance_threshold)
					{	
						for(Location l:ordinaryLocations)
						{	
							// ��λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ��� λ�õ㣬 ֱ���ӹ�
							if(!l.isContact && records.containsKey(l.id)) records.put(l.id, 0);
						}
						continue;
					}
					// �����붼С���޶ȣ�������ȫ��objects�ĸ�Ⱦʱ��+1
					else if(max_dist <= distance_threshold)
					{
						for(Location l1:ordinaryLocations)
						{	
							// ��λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬 ֱ���ӹ�
							if(l1.isContact) continue;
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
						}
						continue;
					}
					
				}// ����Ԥ���
				
				// Ԥ����ʣ�µĲż��� ordinaryLocations �� patientLocations ����֮�����С����
				for(Location l1: ordinaryLocations)
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
//					if(!l1.isContact && records.containsKey(l1.id)) records.put(l1.id, 0);
				}
			} 
		} // End 2
		
		// 3. Ҫ��ʱ����������record��¼�� û�з����������սӴ��� object ��Ⱦʱ����0
		for(Integer id: records.keySet())
		{	
			if (!isInfect.containsKey(id)) records.put(id, 0);
		}
		isInfect.clear();
		// ����֤res�в�����
		return res;
	}
	
	
	
	
}
