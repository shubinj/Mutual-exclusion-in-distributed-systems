#!/bin/bash


# Change this to your netid
netid=$2

#
# Root directory of your project
PROJDIR=$(pwd)

#
CONFIG=$1

#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
PROG=Application

n=1
javac -Xlint Application.java

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" | awk '{ print $1,$2 }' | \
while read i j;
do
if [ "$j" != "$netid" ] && [[ "$j" == *"dc"* ]]; then
ssh -o StrictHostKeyChecking=no $netid@$j.utdallas.edu "cd $PROJDIR;java $PROG $i $CONFIG > log-$i.out" &
fi
done
