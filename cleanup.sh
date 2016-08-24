#!/bin/bash


# Change this to your netid
netid=$2

#
# Root directory of your project
PROJDIR=$(pwd)

#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
CONFIG=$1

#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
PROG=Project2

n=1

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" | awk '{ print $2 }' | \
while read i;
do
if [ "$i" != "$netid" ] && [[ "$i" == *"dc"* ]]; then
ssh $netid@$i.utdallas.edu "killall -9 -u $netid" &
fi
done
echo "Cleanup complete"

