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
    printf "%4s %-10s %-54s\n" "" "-p ARG" "configuration profile to use (default: local, other: cluster, cluster-cebitec)"
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
. "$SCRIPTDIR/checkGnuTools.sh"
EVALUATIONSCRIPTDEFAULT="BiPace2DEvaluation.groovy"
OUTPUTBASEDIR="$SCRIPTDIR/../results"
CFGENVIRONMENT="local"
echo "Script is in $SCRIPTDIR"
while [ $# -gt 0 ]; do
    case "$1" in
        -e)
            shift
            EVALUATIONSCRIPT="$1"
            shift
            ;;
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
        -o) 
			      shift
            OUTPUTBASEDIR="$1"
            shift
            ;;
        -p)
			      shift
            CFGENVIRONMENT="$1"
            shift
            ;;
#		-s)
#			shift
#			SETTINGS="$1"
#			shift
#			;;
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
if [[ `uname` == 'Darwin' ]]; then
    WORKDIR="$(greadlink -f $WORKDIR)"
else
    WORKDIR="$(readlink -f $WORKDIR)"
fi

echo "Using working dir $WORKDIR"
echo "Storing output below $OUTPUTBASEDIR"

#set evaluation script default, if no evaluationscript variable is defined
if [ -z "$EVALUATIONSCRIPT" ] ; then
	EVALUATIONSCRIPT="$EVALUATIONSCRIPTDEFAULT"
fi
#
if [ ! -z "$LISTINSTANCES" ] ; then
	while IFS=$'\t' read -r -a lineArray
	do
		echo "${lineArray[0]}"
	done < "$WORKDIR/etc/instances.txt"
	exit 1
else
	if [ ! -z "$EVALUATIONSCRIPT" ] ; then	
		echo "Retrieving evaluation script from etc/instances.txt"
		while IFS=$'\t' read -r -a lineArray
		do
			if [ ${lineArray[0]} == $INSTANCENAME ] ; then
				EVALUATIONSCRIPT=${lineArray[1]}
			fi
		done < "$WORKDIR/etc/instances.txt"
	else
		echo "Using user-defined evaluation script $EVALUATIONSCRIPT"
	fi
fi

if [ ! -f "$WORKDIR/scripts/src/main/scripts/$EVALUATIONSCRIPT" ] ; then
	echo "Requested evaluation script does not exist at location: $WORKDIR/scripts/src/main/scripts/$EVALUATIONSCRIPT !"
	exit 1
fi

echo "Starting up..."

SETTINGS="settings-$CFGENVIRONMENT.sh"

if [ -f "$WORKDIR/etc/$SETTINGS" ]; then
    echo $(arch)
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

echo -e "Running on host: $HOSTNAME"

gradle build
retval=$?
if [ $retval != 0 ] ; then
    echo "Build failed with value $retval"
    exit $retval
fi
MALTCMSHOME=maltcms/maltcms.jar
GROOVYCP=build/libs/bipace2d-eval.jar
GROOVY_HOME="$(pwd)/groovy"
chmod u+x groovy/bin/*
chmod u+x ../SWPA/swpa/*.R
chmod u+x ../mSPA/mspa/*.R
#echo -e "Maltcms location: ${MALTCMSHOME}"
#echo -e "Groovy location: ${GROOVYCP}"
groovy/bin/groovy -cp "$MALTCMSHOME:$GROOVYCP" -Djava.util.logging.config.file="../maltcms/cfg/logging.properties" "src/main/scripts/$EVALUATIONSCRIPT" -c "src/main/scripts/cfg/$INSTANCENAME.groovy" -b . -o "$OUTPUTBASEDIR" -e "$CFGENVIRONMENT"
retval=$?
if [ $retval != 0 ] ; then
    echo "Execution failed with value $retval"
    exit $retval
fi
#create tables
#groovy/bin/groovy -cp "$MALTCMSHOME:$GROOVYCP" "src/main/scripts/CreateTables.groovy" -c "src/main/scripts/cfg/$INSTANCENAME.groovy" -b . -o "$OUTPUTBASEDIR" -e "$CFGENVIRONMENT"
#retval=$?
#if [ $retval != 0 ] ; then
#    echo "Execution failed with value $retval"
#    exit $retval
#fi
#merge tables
#chmod u+x src/main/scripts/R/*.R
#src/main/scripts/R/createEvaluationTable.R --dir="$OUTPUTBASEDIR/$INSTANCENAME/evaluation/"
#retval=$?
#if [ $retval != 0 ] ; then
#    echo "Execution failed with value $retval"
#    exit $retval
#fi
