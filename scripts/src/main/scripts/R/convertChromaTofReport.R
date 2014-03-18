#!/usr/bin/env Rscript
suppressPackageStartupMessages(library("optparse"))
option_list <- list(
    make_option("--dataPath", default="../data/SWPA_Dataset_I",
        help="Path relative to current directory containing input files. [default
        \"%default\"]"),
    make_option("--directory", default="swpa-output",
        help="Base directory for output relative to current path. [default \"%default\"]")
)

opt <- parse_args(OptionParser(option_list=option_list))

dataPath = opt$dataPath
outputPath = file.path(getwd(),opt$directory)
pattern = "*.csv"
#NH create output directory
if(!file.exists(outputPath)) {
    output = dir.create(outputPath,recursive=TRUE)
}

# NH cat data and output path
cat("Data path: ",dataPath,"\n")
cat("Output path: ",outputPath,"\n")

files <- list.files(path=dataPath,pattern=pattern,recursive=FALSE,full.names=TRUE)
cnames <- c("Name","R.T. (s)","Type","UniqueMass","Concentration","Sample Concentration","Match","Quant Masses","Quant S/N","Area","BaselineModified","Quantification","Full Width at Half Height","IntegrationBegin","IntegrationEnd","Hit 1 Name","Hit 1 Similarity","Hit 1 Reverse","Hit 1 Probability","Hit 1 CAS","Hit 1 Library","Hit 1 Id","Hit 1 Formula","Hit 1 Weight","Hit 1 Contributor","Spectra","1st Dimension Time (s)","2nd Dimension Time (s)")
cnamesNew <- c("Name","Hit 1 CAS","1st Dimension Time (s)","2nd Dimension Time (s)","Area","Hit 1 Similarity","Hit 1 Reverse","Hit 1 Probability","UniqueMass","Quant Masses","Spectra")
targetcnames <- c("Name","CAS","1st Dimension Time (s)","2nd Dimension Time (s)","Area","Similarity","Reverse","Probability","UniqueMass","Quant Masses","Spectra")
setwd(outputPath)
for(i in 1:length(files)) {
    tbl <- read.csv(files[[i]])
    rts <- strsplit(as.character(tbl$R.T...s.),split=c(" , "))
    # NH: select the first element of every sublist in rts
    t1 <- as.numeric(sapply(rts, "[[", 1))
    # NH: select the second element of every sublist in rts
    t2 <- as.numeric(sapply(rts, "[[", 2))
    tbl$"1st Dimension Time (s)" <- t1
    tbl$"2nd Dimension Time (s)" <- t2
    colnames(tbl) <- cnames
    # NH: Remove all compounds with Unknown name
    upos = grep(pattern="Unknown*",x=tbl$Name)
    tbl <- tbl[-upos,]
    tbl <- tbl[,cnamesNew]
    colnames(tbl) <- targetcnames
    tbl$"1st Dimension Time (s)" <- as.character(tbl$"1st Dimension Time (s)")
    tbl$"2nd Dimension Time (s)" <- as.character(tbl$"2nd Dimension Time (s)")
    tbl$"Area" <- as.character(tbl$"Area")
    tbl$"Similarity" <- as.character(tbl$"Similarity")
    tbl$"Reverse" <- as.character(tbl$"Reverse")
    tbl$"Probability" <- as.character(tbl$"Probability")
    tbl$"UniqueMass" <- as.character(tbl$"UniqueMass")
    tbl$"Quant Masses" <- as.character(tbl$"Quant Masses")
    filename <- basename(files[[i]])
    cat("Colnames:",colnames(tbl),"\n")
    cat("Saving to",filename,"in", outputPath,"\n")
    length(tbl)
    write.csv(tbl,file=filename,quote=T,row.names=F)
}

if(!is.null(warnings()) && length(warnings())>0){
    warnings()
}
