#!/usr/bin/env bash
#
#
# Maltcms, modular application toolkit for chromatography-mass spectrometry.
# Copyright (C) 2008-2013, The authors of Maltcms. All rights reserved.
#
# Project website: http://maltcms.sf.net
#
# Maltcms may be used under the terms of either the
#
# GNU Lesser General Public License (LGPL)
# http://www.gnu.org/licenses/lgpl.html
#
# or the
#
# Eclipse Public License (EPL)
# http://www.eclipse.org/org/documents/epl-v10.php
#
# As a user/recipient of Maltcms, you may choose which license to receive the code
# under. Certain files or entire directories may not be covered by this
# dual license, but are subject to licenses compatible to both LGPL and EPL.
# License exceptions are explicitly declared in all relevant files or in a
# LICENSE file in the relevant directories.
#
# Maltcms is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. Please consult the relevant license documentation
# for details.
#

function printHelp {
    printf "%-70s\n" "Usage: $1 [OPTION]"
    printf "%4s %-10s %-54s\n" "" "-d ARG" "use workdir ARG"
    printf "%4s %-10s %-54s\n" "" "-e ARG" "run evaluation script ARG (default: BiPace2DEvaluation.groovy)"
    printf "%4s %-10s %-54s\n" "" "-l" "list available instances"
    printf "%4s %-10s %-54s\n" "" "-n ARG" "run instance ARG"
    printf "%4s %-10s %-54s\n" "" "-p ARG" "configuration profile to use (default: local, other: cluster)"
    printf "%4s %-10s %-54s\n" "-?" "--help" "display this help and exit"
    exit 1
}
SCRIPTFILE="$0"
SCRIPTNAME="$(basename $SCRIPTFILE)"
# Follow relative symlinks to resolve script location
while [ -h "$SCRIPTFILE" ] ; do
    ls=`ls -ld "$SCRIPTFILE"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        SCRIPTFILE="$link"
    else
        SCRIPTFILE=`dirname "$SCRIPTFILE"`"/$link"
    fi
done

#change to the resolved scriptfile location
cd "`dirname \"$SCRIPTFILE\"`"
SCRIPTDIR="`pwd -P`"
OUTPUTBASEDIR="$SCRIPTDIR/../results"
CFGENVIRONMENT="local"
echo "Script is in $SCRIPTDIR"
while [ $# -gt 0 ]; do
    case "$1" in
        -d)
            shift
            WORKDIR="$1"
            shift
            ;;
        -l)
            shift
            LISTINSTANCES="TRUE"
            ;;
        -n)
            shift
            INSTANCENAME="$1"
            shift
            ;;
        -o) shift
            OUTPUTBASEDIR="$1"
            shift
            ;;
        -p) shift
            CFGENVIRONMENT="$1"
            shift
            ;;
        --help|-h|-\?)
            printHelp $SCRIPTNAME
            exit 1
            ;;
        *)
            printHelp $SCRIPTNAME
            exit 1
            ;;
    esac
done

if [ ! -z "$LISTINSTANCES" ] ; then
    echo "Retrieving available instances!"
else
    if [ -z "$INSTANCENAME" ] ; then
        printHelp $SCRIPTNAME
        exit 1
    fi
fi
#change to work directory
if [ -z "$WORKDIR" ]; then
    WORKDIR="$SCRIPTDIR/.."
fi
WORKDIR="$(readlink -f $WORKDIR)"

echo "Using working dir $WORKDIR"
echo "Storing output below $OUTPUTBASEDIR"

#
if [ ! -z "$LISTINSTANCES" ] ; then
	while IFS=$'\t' read -r -a lineArray
	do
		echo "${lineArray[0]}"
	done < "$WORKDIR/etc/instances.txt"
	exit 1
fi

echo "Starting up..."

SETTINGS="settings-$CFGENVIRONMENT.sh"

if [ -f "$WORKDIR/etc/$SETTINGS" ]; then
    echo "Sourcing settings file $WORKDIR/etc/$SETTINGS"
    source "$WORKDIR/etc/$SETTINGS"
else
    JAVA_OPTS="-d64 -Xms16G -Xmx16G -XX:MaxPermSize=2G"
    echo "Exporting default JAVA_OPTS=$JAVA_OPTS"
    export JAVA_OPTS
fi

if [ ! -d "$WORKDIR/scripts" ]; then
    echo "Could not find 'scripts' directory in $WORKDIR"
    exit 1
else
    #change to scripts dir
    echo "Running scripts from below $WORKDIR/scripts"
    cd "$WORKDIR/scripts"
fi

#gradle build
#retval=$?
#if [ $retval != 0 ] ; then
#    echo "Build failed with value $retval"
#    exit $retval
#fi
MALTCMSHOME=maltcms/maltcms.jar
HSQLCP=lib/hsqldb.jar
GROOVYCP=build/libs/bipace2d-eval.jar
GROOVY_HOME="$(pwd)/groovy"
#echo -e "Maltcms location: ${MALTCMSHOME}"
#echo -e "Groovy location: ${GROOVYCP}"
#if you want to recreate tables, simply delete evaluation.csv in your evaluation output directory
chmod u+x src/main/scripts/R/*.R
chmod u+x groovy/bin/*

if [ ! -f "$OUTPUTBASEDIR/$INSTANCENAME/evaluation/evaluation.csv" ]; then
	echo "Creating tables"
	groovy/bin/groovy -cp "$MALTCMSHOME:$HSQLCP:$GROOVYCP" "src/main/scripts/CreateTables.groovy" -c "src/main/scripts/cfg/$INSTANCENAME.groovy" -b . -o "$OUTPUTBASEDIR" -e "$CFGENVIRONMENT"
	retval=$?
	if [ $retval != 0 ] ; then
	    echo "Execution failed with value $retval"
	    exit $retval
	fi
	echo "Merging tables"
	#merge tables
	src/main/scripts/R/createEvaluationTable.R --dir="$OUTPUTBASEDIR/$INSTANCENAME/evaluation/"
	retval=$?
	if [ $retval != 0 ] ; then
	    echo "Execution failed with value $retval"
	    exit $retval
	fi
  echo "Merging pairwise tables"
	#merge pairwise tables
  mkdir "$OUTPUTBASEDIR/$INSTANCENAME/evaluation/pairwise"
	src/main/scripts/R/createPairwiseEvaluationTable.R --dir="$OUTPUTBASEDIR/$INSTANCENAME/evaluation/pairwise"	--performance="../pairwise-performance.csv" --parameters="../parameters.csv" --executionMetrics="../executionMetrics.csv"
	retval=$?
	if [ $retval != 0 ] ; then
	    echo "Execution failed with value $retval"
	    exit $retval
	fi
else
	echo "Tables already exist, skipping generation! Delete 'evaluation.csv' and rerun this script to regenerate them!"
fi
echo "Plotting evaluation results"
src/main/scripts/R/plotResults2.R --dir="$OUTPUTBASEDIR/$INSTANCENAME/evaluation/" --tex=TRUE
retval=$?
if [ $retval != 0 ] ; then
    echo "Execution failed with value $retval"
    exit $retval
fi

echo "Plotting pairwise evaluation results"
src/main/scripts/R/plotPairwiseResults.R --dir="$OUTPUTBASEDIR/$INSTANCENAME/evaluation/pairwise" --tex=TRUE
retval=$?
if [ $retval != 0 ] ; then
    echo "Execution failed with value $retval"
    exit $retval
fi

