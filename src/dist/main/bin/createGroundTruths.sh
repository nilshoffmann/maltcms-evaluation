#!/usr/bin/env bash
SCRIPT=$(readlink -f "$0")
SCRIPTDIR=$(dirname "$SCRIPT")
BASEDIR=$(readlink -f "$SCRIPTDIR/../")
echo "Script is at $SCRIPT"
echo "Basedir is $BASEDIR"
echo "Creating ground truth for mSPA_Dataset_I"
$SCRIPTDIR/createGroundTruth_mSPA_Dataset_I.sh > mSPA_Dataset_I.log 2>&1 
wait
echo "Creating ground truth for mSPA_Dataset_II"
$SCRIPTDIR/createGroundTruth_mSPA_Dataset_II.sh > mSPA_Dataset_II.log 2>&1 
wait
echo "Creating ground truth for SWPA_Dataset_I"
$SCRIPTDIR/createGroundTruth_SWPA_Dataset_I.sh > SWPA_Dataset_I.log 2>&1 
wait
echo "Creating ground truth for chlamy_Dataset_I"
createGroundTruth_chlamy_Dataset_I.sh > chlamy_Dataset_I.log 2>&1
wait
