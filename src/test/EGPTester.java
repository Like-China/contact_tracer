package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data_loader.Location;
import data_loader.Stream;
import trace.EGP_Tracer;
import trace.Settings;
import trace.Util;

public class EGPTester {
	
	public static void main(String[] args) {
		// 1. ��ȡ�ò������µ����й켣���ļ���
		File[] files = new File(Settings.dataPath).listFiles();
		// 2. ����һ��Tracerʵ�����󣬴���ʵʱ��ȡ���Ĺ켣λ�õ�
		EGP_Tracer tracer = new EGP_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. ��ʼ��һ��query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. ��¼һЩͳ������
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> EGP_res = new HashMap<>();
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
				System.out.printf("\n%s %s return locations %d\n", batch.get(0).date, batch.get(0).time, batch.size());
				// EGP��ѯ
				long startTime = System.currentTimeMillis();
//				ArrayList<Integer> EGP_cases = tracer.trace_no_checking(batch);
				ArrayList<Integer> EGP_cases = tracer.trace(batch);
				if(!EGP_cases.isEmpty()) EGP_res.put(tsNum, EGP_cases);
				long endTime = System.currentTimeMillis();
				System.out.println("��ʱ: "+ (endTime-startTime));
				runtime += endTime-startTime;
				tsNum += 1;
				batch = stream.read_batch();
			} // End 'While' Loop
			dayNum += 1;
			if (dayNum >= Settings.maxProcessDays)
			{
				break;
			}
		} // End 'For' Loop


		// չʾһЩ���
		System.out.printf("������%d��λ�ã� %d��ʱ�� " , locNum, tsNum);
		System.out.println("��ʱ:  " + runtime + " ƽ����ʱ:  " + (double)runtime/tsNum);
		HashSet<Integer>  EGP_cases = new HashSet<>();
		for(Integer key: EGP_res.keySet())
		{
			EGP_cases.addAll(EGP_res.get(key));
		}
		System.out.println("������cases of exposure: " + EGP_cases.size());
	}
	
}
