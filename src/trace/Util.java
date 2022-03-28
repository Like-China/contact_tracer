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
	 * 获取一批位置点MBR的四个顶点经纬度坐标
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
			// 该位置点为已经探测 且属于 密切接触的位置点， 直接掠过
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
	
	
	// 计算近似算法延迟
	public static double delay(HashMap<Integer,ArrayList<Integer>> accurate, HashMap<Integer,ArrayList<Integer>> Estimate){

        HashMap<Integer,Integer> Delay_per_mp=new HashMap<Integer,Integer>();   //key鏄痠d,value鏄簿纭繑鍥炵殑鏃堕棿
        
        double delay_sum=0;
        double delay_num=0;
      
        Set<Integer> set=accurate.keySet();
        Object[] arr=set.toArray();
        Arrays.sort(arr);
        
        for(Object i : arr){  //所有被延迟输出的id
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
        
        for(Integer i:Estimate.keySet()) {   //被延迟输出了多少
        	ArrayList<Integer> appro_set=Estimate.get(i);
        	for(int j=0;j<appro_set.size();j++) {
        		Integer delay_id=appro_set.get(j);
        		if(Delay_per_mp.containsKey(delay_id)) {
        			Integer t=Delay_per_mp.get(delay_id);
        			if(t<i) {   //这里是因为近似算法可能会由于近似而把一个在精确算法中在t才判断为contact的提前输出
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
	
	// 计算近似算法准确率
	public static double accuracy(HashSet<Integer> accu_id, HashSet<Integer> appro_id){
        Integer correct_num=0;
        
        for(Integer id: accu_id){
            if(appro_id.contains(id)){
                correct_num+=1;
            }
        }
        return  (double)correct_num*100/appro_id.size();
    }
	
	// 获得一批初始化病人
	/**
	 * 输入总人数和初始化的病人人数，产生一批随机的病人Id
	 * @param totalNum 总人数
	 * @param patientNum 初始化的病人人数
	 * @return 一批随机的病人Id
	 */
 	public static HashSet<Integer> initPatientIds(int totalNum, int patientNum, boolean isRandom)
	{	
 		// 如果不要求随机生成
 		HashSet<Integer> patientIDs = new HashSet<Integer>();
 		if (! isRandom )
 		{
 			for(int i=0;i<patientNum;i++)
 			{
 				patientIDs.add(i);
 			}
 			return patientIDs;
 		}
		int[] num1=new int[patientNum];//定义一个长度10的数组（10个人）
		int[] num2=new int[totalNum];//这个数组用来存储数据（总共1000人）
		//先给数组2赋值，不然空数组会导致后面取随机数重复
		for(int i=0;i<num2.length;i++){
			//这里＋1是因为取随机数的范围是从零开始的，为了避免数组出现零，所以＋1
			num2[i]=i+1;
		}
		Random r = new Random();//生成随机数的类
		int index=-1;//定义一个变量记录下标
		//创建10个不重复的随机数（核心代码块）
		for(int i=0;i<num1.length;i++){
			index=r.nextInt(num2.length-i);
			num1[i]=num2[index];
			// 下面代码是用来把选择的数调换到数组的最后一位，避免重复
			int b=num2[index];
			num2[index]=num2[num2.length-1-i];
			num2[num2.length-1-i]=b;
		}
		//排序数组并添加到病人集合中，集合中各元素互异
		Arrays.sort(num1);
		for(int i:num1)
		{
			patientIDs.add(i);
		}
		// 返回
		return patientIDs;
	}
	

}



