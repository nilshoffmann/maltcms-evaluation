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
option_list <- list(make_option("--table1", default="NULL",
            help="Table containing aligned entities identified by a positive integer, one group of aligned features per row. [default \"%default\"]"),
make_option("--table2", default="NULL",
            help="Table containing aligned entities identified by a positive integer, one group of aligned features per row. [default \"%default\"]"),
make_option("--table3", default="NULL",
            help="Table containing aligned entities identified by a positive integer, one group of aligned features per row. [default \"%default\"]"),
make_option("--table4", default="NULL",
            help="Table containing aligned entities identified by a positive integer, one group of aligned features per row. [default \"%default\"]"),
make_option("--table5", default="NULL",
            help="Table containing aligned entities identified by a positive integer, one group of aligned features per row. [default \"%default\"]"),
make_option("--labels", default="NULL",
            help="Comma separated list of labels. [default \"%default\"]"),
make_option("--sep", default="\t",
            help="Separator to use while parsing. [default \"%default\"]"),
make_option("--suffix", default="NULL",
	    help="Suffix for plot file names. [default \"%default\"]"),
make_option("--outdir", default="",
            help="The output directory to store plots in. [default \"%default\"]")	    
)
options(error=traceback)
opt <- parse_args(OptionParser(option_list=option_list))
call.dir <- getwd()
script.name <- "createVennDiagram.R"
script.basedir <- call.dir
initial.options <- commandArgs(trailingOnly = FALSE)
#if we were called as a script from the command line
#--file gives the script to execute
if(length(grep("--file",commandArgs(trailingOnly = FALSE)))>0) {
  cat("We were called as a script and not sourced!\n")
  file.arg.name <- "--file="
  script.name <- sub(file.arg.name, "", initial.options[grep(file.arg.name, initial.options)])
  if(length(grep("^/{1}",script.name))==0) {
    cat("Relative path\n")
    script.basedir <- dirname(paste(sep="/",call.dir,script.name))
  }else{
    cat("Absolute path\n")
    script.basedir <- dirname(script.name)
  }
}
vennDiagram.script <- paste(sep="/", script.basedir, "vennDiagram.R")
outdir <- opt$outdir
if(!dir.exists(file.path(outdir))) { 
  dir.create(file.path(outdir))
}
#setwd(opt$directory)
sep <- opt$sep
file.suffix = opt$suffix
tables <- c()
if(opt$table1!="NULL") {
  tables <- c(tables,opt$table1)
}
if(opt$table2!="NULL") {
  tables <- c(tables,opt$table2)
}
if(opt$table3!="NULL") {
  tables <- c(tables,opt$table3)
}
if(opt$table4!="NULL") {
  tables <- c(tables,opt$table4)
}
if(opt$table5!="NULL") {
  tables <- c(tables,opt$table5)
}
labels <- opt$labels
if(labels=="NULL") {
  labels = seq(1,length(tables),by=1)
}else{
  splits <- unlist(strsplit(labels,","))
  print(splits)
  labels <- c()
  for(i in 1:length(splits)) {
    labels = c(labels,splits[[i]])
    print(labels)
  }
}
source(vennDiagram.script)
setwd(outdir)
alignmentTables <- readAlignmentTables(tables,labels)
instanceIds <- createIds(alignmentTables)
createVennPlot(allInstanceIds=instanceIds,tables=alignmentTables,plotName=paste0("venn-diagram.",file.suffix))
setwd(call.dir)
