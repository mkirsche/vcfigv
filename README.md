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
  ```
  
Then, in IGV (v1.5+), click on Tools->Run Batch Script, and select the generated batch script igv.bat from the working directory.
