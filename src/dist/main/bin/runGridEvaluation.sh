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
	qsub -V -cwd -j y -b y -pe multislot 32 -q all.q@@sucslin -l "arch=lx24-amd64,mem_free=16G" -o "$INSTANCENAME.out" -N "$INSTANCENAME" "$SCRIPTDIR/runEvaluation.sh" -d "$WORKDIR" -n "$INSTANCENAME" -o "$OUTPUTBASEDIR" -p "$CFGENVIRONMENT"
else
	qsub -V -cwd -j y -b y -pe multislot 32 -q all.q@@sucslin -l "arch=lx24-amd64,mem_free=16G" -o "$INSTANCENAME.out" -N "$INSTANCENAME" "$SCRIPTDIR/runEvaluation.sh" -d "$WORKDIR" -n "$INSTANCENAME" -e "$EVALUATIONSCRIPT" -o "$OUTPUTBASEDIR" -p "$CFGENVIRONMENT"
fi

