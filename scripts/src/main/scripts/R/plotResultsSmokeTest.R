#!/usr/bin/env Rscript
#
# Copyright (C) 2012 Nils Hoffmann
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
suppressPackageStartupMessages(library("optparse"))
library("xtable")
library("plyr")
option_list <- list(
    make_option("--table", default="evaluation.csv",
    help="Table containing one tool parameterization and classification results per row and multiple traits (columns),
    first row contains column names, first column (without header) contains instance names. [default \"%default\"]"),
    make_option("--directory", default=".",
        help="Base directory for output. [default \"%default\"]"),
    make_option("--referenceName", default="default", help="Value for ReferenceName column if not present in table. [default \"%default\"]"),
    make_option("--tex",default=F, help="Whether to output a tex table of the best results. [default \"%default\"]")
)

opt <- parse_args(OptionParser(option_list=option_list))
#print options
#print(opt)

setwd(opt$directory)
#read data table
evaluation <- read.table(opt$table,header=T)
#exclude instances with empty alignments
if('ReferenceName' %in% colnames(evaluation)) {
  evaluation <- evaluation[which(!is.na(evaluation$ReferenceName)),]

  #replace mgma and mspa names with final names used in manuscript -> MGMA and GMA
  evaluation$ReferenceName <- sub(pattern="mSPA",replacement="GMA",evaluation$ReferenceName,fixed=T)
  evaluation$ReferenceName <- sub(pattern="mgma",replacement="MGMA",evaluation$ReferenceName,fixed=T)
  evaluation$ReferenceName <- sub(pattern="manual",replacement="MANUAL",evaluation$ReferenceName,fixed=T)
}else{
  evaluation$ReferenceName <- opt$referenceName
}

#replace shorthands with more user-friendly names
evaluation$Similarity <- sub(pattern="cosine",replacement="Cosine of Angle",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="dot",replacement="Dot Product",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="euclidean",replacement="neg. Euclidean Dist.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="linCorr",replacement="Pearson's Corr.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="rankCorr",replacement="Spearman's Rank Corr.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="weightedCos",replacement="Weighted Cos.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="bhattacharryyaSimilarity",replacement="Bhattach.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="tanimotoSimilarity",replacement="Tanimoto",evaluation$Similarity,fixed=T)

#prepare ggplot2
suppressPackageStartupMessages(library("ggplot2"))
theme_set(theme_bw(base_size=10))
theme_update(
    legend.position="top",
    #justify labels to right end (right align)
    axis.text.x = element_text(hjust=1.0,angle=90),
    axis.title.x = element_text(vjust=0.5,size=10),
    axis.title.y = element_text(hjust=0.5,size=10, angle=90)
)
#create alpha values to ease overplotting
pointAlpha <- rep(1,nrow(evaluation))
evaluation$pointAlpha <- pointAlpha
colorPalette <- "Dark2"
#Set2
#adjust for height issues with only one facet
NRef = 2
SNGLCOL <- 8.6 #cm
DBLCOL <- 17.8 #cm
PH <- 5.5 #cm
HEIGHT <- PH
#print column names
cat(colnames(evaluation),"\n")
#extract toolnames
toolnames <- sort(unique(evaluation$name))
for(toolIdx in 1:length(toolnames)) {
  toolname <- toolnames[toolIdx]
  idxs <- which(evaluation$name==toolname)
  if(length(idxs)>0) {
    #adapt alpha value to reciprocal of size of tool results
    evaluation[idxs,]$pointAlpha <- 1.0#/length(idxs)
  }
}
#print stats on pointAlpha
summary(evaluation$pointAlpha)

evaluation$npeaks <- as.factor(evaluation$npeaks)

elabels <- paste(evaluation$nfiles,"\n","(",evaluation$npeaks,")",sep="")
elabels

############################################################################
# Runtime histogram
############################################################################
cat("> Runtime histogram","\n")
rtplot <- qplot(as.factor(nfiles),runtime,data=evaluation,colour=Similarity,geom="point") +
  facet_grid(threads~name)+
  scale_colour_brewer(palette=colorPalette)+ xlab("Number of Files")+ ylab("Runtime (s)") + ylim(0,max(na.omit(evaluation$runtime)))
ggsave(file = "runtime-histogram.pdf", plot = rtplot, width = DBLCOL, height = 3*PH, units="cm")
ggsave(file = "runtime-histogram.png", plot = rtplot, width = DBLCOL, height = 3*PH, units="cm")

############################################################################
# Memory Histogram
############################################################################
cat("> Memory histogram","\n")
memplot <- qplot(as.factor(nfiles),memory,data=evaluation,colour=Similarity,geom="point") +
  facet_grid(threads~name)+
  scale_colour_brewer(palette=colorPalette) + xlab("Number of Files (Peaks)")+ ylab("Memory (MB)")+#+theme(axis.title.x = element_blank())
  ylim(0,max(na.omit(evaluation$memory)))#+scale_x_discrete(labels=elabels)
ggsave(file = "memory-histogram.pdf", plot = memplot, width = DBLCOL, height = 3*PH, units="cm")
ggsave(file = "memory-histogram.png", plot = memplot, width = DBLCOL, height = 3*PH, units="cm")
