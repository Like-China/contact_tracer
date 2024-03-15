package indexes;

/* Reference
* https://code.tutsplus.com/quick-tip-use-quadtrees-to-detect-likely-collisions-in-2d-space--gamedev-374t
* coding?gbk
If any question, see Quadtree for more reference, this code has deleted some comments
III  IV
II  I
*/
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import trace.Settings;

public class QueryQuadTree {
    // how many objects a node can hold before it splits, this is useless when
    // preSplit() is employed
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
    public QueryQuadTree[] nodes;
    public Distance D = new Distance();

    public static int totalCheckNB = 0;

    /*
     * Constructor
     */
    public QueryQuadTree(int pLevel, MyRectangle pBounds) {
        level = pLevel;
        objects = new ArrayList<>();
        bounds = pBounds;
        nodes = new QueryQuadTree[4];
        preSplit();
    }

    public void preSplit() {
        if (level >= MAX_LEVELS) {
            return;
        }

        if (this.nodes[0] == null) {
            this.split();
        }

        for (int i = 0; i < nodes.length; i++) {
            nodes[i].split();
        }
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
        nodes[0] = new QueryQuadTree(level + 1, new MyRectangle(-1, x + subWidth, y, subWidth, subHeight));
        nodes[1] = new QueryQuadTree(level + 1, new MyRectangle(-1, x, y, subWidth, subHeight));
        nodes[2] = new QueryQuadTree(level + 1, new MyRectangle(-1, x, y + subHeight, subWidth, subHeight));
        nodes[3] = new QueryQuadTree(level + 1,
                new MyRectangle(-1, x + subWidth, y + subHeight, subWidth, subHeight));
    }

    /*
     * Determine which node the rectangle object belongs to. -1 means object cannot
     * completely
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
     * Determine which node the location object belongs to. -1 means object cannot
     * completely
     * fit within a child node and is part of the parent node
     */
    public int getLocationIndex(double x, double y) {
        int index = -1;
        double verticalMidpoint = bounds.getX() + (bounds.getWidth() / 2);
        double horizontalMidpoint = bounds.getY() + (bounds.getHeight() / 2);
        // Object can completely fit within the top quadrants
        boolean topQuadrant = (y <= horizontalMidpoint);
        // Object can completely fit within the bottom quadrants
        boolean bottomQuadrant = (y > horizontalMidpoint);
        // Object can completely fit within the left quadrants
        if (x <= verticalMidpoint) {
            if (topQuadrant) {
                index = 1;
            } else if (bottomQuadrant) {
                index = 2;
            }
        }
        // Object can completely fit within the right quadrants
        else if (x > verticalMidpoint) {
            if (topQuadrant) {
                index = 0;
            } else if (bottomQuadrant) {
                index = 3;
            }
        }
        return index;
    }

    // iterative insertion
    public void insert(MyRectangle pRect) {
        QueryQuadTree current = this;
        while (true) {
            // reach leaf nodes
            if (current.nodes[0] == null) {
                current.objects.add(pRect);
                break;
            }
            // get index
            int index = current.getIndex(pRect);
            // cannot totally covered by any one sub-node, store it in current node
            if (index == -1) {
                current.objects.add(pRect);
                break;
            } else {
                // totally covered by one sub-node
                current = current.nodes[index];
            }
        }

    }

    /*
     * Return all objects that could collide with the given object
     * It returns all objects in all nodes that the given object could potentially
     * collide with. This method is what helps to reduce the number of pairs to
     * check collision against.
     */
    public void retrieveByLocation(HashSet<MyRectangle> returnObjects, double x, double y) {
        int index = getLocationIndex(x, y);
        // check if object is totally covered by the subnode
        if (index != -1 && nodes[0] != null) {
            nodes[index].retrieveByLocation(returnObjects, x, y);
        }
        // index != -1 && nodes[0] != null covered by non-leaf
        // index != -1 && nodes[0] == null exactly covered by leaf nodes
        // index == -1 && nodes[0] != null covered by current node but multple
        // intersections with four subnodes
        // index == -1 && nodes[0] == null dose not exist
        Iterator<MyRectangle> iterator = objects.iterator();
        while (iterator.hasNext()) {
            MyRectangle rec = iterator.next();
            if (rec.isCover(x, y)) {
                double dis = D.distance(y, x, rec.getY() + rec.getWidth() / 2,
                        rec.getX() + rec.getHeight() / 2);
                totalCheckNB += 1;
                if (dis <= Settings.epsilon) {
                    returnObjects.add(rec);
                    return;
                }
            }
        }
        // check if object overlaps the subnode, not within the subnode return., -1
        if (index == -1 && nodes[0] != null) {
            for (QueryQuadTree node : nodes) {
                if (node.bounds.isCover(x, y)) {
                    node.retrieveByLocation(returnObjects, x, y);
                }
            }
        }
    }

    /* Traveling the Graph using Depth First Search */
    public void dfs(ArrayList<MyRectangle> recList) {
        if (this == null || this.nodes[0] == null)
            return;
        System.out.println("\n*******Parent Node*******");
        System.out.printf("Level = %d [X1=%03.1f Y1=%03.1f] \t[X2=%03.1f Y2=%03.1f] -1 count = %d",
                this.level, this.bounds.getX(), this.bounds.getY(),
                this.bounds.getX() + this.bounds.getWidth(), this.bounds.getY() + this.bounds.getHeight(),
                this.objects.size());
        recList.addAll(this.objects);

        System.out.println("\nChild Node");
        for (QueryQuadTree node : this.nodes) {
            System.out.printf("Level = %d [X1=%06.5f Y1=%06.5f] \t[X2=%06.5f Y2=%06.5f] eleCount = %d\n",
                    node.level, node.bounds.getX(), node.bounds.getY(),
                    node.bounds.getX() + node.bounds.getWidth(), node.bounds.getY() + node.bounds.getHeight(),
                    node.objects.size());
            if (node.nodes[0] == null) {
                System.out.printf("\t  Leaf Node. \n");
            }
        }

        for (int i = 0; i < 4; i++) {
            if (this.nodes[i] != null)
                this.nodes[i].dfs(recList);
        }

    }

}
