/*
 * Converts a VCF to a batch script for generating IGV screenshots
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/*
 * Converts a list of SNPS and small indels to a directory of IGV screenshots
 */
public class Vcf2Bat
{
	// Input file names
	static String genomeFn, bamFn, vcfFn;
	
	// The prefix of the output directory
	static String outPrefix = "igv";
	
	// Number of padding bases on each side of the variants to include
	static int padding = 50;
	
	// Whether or not to combine nearby variants into a single screenshot
	static boolean combineNearby = true;
	
	// Whether or not to squish the view before snapshotting
	static boolean squish = false;
	
	/*
	 * Prints out usage instructions
	 */
	static void usage()
	{
		System.out.println("Usage: java -cp src Vcf2Bat [args]");
		System.out.println("  Example: java -cp src Vcf2Bat aln=jhu004.bam var=snps.vcf genome=ref.fasta");
		System.out.println();
		System.out.println("Required args:");
		System.out.println("  aln    (String) - a BAM file with the read alignments");
		System.out.println("  var    (String) - a VCF file with the variants to visualize");
		System.out.println("  genome (String) - a FASTA file with the reference genome");

		System.out.println();
		System.out.println("Optional args:");
		System.out.println("  padding      (int)    [50]     - the number of bases on each side of the variant to include");
		System.out.println("  outprefix (String)    [igv]    - the name of the directory to put screenshots into");
		System.out.println("  --nocombine                    - don't combine nearby variants into single screenshots");
		System.out.println("  --squish                       - squish the screenshots to capture more reads");

		System.out.println();
	}
	
	/*
	 * Parses command line arguments
	 */
	static void parseArgs(String[] args)
	{
		for(String str : args)
		{
			String s = str;
			int equalsIdx = s.indexOf('=');
			if(equalsIdx == -1)
			{
				s = s.toLowerCase();
				if(s.endsWith("nocombine"))
				{
					combineNearby = false;
				}
				if(s.endsWith("squish"))
				{
					squish = true;
				}
			}
			else
			{
				String key = s.substring(0, equalsIdx).toLowerCase();
				String val = s.substring(1 + equalsIdx);
				
				if(key.equals("aln"))
				{
					bamFn = val;
				}
				else if(key.equals("var"))
				{
					vcfFn = val;
				}
				else if(key.equals("genome"))
				{
					genomeFn = val;
				}
				else if(key.equals("padding"))
				{
					padding = Integer.parseInt(val);
				}
				else if(key.equals("outprefix"))
				{
					outPrefix = val;
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		if(args.length == 0 || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help"))
		{
			usage();
			System.exit(1);
		}
		parseArgs(args);
		if(genomeFn == null || !new File(genomeFn).exists())
		{
			System.out.println("Invalid genome file:" + genomeFn);
			System.exit(1);
		}
		if(bamFn == null || !new File(bamFn).exists())
		{
			System.out.println("Invalid alignments file:" + bamFn);
			System.exit(1);
		}
		if(!new File(bamFn+".bai").exists())
		{
			System.out.println("Bam file index missing: " + bamFn + ".bai");
			System.exit(1);
		}
		if(vcfFn == null || !new File(vcfFn).exists())
		{
			System.out.println("Invalid variants file:" + vcfFn);
			System.exit(1);
		}
		
		// Create new directory if not there already
		Path currentRelativePath = Paths.get("");
		String outDir = currentRelativePath.toAbsolutePath().toString() + "/" + outPrefix;
		File f = new File(outDir);
		if(f.isDirectory())
		{
			String[] entries = f.list();
			for(String s: entries){
			    File currentFile = new File(f.getPath(),s);
			    currentFile.delete();
			}
		}
		if(f.exists())
		{
			f.delete();
		}
		f.mkdir();
		
		// Start writing batch file for IGV
		String ofn = outPrefix + ".bat";
		PrintWriter out = new PrintWriter(new File(ofn));
		out.println("new");
		out.println("genome " + (genomeFn.startsWith("/") ? 
				genomeFn : (currentRelativePath.toAbsolutePath().toString() + "/" + genomeFn)));
		out.println("load " + (bamFn.startsWith("/") ? 
				bamFn : (currentRelativePath.toAbsolutePath().toString() + "/" + bamFn)));
		out.println("snapshotDirectory " + outDir);
		
		ArrayList<Variant> vars = new ArrayList<Variant>();
		Scanner input = new Scanner(new FileInputStream(new File(vcfFn)));
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.startsWith("#"))
			{
				continue;
			}
			String[] tokens = line.split("\t");
			String chr = tokens[0];
			int pos = Integer.parseInt(tokens[1]);
			int refLen = tokens[3].length();
			vars.add(new Variant(chr, pos, refLen));
		}
		input.close();
		
		if(combineNearby)
		{
			vars = combine(vars);
		}
		
		for(Variant v : vars)
		{
			int[] window = v.window();
			out.println("goto " + v.chr + ":" + window[0] + "-" + window[1]);
			out.println("sort position");
			out.println("collapse");
			
			if(squish)
			{
				out.println("squish");
			}
			
			out.println("snapshot " + v.getId() + ".png");
		}
		out.println("exit");
		
		out.close();
	}
	
	static ArrayList<Variant> combine(ArrayList<Variant> vars)
	{
		ArrayList<Variant> res = new ArrayList<Variant>();
		int n = vars.size();
		for(int i = 0; i<n; i++)
		{
			int j = i;
			Variant cur = vars.get(i);
			while(j < n - 1)
			{
				Variant next = vars.get(j+1);
				if(cur.overlaps(next))
				{
					cur = cur.combine(next);
					j++;
				}
				else
				{
					break;
				}
			}
			res.add(cur);
			i = j;
		}
		return res;
	}
	static class Variant implements Comparable<Variant>
	{
		String chr;
		int pos, refLen;
		ArrayList<Integer> positions;
		Variant(String chr, int pos, int refLen)
		{
			this.chr = chr;
			this.pos = pos;
			this.refLen = refLen;
			positions = new ArrayList<Integer>();
			positions.add(pos);
		}
		public int compareTo(Variant o)
		{
			if(!chr.equals(o.chr))
			{
				return chr.compareTo(o.chr);
			}
			return pos - o.pos;
		}
		int[] window()
		{
			return new int[] { Math.max(1, pos - padding), pos + refLen + padding};
		}	
		boolean overlaps(Variant v)
		{
			if(!v.chr.equals(chr)) return false;
			int[] myWindow = window(), theirWindow = v.window();
			if(myWindow[1] < theirWindow[0]) return false;
			if(myWindow[0] > theirWindow[1]) return false;
			return true;
		}
		Variant combine(Variant v)
		{
			int minPos = Math.min(pos, v.pos);
			int maxEnd = Math.max(pos + refLen,  v.pos + v.refLen);
			Variant res = new Variant(chr, minPos, maxEnd - minPos);
			res.positions = new ArrayList<Integer>();
			for(int x : positions) res.positions.add(x);
			for(int x : v.positions) res.positions.add(x);
			Collections.sort(res.positions);
			return res;
		}
		String getId()
		{
			StringBuilder res = new StringBuilder(chr);
			for(int x : positions) res.append("_" + x);
			return res.toString();
		}
	}
}