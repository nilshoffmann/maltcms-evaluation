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
option_list <- list(
    make_option("--table", default="evaluation.csv",
    help="Table containing one tool parameterization and classification results per row and multiple traits (columns),
    first row contains column names, first column (without header) contains instance names. [default \"%default\"]"),
    make_option("--directory", default=".",
        help="Base directory for output. [default \"%default\"]")
)

opt <- parse_args(OptionParser(option_list=option_list))
#print options
#print(opt)

setwd(opt$directory)

evaluation <- read.table(opt$table)
evaluation$runtime <- evaluation$runtime/60.0
suppressPackageStartupMessages(library("ggplot2"))
theme_set(theme_bw())
theme_update(
    axis.text.x = theme_text(angle=90),
    axis.title.x = theme_text(vjust=0.5,size=12),
    axis.title.y = theme_text(hjust=0.5,size=12, angle=90)
)
pointAlpha <- 100.0

#bipace plots
bipaceRobinson <- rbind(evaluation[grep(pattern="BiPACE",x=evaluation$name),],evaluation[grep(pattern="Robinson",
x=evaluation$name),])

bipace <- evaluation[grep(pattern="BiPACE",x=evaluation$name),]

#tp/fp plot
cat("Plot1","\n")
plot1 <- qplot(FP,TP,data=bipaceRobinson,colour=Similarity,shape=Similarity,alpha=pointAlpha) +
facet_grid(T ~ D, labeller="label_both") +
geom_rug() + scale_colour_brewer(palette="Set2") +
#scale_shape_manual(values=as.numeric(factor(bipaceRobinson$Similarity))) +
#5=number of different similarities
scale_shape_manual(values=1:5) +
scale_alpha(legend=FALSE)

ggsave(file = "bipace-fp-vs-tp-rtThres-rtTol.pdf", plot = plot1, width = 12, height=10)
ggsave(file = "bipace-fp-vs-tp-rtThres-rtTol.png", plot = plot1, width = 12, height = 10)

#tp/fp mcs plot
cat("Plot2","\n")
plot2 <- qplot(FP,TP,data=bipaceRobinson,colour=Similarity,shape=Similarity,alpha=pointAlpha) +
facet_grid(. ~ MCS, labeller="label_both") +
geom_rug() + scale_colour_brewer(palette="Set2") +
#scale_shape_manual(values=as.numeric(factor(bipaceRobinson$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(legend=FALSE)
ggsave(file = "bipace-fp-vs-tp-mcs.pdf", plot = plot2, width = 14, height = 5)
ggsave(file = "bipace-fp-vs-tp-mcs.png", plot = plot2, width = 14, height = 5)

#recall/precision plot
cat("Plot3","\n")
plot3 <- qplot(Recall,Precision,data=bipaceRobinson,
colour=Similarity,shape=Similarity,alpha=pointAlpha) + facet_grid(. ~ name) + geom_rug() +
scale_colour_brewer(palette="Set2") +
#scale_shape_manual(values=as.numeric(factor(bipaceRobinson$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(legend=FALSE)
ggsave(file = "bipace-recall-vs-precision.pdf", plot = plot3, width = 6, height = 5)
ggsave(file = "bipace-recall-vs-precision.png", plot = plot3, width = 6, height = 5)

#runtime histogram
cat("Plot4a","\n")
plot4 <- qplot(Similarity,runtime,data=bipace,geom="boxplot",colour=Similarity, ylab="Runtime (min)") +
facet_grid(. ~ name) +
scale_colour_brewer(palette="Set2", legend=FALSE)
ggsave(file = "bipace-runtime-histogram.pdf", plot = plot4, width = 6, height = 5)
ggsave(file = "bipace-runtime-histogram.png", plot = plot4, width = 6, height = 5)

#memory histogram
cat("Plot4b","\n")
plot4 <- qplot(Similarity,memory,data=bipace,geom="boxplot",colour=Similarity, ylab="Memory (MB)") +
facet_grid(. ~ name) +
scale_colour_brewer(palette="Set2", legend=FALSE)
ggsave(file = "bipace-memory-histogram.pdf", plot = plot4, width = 6, height = 5)
ggsave(file = "bipace-memory-histogram.png", plot = plot4, width = 6, height = 5)

#cemapp dtw plots
cemappDtw <- evaluation[grep(pattern="CeMAPP-DTW",x=evaluation$name),]
#tp/fp plot
#anchor usage vs D
cat("Plot5","\n")
plot5 <- qplot(FP,TP,data=cemappDtw, colour=Similarity, shape=Similarity, alpha=pointAlpha) +
facet_grid(D ~ DTW, labeller="label_both") + geom_rug() +
scale_colour_brewer(palette="Set2") +
#scale_shape_manual(values=as.numeric(factor(cemappDtw$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(legend=FALSE)
ggsave(file = "cemapp-fp-vs-tp-anchor-rtTol.pdf", plot = plot5, width = 6, height = 10)
ggsave(file = "cemapp-fp-vs-tp-anchor-rtTol.png", plot = plot5, width = 6, height = 10)

#matchWeight W vs anchorRadius R
cat("Plot6","\n")
plot6 <- qplot(FP,TP,data=cemappDtw, colour=Similarity, shape=Similarity, alpha=pointAlpha) +
facet_grid(W ~ R, labeller="label_both") + geom_rug() +
scale_colour_brewer(palette="Set2") +
#scale_shape_manual(values=as.numeric(factor(cemappDtw$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(legend=FALSE)
ggsave(file = "cemapp-fp-vs-tp-matchWeight-anchorRadius.pdf", plot = plot6, width = 6, height = 10)
ggsave(file = "cemapp-fp-vs-tp-matchWeight-anchorRadius.png", plot = plot6, width = 6, height = 10)

#band constraint BandConstraint vs band constraint width BC
cat("Plot7","\n")
plot7 <- qplot(FP,TP,data=cemappDtw, colour=Similarity, shape=Similarity, alpha=pointAlpha) +
facet_grid(BCScope ~ BC, labeller="label_both") + geom_rug() +
scale_colour_brewer(palette="Set2") +
#scale_shape_manual(values=as.numeric(factor(cemappDtw$Similarity))) +
scale_shape_manual(values=1:5) +
scale_alpha(legend=FALSE)
ggsave(file = "cemapp-fp-vs-tp-globalBand-BW.pdf", plot = plot7, width = 6, height = 10)
ggsave(file = "cemapp-fp-vs-tp-globalBand-BW.png", plot = plot7, width = 6, height = 10)

#runtime histogram
cat("Plot8a","\n")
plot8 <- qplot(Similarity,runtime,data=cemappDtw,geom="boxplot",colour=Similarity, ylab="Runtime (min)") +
facet_grid(DTW ~ name) +
scale_colour_brewer(palette="Set2", legend=FALSE)
ggsave(file = "cemapp-runtime-histogram.pdf", plot = plot8, width = 6, height = 5)
ggsave(file = "cemapp-runtime-histogram.png", plot = plot8, width = 6, height = 5)

#memory histogram
cat("Plot8b","\n")
plot8 <- qplot(Similarity,memory,data=cemappDtw,geom="boxplot",colour=Similarity, ylab="Memory (MB)") +
facet_grid(DTW ~ name) +
scale_colour_brewer(palette="Set2", legend=FALSE)
ggsave(file = "cemapp-memory-histogram.pdf", plot = plot8, width = 6, height = 5)
ggsave(file = "cemapp-memory-histogram.png", plot = plot8, width = 6, height = 5)
