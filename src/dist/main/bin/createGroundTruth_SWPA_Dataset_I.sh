#!/usr/bin/env bash
SCRIPT=$(readlink -f "$0")
SCRIPTDIR=$(dirname "$SCRIPT")
BASEDIR=$(readlink -f "$SCRIPTDIR/../")
echo "Script is at $SCRIPT"
echo "Basedir is $BASEDIR"
VARIANT="--referenceAlignmentVariant=mspa"
FIXED="--createReferenceAlignment=TRUE --plot=TRUE"
#instances
################################################################
NAME="SWPA_Dataset_I"
################################################################
SD1THRES="--sd1Thres=800.0"
SD2THRES="--sd2Thres=0.5"
PREFIX="--filePrefix=STANDARD_"
GTDIR="$BASEDIR/SWPA/groundTruth/$NAME"
DPATH="--dataPath=$BASEDIR/SWPA/data/$NAME"
OUTDIR="--directory=$NAME"
$BASEDIR/mSPA/mspa/mspa-evaluation.R $FIXED $PREFIX $DPATH $OUTDIR $SD1THRES $SD2THRES $VARIANT
$BASEDIR/scripts/src/main/scripts/R/createVennDiagram.R --table1=$GTDIR/reference-alignment.txt --table2=$GTDIR/reference-alignment-mgma.txt --labels=SWPA,SWPA-MGMA --suffix=$NAME --outdir=$BASEDIR/$NAME
$BASEDIR/scripts/src/main/scripts/R/plotGroundTruthAssignments.R --basedir=$GTDIR --name=$NAME --outdir=$BASEDIR/$NAME
wait
