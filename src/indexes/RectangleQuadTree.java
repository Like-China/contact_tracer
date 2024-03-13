package indexes;

/* Reference
* https://code.tutsplus.com/quick-tip-use-quadtrees-to-detect-likely-collisions-in-2d-space--gamedev-374t
* coding£ºgbk
If any question, see Quadtree for more reference, this code has deleted some comments
III  IV
II  I
*/
import java.util.ArrayList;
import java.util.HashSet;

public class RectangleQuadTree {
    // how many objects a node can hold before it splits
    public int MAX_OBJECTS = 10;
    // the deepest level subnode
    public int MAX_LEVELS = 5;
    // the current node level (0 being the topmost node)
    public int level;
    // objects that cannot completely fit within a child node and is part of the
    // parent node
    public ArrayList<MyRectangle> objects = new ArrayList<>();
    // the 2D space that the node occupies MyRectangle(min_x, min_y, width, height)
    public MyRectangle bounds;
    // the four subnodes
    public RectangleQuadTree[] nodes;

    /*
     * Constructor
     */
    public RectangleQuadTree(int pLevel, MyRectangle pBounds) {
        level = pLevel;
        objects = new ArrayList<>();
        bounds = pBounds;
        nodes = new RectangleQuadTree[4];
    }

    /*
     * Clears the quadtree by recursively clearing all objects from all nodes
     */
    public void clear() {
        objects.clear();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].clear();
                nodes[i] = null;
            }
        }
    }

    /*
     * Splits the node into 4 subnodes
     * The split method splits the node into four subnodes by dividing the node into
     * four equal parts and initializing the four subnodes with the new bounds.
     */
    public void split() {
        double subWidth = bounds.getWidth() / 2;
        double subHeight = bounds.getHeight() / 2;
        double x = bounds.getX();
        double y = bounds.getY();
        nodes[0] = new RectangleQuadTree(level + 1, new MyRectangle(null, x + subWidth, y, subWidth, subHeight));
        nodes[1] = new RectangleQuadTree(level + 1, new MyRectangle(null, x, y, subWidth, subHeight));
        nodes[2] = new RectangleQuadTree(level + 1, new MyRectangle(null, x, y + subHeight, subWidth, subHeight));
        nodes[3] = new RectangleQuadTree(level + 1,
                new MyRectangle(null, x + subWidth, y + subHeight, subWidth, subHeight));
    }

    /*
     * Determine which node the object belongs to. -1 means object cannot completely
     * fit within a child node and is part of the parent node
     */
    public int getIndex(MyRectangle pRect) {
        int index = -1;
        double verticalMidpoint = bounds.getX() + (bounds.getWidth() / 2);
        double horizontalMidpoint = bounds.getY() + (bounds.getHeight() / 2);
        // Object can completely fit within the top quadrants
        boolean topQuadrant = (pRect.getY() < horizontalMidpoint
                && pRect.getY() + pRect.getHeight() < horizontalMidpoint);
        // Object can completely fit within the bottom quadrants
        boolean bottomQuadrant = (pRect.getY() > horizontalMidpoint);
        // Object can completely fit within the left quadrants
        if (pRect.getX() < verticalMidpoint && pRect.getX() + pRect.getWidth() < verticalMidpoint) {
            if (topQuadrant) {
                index = 1;
            } else if (bottomQuadrant) {
                index = 2;
            }
        }
        // Object can completely fit within the right quadrants
        else if (pRect.getX() > verticalMidpoint) {
            if (topQuadrant) {
                index = 0;
            } else if (bottomQuadrant) {
                index = 3;
            }
        }
        return index;
    }

    /*
     * Insert the object into the quadtree. If the node exceeds the capacity, it
     * will split and add all objects to their corresponding nodes.
     */
    public void insert(MyRectangle pRect) {
        // nodes[0] is not null iff nodes has been split
        if (nodes[0] != null) {
            int index = getIndex(pRect);
            if (index != -1) {
                nodes[index].insert(pRect);
                return;
            }
        }
        // if object cannot completely fit within a child node and is part of the parent
        // node, store it in the parent node
        objects.add(pRect);
        // When reaching the max capacity, split and assign each recrangle into
        // corresponding subnode
        if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
            if (nodes[0] == null) {
                split();
            }
            int i = 0;

            while (i < objects.size()) {
                int index = getIndex(objects.get(i));
                if (index != -1) {
                    nodes[index].insert(objects.remove(i));
                }
                i++;
            }
        }
    }

    /*
     * Return all objects that could collide with the given object
     * It returns all objects in all nodes that the given object could potentially
     * collide with. This method is what helps to reduce the number of pairs to
     * check collision against.
     */
    public void retrieve(HashSet<MyRectangle> returnObjects, MyRectangle pRect) {
        int index = getIndex(pRect);
        // check if object is totally covered by the subnode
        if (index != -1 && nodes[0] != null) {
            nodes[index].retrieve(returnObjects, pRect);
        }
        // modified (2024/3/13)
        for (MyRectangle rec : objects) {
            if (rec.intersects(pRect)) {
                returnObjects.add(rec);
            }
        }
        // returnObjects.addAll(objects);
        // check if object overlaps the subnode, not within the subnode£¨i.e., -1
        // status)
        if (index == -1 && nodes[0] != null) {
            for (RectangleQuadTree node : nodes) {
                if (node.bounds.intersects(pRect)) {
                    node.retrieve(returnObjects, pRect);
                }
            }
        }
    }

    /* Traveling the Graph using Depth First Search */
    public void dfs() {
        if (this == null || this.nodes[0] == null)
            return;
        System.out.println("\n*******Parent Node*******");
        System.out.printf("Level = %d [X1=%03.1f Y1=%03.1f] \t[X2=%03.1f Y2=%03.1f] -1 count = %d",
                this.level, this.bounds.getX(), this.bounds.getY(),
                this.bounds.getX() + this.bounds.getWidth(), this.bounds.getY() + this.bounds.getHeight(),
                this.objects.size());

        System.out.println("\nChild Node");
        for (RectangleQuadTree node : this.nodes) {
            System.out.printf("Level = %d [X1=%03.1f Y1=%03.1f] \t[X2=%03.1f Y2=%03.1f] eleCount = %d\n",
                    node.level, node.bounds.getX(), node.bounds.getY(),
                    node.bounds.getX() + node.bounds.getWidth(), node.bounds.getY() + node.bounds.getHeight(),
                    node.objects.size());
            if (node.nodes[0] == null) {
                System.out.printf("\t  Leaf Node. \n");
            }
        }

        for (int i = 0; i < 4; i++) {
            if (this.nodes[i] != null)
                this.nodes[i].dfs();
        }

    }

}
