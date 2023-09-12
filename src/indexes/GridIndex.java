package indexes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import data_loader.Location;

public class GridIndex {

	public double scale;
	public float[] lonRange;
	public float[] latRange;
	public int xmax;
	public int ymax;
	public int areaNum;
	public HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations = new HashMap<Integer, ArrayList<Location>>();
	public HashMap<Integer, ArrayList<Location>> patientAreasLocations = new HashMap<Integer, ArrayList<Location>>();
	// MBR: minLon, maxLon, minLat, maxLat
	public HashMap<Integer, Float[]> ordinaryAreasMBR = new HashMap<>();
	public HashMap<Integer, Float[]> patientAreasMBR = new HashMap<>();

	public HashMap<Integer, ArrayList<Location>> getPatientAreasLocations() {
		return patientAreasLocations;
	}

	public void setPatientAreasLocations(HashMap<Integer, ArrayList<Location>> patientAreasLocations) {
		this.patientAreasLocations = patientAreasLocations;
	}

	public HashMap<Integer, ArrayList<Location>> getOrdinaryAreasLocations() {
		return ordinaryAreasLocations;
	}

	public void setOrdinaryAreasLocations(HashMap<Integer, ArrayList<Location>> ordinaryAreasLocations) {
		this.ordinaryAreasLocations = ordinaryAreasLocations;
	}

	public GridIndex(double scale, String cityname) {
		this.scale = scale;
		if (cityname == "beijing") {
			lonRange = new float[] { 116.25f - 0.001f, 116.55f + 0.001f };
			latRange = new float[] { 39.83f - 0.001f, 40.03f + 0.001f };
		} else {
			lonRange = new float[] { -8.735f - 0.0015f, -8.156f + 0.0015f };
			latRange = new float[] { 40.953f - 0.0015f, 41.307f + 0.0015f };
		}
		this.xmax = getX(lonRange[1]);
		this.ymax = getY(latRange[1]);
		areaNum = this.xmax * this.ymax - 1;

	}

	public int getX(float lon) {
		if (lon < lonRange[0] || lon > lonRange[1]) {
			System.out.println("X is out of range");
			return -1;
		}
		return (int) ((lon - lonRange[0]) / scale);
	}

	public int getY(float lat) {
		if (lat < latRange[0] || lat > latRange[1]) {
			System.out.println("Y is out of range");
			return -1;
		}
		return (int) ((lat - latRange[0]) / scale);
	}

	public int getID(float lon, float lat) {
		int x = getX(lon);
		int y = getY(lat);
		return y * xmax + x;
	}

	public int id2x(int id) {
		return id % xmax;
	}

	public int id2y(int id) {
		return id / xmax;
	}

	// get influenced areas by infected area, including infeteced area itself
	public int[] getAffectAreas(int areaID) {

		int x = areaID % xmax;
		int y = areaID / xmax;
		int[] nn = new int[25];
		Arrays.fill(nn, -1);
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				if ((x - 2 + i) <= xmax && (x - 2 + i) >= 0 && (y - 2 + j) <= ymax && (y - 2 + j) >= 0) {
					nn[5 * i + j] = (y - 2 + j) * xmax + (x - 2 + i);
				}
			}
		}
		nn[0] = -1;
		nn[4] = -1;
		nn[20] = -1;
		nn[24] = -1;
		// nn[12] = -1; regard infected area as a case of influenced areas
		return nn;
	}

}
