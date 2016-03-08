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
library("scales")
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
evaluation <- read.table(opt$table,header=T,na.strings=c("NA","NaN"))
evaluation$F1 <- as.numeric(evaluation$F1)
evaluation$Precision <- as.numeric(evaluation$Precision)
evaluation$Recall <- as.numeric(evaluation$Recall)
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

#replace BiPACE w/ RT and BiPACE w/ 2D RT with names used in manuscript
#evaluation$name <- sub(pattern="BiPACE w/ 2D RT Inv",replacement="BiPACE 2D IG",evaluation$name,fixed=T)
#evaluation$name <- sub(pattern="BiPACE w/ 2D RT",replacement="BiPACE 2D",evaluation$name,fixed=T)
#evaluation$name <- sub(pattern="BiPACE w/ RT",replacement="BiPACE RT",evaluation$name,fixed=T)
cat("Unique names:","\n")
print(unique(evaluation$name))
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
NRef <- length(unique(evaluation$ReferenceName))
if(NRef==1) {
  #adjust for height issues with only one facet
  NRef = 2
}
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

#set NA F1 cases to 0
evaluation$F1 <- replace(evaluation$F1, is.na(evaluation$F1), 0)
evaluation$Precision <- replace(evaluation$Precision, is.na(evaluation$Precision), 0)
evaluation$Recall <- replace(evaluation$Recall, is.na(evaluation$Recall), 0)
evaluation <- evaluation[order(evaluation$ReferenceName,evaluation$name,-evaluation$F1),]
############################################################################
# F1 vs Accuracy plot
############################################################################
# cat("> Accuracy vs F1 plot","\n")
# #COL <- length(unique(evaluation$name))
# acfplot <- qplot(Accuracy,F1,data=evaluation,colour=Similarity,shape=Similarity
#                   ,alpha=pointAlpha) +
#   facet_grid(ReferenceName ~ name, scales="free_y") + geom_rug() + 
#   scale_colour_brewer(palette=colorPalette) +
#   scale_shape_manual(values=1:5) +
#   scale_alpha(guide="none")
# ggsave(file = "accuracy-vs-f1.pdf", plot = acfplot, width = DBLCOL, height = NRef*PH)
# ggsave(file = "accuracy-vs-f1.png", plot = acfplot, width = DBLCOL, height = NRef*PH)
#retrieve maximum F1 value for each reference name
# f1byRef <- ddply(evaluation,.(ReferenceName,uid),summarise,F1Max=max(F1))
f1ByRef <- ddply(evaluation,.(ReferenceName,uid,name,Similarity),summarise,mean=mean(F1),sd=sd(F1),min=min(F1),max=max(F1))
write.table(f1ByRef,file="f1-summary.csv",sep="\t",row.names=FALSE)
precByRef <- ddply(evaluation,.(ReferenceName,uid,name,Similarity),summarise,mean=mean(Precision),sd=sd(Precision))
precByRef <- na.omit(precByRef)
write.table(precByRef,file="precision-summary.csv",sep="\t",row.names=FALSE)
recallByRef <- ddply(evaluation,.(ReferenceName,uid,name,Similarity),summarise,mean=mean(Recall),sd=sd(Recall))
write.table(recallByRef,file="recall-summary.csv",sep="\t",row.names=FALSE)
# f1byRef
# #find the corresponding instance
f1ByRef <- ddply(f1ByRef,.(ReferenceName,name,Similarity), summarise, bestInCategory=(mean==max(mean)), mean=mean,min=min,max=max,sd=sd,uid=uid,Similarity=Similarity)
f1ByRef <- subset(f1ByRef, f1ByRef$bestInCategory==TRUE, -bestInCategory)
evaluationSubset <- evaluation[evaluation$uid %in% f1ByRef$uid,]
#evaluationDuplicates <- duplicated(evaluationSubset[,c('ReferenceName','F1','PairName')])
#evaluationSubset <- evaluationSubset[!evaluationDuplicates,]
#evaluationSubset
write.table(f1ByRef,file="f1-max.csv",sep="\t",row.names=FALSE)
write.table(evaluationSubset,file="evaluation-subset.csv",sep="\t",row.names=FALSE)

f1MaxValue <- max(f1ByRef$max)

