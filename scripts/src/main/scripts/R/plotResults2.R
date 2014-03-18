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

############################################################################
# Highlighted tables
############################################################################

## 2010-06-25
## (c) Felix Andrews <felix@nfrac.org>
## GPL-2

## https://gist.github.com/floybix/452201
 
## If 'which' is given it should be a logical matrix specifying bold cells.
## Otherwise: in each column or row with numeric data, the maximum or minimum
## value is set bold; 'max' can have entries for each column/row, NA means skip.
 
## Examples:
## library(xtable)
## x <- tail(iris)
## printbold(xtable(x)) #each = "column"
## printbold(xtable(x), each = "column", max = c(F,NA,NA,T,NA))
## printbold(xtable(x), each = "row", max = FALSE)
## printbold(xtable(x), x >= 6.5, type = "html")
 
printbold <-
function(x, which = NULL, each = c("column", "row"), max = TRUE,
NA.string = "", type = c("latex", "html"),
sanitize.text.function = force,
sanitize.rownames.function = NULL,
sanitize.colnames.function = NULL,
row.group.pos = NULL,
 ...)
{
stopifnot(inherits(x, "xtable"))
each <- match.arg(each)
type <- match.arg(type)
digits <- rep(digits(x), length = ncol(x)+1)
if (!is.null(which)) {
stopifnot(nrow(which) == nrow(x))
stopifnot(ncol(which) == ncol(x))
boldmatrix <- which
} else {
boldmatrix <- matrix(FALSE, ncol = ncol(x), nrow = nrow(x))
## round values before calculating max/min to avoid trivial diffs
for (i in 1:ncol(x)) {
if (!is.numeric(x[,i])) next
x[,i] <- round(x[,i], digits = digits[i+1])
}
if (each == "column") {
max <- rep(max, length = ncol(x))
startRow <- 1
endRow = row.group.pos[[1]]
for (j in length(row.group.pos)) {
for (i in 1:ncol(x)) {
xi <- x[startRow:endRow,i]
if (!is.numeric(xi)) next
if (is.na(max[i])) next
imax <- max(xi, na.rm = TRUE)
if (!max[i])
imax <- min(xi, na.rm = TRUE)
boldmatrix[xi == imax, i] <- TRUE
}
startRow = endRow+1
endRow = row.group.pos[[j]]-1
}
} else if (each == "row") {
max <- rep(max, length = nrow(x))
for (i in 1:nrow(x)) {
xi <- x[i,]
ok <- sapply(xi, is.numeric)
if (!any(ok)) next
if (is.na(max[i])) next
imax <- max(unlist(xi[ok]), na.rm = TRUE)
if (!max[i])
imax <- min(unlist(xi[ok]), na.rm = TRUE)
whichmax <- sapply(xi, identical, imax)
boldmatrix[i, whichmax] <- TRUE
}
}
}
## need to convert to character
## only support per-column formats, not cell formats
display <- rep(display(x), length = ncol(x)+1)
for (i in 1:ncol(x)) {
if (!is.numeric(x[,i])) next
ina <- is.na(x[,i])
x[,i] <- formatC(x[,i], digits = digits[i+1],
format = display[i+1])
x[ina, i] <- NA.string
display(x)[i+1] <- "s"
## embolden
yes <- boldmatrix[,i]
if (type == "latex") {
x[yes,i] <- paste("\\textbf{", x[yes,i], "}", sep = "")
} else {
x[yes,i] <- paste("<strong>", x[yes,i], "</strong>", sep = "")
}
}
print(x, ..., type = type, NA.string = NA.string,
sanitize.text.function = sanitize.text.function,
sanitize.rownames.function = sanitize.rownames.function,
sanitize.colnames.function = sanitize.colnames.function)
}

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
      tf <- data.frame(Reference=refs,Method=tools,F1=subs$F1,Precision=subs$Precision,Recall=subs$Recall,TP=subs$TP,FP=subs$FP,TN=subs$TN,FN=subs$FN,Unmatched=subs$Gtunmatched,Runtime=subs$runtime,Memory=subs$memory)
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
colnames(evalSummary) <- c("Reference","Method","F1","Precision","Recall","TP","FP","TN","FN","Unm. in Ref.","Runtime (s)","Memory (MBytes)")

