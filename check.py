import sys
import shlex
import subprocess
import os
import commands

configFile = sys.argv[1]
c=0
f=open(configFile, "r")
g=configFile.split(".")
#print g[0]
for line in open(configFile, "r"):
    li=line.strip()
    if not li.startswith(" "):
        if not li.startswith("#"):
            if c==0:
                if li.__contains__("#"):
                    info,other=li.split("#")
                    nodes,msg,delay=info.split()
                else:
                    nodes,msg,delay=li.split()
                c=1
                #print nodes
                #print msg
                #print delay
            else:
                break
f.close()

"""command1="diff config-0.out config-2.out"
args=shlex.split(command1)
subprocess.call(args)
"""
outfile = open("ordercheck.txt",'a')
fList = []
for i in range(int(nodes)):
    if ((i+1)== int(nodes)):
        f1= g[0]+"-"+str(i)+".out"
        f2= g[0]+"-"+str(0)+".out"
	if (not fList.__contains__(f1)):
		fList.append(f1)
	if (not fList.__contains__(f2)):
	        fList.append(f2)
        #print f1
        #print f2
        command ="diff %(f1)s %(f2)s"%vars()
        args = shlex.split(command)
        subprocess.call(args,stdout=outfile)
    else:
        f1= g[0]+"-"+str(i)+".out"
        f2= g[0]+"-"+str(i+1)+".out"
        if (not fList.__contains__(f1)):
                fList.append(f1)
        if (not fList.__contains__(f2)):
	        fList.append(f2)

        #print f1
        #print f2
        command ="diff %(f1)s %(f2)s"%vars()
        args = shlex.split(command)
        subprocess.call(args,stdout=outfile)

outfile.close()
try:
 for items in fList:
	tmp = commands.getoutput("cat "+items+" | wc -l")
	if (not tmp.__contains__("No such fi")):
		print "No of lines in "+items+":"+tmp
except:
	print "Exception occured while printing lines!"
if(os.stat("ordercheck.txt").st_size == 0):
    print ">>>>>>>>>>> Messages are In Order <<<<<<<<<<<"
else:
    print ">>>>>>>>>>> Messages are Out Of Order <<<<<<<<<<<"





