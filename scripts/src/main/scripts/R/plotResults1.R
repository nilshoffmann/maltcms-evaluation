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
#replace BiPACE w/ RT and BiPACE w/ 2D RT with names used in manuscript
evaluation$name <- sub(pattern="BiPACE w/ RT",replacement="BiPACE RT",evaluation$name,fixed=T)
evaluation$name <- sub(pattern="BiPACE w/ 2D RT",replacement="BiPACE 2D",evaluation$name,fixed=T)
#replace shorthands with more user-friendly names
evaluation$Similarity <- sub(pattern="cosine",replacement="Cosine of Angle",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="dot",replacement="Dot Product",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="euclidean",replacement="neg. Euclidean Dist.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="linCorr",replacement="Pearson's Corr.",evaluation$Similarity,fixed=T)
evaluation$Similarity <- sub(pattern="rankCorr",replacement="Spearman's Rank Corr.",evaluation$Similarity,fixed=T)

#prepare ggplot2
suppressPackageStartupMessages(library("ggplot2"))
theme_set(theme_bw(base_size=10))
theme_update(
  legend.position="right",
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
    #evaluation[idxs,]$pointAlpha <- 1.0/length(idxs)
  }
}
#print stats on pointAlpha
summary(evaluation$pointAlpha)
#total number of entities of reference alignment 
N <- evaluation$TP+evaluation$FP+evaluation$FN+evaluation$TN+evaluation$Gtunmatched
#invariant
stopifnot(length(N)==length(evaluation$Gtsize))
#add accuracy
Accuracy <- (evaluation$TP+evaluation$TN)/N
evaluation$Accuracy <- Accuracy

############################################################################
# The direct output only has the uncorrected F1 score. We need to 
# correct for the peaks within the ground truth that were not covered 
# by the reported alignment.
#
# Correct to size of intersection + size of set difference 
# between ground truth and tool :
# (|gt \cut tool|) + (|gt| \ |tool|)
# = (TP+FP+TN+FN) + (|gt| - (|tool| - |gt \cut tool|))
#
# Gtunmatched = |gt| - |tool| - |gt \cut tool|
# is the number of peaks contained in the ground truth that belong to a group
# that was not reported by the tool.
# Precision is invariant to the number of FNs, so we need no correction.
############################################################################
Precision <- evaluation$TP / (evaluation$TP + evaluation$FP)
Recall <- evaluation$TP / (evaluation$TP + evaluation$FN + evaluation$Gtunmatched)
F1 <- 2*(Precision*Recall)/(Precision+Recall)
evaluation$F1 <- F1
evaluation$Precision <- Precision
evaluation$Recall <- Recall
evaluation <- evaluation[order(evaluation$ReferenceName,evaluation$name,-evaluation$F1),]
write.table(evaluation,file="evaluation-FN-corrected.csv",sep="\t",row.names=FALSE)

bestResults <- data.frame()
#extract referencenames
refnames <- sort(unique(evaluation$ReferenceName))
for(refIdx in 1:length(refnames)) {
  refname <- refnames[refIdx]
  for(toolIdx in 1:length(toolnames)) {
    toolname <- toolnames[toolIdx]
    idxs <- which(evaluation$name==toolname)
    if(length(idxs)>0) {
      #append matches to bestResults
      tmpEvaluation <- evaluation[idxs,]
      tmpEvaluation <- tmpEvaluation[tmpEvaluation$ReferenceName==refname,]
      bestResults <- rbind(bestResults,head(tmpEvaluation[order(-tmpEvaluation$F1,-tmpEvaluation$runtime,-tmpEvaluation$memory),],1))
    }
  }
}

colnames(bestResults) <- colnames(evaluation)
write.table(bestResults,file="evaluation-FN-corrected-best.csv",sep="\t",row.names=FALSE)