write.table(evalSummary,file="evaluation-FN-corrected-best-summary.csv",sep="\t",row.names=FALSE)
print(nrow(evalSummary))
hlines <- c(-1,0,nrow(evalSummary))
rownames(evalSummary) <- rnames
if(opt$tex){
  tbl <- xtable(evalSummary, caption=c("",""), align=c("r","r","r","l","l","l","l","l","l","l","l","l","l"),digits=c(0,0,0,4,4,4,0,0,0,0,0,2,2),display=c("s","s","s","f","f","f","f","f","f","f","f","f","f"))
  if(length(addToRow.pos)==0) {
    printbold(tbl,each="column",file="evaluation-FN-corrected-best.tex",row.group.pos=addToRow.pos,width="\\textwidth",caption.placement="top",booktabs=T,include.rownames=FALSE,hline.after=hlines,tabular.environment="tabularx",floating=TRUE,floating.environment="table*",max = c(NA,NA,T,T,T,T,F,T,F,F,F,F))
  }else{
    printbold(tbl,each="column",file="evaluation-FN-corrected-best.tex",row.group.pos=addToRow.pos,width="\\textwidth",caption.placement="top",booktabs=T,include.rownames=FALSE,hline.after=hlines,add.to.row=list(pos=addToRow.pos,command=addToRow.command),tabular.environment="tabularx",floating=TRUE,floating.environment="table*",max = c(NA,NA,T,T,T,T,F,T,F,F,F,F))
  }
}
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
f1byRef <- ddply(evaluation,.(ReferenceName),summarise,F1Max=max(F1))
f1byRef
#find the corresponding instance
f1BestInstanceByRef <- ddply(evaluation,.(ReferenceName),subset, (F1 == max(F1)))
#condense multiples with same numbers
#should contain one value per facet -> one instance per tool name
f1BestInstanceByRef <- unique(f1BestInstanceByRef[,c('name','ReferenceName','F1')])
f1BestInstanceByRef$color <- 'gray'
f1BestInstanceByRef$x <- f1BestInstanceByRef$name
f1BestInstanceByRef$y <- f1BestInstanceByRef$F1+0.1
f1BestInstanceByRef$label <- format.default(f1BestInstanceByRef$F1, digits = 4)
############################################################################
# F1 plot
############################################################################
cat("> F1 plot","\n")
#COL <- length(unique(evaluation$MCS))
f1plot <- qplot(name,F1,data=evaluation,colour=Similarity, geom="boxplot")+
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
#  annotate("text",x=f1BestInstanceByRef$name, y = f1BestInstanceByRef$F1+0.2, size=2, color=f1BestInstanceByRef$color, label = f1BestInstanceByRef$F1)+
  facet_grid(ReferenceName ~ .) + #, nrow=NRef) +xlab("Method")
  xlab("Method")+theme(axis.title.x = element_blank())+coord_cartesian(ylim=c(0,1.2))+
  geom_hline(data=f1byRef, aes(yintercept = F1Max), linetype=c(2), color='gray')+
  geom_text(data=f1BestInstanceByRef, aes(x=x, y=y, label=label), size=3, colour="black", inherit.aes=FALSE, parse=FALSE, fontface=1)
ggsave(file = "f1-vs-name.pdf", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "f1-vs-name.png", plot = f1plot, width = DBLCOL, height = NRef*PH, units="cm")

############################################################################
# F1 MCS max plot
############################################################################
cat("> F1 mcs plot","\n")
COL <- length(unique(evaluation$MCS))
f1fmcsplot <- qplot(name,F1,data=evaluation,colour=Similarity,geom="boxplot")+
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ MCS) +
  xlab("Method")+theme(axis.title.x = element_blank())+ylim(0,1)+
  geom_hline(data=f1byRef, aes(yintercept = F1Max), linetype=c(2), color='gray')#+
  #geom_text(data=f1BestInstanceByRef, aes(x=name, y=F1+0.1, label=format.default(F1, digits = 4)), size=3, colour="black", inherit.aes=FALSE, parse=FALSE)
