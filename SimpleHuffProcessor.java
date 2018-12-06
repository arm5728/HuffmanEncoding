/*  Student information for assignment:
 *
 *  On OUR honor, Adrian Melendez Relli and Ziyi Liew this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 0 
 *
 *  Student 1 (Student whose turnin account is being used)
 *  UTEID: arm5728
 *  email address: adrianmelendezrelli@gmail.com
 *  Grader name: ETHAN
 *
 *  Student 2
 *  UTEID: zl7279
 *  email address: zliew@utexas.edu
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

public class SimpleHuffProcessor implements IHuffProcessor {
    private IHuffViewer myViewer;
    private int bitCounter;
    private int originalCounter;
    private final static int FUCK = 3;
    private TreeMap<Integer, String> codes;
    private int[] values;
    private int type;
    private HuffTree tree;

    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
		BitInputStream input = new BitInputStream(in);
    	BitOutputStream output = new BitOutputStream(out);
    	
    	int counter = 0;
    	
    	//1. Magic Number constant
    	output.writeBits(BITS_PER_INT, MAGIC_NUMBER);
    	counter += BITS_PER_INT;
    	
    	
    	//2 & 3. Type and Header
    	if (type == STORE_COUNTS) {
    		counter += countFormatCompress(input, output, counter);
    	} else if (type == STORE_TREE){
    		counter += treeFormatCompress(input, output, counter);
    	}
    	
    	
    	//4.Data
    	int bit = input.readBits(BITS_PER_WORD);
    	while (bit != -1) {
    		String data = codes.get(bit);
    		for (int letter = 0; letter < data.length(); letter++) {
    			int toPut = -1;
    			if (data.charAt(letter) == '0'){
    				toPut = 0;
    			} else {
    				toPut = 1;
    			}
    			output.writeBits(1, toPut);
    			counter += 1;
    		}				
    		bit = input.readBits(BITS_PER_WORD);
    	}	
    	
    	
    	//5. PSEUDO EOF
		String data = codes.get(PSEUDO_EOF);
		for (int letter = 0; letter < data.length(); letter++) {
			int toPut = -1;
			if (data.charAt(letter) == '0'){
				toPut = 0;
			} else {
				toPut = 1;
			}
			output.writeBits(1, toPut);
			counter += 1;
		}  	

    	input.close();
    	output.close();
    	
    	
    	return counter;
    }
    
    private int countFormatCompress (BitInputStream input, BitOutputStream output, int counter) throws IOException {
    	//Store counts or Store Tree
    	output.writeBits(BITS_PER_INT, STORE_COUNTS);
    	counter += BITS_PER_INT;
    		
    	//Header data
    	for (int index = 0; index < ALPH_SIZE; index++) {
    		output.writeBits(BITS_PER_INT, values[index]);
        	counter += BITS_PER_INT;
    	}
    
    	return counter;
    }
    
    private int treeFormatCompress (BitInputStream input, BitOutputStream output, int counter) throws IOException {
    	//Add type constant
    	output.writeBits(BITS_PER_INT, STORE_TREE);
    	counter += BITS_PER_INT;
    	
    	//Size Value
    	int bitsInHeader = (tree.getNumLeafs() * (BITS_PER_WORD + 2)) + (tree.getNumInternalNodes());
    	output.writeBits(BITS_PER_INT, bitsInHeader);	
    	counter += BITS_PER_INT;
    	counter += bitsInHeader;
    	
    	//Data
    	TreeNode node = tree.getRoot();
    	treeRecurse(output, node);
    	
    	return counter;
    }

    private void treeRecurse (BitOutputStream output, TreeNode node) {
    	if (node.isLeaf()) {
    		output.writeBits(1, 1);
    		output.writeBits(BITS_PER_WORD, node.getValue());
    	} else {
    		output.writeBits(1, 0);
    		if (node.getLeft() != null) {
    			treeRecurse(output, node.getLeft());    		
    		}
    		if (node.getRight() != null) {
    			treeRecurse(output, node.getRight());
    		}
    	}
    }
    
	public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
		//Set format
		type = headerFormat;
		
		//Initialize input streamhowManyBits
		BitInputStream input = new BitInputStream(in);
		
		//Initialize, but account for the magic number (start at 32)
		bitCounter = BITS_PER_INT * 2;
		originalCounter = 0;
		
		// get each value, input into list
		ArrayList<TreeNode> toConvert = inputVals(input);
		tree = new HuffTree();
		
		// add all the nodes into tree
		for (TreeNode current : toConvert) {
			tree.add(current);
		}
		
		// make the HuffTree
		tree.makeTree();
		tree.makeMap();
		codes = tree.getCodes();
    	Set<Integer> keys = codes.keySet();
		for (Integer key: keys) {
			bitCounter += codes.get(key).length();
		}

    	return originalCounter - bitCounter;   
    }

    private ArrayList<TreeNode> inputVals(BitInputStream input) throws IOException {
    	int bit = input.readBits(BITS_PER_WORD);
    	values = new int [ALPH_SIZE + 1];
    	ArrayList<TreeNode> result = new ArrayList<TreeNode>();
    	
    	//While there is data to read
    	while (bit != -1) {
    		//Increment the frequency by adding one to the storage at index
    		bitCounter += BITS_PER_WORD;
    		originalCounter += BITS_PER_WORD;
    		values[bit] += 1;
    		
    		//Move to next bit
    		bit = input.readBits(BITS_PER_WORD);
    	}

    	//Assign a frequency of 1 to index 256, add it to the counter
    	values[PSEUDO_EOF] = 1;
    	bitCounter += (BITS_PER_WORD - FUCK);
    	
    	//Loop through values
    	for (int val = 0; val < values.length; val++) {
    		//Store frequency of this value
    		int freq = values[val];
    		//If its not 0, create a new TreeNode and add it to the result
    		if (freq != 0)
    			result.add(new TreeNode(val,freq)); 
    	} 	

    	return result;
    }

  
    public int uncompress(InputStream in, OutputStream out) throws IOException {
		//Open Streams
    	BitInputStream input = new BitInputStream(in);
		BitOutputStream output = new BitOutputStream(out);
		
		//Store Frequencies
		int [] frequencies = new int [ALPH_SIZE + 1];
		
		//Item #1: Magic Number
		int magic = input.readBits(BITS_PER_INT);
		if (magic != MAGIC_NUMBER) {
		    throw new IOException("File did not start with the huff magic number.");	   
		} 
		
		//Item #2 and #3: STC or STF
		int constant = input.readBits(BITS_PER_INT);
		
		if (constant == STORE_COUNTS) {
			countFormat(frequencies, input, output);
		} else if (constant == STORE_TREE){
			treeFormat(frequencies, input, output);
		}
			 
		
		
		//Items #4 and #5, Data
		TreeNode node = tree.getRoot();
		boolean done = false;
		while(!done) {
			int data = input.readBits(1);
	        if (data == -1) {
	    	    throw new IOException("error reading bits, no PSEUDO-EOF");
	        }
			if (data == 0) {
				node = node.getLeft();	
			} else {
				node = node.getRight();
			}
			if (node.isLeaf()) {
				if (node.getValue() == PSEUDO_EOF) {
					done = true;
				} else {
					output.writeBits(BITS_PER_WORD, node.getValue());
					node = tree.getRoot();
				}
			}
		}
		
		//Close Streams
		output.close();
		input.close();
		return 0;
    }
    
    private void countFormat (int [] frequencies, BitInputStream input, BitOutputStream output) throws IOException {
		//get frequencies back
		for (int index = 0; index < frequencies.length -1; index++) {
			int value = input.readBits(BITS_PER_INT);
			frequencies[index] = value;
		}
		frequencies[ALPH_SIZE] = 1;

		//reconstruct the tree
		ArrayList<TreeNode> newTree = new ArrayList<TreeNode>();
    	for (int frequency = 0; frequency < frequencies.length; frequency++) {
    		//Store frequency of this value
    		int freq = frequencies[frequency];
    		//If its not 0, create a new TreeNode and add it to the result
    		if (freq != 0)
    			newTree.add(new TreeNode(frequency,freq)); 
    	} 	
    	
    	HuffTree tree = new HuffTree();
		
		// add all the nodes into tree
		for (TreeNode current : newTree) {
			tree.add(current);
		}
		tree.makeTree();
		tree.makeMap();
		//Decode the data	
		codes = tree.getCodes();

    }
    
    private void treeFormat (int [] frequencies, BitInputStream input, BitOutputStream output) throws IOException { 
    	int sizeOfHeaderData = input.readBits(BITS_PER_INT);
    	 
    }
	
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s){
        if(myViewer != null)
            myViewer.update(s);
    }
}