#!/usr/bin/env bash
#export JMX management bean for vm monitoring
JMX_PORT=17400
#settings for the groovy evaluation script
JAVA_OPTS="-d64 -Xmx32G -XX:MaxPermSize=4G -Xverify:none -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
export JAVA_OPTS
#optional set R_LIBS location
R_LIBS="/vol/cluster-data/hoffmann/r-modules"
export R_LIBS
#SGE settings to use correct native drmaa bindings
SGE_ROOT=/vol/codine-6.2; export SGE_ROOT

ARCH=`$SGE_ROOT/util/arch`
DEFAULTMANPATH=`$SGE_ROOT/util/arch -m`
MANTYPE=`$SGE_ROOT/util/arch -mt`

SGE_CELL=default; export SGE_CELL
SGE_CLUSTER_NAME=codine62; export SGE_CLUSTER_NAME
SGE_QMASTER_PORT=6444; export SGE_QMASTER_PORT
SGE_EXECD_PORT=6445; export SGE_EXECD_PORT

if [ "$MANPATH" = "" ]; then
	   MANPATH=$DEFAULTMANPATH
fi
MANPATH=$SGE_ROOT/$MANTYPE:$MANPATH; export MANPATH

PATH=$SGE_ROOT/bin/$ARCH:$PATH; export PATH
# library path setting required only for architectures where RUNPATH is not supported
shlib_path_name=`$SGE_ROOT/util/arch -lib`
old_value=`eval echo '$'$shlib_path_name`
if [ x$old_value = x ]; then
      eval $shlib_path_name=$SGE_ROOT/lib/$ARCH
else
      eval $shlib_path_name=$SGE_ROOT/lib/$ARCH:$old_value
fi
export $shlib_path_name
unset shlib_path_name old_value
unset ARCH DEFAULTMANPATH MANTYPE

echo $ARCH
echo $LD_LIBRARY_PATH
	 