# # #condense multiples with same numbers
# # #should contain one value per facet -> one instance per tool name
f1BestInstanceByRef <- f1ByRef
f1BestInstanceByRef$color <- 'gray'
f1BestInstanceByRef$x <- f1BestInstanceByRef$name
#f1BestInstanceByRef$y <- f1MaxValue+0.1
f1BestInstanceByRef$label <- paste(format.default(f1BestInstanceByRef$mean, digits = 4),"+/-",format.default(f1BestInstanceByRef$sd, digits = 2),sep=" ")
#f1BestInstanceByRef <- subset(f1BestInstanceByRef, mean==unique(max(f1BestInstanceByRef$mean)))
f1BestInstanceByRef <- ddply(f1BestInstanceByRef,.(ReferenceName),subset,(mean==max(mean)))
f1BestInstanceByRef$y <- f1BestInstanceByRef$max+0.1
duplicates <- duplicated(f1BestInstanceByRef[,c('ReferenceName','mean')])
f1BestInstanceByRef <- f1BestInstanceByRef[!duplicates,]
write.table(f1BestInstanceByRef,file="f1BestInstanceByRef.csv",sep="\t",row.names=FALSE)
# f1BestInstanceByRef
# #F1Max <- unique(max(f1BestInstanceByRef$F1))
# #F1Max
# f1BestInstanceByRef$x
# f1BestInstanceByRef$y
# f1BestInstanceByRef$label
# f1BestInstanceByRef$ReferenceName
# 
# f1ByGroup <- f1BestInstanceByRef#ddply(f1BestInstanceByRef,.(ReferenceName),subset, (mean==max(mean)))
# f1ByGroup
############################################################################
# F1 plot
############################################################################
cat("> F1 plot","\n")
#COL <- length(unique(evaluation$MCS))
f1plot <- ggplot(evaluationSubset,aes(name,F1))+
  geom_boxplot(aes(color=Similarity),outlier.size=1.0)+
  #geom_jitter(height = 0,size=0.3)+
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  #geom_errorbar(aes(ymin=min,ymax=max),position="dodge")+
#  geom_point(shape=21,size=2,fill="white",position="dodge")+
#  annotate("text",x=f1BestInstanceByRef$name, y = f1BestInstanceByRef$F1+0.2, size=2, color=f1BestInstanceByRef$color, label = f1BestInstanceByRef$F1)+
  facet_grid(ReferenceName ~ .) + #, nrow=NRef) +xlab("Method")
  xlab("Method")+ylab("Pairwise F1")+theme(axis.title.x = element_blank())+coord_cartesian(ylim=c(0,1.2))+
  geom_hline(data=f1BestInstanceByRef,aes(yintercept = mean), linetype=c(2), color='gray')+
  geom_text(data=f1BestInstanceByRef, aes(x=x, y=y, label=label), size=3, colour="black", inherit.aes=FALSE, parse=FALSE, fontface=1)+
  scale_y_continuous(breaks=c(0,0.25,0.5,0.75,1.0), labels=c("0.0","0.25","0.5","0.75","1.0"))
ggsave(file = "f1-vs-name.pdf", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "f1-vs-name.png", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")

############################################################################
# Precision Recall plot
############################################################################
cat("> Precision Recall plot","\n")
prplot <- qplot(Recall,Precision,data=evaluationSubset, colour=Similarity,shape=Similarity,alpha=pointAlpha) + 
  facet_grid(ReferenceName ~ name) + geom_rug() +
  scale_colour_brewer(palette=colorPalette) +
  scale_shape_manual(values=1:length(unique(evaluationSubset$Similarity))) +
  scale_alpha(guide="none")+ylim(0,1)+xlim(0,1)+xlab("Pairwise Recall")+ylab("Pairwise Precision")
ggsave(file = "recall-vs-precision.pdf", plot = prplot, width = DBLCOL*2, height = NRef*PH, units="cm")
ggsave(file = "recall-vs-precision.png", plot = prplot, width = DBLCOL*2, height = NRef*PH, units="cm")

############################################################################
# True Positives vs False Positives plot
############################################################################
cat("> TP FP plot","\n")
tpfpplot <- qplot(FP,TP,data=evaluationSubset,colour=Similarity,shape=Similarity
                  ,alpha=pointAlpha) +
  facet_grid(ReferenceName ~ name) + geom_rug() + 
  scale_colour_brewer(palette=colorPalette) +
  scale_shape_manual(values=1:length(unique(evaluationSubset$Similarity))) +
  scale_alpha(guide="none") + ylim(0,max(evaluation$TP)) + xlim(0,max(evaluation$FP))+
  ylab("Pairwise TP")+xlab("Pairwise FP")
ggsave(file = "fp-vs-tp.pdf", plot = tpfpplot, width = DBLCOL*2, height = NRef*PH, units="cm")
ggsave(file = "fp-vs-tp.png", plot = tpfpplot, width = DBLCOL*2, height = NRef*PH, units="cm")

############################################################################
# True Negatives vs False Negatives plot
############################################################################
cat("> TN FN plot","\n")
tnfnplot <- qplot(FN,TN,data=evaluationSubset,colour=Similarity,shape=Similarity
                  ,alpha=pointAlpha) +
  facet_grid(ReferenceName ~ name) + geom_rug() + 
  scale_colour_brewer(palette=colorPalette) +
  scale_shape_manual(values=1:length(unique(evaluationSubset$Similarity))) +
  scale_alpha(guide="none") + ylim(0,max(evaluation$TN)) + xlim(0,max(evaluation$FN))+
  ylab("Pairwise TN")+xlab("Pairwise FN")
ggsave(file = "fn-vs-tn.pdf", plot = tnfnplot, width = DBLCOL*2, height = NRef*PH, units="cm")
ggsave(file = "fn-vs-tn.png", plot = tnfnplot, width = DBLCOL*2, height = NRef*PH, units="cm")