evalSummary <- data.frame()
rnames <- c()
addToRow.pos <- list()
addToRow.command <- c()
row <- 0
#F1, Precision, Recall, TP, FP, TN, FN
for(refIdx in 1:length(refnames)) {
  reference <- refnames[refIdx]
  #print(paste("Reference: ",refnames[refIdx],sep=""))
  for(toolIdx in 1:length(toolnames)) {
    #print(paste("Toolname: ",toolnames[toolIdx],sep=""))
    toolname <- toolnames[toolIdx]
    subs <- subset(evaluation,name==toolnames[toolIdx] & ReferenceName==refnames[refIdx])
    if(nrow(subs)==0) {
      cat("Skipping empty subset!","\n",sep="")
    }else{
      refs <- as.factor(rep(reference,times=nrow(subs)))
      tools <- as.factor(rep(toolname,times=nrow(subs)))
      #rnames <- c(rnames,toolnames[toolIdx])
      tf <- data.frame(Reference=refs,Method=tools,F1=subs$F1,Precision=subs$Precision,Recall=subs$Recall,TP=subs$TP,FP=subs$FP,TN=subs$TN,FN=subs$FN,Unmatched=subs$Gtunmatched,Runtime=subs$runtime)
      #print(tf)
      evalSummary <- rbind(evalSummary,head(tf[order(tf$Reference,tf$Method,-tf$F1),],1))
    }
    row <- row + 1
  }
  if(refIdx<length(refnames)) {
    addToRow.pos[[refIdx]] <- row
    addToRow.command <- c(addToRow.command,"\\midrule 
  ")
  }
}
colnames(evalSummary) <- c("Reference","Method","F1","Precision","Recall","TP","FP","TN","FN","Unmatched in Reference","Runtime (s)")

write.table(evalSummary,file="evaluation-FN-corrected-best-summary.csv",sep="\t",row.names=FALSE)
print(nrow(evalSummary))
hlines <- c(-1,0,nrow(evalSummary))
rownames(evalSummary) <- rnames
if(opt$tex){
  tbl <- xtable(evalSummary, caption=c("",""), align=c("r","r","r","l","l","l","l","l","l","l","l","l"),digits=c(0,0,0,4,4,4,0,0,0,0,0,2),display=c("s","s","s","f","f","f","f","f","f","f","f","f"))
  if(length(addToRow.pos)==0) {
    print(tbl,file="evaluation-FN-corrected-best.tex",width="\\textwidth",caption.placement="top",booktabs=T,include.rownames=FALSE,hline.after=hlines,tabular.environment="tabularx",floating=TRUE,floating.environment="table*")
  }else{
    print(tbl,file="evaluation-FN-corrected-best.tex",width="\\textwidth",caption.placement="top",booktabs=T,include.rownames=FALSE,hline.after=hlines,add.to.row=list(pos=addToRow.pos,command=addToRow.command),tabular.environment="tabularx",floating=TRUE,floating.environment="table*")
  }
}

#bipace plots
bipaceRobinson <- rbind(evaluation[grep(pattern="BiPACE",x=evaluation$name),],evaluation[grep(pattern="Robinson",
x=evaluation$name),])

bipace <- evaluation[grep(pattern="BiPACE",x=evaluation$name),]

#tp/fp plot
cat("Plot1","\n")
plot1 <- qplot(FP,TP,data=bipaceRobinson,colour=Similarity,shape=Similarity,alpha=pointAlpha) +
facet_grid(T ~ D, labeller="label_both") +
geom_rug() + scale_colour_brewer(palette=colorPalette) +
#scale_shape_manual(values=as.numeric(factor(bipaceRobinson$Similarity))) +
#5=number of different similarities
scale_shape_manual(values=1:5) +
scale_alpha(guide="none")

ggsave(file = "bipace-fp-vs-tp-rtThres-rtTol.pdf", plot = plot1, width = 12, height=10)
ggsave(file = "bipace-fp-vs-tp-rtThres-rtTol.png", plot = plot1, width = 12, height = 10)

#tp/fp mcs plot
cat("Plot2","\n")
plot2 <- qplot(FP,TP,data=bipaceRobinson,colour=Similarity,shape=Similarity,alpha=pointAlpha) +
facet_grid(. ~ MCS, labeller="label_both") +
geom_rug() + scale_colour_brewer(palette=colorPalette) +
#scale_shape_manual(values=as.numeric(factor(bipaceRobinson$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(guide="none")
ggsave(file = "bipace-fp-vs-tp-mcs.pdf", plot = plot2, width = 14, height = 5)
ggsave(file = "bipace-fp-vs-tp-mcs.png", plot = plot2, width = 14, height = 5)

#recall/precision plot
cat("Plot3","\n")
plot3 <- qplot(Recall,Precision,data=bipaceRobinson,
colour=Similarity,shape=Similarity,alpha=pointAlpha) + facet_grid(. ~ name) + geom_rug() +
scale_colour_brewer(palette=colorPalette) +
#scale_shape_manual(values=as.numeric(factor(bipaceRobinson$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(guide="none")+ylim(0,1)+xlim(0,1)
ggsave(file = "bipace-recall-vs-precision.pdf", plot = plot3, width = 6, height = 5)
ggsave(file = "bipace-recall-vs-precision.png", plot = plot3, width = 6, height = 5)

#runtime histogram
cat("Plot4a","\n")
plot4 <- qplot(Similarity,runtime,data=bipace,geom="boxplot",colour=Similarity, ylab="Runtime (min)") +
facet_grid(. ~ name) +
scale_colour_brewer(palette=colorPalette, guide="none")
ggsave(file = "bipace-runtime-histogram.pdf", plot = plot4, width = 6, height = 5)
ggsave(file = "bipace-runtime-histogram.png", plot = plot4, width = 6, height = 5)

#memory histogram
cat("Plot4b","\n")
plot4 <- qplot(Similarity,memory,data=bipace,geom="boxplot",colour=Similarity, ylab="Memory (MB)") +
facet_grid(. ~ name) +
scale_colour_brewer(palette=colorPalette, guide="none")
ggsave(file = "bipace-memory-histogram.pdf", plot = plot4, width = 6, height = 5)
ggsave(file = "bipace-memory-histogram.png", plot = plot4, width = 6, height = 5)

#cemapp dtw plots
cemappDtw <- evaluation[grep(pattern="CeMAPP-DTW",x=evaluation$name),]
#tp/fp plot
#anchor usage vs D
cat("Plot5","\n")
plot5 <- qplot(FP,TP,data=cemappDtw, colour=Similarity, shape=Similarity, alpha=pointAlpha) +
facet_grid(D ~ DTW, labeller="label_both") + geom_rug() +
scale_colour_brewer(palette=colorPalette) +
#scale_shape_manual(values=as.numeric(factor(cemappDtw$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(guide="none")
ggsave(file = "cemapp-fp-vs-tp-anchor-rtTol.pdf", plot = plot5, width = 6, height = 10)
ggsave(file = "cemapp-fp-vs-tp-anchor-rtTol.png", plot = plot5, width = 6, height = 10)

#matchWeight W vs anchorRadius R
cat("Plot6","\n")
plot6 <- qplot(FP,TP,data=cemappDtw, colour=Similarity, shape=Similarity, alpha=pointAlpha) +
facet_grid(W ~ R, labeller="label_both") + geom_rug() +
scale_colour_brewer(palette=colorPalette) +
#scale_shape_manual(values=as.numeric(factor(cemappDtw$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(guide="none")
ggsave(file = "cemapp-fp-vs-tp-matchWeight-anchorRadius.pdf", plot = plot6, width = 6, height = 10)
ggsave(file = "cemapp-fp-vs-tp-matchWeight-anchorRadius.png", plot = plot6, width = 6, height = 10)

#band constraint BandConstraint vs band constraint width BC
cat("Plot7","\n")
plot7 <- qplot(FP,TP,data=cemappDtw, colour=Similarity, shape=Similarity, alpha=pointAlpha) +
facet_grid(BCScope ~ BC, labeller="label_both") + geom_rug() +
scale_colour_brewer(palette=colorPalette) +
#scale_shape_manual(values=as.numeric(factor(cemappDtw$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(guide="none")
ggsave(file = "cemapp-fp-vs-tp-globalBand-BW.pdf", plot = plot7, width = 6, height = 10)
ggsave(file = "cemapp-fp-vs-tp-globalBand-BW.png", plot = plot7, width = 6, height = 10)

#runtime histogram
cat("Plot8a","\n")
plot8a <- qplot(Similarity,runtime,data=cemappDtw,geom="boxplot",colour=Similarity, ylab="Runtime (min)") +
facet_grid(DTW ~ name) +
scale_colour_brewer(palette=colorPalette, guide="none")
ggsave(file = "cemapp-runtime-histogram.pdf", plot = plot8a, width = 6, height = 5)
ggsave(file = "cemapp-runtime-histogram.png", plot = plot8a, width = 6, height = 5)

#memory histogram
cat("Plot8b","\n")
plot8b <- qplot(Similarity,memory,data=cemappDtw,geom="boxplot",colour=Similarity, ylab="Memory (MB)") +
facet_grid(DTW ~ name) +
scale_colour_brewer(palette=colorPalette, guide="none")
ggsave(file = "cemapp-memory-histogram.pdf", plot = plot8b, width = 6, height = 5)
ggsave(file = "cemapp-memory-histogram.png", plot = plot8b, width = 6, height = 5)

############################################################################
# F1 plot
############################################################################
cat("> BiPACE F1 plot","\n")
#COL <- length(unique(evaluation$MCS))
f1plot <- qplot(name,F1,data=bipace,colour=Similarity, geom="boxplot")+
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ .) + #, nrow=NRef) +xlab("Method")
  xlab("Method")+theme(axis.title.x = element_blank())+ylim(0,1)
