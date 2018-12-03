import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String [] codings = new String [ALPH_SIZE+1];
				makeCodingsFromTree(root, "", codings);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset(); //reset will take pointer and put it back at front of BitInputStream
		writeCompressedBits(codings, in, out);
		out.close();
	}
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	private int[] readForCounts(BitInputStream in) { //using an array because ASCII values only go up to 256
		int [] counts = new int [ALPH_SIZE+1]; //ascii has 258 values
		while(true) {
			int val = in.readBits(BITS_PER_WORD); //returns the ascii number
			if(val==-1) {
				counts[PSEUDO_EOF]=1; //1 is the minimum value that something can appear so should be furthest right
				break;
			}
			counts[val]+=1;
			
		}
		return counts;
	}
	
	private HuffNode makeTreeFromCounts(int[] counts) { //Greedy algorithm consider input given, and without con
		PriorityQueue<HuffNode> pq = new PriorityQueue<>(); //as you add it will keep it sorted according to huffnode comparable
        
		for(int i=0; i<counts.length; i++) {
			if(counts[i]<=0) {
				continue; //just goes to next one
			}
		    pq.add(new HuffNode(i,counts[i],null,null));
		}
        pq.add(new HuffNode(PSEUDO_EOF, 0));
        
		while (pq.size() > 1) { //now top of priorityqueue has most frequency (max frequency at the top)
		    HuffNode left = pq.remove(); //want maximum to be to the left they said it
		    HuffNode right = pq.remove();// create new HuffNode t with weight from
		    HuffNode t = new HuffNode(-1, left.myWeight+right.myWeight, left, right ); //you don't want it havign the character yet because ones in the middle
		    		                        //don't care about the leaves with the values, we are compressing
		    
		    pq.add(t);                          // left.weight+right.weight and left, right subtrees
		   
		} //each time pq runs you are compressing the tree and adding the smaller weights together
		return pq.remove();	 //root codes contains all the other nodes                                               //considering other input takes what it wants as opposed to scanning array
		//pq knows how to sort it because of comparator I gave HuffNode
	}
	
	
	private void makeCodingsFromTree(HuffNode root, String currentPath, String [] codes) { //characters stored in leafs so want path root to leaf
		if(root.myLeft==null && root.myRight==null) {
			codes[root.myValue]=currentPath; //add current path to ascii value index in major codes array
			return;
		}
	
    makeCodingsFromTree(root.myLeft,currentPath+'0', codes); //recurse left and add 0 to path
    makeCodingsFromTree( root.myRight, currentPath+'1',  codes);
}
	//trie is just a tree with more than 2 children
	
	private void writeHeader(HuffNode root, BitOutputStream out) {
	 if(root.myLeft==null && root.myRight==null) {
		 out.writeBits(1,1);
		 out.writeBits(BITS_PER_WORD+1, root.myValue);
		 return; //it's a lead it's the end
	 }
	 else {
		 out.writeBits(1,0);
		 writeHeader(root.myLeft, out );
		 writeHeader(root.myRight, out); 
	 }
	}

	private void writeCompressedBits(String [] codings, BitInputStream in, BitOutputStream out) {
		while(true) {//goes on forever, so will need something to eventually break it
			int val = in.readBits(BITS_PER_WORD); //reaches a character
			if(val == -1) { //you still want to write it out, and then break after that
				String code = codings[PSEUDO_EOF];
				out.writeBits(code.length(),Integer.parseInt(code, 2) );
				break;
			}
			String code = codings[val]; //before you do this you already found codes to each character form tree, val in int prints out the ascii code, if put char would print character
			out.writeBits(code.length(), Integer.parseInt(code, 2)); // code is a String, and Java defined parseInt will turn it into binary numbers (i.e.g the '2' means binary). 

		}
	}
	
	public void decompress(BitInputStream in, BitOutputStream out){

		
			int val = in.readBits(BITS_PER_INT);
			//if(val != HUFF_TREE) {
				//throw new HuffException("Illegal header starts with "+ val);
			//}
			
			HuffNode root = readTreeHeader(in);
			readCompressedBits(root,in,out);		
		out.close();
		
		}
	private HuffNode readTreeHeader(BitInputStream in) { //to read a single bit is in.readBits(number of bitsyou want to read)
		int value = in.readBits(1);
		if(value== -1) {
			throw new HuffException("Reading bits failed");
		}
		if(value==0) { //not actually making a tree yet
			HuffNode left = readTreeHeader(in); //bit input stream is like a scanner, pointer just goes to the enxt one
		    HuffNode right = readTreeHeader(in); //don't need in.left (there's no real left or right, 
			return new HuffNode(0,0, left, right);
		}
		else {
			int value2= in.readBits(BITS_PER_WORD+1);
			return new HuffNode(value2, 0, null, null);
		}
	}
		
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode current = root; 
		   while (true) {
		       int bits = in.readBits(1);
		       if (bits == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       }
		       else { 
		           if (bits == 0) current = current.myLeft;
		      else current = current.myRight;

		           if (current.myLeft==null && current.myRight== null) {
		               if (current.myValue == PSEUDO_EOF) 
		                   break;   // out of loop
		               else { //.writeBits is java
		                   out.writeBits(BITS_PER_WORD, current.myValue); //to write bits, takes character in current myValue and writes it to 8 BITS (bits per word0
		                   current = root; // start back after leaf
		               }
		           }
		       }
		   }
	}
				/**int val = in.readBits(BITS_PER_WORD);
				if (val == -1) break;
				out.writeBits(BITS_PER_WORD, val);**/
			}
	
