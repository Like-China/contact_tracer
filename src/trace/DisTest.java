package trace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


import data_loader.Location;
import data_loader.Stream;

public class DisTest {
	
	public static void test(int days, int exp_num)
	{	
		float[] diss = {0.0002f, 0.0004f, 0.0006f, 0.0008f, 0.001f};
		int[] durations = {5,10,15,20,25};
		int[] nums = {200, 400, 600, 800, 1000};
		for(Float dis: diss)
		{
			for(Integer duration: durations)
			{
				long time1 = System.currentTimeMillis();
				for(Integer init_num: nums)
				{	
					System.out.printf("\n距离%f  时长%d query数目：%d\n", dis*10000, duration, init_num);
					float gradScale = (float)(dis/Math.sqrt(2));
					double ETA_mean_time = 0;
					double MBR_mean_time = 0;
					double MBR1_mean_time = 0;
					double APP_mean_time = 0;
					double APP1_mean_time = 0;
					
					double ETA_mean_num = 0;
					double MBR_mean_num = 0;
					double MBR1_mean_num = 0;
					double APP_mean_num = 0;
					double APP1_mean_num = 0;
					
					double mean_acc = 0;
					for(int expNum=0; expNum<exp_num;expNum++)
					{
						// 1. 获取该采样率下的所有轨迹点文件名
						File[] files = new File("D:/data/"+"beijing"+Settings.sr).listFiles();
						// 2. 创建一个Tracer实例对象，处理实时读取到的轨迹位置点
						Tracer ETA_tracer = new Tracer(gradScale, duration, "beijing");
						Tracer MBR_tracer = new Tracer(gradScale, duration, "beijing");
						Tracer MBR1_tracer = new Tracer(gradScale, duration, "beijing");
						Tracer APP_trace = new Tracer(gradScale, duration, "beijing");
						Tracer APP1_trace = new Tracer(gradScale, duration, "beijing");
						// 3. 随机初始化一批query patients id, 假设他们是初始化的病人
						ETA_tracer.initPatientIds(Settings.objectNum, init_num, Settings.isRandom);
						MBR_tracer.patientIDs = (HashSet<Integer>) ETA_tracer.patientIDs.clone();
						MBR1_tracer.patientIDs = (HashSet<Integer>) ETA_tracer.patientIDs.clone();
						APP_trace.patientIDs = (HashSet<Integer>) ETA_tracer.patientIDs.clone();
						APP1_trace.patientIDs = (HashSet<Integer>) ETA_tracer.patientIDs.clone();
						// 4. 开始读取轨迹位置流并实现实时处理
						// 5. 记录总的位置点数目
						int location_num = 0;
						// 记录所有时刻的检测输出
						HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<Integer, ArrayList<Integer>>();
						HashMap<Integer, ArrayList<Integer>> MBR_res = new HashMap<Integer, ArrayList<Integer>>();
						HashMap<Integer, ArrayList<Integer>> MBR1_res = new HashMap<Integer, ArrayList<Integer>>();
						HashMap<Integer, ArrayList<Integer>> APP_res = new HashMap<Integer, ArrayList<Integer>>();
						HashMap<Integer, ArrayList<Integer>> APP1_res = new HashMap<Integer, ArrayList<Integer>>();
						// 近似算法用 记录几个时刻的exposure case
						ArrayList<ArrayList<Integer>> all_EC = new ArrayList<ArrayList<Integer>>();
						ArrayList<Integer> EC = new ArrayList<Integer>();
						ArrayList<Integer> EC1 = new ArrayList<Integer>();
						int index = 0; // 0-infectPeriod-1
						int ts = 0;
						// 记录总运行时间
						long ETA_time = 0;
						long MBR_time = 0;
						long MBR1_time = 0;
						long APP_time = 0;
						long APP1_time = 0;
						// 记录读到第几天
						int day = 0;
						days = files.length >= days?days:files.length;
						for(File f:files)
						{	
							Stream stream  = new Stream(f.toString());
							ArrayList<Location> batch = stream.read_batch(); // 流式读取当天一个时刻的位置点
							while (batch != null)
							{  
								batch = stream.read_batch();
								if(batch == null || batch.isEmpty()) break; // 如果当天时间流已经结束，则跳出循环进行下一天的查询，感染时长仍然会累计
								if (batch.get(0).ts % Settings.sr != 0) continue; // 如果不在采样时间点上，不处理
								location_num += batch.size();
				//				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
								
								// 0. ETA
								long  startTime = 0;
								long  endTime = 0;
								if (ts <= 100)
								{
									startTime = System.currentTimeMillis();
									ArrayList<Integer> infectionCases0 = ETA_tracer.traceByExact(batch);
									if(!infectionCases0.isEmpty()) ETA_res.put(ts, infectionCases0);
									endTime = System.currentTimeMillis();
									ETA_time += endTime-startTime;
								}
								for(Location l: batch)
								{
									l.isContact = false;
								}
								
								// 1. MBR without precheck
								startTime = System.currentTimeMillis();
								ArrayList<Integer> infectionCases1 = MBR_tracer.traceByDistance(batch);
								if(!infectionCases1.isEmpty()) MBR_res.put(ts, infectionCases1);
								endTime = System.currentTimeMillis();
								MBR_time += endTime-startTime;
								
								for(Location l: batch)
								{
									l.isContact = false;
								}
								
								// 2. MBR with prechecking
								startTime = System.currentTimeMillis();
								ArrayList<Integer> infectionCases2 = MBR1_tracer.traceByMBR(batch);
								if(!infectionCases2.isEmpty()) MBR1_res.put(ts, infectionCases2);
								endTime = System.currentTimeMillis();
								MBR1_time += endTime-startTime;
								
								for(Location l: batch)
								{
									l.isContact = false;
								}
								
								// 3. APP without hop
								startTime = System.currentTimeMillis();
								ArrayList<Integer> infectionCases3 = APP_trace.traceByAPProximate(batch, ts);
								if(!infectionCases3.isEmpty()) APP_res.put(ts, infectionCases3);
								endTime = System.currentTimeMillis();
								APP_time += endTime-startTime;
								
								for(Location l: batch)
								{
									l.isContact = false;
								}
								
								
								
								// 4. APP with hop
								startTime = System.currentTimeMillis();
								if (ts % 2 == 0)
								{
									EC = APP1_trace.approximate(batch, false);
									EC1 = new ArrayList<Integer>(EC);
									all_EC.add(EC);
								}else {
									EC = APP1_trace.approximate(batch, false);
									EC1 = new ArrayList<Integer>(EC);
									all_EC.add(EC);
								}
								if(index % duration == duration-1)
								{	
									for(int ii=0;ii<all_EC.size();ii+=2)
									{	
										EC1.retainAll(all_EC.get(ii));
									}
									
									if(!EC1.isEmpty())
									{	
										APP1_res.put(ts, EC1);
										APP1_trace.patientIDs.addAll(EC1);
									}
									// 移除最远的记录
									all_EC.remove(0);
									index -= 1;
								}
								index += 1;
								ts += 1;
								endTime = System.currentTimeMillis();
								APP1_time += endTime-startTime;
								
							}
							day += 1;
							if (day == days) break;
							
						}
						
						System.out.println("总处理位置点数目: "+location_num);
						System.out.printf("共处理%d个时刻\n", ts);
						System.out.println("总用时(s) (ETA MBR MBR1 APP APP1): " + ETA_time + " "+ MBR_time + " "+MBR1_time + " "+APP_time+ " "+APP1_time);
						System.out.println("平均用时(s) (ETA MBR MBR1 APP APP1): " + (double)(ETA_time)/ts + " "+ (double)(MBR_time)/ts + " "+(double)(MBR1_time)/ts + " "+(double)(APP_time)/ts+ " "+(double)(APP1_time)/ts);
						
						ETA_mean_time += (double)(ETA_time)/ts;
						MBR_mean_time += (double)(MBR_time)/ts;
						MBR1_mean_time += (double)(MBR1_time)/ts;
						APP_mean_time += (double)(APP_time)/ts;
						APP1_mean_time += (double)(APP1_time)/ts;
						
						
						
						HashSet<Integer>  MBR_ids = new HashSet<Integer>();
						HashSet<Integer>  MBR1_ids = new HashSet<Integer>();
						HashSet<Integer>  APP_ids = new HashSet<Integer>();
						HashSet<Integer>  APP1_ids = new HashSet<Integer>();
						for(Integer key: MBR_res.keySet())
						{
							MBR_ids.addAll(MBR_res.get(key));
						}
						for(Integer key: MBR1_res.keySet())
						{
							MBR1_ids.addAll(MBR1_res.get(key));
						}
						for(Integer key: APP_res.keySet())
						{
							APP_ids.addAll(APP_res.get(key));
						}
						for(Integer key: APP1_res.keySet())
						{
							APP1_ids.addAll(APP1_res.get(key));
						}
						System.out.println("总数目 (MBR MBR1 APP APP1)："+ MBR_ids.size() + " "+ MBR1_ids.size() + " "+APP_ids.size()+ " "+APP1_ids.size());
						
						MBR_mean_num += MBR_ids.size();
						MBR1_mean_num += MBR1_ids.size();
						APP_mean_num += APP_ids.size();
						APP1_mean_num += APP1_ids.size();
						
						// 计算正确率
						double accu = Util.accuracy(MBR_ids, APP_ids);
				        System.out.println("mbr-app准确率"+accu);
				        accu = Util.accuracy(MBR_ids, APP1_ids);
				        System.out.println("mbr-app1准确率"+accu);
				        System.out.println();
				        
				        mean_acc += accu;
					}
					
					ETA_mean_time /= exp_num;
					MBR_mean_time /= exp_num;
					MBR1_mean_time /= exp_num;
					APP_mean_time /= exp_num;
					APP1_mean_time /= exp_num;
					
					MBR_mean_num /= exp_num;
					MBR1_mean_num /= exp_num;
					APP_mean_num /= exp_num;
					APP1_mean_num /= exp_num;
					
					mean_acc /= exp_num;
					System.out.println("平均用时(s) (ETA MBR MBR1 APP APP1): " + ETA_mean_time/20 + " "+ MBR_mean_time + " "+MBR1_mean_time + " "+APP_mean_time+ " "+APP1_mean_time);
					System.out.println("平均数目 (MBR MBR1 APP APP1)："+ MBR_mean_num + " "+ MBR1_mean_num + " "+APP_mean_num+ " "+APP1_mean_num);
					System.out.println("平均准确率: "+mean_acc);
					System.out.println("*********************************************");
				}
				long time2 = System.currentTimeMillis();
				System.out.println("一轮用时: "+ (time2-time1));
			}
		}
		
		
	}
	

	public static void main(String[] args) {
		int exp_num = 1;
		test(Settings.days, exp_num);
	}
	
	
}
