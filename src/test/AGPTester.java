package test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data_loader.Location;
import data_loader.Stream;
import trace.AGP_Tracer;
import trace.Settings;
import trace.Util;

public class AGPTester {
	
	public static void main(String[] args) {
		// 1. ��ȡ�ò������µ����й켣���ļ���
		File[] files = new File(Settings.dataPath).listFiles();
		// 2. ����һ��Tracerʵ�����󣬴���ʵʱ��ȡ���Ĺ켣λ�õ�
		AGP_Tracer tracer = new AGP_Tracer(Settings.distance_threshold, Settings.duration_threshold, Settings.city_name);
		// 3. ��ʼ��һ��query 
		tracer.patientIDs = Util.initPatientIds(Settings.objectNum, Settings.initPatientNum, Settings.isRandom);
		// 4. ��¼ͳ������
		long runtime = 0;
		int locNum = 0;
		int dayNum = 0;
		int tsNum = 0;
		HashMap<Integer, ArrayList<Integer>> AGP_res = new HashMap<>();
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
				System.out.printf("\n%s %s return locations %d", batch.get(0).date, batch.get(0).time, batch.size());
				// AGP��ѯ
				long startTime = System.currentTimeMillis();
				ArrayList<Integer> AGP_cases = tracer.trace(batch, tsNum);
				// �����������
				if(!AGP_cases.isEmpty())
				{
					AGP_res.put(tsNum, AGP_cases);
				}
				long endTime = System.currentTimeMillis();
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


		// չʾʵ����
		System.out.printf("������%d��λ�ã� %d��ʱ�� " , locNum, tsNum);
		System.out.println("��ʱ:  " + runtime + " ƽ����ʱ:  " + (double)runtime/tsNum);
		HashSet<Integer>  AGP_cases = new HashSet<>();
		for(Integer key: AGP_res.keySet())
		{
			AGP_cases.addAll(AGP_res.get(key));
		}
		System.out.println("������cases of exposure: " + AGP_cases.size());
	}
	
}
