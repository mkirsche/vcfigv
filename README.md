# vcfigv

vcfigv allows the automated production of an IGV batch file from a VCF of SNPs and small indels, which enables the creation of screenshots surrounding each of the variants in the file.

## Compilation

``javac src/*.java``


## Running

```
Usage: java -cp src Vcf2Bat [args]
  Example: java -cp src Vcf2Bat aln=jhu004.bam var=snps.vcf genome=ref.fasta

Required args:
  aln    (String) - a BAM file with the read alignments
  var    (String) - a VCF file with the variants to visualize
  genome (String) - a FASTA file with the reference genome

Optional args:
  padding      (int)    [50]     - the number of bases on each side of the variant to include
  outprefix (String)    [igv]    - the name of the directory to put screenshots into
  --nocombine                    - don't combine nearby variants into single screenshots
  --squish                       - squish the screenshots to capture more reads
  --svg                          - output the snapshot to svg files instead of png files

  ```
  
Then, in IGV (v1.5+), click on Tools->Run Batch Script, and select the generated batch script igv.bat from the working directory.

## Generating screenshots from the command line

To run the batch script from the command line instead of the IGV interface, an off-screen server such as xvfb is required.  The following commands show how to do this:

```
sudo apt-get install xvfb
xvfb-run --auto-servernum PATH_TO/igv.sh -b BATCH_FILE
```
