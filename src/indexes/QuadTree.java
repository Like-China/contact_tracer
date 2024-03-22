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

public class QuadTree {
    // how many objects a node can hold before it splits, this is useless when
    // preSplit() is employed
    public int MAX_OBJECTS = 10;
    // the deepest level subnode
    public int MAX_LEVELS = 6;
    // the current node level (0 being the topmost node)
    public int level;
    // objects that cannot completely fit within a child node and is part of the
    // parent node
    public ArrayList<MyRectangle> objects = new ArrayList<>();
    // the 2D space that the node occupies MyRectangle(min_x, min_y, width, height)
    public MyRectangle bounds;
    // the four subnodes
    public QuadTree[] nodes;
    public Distance D = new Distance();

    public static int totalCheckNB = 0;

    public boolean isStatic;

    /*
     * Constructor
     */
    public QuadTree(int pLevel, MyRectangle pBounds, boolean isStatic) {
        level = pLevel;
        objects = new ArrayList<>();
        bounds = pBounds;
        nodes = new QuadTree[4];
        this.isStatic = isStatic;
        if (isStatic) {
            preSplit();
        }

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
        nodes[0] = new QuadTree(level + 1, new MyRectangle(-1, x + subWidth, y, subWidth, subHeight), isStatic);
        nodes[1] = new QuadTree(level + 1, new MyRectangle(-1, x, y, subWidth, subHeight), isStatic);
        nodes[2] = new QuadTree(level + 1, new MyRectangle(-1, x, y + subHeight, subWidth, subHeight), isStatic);
        nodes[3] = new QuadTree(level + 1,
                new MyRectangle(-1, x + subWidth, y + subHeight, subWidth, subHeight), isStatic);
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

    public void dynamicInsert(MyRectangle pRect) {
        // nodes[0] is not -1 iff nodes has been split
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
            // Reinsert objects into appropriate child nodes
            Iterator<MyRectangle> iterator = objects.iterator();
            while (iterator.hasNext()) {
                MyRectangle rect = iterator.next();
                int index = getIndex(rect);
                if (index != -1) {
                    nodes[index].insert(rect);
                    iterator.remove();
                }
            }

        }
    }

    // iterative insertion
    public void insert(MyRectangle pRect) {
        if (!this.isStatic) {
            dynamicInsert(pRect);
            return;
        }
        QuadTree current = this;
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
    public void retrieveByLocation(HashSet<MyRectangle> returnObjects, double x, double y, boolean isEarlyStop,
            double epsilon) {
        int index = getLocationIndex(x, y);
        // check if object is totally covered by the subnode
        if (index != -1 && nodes[0] != null) {
            nodes[index].retrieveByLocation(returnObjects, x, y, isEarlyStop, epsilon);
        }
        // index != -1 && nodes[0] != null covered by non-leaf
        // index != -1 && nodes[0] == null exactly covered by leaf nodes
        // index == -1 && nodes[0] != null covered by current node but multple
        // intersections with four subnodes
        // index == -1 && nodes[0] == null dose not exist
        // check if object overlaps the subnode, not within the subnode return., -1
        if (index == -1 && nodes[0] != null) {
            for (QuadTree node : nodes) {
                if (node.bounds.isCover(x, y)) {
                    node.retrieveByLocation(returnObjects, x, y, isEarlyStop, epsilon);
                }
            }
        }
        if (returnObjects.size() > 0) {
            return;
        }
        Iterator<MyRectangle> iterator = objects.iterator();
        while (iterator.hasNext()) {
            MyRectangle rec = iterator.next();
            if (rec.isCover(x, y)) {
                if (isEarlyStop) {
                    returnObjects.add(rec);
                    return;
                }
                double dis = D.distance(y, x, rec.getY() + rec.getWidth() / 2,
                        rec.getX() + rec.getHeight() / 2);
                totalCheckNB += 1;
                if (dis <= epsilon) {
                    returnObjects.add(rec);
                    return;
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
        for (QuadTree node : this.nodes) {
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
