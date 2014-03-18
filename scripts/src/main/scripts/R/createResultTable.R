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
  make_option("--referenceName", default="default", help="Value for ReferenceName column if not present in table. [default \"%default\"]")
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
}else{
  evaluation$ReferenceName <- opt$referenceName
}
evaluation <- evaluation[which(!is.na(evaluation$F1)),]
toolnames <- sort(unique(evaluation$name))
references <- unique(evaluation$ReferenceName)

#print(toolnames)
#print(references)
#print(colnames(evaluation))
evalSummary <- data.frame()
rnames <- c()
addToRow.pos <- list()
addToRow.command <- c()
row <- 0
#F1, Precision, Recall, TP, FP, TN, FN
for(refIdx in 1:length(references)) {
  reference <- references[refIdx]
  print(paste("Reference: ",references[refIdx],sep=""))
  for(toolIdx in 1:length(toolnames)) {
    print(paste("Toolname: ",toolnames[toolIdx],sep=""))
    toolname <- toolnames[toolIdx]
    subs <- subset(evaluation,name==toolnames[toolIdx] & ReferenceName==references[refIdx])
    if(nrow(subs)==0) {
      cat("Skipping empty subset!","\n",sep="")
    }else{
      refs <- as.factor(rep(reference,times=nrow(subs)))
      tools <- as.factor(rep(toolname,times=nrow(subs)))
      #rnames <- c(rnames,toolnames[toolIdx])
      tf <- data.frame(Method=tools,Reference=refs,F1=subs$F1,Precision=subs$Precision,Recall=subs$Recall,Runtime=subs$runtime)
      print(tf)
      evalSummary <- rbind(evalSummary,tf)
    }
    row <- row + 1
  }
  if(refIdx<length(references)) {
    addToRow.pos[[refIdx]] <- row
    addToRow.command <- c(addToRow.command,"\\midrule 
  ")
  }
}
#hlines <- c(hlines)
print(nrow(evalSummary))
hlines <- c(-1,0,nrow(evalSummary))
#rownames(evalSummary) <- rnames
#latex(evalSummary, rowname=NULL,na.blank=TRUE, booktabs=TRUE, table.env=FALSE, center="none", file="evalSummary.tex", title="")
tbl <- xtable(evalSummary, caption=c("",""), align=c("c","c","r","l","l","l","l"),digits=c(0,0,0,4,4,4,2),display=c("s","s","s","f","f","f","f"))
if(length(addToRow.pos)==0) {
  print(tbl,file="evalSummary.tex",caption.placement="top",booktabs=T,include.rownames=FALSE,hline.after=hlines,tabular.environment="tabularx",floating=TRUE,floating.environment="table*")
}else{
  print(tbl,file="evalSummary.tex",caption.placement="top",booktabs=T,include.rownames=FALSE,hline.after=hlines,add.to.row=list(pos=addToRow.pos,command=addToRow.command),tabular.environment="tabularx",floating=TRUE,floating.environment="table*")
}
