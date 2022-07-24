/*
 * @Author: your name
 * @Date: 2022-03-30 12:02:25
 * @LastEditTime: 2022-03-30 21:21:23
 * @LastEditors: Paulzzzhang
 * @Description: 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 * @FilePath: /contact_tracer/src/test/StreamTester.java
 */
package test;

import java.io.File;
import java.util.ArrayList;

import trace.Settings;
import trace.Util;
import data_loader.Location;
import data_loader.Stream;

public class StreamTester {

	public static void main(String[] args) {
		File[] files = Util.orderByName(Settings.dataPath);
		if (files == null) {
			System.out.println("No valid files found!!");
			return;
		}
		for (File f : files) {
			long t1 = System.currentTimeMillis();
			Stream stream = new Stream(f.toString());
			ArrayList<Location> batch = stream.read_batch(); 
			System.out.println("First location of current day: " + batch.get(0));
			while (!batch.isEmpty()) {
				System.out.println("Number of locations at current timestamp: " + batch.size());
				System.out.println("First location of current timestamp: \n" + batch.get(0));
				System.out.println("Last location of current timestamp: \n" + batch.get(batch.size() - 1));
				System.out.println("******************");
				batch = stream.read_batch();
				Util.sleep(1);
			}
			long t2 = System.currentTimeMillis();
			System.out.println("time cost: ");
			System.out.println(t2 - t1);
			if (batch.isEmpty()) {
				break;
			}
		}

	}
}
