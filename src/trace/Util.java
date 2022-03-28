package trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import data_loader.Location;

public class Util {
	
	public static void sleep(long duration)
	{
		try {
			Thread.sleep(duration);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}
	}
	
	public Util() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * ��ȡһ��λ�õ�MBR���ĸ����㾭γ������
	 * @param locations
	 * @return
	 */
	public static float[][] getMBR(ArrayList<Location> locations)
	{
		float minLon = 1000f;
		float maxLon = -1000f;
		float minLat = 1000f;
		float maxLat = -1000f;
		float[][] res = new float[4][2];
		for(Location l: locations)
		{	
			// ��λ�õ�Ϊ�Ѿ�̽�� ������ ���нӴ���λ�õ㣬 ֱ���ӹ�
			if(l.isContact) continue;
			minLon = Math.min(minLon, l.lon);
			maxLon = Math.max(maxLon, l.lon);
			minLat = Math.min(minLat, l.lat);
			maxLat = Math.max(maxLat, l.lat);
		}
		res[0] = new float[]{minLon, minLat};
		res[1] = new float[]{minLon, maxLat};
		res[2] = new float[]{maxLon, minLat};
		res[3] = new float[]{maxLon, maxLat};
		return res;
	}
	
	
	// ��������㷨�ӳ�
	public static double delay(HashMap<Integer,ArrayList<Integer>> accurate, HashMap<Integer,ArrayList<Integer>> Estimate){

        HashMap<Integer,Integer> Delay_per_mp=new HashMap<Integer,Integer>();   //key是id,value是精确返回的时间
        
        double delay_sum=0;
        double delay_num=0;
      
        Set<Integer> set=accurate.keySet();
        Object[] arr=set.toArray();
        Arrays.sort(arr);
        
        for(Object i : arr){  //���б��ӳ������id
            ArrayList<Integer> accu_set=accurate.get(i);
            if(Estimate.containsKey(i)){   
                ArrayList<Integer> appro_set=Estimate.get(i);
                for(int j=0;j<accu_set.size();j++){
                    Integer id=accu_set.get(j);
                    if(!appro_set.contains(id)){  
                        Delay_per_mp.put(id, (Integer)i);
                    }
                }
            }
            else{   
             for(int j=0;j<accu_set.size();j++){
                 Integer id=accu_set.get(j);
                 Delay_per_mp.put(id, (Integer)i);
             }
            }
        }
        
        for(Integer i:Estimate.keySet()) {   //���ӳ�����˶���
        	ArrayList<Integer> appro_set=Estimate.get(i);
        	for(int j=0;j<appro_set.size();j++) {
        		Integer delay_id=appro_set.get(j);
        		if(Delay_per_mp.containsKey(delay_id)) {
        			Integer t=Delay_per_mp.get(delay_id);
        			if(t<i) {   //��������Ϊ�����㷨���ܻ����ڽ��ƶ���һ���ھ�ȷ�㷨����t���ж�Ϊcontact����ǰ���
        				delay_num+=1;
        				delay_sum+=(i-t);
        				Delay_per_mp.remove(delay_id);
        			}
        		}
        	}
        }
        if(delay_num==0) {
        	return 0;
        }
        return delay_sum/delay_num;
    }
	
	// ��������㷨׼ȷ��
	public static double accuracy(HashSet<Integer> accu_id, HashSet<Integer> appro_id){
        Integer correct_num=0;
        
        for(Integer id: accu_id){
            if(appro_id.contains(id)){
                correct_num+=1;
            }
        }
        return  (double)correct_num*100/appro_id.size();
    }
	
	// ���һ����ʼ������
	/**
	 * �����������ͳ�ʼ���Ĳ�������������һ������Ĳ���Id
	 * @param totalNum ������
	 * @param patientNum ��ʼ���Ĳ�������
	 * @return һ������Ĳ���Id
	 */
 	public static HashSet<Integer> initPatientIds(int totalNum, int patientNum, boolean isRandom)
	{	
 		// �����Ҫ���������
 		HashSet<Integer> patientIDs = new HashSet<Integer>();
 		if (! isRandom )
 		{
 			for(int i=0;i<patientNum;i++)
 			{
 				patientIDs.add(i);
 			}
 			return patientIDs;
 		}
		int[] num1=new int[patientNum];//����һ������10�����飨10���ˣ�
		int[] num2=new int[totalNum];//������������洢���ݣ��ܹ�1000�ˣ�
		//�ȸ�����2��ֵ����Ȼ������ᵼ�º���ȡ������ظ�
		for(int i=0;i<num2.length;i++){
			//���1����Ϊȡ������ķ�Χ�Ǵ��㿪ʼ�ģ�Ϊ�˱�����������㣬���ԣ�1
			num2[i]=i+1;
		}
		Random r = new Random();//�������������
		int index=-1;//����һ��������¼�±�
		//����10�����ظ�������������Ĵ���飩
		for(int i=0;i<num1.length;i++){
			index=r.nextInt(num2.length-i);
			num1[i]=num2[index];
			// ���������������ѡ�������������������һλ�������ظ�
			int b=num2[index];
			num2[index]=num2[num2.length-1-i];
			num2[num2.length-1-i]=b;
		}
		//�������鲢��ӵ����˼����У������и�Ԫ�ػ���
		Arrays.sort(num1);
		for(int i:num1)
		{
			patientIDs.add(i);
		}
		// ����
		return patientIDs;
	}
	

}