ggsave(file = "bipace-f1-vs-name.pdf", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "bipace-f1-vs-name.png", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")

############################################################################
# F1 MCS max plot
############################################################################
cat("> BiPACE F1 mcs plot","\n")
COL <- length(unique(bipace$MCS))
f1fmcsplot <- qplot(name,F1,data=bipace,colour=Similarity,alpha=pointAlpha,geom="boxplot")+
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ MCS) +
  scale_alpha(guide="none")+xlab("Method")+theme(axis.title.x = element_blank())+ylim(0,1)
ggsave(file = "bipace-f1-mcs-vs-name.pdf", plot = f1fmcsplot, width = DBLCOL*3, height = NRef*PH, units="cm")
ggsave(file = "bipace-f1-mcs-vs-name.png", plot = f1fmcsplot, width = DBLCOL*3, height = NRef*PH, units="cm")

############################################################################
# F1 plot
############################################################################
cat("> CeMAPP-DTW F1 plot","\n")
#COL <- length(unique(evaluation$MCS))
f1plot <- qplot(name,F1,data=cemappDtw,colour=Similarity, geom="boxplot")+
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ .) + #, nrow=NRef) +xlab("Method")
  xlab("Method")+theme(axis.title.x = element_blank())+ylim(0,1)
ggsave(file = "cemapp-f1-vs-name.pdf", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "cemapp-f1-vs-name.png", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")

###########################################################################
# CoverageR is the fraction of entities in the 
# tool that were assigned to the reference and the total number of entities 
# contained in the REFERENCE alignment
#
# CoverageT is the fraction of entities in the 
# tool that were assigned to the reference and the total number of entities 
# contained in the TOOL alignment 
############################################################################
evaluation$CoverageR <- (evaluation$TP+evaluation$FP+evaluation$FN+evaluation$TN)/evaluation$Gtsize
evaluation$CoverageT <- (evaluation$TP+evaluation$FP+evaluation$FN+evaluation$TN)/evaluation$Toolsize
############################################################################
# CoverageR plot
############################################################################
cat("> CoverageR plot","\n")
coverageRPlot <- qplot(name,CoverageR,data=evaluation,colour=Similarity,alpha=pointAlpha,geom="boxplot") +
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ .) +
  scale_alpha(guide="none")+xlab("Method")+ylab(expression(paste("Coverage" [R])))+
  theme(axis.title.x = element_blank())+ylim(0,1)
ggsave(file = "coverageR-vs-name.pdf", plot = coverageRPlot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "coverageR-vs-name.png", plot = coverageRPlot, width = DBLCOL, height = NRef*PH, units="cm")

############################################################################
# CoverageT plot
############################################################################
cat("> CoverageT plot","\n")
coverageTPlot <- qplot(name,CoverageT,data=evaluation,colour=Similarity,alpha=pointAlpha,geom="boxplot") +
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ .) +
  scale_alpha(guide="none")+xlab("Method")+ylab(expression(paste("Coverage" [T])))+
  theme(axis.title.x = element_blank())+ylim(0,1)
ggsave(file = "coverageT-vs-name.pdf", plot = coverageTPlot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "coverageT-vs-name.png", plot = coverageTPlot, width = DBLCOL, height = NRef*PH, units="cm")
