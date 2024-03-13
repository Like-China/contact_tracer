/*
 * @Descripttion: Rika's code
 * @version: 1.0.0
 * @Author: Rika
 * @Date: 2023-11-19 13:04:00
 * @LastEditors: Rika
 * @LastEditTime: 2024-03-12 14:03:19
 */
package indexes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import data_loader.Location;
import data_loader.Stream;
import trace.Settings;

public class TestGenerator {
    public static void test() {

        // load locations
        long t1 = System.currentTimeMillis();
        Stream stream = new Stream(Settings.dataPath);
        ArrayList<Location> allLocs = stream.batch(10000);
        long t2 = System.currentTimeMillis();
        System.out.println("time cost of loading locations (ms): " + (t2 - t1));
        int n = allLocs.size();
        System.out.println("Location size: " + n);
        // generate test data
        int expNB = 20;
        double[][] queryPoint = new double[n][2];
        // query for muQuadTree
        ArrayList<MyRectangle> query = new ArrayList<>();
        double minLon = 10000;
        double minLat = 10000;
        double maxLat = -10000;
        double maxLon = -10000;
        for (int i = 0; i < n; i++) {
            Location l = allLocs.get(i);
            queryPoint[i] = new double[] { l.lat, l.lon };
            minLon = minLon < l.lon ? minLon : l.lon;
            minLat = minLat < l.lat ? minLat : l.lat;
            maxLon = maxLon < l.lon ? l.lon : maxLon;
            maxLat = maxLat < l.lat ? l.lat : maxLat;
        }
        Random r = new Random();
        for (int i = 0; i < n; i++) {
            double x1Vary = 2 / 10000;
            double y1Vary = 2 / 10000;
            double x1 = queryPoint[i][0];
            double y1 = queryPoint[i][1];
            query.add(new MyRectangle(null,x1, y1, x1Vary, y1Vary));
        }
        t1 = System.currentTimeMillis();

        // retrieve
        int count = 0;
        int calc_count = 0;
        for (int i = 0; i < expNB; i++) {
            RectangleQuadTree quad = new RectangleQuadTree(0, new MyRectangle(null,minLat, minLon, maxLat - minLat, maxLon - minLon));
            for (MyRectangle rec : query) {
                quad.insert(rec);
            }
            for (int j = 0; j < n; j++) {
                HashSet<MyRectangle> returnObjects = new HashSet<>();
                ArrayList<Integer> indexSeq = new ArrayList<>();
                MyRectangle queryRec = query.get(j);
                quad.retrieve(returnObjects, queryRec);
                for (MyRectangle dbRectangle : returnObjects) {
                    calc_count++;
                    if (queryRec.intersects(dbRectangle)) {
                        count++;
                    }
                }
            }

        }
        System.out.println(calc_count + "/" + count);
        t2 = System.currentTimeMillis();
        System.out.println("time consume:" + (t2 - t1));

        t1 = System.currentTimeMillis();
        // bruteful
        count = 0;
        calc_count = 0;
        for (int i = 0; i < expNB; i++) {
            for (int j = 0; j < n; j++) {
                MyRectangle queryRec = query.get(j);
                for (MyRectangle dbRectangle : query) {
                    calc_count++;
                    if (queryRec.intersects(dbRectangle)) {
                        count++;
                    }
                }
            }
        }
        System.out.println(calc_count + "/" + count);
        t2 = System.currentTimeMillis();
        System.out.println("time consume:" + (t2 - t1));

    }

    public static void main(String[] args) {
        test();
    }
}
