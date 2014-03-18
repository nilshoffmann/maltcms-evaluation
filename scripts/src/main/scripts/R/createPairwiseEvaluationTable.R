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
    make_option("--parameters", default="parameters.csv",
    help="Path relative to dir to table containing one tool parameterization per row and multiple traits (columns),
    first row contains column names, first column (without header) contains instance names. [default \"%default\"]"),
    make_option("--performance", default="performance.csv",
        help="Path relative to dir to table containing one tool classification result per row and multiple traits
        (columns),
        first row contains column names, first column (without header) contains instance names. [default
        \"%default\"]"),
    make_option("--executionMetrics", default="executionMetrics.csv",
            help="Path relative to dir to table containing one tool execution metrics result per row and multiple
            traits
            (columns),
            first row contains column names, first column (without header) contains instance names/ids. [default
            \"%default\"]"),
    make_option("--directory", default=".",
        help="Base directory for output. [default \"%default\"]")
)

opt <- parse_args(OptionParser(option_list=option_list))
#print options
#print(opt)

setwd(opt$directory)
parameters <- read.table(opt$parameters,header=T)
performance <- read.csv(opt$performance,header=T,fill=T,row.names=NULL,sep="\t")
executionMetrics <- read.table(opt$executionMetrics,header=T)
#convert runtime to seconds
#merge by rownames
cases <- merge(performance,parameters,by="uid",all.x=T)
cases <- merge(cases,executionMetrics,by="uid",all.x=T)
cases$runtime <- cases$runtime/1000.0
cases$memory <- cases$memory/(1024.0*1024.0)
colnames(cases)[grep(pattern="arraySimilarity",x=colnames(cases))] <- "Similarity"
colnames(cases)[grep(pattern="similarityFunction",x=colnames(cases))] <- "compositeSimilarity"
colnames(cases)[grep(pattern="BandConstraint",x=colnames(cases))] <- "BCScope"
cases$Similarity <- sub(pattern="Similarity",replacement="",cases$Similarity,fixed=T)
cases$Similarity <- sub(pattern="lp",replacement="euclidean",cases$Similarity,fixed=T)
cases$name <- sub(pattern="robinsonAuto",replacement="Robinson w/ RT",cases$name,fixed=T)
cases$name <- sub(pattern="bipacePlain",replacement="BiPACE",cases$name,fixed=T)
cases$name <- sub(pattern="bipaceRt",replacement="BiPACE w/ RT",cases$name,fixed=T)
cases$name <- sub(pattern="bipace2DInv",replacement="BiPACE w/ 2D RT Inv",cases$name,fixed=T)
cases$name <- sub(pattern="bipace2D",replacement="BiPACE w/ 2D RT",cases$name,fixed=T)
cases$name <- sub(pattern="cemappDtwPlain",replacement="CeMAPP-DTW",cases$name,fixed=T)
cases$name <- sub(pattern="cemappDtwRt",replacement="CeMAPP-DTW w/ RT",cases$name,fixed=T)
cases$name <- sub(pattern="mspa-pad",replacement="mSPA PAD",cases$name,fixed=T)
cases$name <- sub(pattern="mspa-pas",replacement="mSPA PAS",cases$name,fixed=T)
cases$name <- sub(pattern="mspa-pam",replacement="mSPA PAM",cases$name,fixed=T)
cases$name <- sub(pattern="mspa-swpad",replacement="mSPA SWPAD",cases$name,fixed=T)
cases$name <- sub(pattern="mspa-dwpas",replacement="mSPA DWPAS",cases$name,fixed=T)
cases$name <- sub(pattern="swpa-swrm",replacement="SWPA SWRM",cases$name,fixed=T)
cases$name <- sub(pattern="swpa-swre",replacement="SWPA SWRE",cases$name,fixed=T)
cases$name <- sub(pattern="swpa-swrme",replacement="SWPA SWRME",cases$name,fixed=T)
cases$name <- sub(pattern="swpa-swrme2",replacement="SWPA SWRME2",cases$name,fixed=T)
cases$name <- sub(pattern="guineu",replacement="Guineu",cases$name,fixed=T)
write.table(cases,file="evaluation.csv",sep="\t",row.names=F)
