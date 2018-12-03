
public class decompress {

	public void decompress(BitInputStream in, BitOutputStream out) {
		int bits= in.readBits(BITS_PER_INT);

		if(bits != HUFF_TREE) {
			throw new HuffException("Illegal header starts with "+ bits);
		}
		HuffNode root= readTreeHeader(in);
		readCompressedBits(root,in,out);
		out.close();
	}
	public void readTreeHeader(TreeNode inRoot) {
		int value = in.readBits(inRoot);
		if(value== -1) {
			throw new HuffException("Reading bits failed");
		}
		if(value==0) {
			TreeNode left = readTreeHeader(inRoot.left);
			TreeNode right = readTreeHeader(inRoot.right);
			return new HuffNode(0,0, left, right);
		}
				int val = in.readBits(BITS_PER_WORD);
				if (val == -1) break;
				out.writeBits(BITS_PER_WORD, val);
			}
		}	
		
	}
	public void readCompressedBits(TreeNode root, BitInputStream in, BitOutputStream out) {
		if(bits== -1) {
			throw new HuffException("Reading bits failed");
		}
	}
/** ANALYSIS:
 * Why did you implement decompress first?
 To verify that it works before implementing compress first.
  
What is the purpose of PSEUDO_EOF?

To easily identify when we have finished going throuhg the inputStream, otherwise we'd just go get to null. It's 
just easier to identify the end. 

How can a compressed have more bits than the file being compressed? When does this happen?

If you have a huge tree with a thousand root nodes then you could have 00000000000001 for an 8 bit character word. 
So if the Height of the tree is more than 8, then it's bigger. Recurse from top to bottom to find the height,
if there is little repetition, if there are a ton of characters that only show up once or twice then it is very
inefficient. 

What compresses more: image file or text files, why do you think this happens?
There's only 256 possible text values, but with images a colour a pixel is determined by RGB and can be R(0-255), G(0-255), B(0-251). 
Possible text values are more possible than the pixel, so the text file compressed more. 
**/