
Authors: Priya Sivaraj,
	Shubin Jacob


Folder: Project3



Instructions

------------



1/ To launch the Project,

   -  sh launcher.sh <config-file> <net-id> > tmp


2/ Progress of the process can be tracked using:
   - tail -f tmp


3/ To kill all the processes in the dc* machines created by the Project,

   sh cleanup.sh <config-file> <net-id>	




Files used

-----------

- Application.java

- Lamport.java
- TOB.java

- Project3.java (Initiator of the project)




NOTE: 

-----

launcher.sh will :

- parse through config.txt to get the machine nodes.

- compile Project3.java

- launch the nodes mentioned.

Hence, no manual intervention is required to compile or initiate the nodes in the dc* machines given in config.txt 
