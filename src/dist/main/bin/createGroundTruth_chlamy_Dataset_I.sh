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
NAME="chlamy_Dataset_I"
################################################################
SD1THRES="--sd1Thres=250.0"
SD2THRES="--sd2Thres=0.5"
PREFIX="--filePrefix=^mut_*|^wt_*"
GTDIR="$BASEDIR/chlamy/groundTruth/$NAME"
DPATH="--dataPath=$BASEDIR/chlamy/data/$NAME"
OUTDIR="--directory=$NAME"
$BASEDIR/mSPA/mspa/mspa-evaluation.R $FIXED $PREFIX $DPATH $OUTDIR $SD1THRES $SD2THRES $VARIANT
$BASEDIR/scripts/src/main/scripts/R/createVennDiagram.R --table2=$GTDIR/reference-alignment.txt --table3=$GTDIR/reference-alignment-mgma.txt --table1=$GTDIR/../manualReference/multiple-alignment-manual.txt --labels=MANUAL,mSPA-GMA,MGMA --suffix=$NAME --outdir=$BASEDIR/$NAME
$BASEDIR/scripts/src/main/scripts/R/plotGroundTruthAssignments.R --basedir=$GTDIR --name=$NAME --outdir=$BASEDIR/$NAME
wait
