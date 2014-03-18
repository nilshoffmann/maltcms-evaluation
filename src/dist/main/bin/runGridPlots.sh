#!/usr/bin/env bash
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

# Maltcms startup script for Unix/MacOS X/Linux, adapted from www.gradle.org gradle.sh

function printHelp {
    printf "%-70s\n" "Usage: $1 [OPTION]"
    printf "%4s %-10s %-54s\n" "" "-d ARG" "use workdir ARG"
    printf "%4s %-10s %-54s\n" "" "-e ARG" "run evaluation script ARG (default: BiPace2DEvaluation.groovy)"
    printf "%4s %-10s %-54s\n" "" "-l" "list available instances"
    printf "%4s %-10s %-54s\n" "" "-n ARG" "run instance ARG"
    printf "%4s %-10s %-54s\n" "-?" "--help" "display this help and exit"
    exit 1
}

SCRIPTFILE="$0"
SCRIPTNAME="$(basename $0)"
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
EVALUATIONSCRIPT=""
OUTPUTBASEDIR="$SCRIPTDIR/../results"
CFGENVIRONMENT="cluster"
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
#        -s)
#            shift
#            SETTINGS="$1"
#            shift
#            ;;
        --help|-h|-\?)
            printHelp $SCRIPTNAME
            ;;
        *)
            printHelp $SCRIPTNAME
            ;;
    esac
done

if [ ! -z "$LISTINSTANCES" ] ; then
    echo "Retrieving available instances!"
else
    if [ -z "$INSTANCENAME" ] ; then
        printHelp $SCRIPTNAME
    fi
fi

#change to work directory
if [ -z "$WORKDIR" ]; then
    WORKDIR="$SCRIPTDIR/.."
fi
WORKDIR="$(readlink -f $WORKDIR)"
echo "Using working dir $WORKDIR"
echo "Using output dir $OUTPUTBASEDIR"
echo "Using profile $CFGENVIRONMENT"
echo "Running instance $INSTANCENAME"

cd "$WORKDIR"
#You may need to adapt the following line to suit your grid environment
#if [ -z "$EVALUATIONSCRIPT" ] ; then
#	qsub -V -cwd -j y -b y -pe multislot 8 -l "arch=lx24-amd64,mem_free=20G" -o "$INSTANCENAME.out" -N "$INSTANCENAME" "$SCRIPTDIR/runEvaluation.sh" -d "$WORKDIR" -n "$INSTANCENAME" -o "$OUTPUTBASEDIR" -p "$CFGENVIRONMENT"
#else
#	qsub -V -cwd -j y -b y -pe multislot 8 -l "arch=lx24-amd64,mem_free=20G" -o "$INSTANCENAME.out" -N "$INSTANCENAME" "$SCRIPTDIR/runEvaluation.sh" -d "$WORKDIR" -n "$INSTANCENAME" -e "$EVALUATIONSCRIPT" -o "$OUTPUTBASEDIR" -p "$CFGENVIRONMENT"
#fi
if [ -z "$EVALUATIONSCRIPT" ] ; then
	qsub -V -cwd -j y -b y -pe multislot 32 -q all.q@@sucslin -l "arch=lx24-amd64,mem_free=16G" -o "$INSTANCENAME.out" -N "$INSTANCENAME" "$SCRIPTDIR/createPlots.sh" -d "$WORKDIR" -n "$INSTANCENAME" -o "$OUTPUTBASEDIR" -p "$CFGENVIRONMENT"
else
	qsub -V -cwd -j y -b y -pe multislot 32 -q all.q@@sucslin -l "arch=lx24-amd64,mem_free=16G" -o "$INSTANCENAME.out" -N "$INSTANCENAME" "$SCRIPTDIR/createPlots.sh" -d "$WORKDIR" -n "$INSTANCENAME" -e "$EVALUATIONSCRIPT" -o "$OUTPUTBASEDIR" -p "$CFGENVIRONMENT"
fi

