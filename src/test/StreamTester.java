package test;
import java.io.File;
import java.util.ArrayList;

import trace.Settings;
import trace.Util;
import data_loader.Location;
import data_loader.Stream;

/*
��֤λ������ȡ��׼ȷ��
 */

public class StreamTester {

	public static void main(String[] args) {
		//  ��ȡ�ò������µ����й켣���ļ���
		File[] files = new File(Settings.dataPath).listFiles();
		if (files == null)
		{
			System.out.println("No valid files found!!");
			return;
		}
		for(File f:files)
		{	
			long t1 = System.currentTimeMillis();
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); // ��ȡ��������λ�õ�
			System.out.println("First location of current day: "+batch.get(0));
			while(!batch.isEmpty())
			{
				System.out.println("Number of locations at current timestamp: "+batch.size());
				System.out.println("First location of current timestamp: \n"+batch.get(0));
				System.out.println("Last location of current timestamp: \n"+batch.get(batch.size()-1));
				System.out.println("******************");
				batch = stream.read_batch();
				Util.sleep(1);
			}
			long t2 = System.currentTimeMillis();
			System.out.println("��ʱ: ");
			System.out.println(t2-t1);
			if(batch.isEmpty())
			{
				break;
			}
		}
		
	}
}