ggsave(file = "f1-mcs-vs-name.pdf", plot = f1fmcsplot, width = DBLCOL*3, height = NRef*PH, units="cm")
ggsave(file = "f1-mcs-vs-name.png", plot = f1fmcsplot, width = DBLCOL*3, height = NRef*PH, units="cm")

############################################################################
# CoverageR plot
# CoverageR is the fraction of entities in the 
# tool that were assigned to the reference and the total number of entities 
# contained in the REFERENCE alignment
############################################################################
evaluation$CoverageR <- (evaluation$TP+evaluation$FP+evaluation$FN+evaluation$TN)/evaluation$Gtsize
coverageRByRef <- ddply(evaluation,.(ReferenceName),summarise,CoverageRMax=max(CoverageR))
coverageRBestInstanceByRef <- ddply(evaluation,.(ReferenceName),subset, (CoverageR == max(CoverageR)))
coverageRBestInstanceByRef <- unique(coverageRBestInstanceByRef[,c('name','ReferenceName','CoverageR')])
coverageRBestInstanceByRef$color <- 'gray'
coverageRBestInstanceByRef$x <- coverageRBestInstanceByRef$name
coverageRBestInstanceByRef$y <- coverageRBestInstanceByRef$CoverageR+0.1
coverageRBestInstanceByRef$label <- format.default(coverageRBestInstanceByRef$CoverageR, digits = 4)
cat("> CoverageR plot","\n")
coverageRPlot <- qplot(name,CoverageR,data=evaluation,colour=Similarity,geom="boxplot") +
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ .) +
  xlab("Method")+ylab(expression(paste("Coverage" [R])))+
  theme(axis.title.x = element_blank())+coord_cartesian(ylim=c(0,1.2))+
  geom_hline(data=coverageRByRef, aes(yintercept = CoverageRMax), linetype=c(2), color='gray')+
  geom_text(data=coverageRBestInstanceByRef, aes(x=x, y=y, label=label), size=3, colour="black", inherit.aes=FALSE, parse=FALSE)
ggsave(file = "coverageR-vs-name.pdf", plot = coverageRPlot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "coverageR-vs-name.png", plot = coverageRPlot, width = DBLCOL, height = NRef*PH, units="cm")

############################################################################
# CoverageT plot
# CoverageT is the fraction of entities in the 
# tool that were assigned to the reference and the total number of entities 
# contained in the TOOL alignment 
############################################################################
evaluation$CoverageT <- (evaluation$TP+evaluation$FP+evaluation$FN+evaluation$TN)/evaluation$Toolsize
coverageTByRef <- ddply(evaluation,.(ReferenceName),summarise,CoverageTMax=max(CoverageT))
coverageTBestInstanceByRef <- ddply(evaluation,.(ReferenceName),subset, (CoverageT == max(CoverageT)))
coverageTBestInstanceByRef <- unique(coverageTBestInstanceByRef[,c('name','ReferenceName','CoverageT')])
coverageTBestInstanceByRef$color <- 'gray'
coverageTBestInstanceByRef$x <- coverageTBestInstanceByRef$name
coverageTBestInstanceByRef$y <- coverageTBestInstanceByRef$CoverageT+0.1
coverageTBestInstanceByRef$label <- format.default(coverageTBestInstanceByRef$CoverageT, digits = 4)
cat("> CoverageT plot","\n")
coverageTPlot <- qplot(name,CoverageT,data=evaluation,colour=Similarity,geom="boxplot") +
  scale_colour_brewer(palette=colorPalette,name="Similarity") +
  facet_grid(ReferenceName ~ .) +
  xlab("Method")+ylab(expression(paste("Coverage" [T])))+
  theme(axis.title.x = element_blank())+coord_cartesian(ylim=c(0,1.2))+
  geom_hline(data=coverageTByRef, aes(yintercept = CoverageTMax), linetype=c(2), color='gray')+
  geom_text(data=coverageTBestInstanceByRef, aes(x=x, y=y, label=label), size=3, colour="black", inherit.aes=FALSE, parse=FALSE)
