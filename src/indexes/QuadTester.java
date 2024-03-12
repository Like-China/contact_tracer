package indexes;

import java.util.List;
import java.util.Random;
import java.io.File;
import java.util.ArrayList;

import trace.Settings;
import trace.Util;
import data_loader.Location;
import data_loader.Stream;

public class QuadTester {

    public static void test() {
        // load locations
        File[] files = Util.orderByName(Settings.dataPath);
        ArrayList<Location> allLocs = new ArrayList<>();
        long t1 = System.currentTimeMillis();
        for (File f : files) {
            Stream stream = new Stream(f.toString());
            ArrayList<Location> batch = stream.batch();
            allLocs.addAll(batch);
            while (!batch.isEmpty()) {
                batch = stream.batch();
                allLocs.addAll(batch);
                if (allLocs.size() > 10000)
                    break;
            }
            if (batch.isEmpty()) {
                break;
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.println("time cost of loading locations: " + (t2 - t1));
        int n = allLocs.size();
        System.out.println("Location size: " + n);

        // generate test data
        int expNB = 20;
        double[][] inputPoint = new double[n][2];
        double[][] queryRectangle = new double[n][4];
        double minLon = 10000;
        double minLat = 10000;
        double maxLat = -10000;
        double maxLon = -10000;
        for (int i = 0; i < n; i++) {
            Location l = allLocs.get(i);
            inputPoint[i] = new double[] { l.lat, l.lon };
            minLon = minLon < l.lon ? minLon : l.lon;
            minLat = minLat < l.lat ? minLat : l.lat;
            maxLon = maxLon < l.lon ? l.lon : maxLon;
            maxLat = maxLat < l.lat ? l.lat : maxLat;
        }
        Random r = new Random();
        for (int i = 0; i < n; i++) {
            double x1 = minLat + r.nextDouble() * (maxLat - minLat);
            double y1 = minLon + r.nextDouble() * (maxLon - minLon);
            queryRectangle[i] = new double[] { x1, y1, x1 + r.nextDouble() * (maxLat - minLat) * 0.05,
                    y1 + r.nextDouble() * (maxLon - minLon) * 0.05 };
        }
        List<QuadTree.CoordHolder> res;
        /*
         * Create a tree with a dynamic leaf size that reflects the square root of the
         * size of the growing tree.
         */
        t1 = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < expNB; i++) {
            QuadTree qDynamic = new QuadTree();
            qDynamic.DYNAMIC_MAX_OBJECTS = true;
            qDynamic.MAX_OBJ_TARGET_EXPONENT = 0.5;
            for (int j = 0; j < n; j++) {
                qDynamic.place(inputPoint[j][0], inputPoint[j][1], new Location());
            }
            for (double[] query : queryRectangle) {
                res = qDynamic.findAll(query[0], query[1],
                        query[2],
                        query[3]);
            }
            qDynamic.getAllLeafs();
        }
        t2 = System.currentTimeMillis();
        System.out.println("Dynamic Index Time consume:" + (t2 - t1));
        // bruteful
        count = 0;
        for (int i = 0; i < expNB; i++) {
            for (double[] query : queryRectangle) {
                for (int j = 0; j < n; j++) {
                    double[] db = inputPoint[j];
                    if (db[0] >= query[0] && db[0] <= query[2] && db[1] >= query[1] && db[1] <= query[3]) {
                        count++;
                    }
                }
            }
        }
        t2 = System.currentTimeMillis();
        System.out.println(count);
        System.out.println("Bruteful Time consume:" + (t2 - t1));

    }

    public static void main(String[] args) {
        test();
    }
}
