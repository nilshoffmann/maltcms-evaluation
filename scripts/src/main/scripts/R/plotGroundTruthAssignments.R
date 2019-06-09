#!/usr/bin/env Rscript
library(ggplot2)
library(tidyverse)
library(ggrepel)
library(grid)
library(gridExtra)
library(egg)
library(gtable)
library("RColorBrewer")

suppressPackageStartupMessages(library("optparse"))
option_list <- list(make_option("--basedir", default=".",
            help="The base directory to resolve relative input data against. [default \"%default\"]"),
	    make_option("--name", default="mSPA_Dataset_I",
            help="The ground truth instance name. [default \"%default\"]"),
	    make_option("--outdir", default="output",
            help="The output directory to store plots in. [default \"%default\"]")
)
options(error=traceback)
opt <- parse_args(OptionParser(option_list=option_list))
call.dir <- getwd()
script.name <- "plotGroundTruthAssignments.R"
script.basedir <- call.dir
initial.options <- commandArgs(trailingOnly = FALSE)
basedir <- opt$basedir
gtName <- opt$name
outdir <- opt$outdir
if(!dir.exists(file.path(outdir))) { 
  dir.create(file.path(outdir))
}

toLongFormat <- function(tibble, newcolname) {
  tibble_cols <- colnames(tibble)
  tibble_names <- tibble::rowid_to_column(tibble, "ID")
  tibble_namesLong <- tibble_names %>% tidyr::gather(File, !!newcolname, tibble_cols)
  tibble_namesLong
}

toLongFormatName <- function(tibble, newcolname) {
  tibble_names_select <- tibble %>% select(Name, suspicious)
  tibble_names_select
}

combineIt <- function(dir, suffix="") {
  refAlignment_names <- readr::read_tsv(file.path(dir, paste0("reference-alignment-names",suffix,".txt")))
  refAlignment_namesLong <- toLongFormat(refAlignment_names, "Name")
  refAlignment_rt1 <- readr::read_tsv(file.path(dir, paste0("reference-alignment-rt1",suffix,".txt")))
  refAlignment_rt1Long <- toLongFormat(refAlignment_rt1, "RT1")
  refAlignment_rt2 <- readr::read_tsv(file.path(dir, paste0("reference-alignment-rt2",suffix,".txt")))
  refAlignment_rt2Long <- toLongFormat(refAlignment_rt2, "RT2")
  refAlignment_outlier <- readr::read_csv(file.path(dir, "compoundGroupStatsAll.txt"), skip = 0)
  colnames(refAlignment_outlier) <- c("row","Name","t1Stdev","t2Stdev","t1Mean","t2Mean","t1Median","t2Median","areaStdev","size","suspicious")
  refAlignment_outlierLong <- toLongFormatName(refAlignment_outlier, "suspicious")
  refAlignment_all <- refAlignment_namesLong %>% dplyr::full_join(refAlignment_rt1Long, by=c("ID","File")) %>% dplyr::full_join(refAlignment_rt2Long, by=c("ID","File")) %>% dplyr::inner_join(refAlignment_outlierLong, by=c("Name")) %>% na.omit()
  #View(refAlignment_all)
  refAlignment_all
}

plotBoth <- function(tibble1, name1, tibble2, name2) {
  xmin <- min(tibble1$RT1, tibble2$RT1)
  xmax <- max(tibble1$RT1, tibble2$RT1)
  ymin <- min(tibble1$RT2, tibble2$RT2)
  ymax <- max(tibble1$RT2, tibble2$RT2)
  featureNames <- unique(sort(c(tibble1$Name, tibble2$Name)))
  p1 <- plotIt(tibble1, name1, xmin, xmax, ymin, ymax, featureNames)
  p2 <- plotIt(tibble2, name2, xmin, xmax, ymin, ymax, featureNames)
  list(p1=p1,p2=p2)
}

plotIt <- function(tibble, name, xmin, xmax, ymin, ymax, featureNames) {
  hull_aligned_groups <- tibble %>%
    group_by(Name) %>%
    slice(chull(RT1, RT2))
  
  hull_aligned_groups_centers <- hull_aligned_groups %>% group_by(Name) %>% filter(suspicious==TRUE) %>% summarise(RT1C=mean(RT1),RT2C=mean(RT2))

  nitems <- length(unique(featureNames))
  compoundNames <- sort(unique(featureNames))
  
  shapes <- rep(x=0:14,length=as.numeric(nitems))
  legend.position <- "none"
  glegend <- guide_legend(ncol=5,"Compound",title.position="top")
  
  
  #print(
  aplot <- ggplot(data=tibble,aes(x=RT1,y=RT2,colour=Name,shape=Name))+geom_point(alpha=0.5)+
    xlab("RT1 [s]")+ylab("RT2 [s]")+
    scale_shape_manual(values=shapes,limits=featureNames,drop=FALSE,labels=featureNames)+
    scale_color_manual(values = rep(c(brewer.pal(5, "Set1"),brewer.pal(9,"Set1")[7:9]),length=length(featureNames)),drop=FALSE,limits=featureNames,labels=featureNames)+
    scale_fill_manual(values = rep(c(brewer.pal(5, "Set1"),brewer.pal(9,"Set1")[7:9]),length=length(featureNames)),drop=FALSE,limits=featureNames,labels=featureNames)+
    guides(shape = glegend,colour = glegend, fill= glegend)+
    xlim(xmin,xmax)+ylim(ymin,ymax)+ labs(title = paste0("Peak grouping for ", name)) +
    aes(fill = Name) + geom_polygon(data = hull_aligned_groups, alpha = 0.5) + geom_text_repel(data = hull_aligned_groups_centers, aes(label = Name, x=RT1C, y=RT2C))+
    theme(legend.position=legend.position)
  #)
  #dev.off()
  aplot
}

arrangeIt <- function(plotList) {
  g2 <- ggplotGrob(plotList$p1)
  g3 <- ggplotGrob(plotList$p2)
  g <- rbind(g2, g3, size = "first")
  g$widths <- unit.pmax(g2$widths, g3$widths)
  #grid.newpage()
  grobG <- grid.draw(g)
  grobG
}

saveIt <- function(gridGrob, name, width=20, height=12) {
  pdf(paste0(name,".pdf"),width=width,height=height)
  print(gridGrob)
  dev.off()
}

gtaMspaI <- plotBoth(
  combineIt(
    file.path(basedir)),
  paste0("ground-truth-assignments-",gtName),
  combineIt(
    file.path(basedir),
    suffix="-mgma"
  ),
  paste0("ground-truth-assignments-",gtName,"-mgma")
)
setwd(outdir)
saveIt(arrangeIt(gtaMspaI), name = gtName)
setwd(call.dir)
