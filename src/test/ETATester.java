package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data_loader.Location;
import data_loader.Stream;
import trace.ETA_Tracer;
import trace.Settings;
import trace.Util;


public class ETATester {
	
	public static void main(String[] args) {
		// 1. ��ȡ�ò������µ����й켣���ļ���
		File[] files = new File(Settings.dataPath).listFiles();
		// 2. ����һ��Tracerʵ�����󣬴���ʵʱ��ȡ���Ĺ켣λ�õ�
		ETA_Tracer etaTracer = new ETA_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. ��ʼ��һ��query 
		etaTracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. ��¼һЩͳ������
		long etaRuntime = 0; // ����ʱ��
		int locNum = 0; // �����λ�õ���Ŀ
		int dayNum = 0;  // ���������
		int currentTs = 0; // ��ǰʱ�̴����ʱ���
		HashMap<Integer, ArrayList<Integer>> ETA_res = new HashMap<>();
		// 4. ��ʼ����ʽ��ѯ
		if (files == null)
		{
			System.out.println("No valid files found!!");
			return;
		}
		for(File f:files)
		{	
			Stream stream  = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // ��ʽ��ȡ����һ��ʱ�̵�λ�õ�
			while (batch != null && !batch.isEmpty())
			{
				if (batch.get(0).ts % Settings.sr != 0)
				{
					continue; // ������ڲ���ʱ����ϣ�������
				}
				locNum += batch.size();
//				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				// ETA��ѯ
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> ETA_cases = etaTracer.trace(batch);
				if(!ETA_cases.isEmpty()) ETA_res.put(currentTs, ETA_cases);
				long endTime = System.currentTimeMillis();
				etaRuntime += endTime-startTime;
				currentTs += 1;
				batch = stream.read_batch();
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays)
			{
				break;
			}
		} // End 'For' Loop
		
		// չʾһЩ���
		System.out.println("������λ�õ���Ŀ: " + locNum);
		System.out.println("������ʱ: "+ etaRuntime);
		HashSet<Integer> ETA_cases = new HashSet<>();
		for(Integer key: ETA_res.keySet())
		{
			ETA_cases.addAll(ETA_res.get(key));
		}
		System.out.println("������cases of exposure: " + ETA_cases.size());

	}
	
}
