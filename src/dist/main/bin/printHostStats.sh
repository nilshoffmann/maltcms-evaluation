#!/usr/bin/env bash
hostname > ulim.txt
echo "##################" >> ulim.txt
ulimit -a >> ulim.txt

cat /proc/cpuinfo >> cpuinfo.txt
