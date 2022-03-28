package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import data_loader.Location;
import data_loader.Stream;
import trace.ETA_Tracer;
import trace.Settings;
import trace.Util;

public class ETATester {
	
	public static void main(String[] args) {
		// 1. ��ȡ�ò������µ����й켣���ļ���
		File[] files = new File("D:/data/"+"beijing"+Settings.sr).listFiles();
		// 2. ����һ��Tracerʵ�����󣬴���ʵʱ��ȡ���Ĺ켣λ�õ�
		ETA_Tracer tracer = new ETA_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. ��ʼ��һ��query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. ��¼һЩͳ������
		long ETA_time = 0;
		int location_num = 0;
		int current_day = 0;
		int current_ts = 0;
		HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<Integer, ArrayList<Integer>>();
		// 4. ��ʼ����ʽ��ѯ
		for(File f:files)
		{	
			Stream stream  = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // ��ʽ��ȡ����һ��ʱ�̵�λ�õ�
			while (batch != null && !batch.isEmpty())
			{  
				if(batch == null || batch.isEmpty()) break; // �������ʱ�����Ѿ�������������ѭ��������һ��Ĳ�ѯ����Ⱦʱ����Ȼ���ۼ�
				if (batch.get(0).ts % Settings.sr != 0) continue; // ������ڲ���ʱ����ϣ�������
				location_num += batch.size();
				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				
				// ETA��ѯ
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> ETA_cases = tracer.trace(batch);
				if(!ETA_cases.isEmpty()) ETA_res.put(current_ts, ETA_cases);
				long endTime = System.currentTimeMillis();
				System.out.println("��ʱ: "+ (endTime-startTime));
				ETA_time += endTime-startTime;
				current_ts += 1;
				batch = stream.read_batch();
			} // End While Loop
			current_day += 1;
			if (current_day == 2) break;
		} // End For Loop
		
		// չʾһЩ���
		System.out.println("������λ�õ���Ŀ: " + location_num);
		System.out.println("������ʱ: "+ ETA_time);
	}
	
}
