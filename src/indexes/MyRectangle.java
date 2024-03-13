package indexes;

import data_loader.Location;

public class MyRectangle {
    private double x;
    private double y;
    private double width;
    private double height;
    public int objID;
    public Location loc;

    public MyRectangle(Location loc, double x, double y, double width, double height) {
        this.loc = loc;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public boolean intersects(MyRectangle other) {
        // if (this.x < other.x + other.width && this.x + this.width > other.x &&
        // this.y < other.y + other.height && this.y + this.height > other.y) {
        // return true;
        // }
        // if (other.x < this.x + this.width && other.x + other.width > this.x &&
        // other.y < this.y + this.height && other.y + other.height > this.y) {
        // return true;
        // }
        // if (this.x < other.x + other.width && this.x + this.width > other.x &&
        // this.y < other.y + other.height && this.y + this.height > other.y) {
        // return true;
        // }
        boolean xIntersect = !(this.x + this.width < other.x || this.x > other.x + other.width);
        boolean yIntersect = !(this.y + this.height < other.y || this.y > other.y + other.height);
        return xIntersect && yIntersect;
    }

    // You can add more methods here as needed
}
