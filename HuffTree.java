import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class HuffTree {

	private List<TreeNode> queue;
	private TreeMap<Integer, String> codes;
	private int numLeafs;
	private int numInternalNodes;
	
	public HuffTree() {
		queue = new LinkedList<>();
		numLeafs = 0;
		numInternalNodes = 0;
	}
	
	/**
	 * add method that inserts the TreeNode into the priority queue
	 * @param insert
	 * @return
	 */
	public boolean add(TreeNode insert) {
		// check if tree is empty
		if (queue.isEmpty()) {
			// add current to queue
			queue.add(insert);
			return true;
		}
		
		// find place to insert
		int cntr = 0;
		int insertFreq = insert.getFrequency();
		
		while (cntr < queue.size()) {
			// get the frequency of the current element in queue
			int currentFreq = queue.get(cntr).getFrequency();
			
			if (insertFreq < currentFreq) {
				// insert has smaller frequency, insert in front of this node
				queue.add(cntr, insert);
				return true;
			}
			
			cntr++;
		}
		
		// larger than all the elements, insert at end
		return queue.add(insert);
	}
	
	
	private static int COMPLETE_TREE = 1;
	private static int INTERNAL_NODE = -1;
	
	public void makeTree() {
		numLeafs = queue.size();
		// check if only one node
		while (queue.size() > COMPLETE_TREE) {

			// get the left and right to form new tree, remove from list
			TreeNode left = queue.remove(0);
			TreeNode right = queue.remove(0);

			// create new tree to insert back into tree
			TreeNode insert = new TreeNode(left, INTERNAL_NODE, right);
			numInternalNodes++;

			// add back into tree
			add(insert);
		}
	}
	
	public void makeMap() {
		// create map to store the codes
		codes = new TreeMap<>(); //initialize codes	
		makeMapHelp(codes, queue.get(0), "");
	}
	
	private void makeMapHelp(TreeMap<Integer, String> codes, TreeNode current, String path) {
		// check if current is a leaf
		if (current.isLeaf()) {
			codes.put(current.getValue(), path);
		} else {
			if (current.getLeft() != null) {
				String temp = path + "0"; //TODO
				makeMapHelp(codes, current.getLeft(), temp);
			}
			if (current.getRight() != null) {
				String temp = path + "1";
				makeMapHelp(codes, current.getRight(), temp);
			}
		}
		
	}
	
	public TreeNode getRoot () {
		return queue.get(0);
	}
	
	public TreeMap<Integer, String> getCodes() {
		return codes;
	}
	
	public int getNumLeafs() {
		return numLeafs;
	}
	
	public int getNumInternalNodes() {
		return numInternalNodes;
	}
	
}