ggsave(file = "coverageT-vs-name.pdf", plot = coverageTPlot, width = DBLCOL, height = NRef*PH, units="cm")
ggsave(file = "coverageT-vs-name.png", plot = coverageTPlot, width = DBLCOL, height = NRef*PH, units="cm")

############################################################################
# Precision Recall plot
############################################################################
cat("> Precision Recall plot","\n")
prplot <- qplot(Recall,Precision,data=evaluation, colour=Similarity,shape=Similarity,alpha=pointAlpha) + 
  facet_grid(ReferenceName ~ name) + geom_rug() +
  scale_colour_brewer(palette=colorPalette) +
  scale_shape_manual(values=1:length(unique(evaluation$Similarity))) +
  scale_alpha(guide="none")+ylim(0,1)+xlim(0,1)
ggsave(file = "recall-vs-precision.pdf", plot = prplot, width = DBLCOL*2, height = NRef*PH, units="cm")
ggsave(file = "recall-vs-precision.png", plot = prplot, width = DBLCOL*2, height = NRef*PH, units="cm")

############################################################################
# True Positives vs False Positives plot
############################################################################
cat("> TP FP plot","\n")
tpfpplot <- qplot(FP,TP,data=evaluation,colour=Similarity,shape=Similarity
                  ,alpha=pointAlpha) +
  facet_grid(ReferenceName ~ name) + geom_rug() + 
  scale_colour_brewer(palette=colorPalette) +
  scale_shape_manual(values=1:length(unique(evaluation$Similarity))) +
  scale_alpha(guide="none") + ylim(0,max(evaluation$TP)) + xlim(0,max(evaluation$FP))
ggsave(file = "fp-vs-tp.pdf", plot = tpfpplot, width = DBLCOL*2, height = NRef*PH, units="cm")
ggsave(file = "fp-vs-tp.png", plot = tpfpplot, width = DBLCOL*2, height = NRef*PH, units="cm")

############################################################################
# True Negatives vs False Negatives plot
############################################################################
cat("> TN FN plot","\n")
tnfnplot <- qplot(FN,TN,data=evaluation,colour=Similarity,shape=Similarity
                  ,alpha=pointAlpha) +
  facet_grid(ReferenceName ~ name) + geom_rug() + 
  scale_colour_brewer(palette=colorPalette) +
  scale_shape_manual(values=1:length(unique(evaluation$Similarity))) +
  scale_alpha(guide="none") + ylim(0,max(evaluation$TN)) + xlim(0,max(evaluation$FN))
ggsave(file = "fn-vs-tn.pdf", plot = tnfnplot, width = DBLCOL*2, height = NRef*PH, units="cm")
ggsave(file = "fn-vs-tn.png", plot = tnfnplot, width = DBLCOL*2, height = NRef*PH, units="cm")

############################################################################
# Runtime histogram
############################################################################
cat("> Runtime histogram","\n")
rtplot <- qplot(name,runtime,data=evaluation,colour=Similarity,geom="boxplot") +
  scale_colour_brewer(palette=colorPalette)+ xlab("Method")+ ylab("Runtime (s)")+theme(axis.title.x = element_blank()) + ylim(0,max(na.omit(evaluation$runtime)))
ggsave(file = "runtime-histogram.pdf", plot = rtplot, width = DBLCOL, height = 1.5*PH, units="cm")
ggsave(file = "runtime-histogram.png", plot = rtplot, width = DBLCOL, height = 1.5*PH, units="cm")

############################################################################
# Memory Histogram
############################################################################
cat("> Memory histogram","\n")
memplot <- qplot(name,memory,data=evaluation,colour=Similarity,geom="boxplot") +
  scale_colour_brewer(palette=colorPalette) + xlab("Method")+ ylab("Memory (MB)")+theme(axis.title.x = element_blank()) + ylim(0,max(na.omit(evaluation$memory)))
ggsave(file = "memory-histogram.pdf", plot = memplot, width = DBLCOL, height = 1.5*PH, units="cm")
ggsave(file = "memory-histogram.png", plot = memplot, width = DBLCOL, height = 1.5*PH, units="cm")
