createIds <- function(tables,useRowIdx=FALSE) {
  ids <- list()
  ncols <- 0
  for(i in 1:length(tables)) {
    table <- tables[[i]]
    if(ncols==0) {
      ncols = ncol(table)
    }else{
      stopifnot(ncols==ncol(table))
    }
    idFrame <- data.frame()
    columnnames <- colnames(table)
    for(j in 1:ncol(table)) {
      if(useRowIdx) {
        idFrame <- rbind(idFrame,data.frame(id=createIdWithRow(columnnames[j],table[,j],seq(1:nrow(table))),tableIdx=i,columnName=columnnames[j],rowIdx=seq(1:nrow(table))))
      }else{
        nonNas <- which(!is.na(table[,j]))
        #nonNas
        idFrame <- rbind(idFrame,data.frame(id=createId(columnnames[j],table[nonNas,j]),tableIdx=i,columnName=columnnames[j],rowIdx=nonNas))
      }
      #idFrame[,j] <- as.factor(idFrame[,j])
    }
    ids[[i]] <- idFrame
  }
  names(ids) <- names(tables)
  ids
}

createIdWithRow <- function(filename, idx, rows) {
  as.factor(paste(filename,idx,rows,sep="-"))
}

createId <- function(filename, idx) {
  #idx <- na.omit(idx)
  as.factor(paste(filename,idx,sep="-"))
}

createTestData <- function() {
  
  df1 <- data.frame()
  df1c1 <- c(1,4,5,NA,29,NA,2,55,3)
  df1c2 <- c(3,4,7,9,24,NA,35,32,5)
  df1c3 <- c(1,3,6,NA,23,2,8,34,4)
  df1 <- cbind(A=df1c1,B=df1c2,C=df1c3)
  df2 <- data.frame()
  df2c1 <- c(NA,4,5,17,29,3,2,55,3,62)
  df2c2 <- c(3,4,NA,9,24,NA,36,32,5,NA)
  df2c3 <- c(1,3,5,NA,23,2,8,34,4,76)
  df2 <- cbind(A=df2c1,B=df2c2,C=df2c3)
  df3 <- data.frame()
  df3c1 <- c(6,4,5,17,29,3,2,55,3)
  df3c2 <- c(NA,4,2,9,24,NA,36,32,5)
  df3c3 <- c(1,2,5,NA,23,NA,8,33,4)
  df3 <- cbind(B=df3c2,A=df3c1,C=df3c3)
  df4 <- data.frame()
  df4c1 <- c(6,4,5,17,29,3,2,55,8,62)
  df4c2 <- c(NA,4,2,9,24,NA,36,32,5,82)
  df4c3 <- c(1,2,5,NA,23,NA,8,33,4,76)
  df4 <- cbind(C=df3c3,A=df3c1,B=df3c2)
  
  list(Reference=df1,Table2=df2,Table3=df3,Table4=df4)
}

createAllInstances <- function() {
  A <- seq(1:95)
  B <- seq(1:97)
  C <- seq(1:89)
  df <- data.frame()
  df <- rbind(df, data.frame(id=createId("A",A)))
  df <- rbind(df,data.frame(id=createId("B",B)))
  df <- rbind(df,data.frame(id=createId("C",C)))
  df$id
}

readAlignmentTables <- function(files,labels) {
  tables <- list()
  stopifnot(length(files)==length(labels))
  for(i in 1:length(files)) {
    tables[[i]] <- read.table(files[i],sep="\t",header=TRUE,stringsAsFactors=FALSE,na.strings=c("-","NA","NaN"))
  }
  names(tables) = labels
  tables
}

createVennPlot <- function(allInstanceIds,tables,useRowIdx=FALSE, plotName="venn-diagram") {
  if((length(tables)+1)>5) {
    stop("!!! VennDiagram only supports up to 5 sets !!!")
  }
  idList <- createIds(tables,useRowIdx)
  vennIdList <- list()
  #vennIdList[[1]] <- allInstanceIds
  setDiffs <- list()
  setInters <- list()
  for(i in 1:length(idList)) {
    vennIdList[[i]] <- idList[[i]]$id
  }
  names(vennIdList) <- c(names(idList))#c("All",names(idList))
  idx <- 1
  setDiffNames <- c()
  setIntersNames <- c()
  for(i in 1:length(vennIdList)) {
    for(j in 1:length(vennIdList)) {
        setDiffs[[idx]] <- setdiff(vennIdList[[i]],vennIdList[[j]])
        setDiffNames <- c(setDiffNames,paste(names(vennIdList)[i],"and",names(vennIdList)[j],sep="-"))
        setInters[[idx]] <- intersect(vennIdList[[i]],vennIdList[[j]])
        setIntersNames <- c(setIntersNames,paste(names(vennIdList)[i],"and",names(vennIdList)[j],sep="-"))
        idx = idx+1
    }
  }
  print(setDiffNames)
  names(setDiffs) <- setDiffNames
  for(i in 1:length(setDiffs)) {
    print(setDiffs[[i]])
    write.table(as.data.frame(setDiffs[[i]]),file=paste(names(setDiffs)[i],".txt",sep=""),sep="\t")
  }
  names(setInters) <- setIntersNames
  for(i in 1:length(setInters)) {
    print(setInters[[i]])
    write.table(as.data.frame(setInters[[i]]),file=paste(names(setInters)[i],".txt",sep=""),sep="\t")
  }
  #print(setDiffs)

  #print(vennIdList[[1]])
  library("VennDiagram")
  library("RColorBrewer")
  vdplot <- venn.diagram(x=vennIdList,
   filename = NULL,
   width = 8,
   height = 5,
   units = "in",
   resolution = 300,
   scaled = FALSE,
   ext.text = TRUE,
   ext.line.lwd = 2,
   ext.dist = -0.15,
   ext.length = 0.9,
   ext.pos = -4,
   #inverted = TRUE,
   cex = 1.5,
   #cat.cex = 2.5,
   #cat.pos = 0,
   #rotation.degree = 0,
   #main = "Complex Venn Diagram",
   #sub = "Featuring: rotation and external lines",
   main.cex = 2,
   main.fontfamily = "sans",
   sub.fontfamily = "sans",
   sub.cex = 1,
   col = "transparent",
   fill = c(brewer.pal(length(vennIdList),"Set1"))[1:length(vennIdList)],
   alpha = 0.50,
   fontfamily = "sans",
   cat.fontfamily = "sans",
  )
  
  pdf(paste(plotName,".pdf",sep=""),width=8,height=5)
  grid.draw(vdplot)
  dev.off()
  png(paste(plotName,".png",sep=""),width=8,height=5,res=300,units="in")
  grid.draw(vdplot)
  dev.off()
  idList
}
