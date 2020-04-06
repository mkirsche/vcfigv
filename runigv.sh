javac src/*.java
java -cp src Vcf2Bat aln=/home/mkirsche/ncov/data/jhu004.bam var=/home/mkirsche/git/CoverageNormalization/jhu004.vcf genome=/home/mkirsche/git/CoverageNormalization/nCoV-2019.reference.fasta padding=100
xvfb-run --auto-servernum bin/igv.sh -b igv.bat

