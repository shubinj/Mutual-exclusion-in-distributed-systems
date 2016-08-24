import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class MyComparator implements Comparator<msgPacket>
{
	public int compare(msgPacket p1, msgPacket p2) {
	return (p1.logicalTime == p2.logicalTime) ? (Integer.valueOf(p1.nodeId).compareTo(p2.nodeId))
	        : ((p1.logicalTime < p2.logicalTime) ? -1 : 1);
	}
}
public class Mutex {
	int totalNodes = 0;
	ArrayList<Integer> nList = new ArrayList<Integer>();
	public static ArrayList<Integer> nodeList = new ArrayList<Integer>();    
    Hashtable<Integer,String> nHash = new Hashtable<Integer,String>();
    public static Hashtable<Integer,String> rcvHash = new Hashtable<Integer,String>();
    File file = null;
	int numMsg;
	int minDelay;
	int stop=0;
	int nId;
	int lStarted = 0;
	ServerSocket serverSock;
	public static int nodeId;
	public static PriorityBlockingQueue<msgPacket> reqQueue; 
	public static PriorityBlockingQueue<msgPacket> msgQueue;
	public static PriorityBlockingQueue<msgPacket> mQueue;
	public static int waitForCs;
	int logTS = 0;
	public static Object lockForSend = new Object();
	public static Object lockForSend1 = new Object();
	public static int tConn = 0;
	public static msgPacket msg;
	public static Hashtable<Integer,ObjectOutputStream> ooS = new Hashtable<Integer,ObjectOutputStream>();
	public static Hashtable<Integer,Integer> oPortList = new Hashtable<Integer,Integer>();
	public static Hashtable<Integer,Integer> iPortList = new Hashtable<Integer,Integer>();

        public static void println(String output)
	{
	    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
            Date date = new Date();
            System.out.println(dateFormat.format(date)+ " " + output);
	}

	Mutex(String fileName,String nodeId,ServerSocket sSock){
		try {
			this.parseConfig(fileName);
			this.nId = Integer.parseInt(nodeId);					
			String[] tmpNode = this.nHash.get(this.nId).split(":");
			Mutex.println("Mutex:Going start Lamport");
     		this.serverSock = sSock;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	void parseConfig(String fileName) throws IOException{
		File fName = new File(fileName);
		FileReader configFile = new FileReader(fileName);
		BufferedReader bRead = new BufferedReader(configFile);
		String newLine = null;

		int grpHash=1;
		int firstLine=0;
		
		String tmpF = fName.getName();
		String tmpN = tmpF.split("\\.(?=[^\\.]+$)")[0];
	        int i = 0;
		/*	for(i=0;i<this.nodeList.size(); i++){
				   String[] tmpNode = this.nHash.get(i).split(":");
				   Socket clientSock = new Socket(tmpNode[0],Integer.valueOf(tmpNode[1]));
				   //this.outChannel.put(i, clientSock);		           
				   Mutex.ooS.put(i,new ObjectOutputStream(clientSock.getOutputStream()));
				   Mutex.oPortList.put(i, clientSock.getPort());
				}*/
		while ((newLine = bRead.readLine()) != null)
		{ 
			if(newLine.isEmpty())
				continue;
			if (newLine.startsWith("#"))
				continue;
			String[] tmpList = newLine.split("\\s+");
			if (firstLine == 0){
				this.numMsg = Integer.parseInt(tmpList[1]);
				this.minDelay = Integer.parseInt(tmpList[2]);
				firstLine = 1;
				continue;
			}

			if((grpHash == 1) && (tmpList.length!=3))
				continue;

			if (grpHash == 1){
				String[] tmpNL = newLine.split("\\s+");
				nHash.put(Integer.parseInt(tmpNL[0]),tmpNL[1]+":"+tmpNL[2]);
				this.nList.add(Integer.parseInt(tmpNL[0]));
				this.totalNodes= this.totalNodes+1;	
			}				
			if (grpHash == 2){					
				newLine = newLine.split("#")[0];
				tmpList = newLine.split("\\s+");
				//int i;
				String tmpLine=null;					
				for(i=0;i< tmpList.length;i++)
				{
					if (tmpList[i].startsWith("#"))
						continue;
					int idx= Integer.parseInt(tmpList[i]);
					}
											
			}		

		}	
		
	}
	void startLamport() throws InterruptedException{
		MyComparator comparator = new MyComparator();
		Mutex.reqQueue = new PriorityBlockingQueue<msgPacket> (50,comparator);
		Mutex.msgQueue = new PriorityBlockingQueue<msgPacket> (50,comparator);
		 Mutex.mQueue = new PriorityBlockingQueue<msgPacket> (100,comparator);
		Mutex.msg = new msgPacket(this.nId);
		Mutex.nodeId = this.nId;
		Mutex.waitForCs = 0;
		Mutex.nodeList = (ArrayList<Integer>) this.nList.clone();
		class rcvThread implements Runnable{
			//int isActive;		
			Socket sock;
			Integer nodId;
			Integer isValid=1;

    		public rcvThread(Socket sock,Integer nId) throws IOException{
    			this.sock = sock;
    			this.nodId = nId; 
    			//not fully covered.
			Mutex.println("Mutex: Rcvd new connection");
    			if (Mutex.oPortList.containsKey(nId) == false)
    				Mutex.oPortList.put(nId, sock.getPort());
    			else
    				this.isValid = 0;
    		}    	


    		public void run()
    		{	  				
				try {
					ObjectInputStream os;
					//this.sock.setKeepAlive(true);
					os = new ObjectInputStream(this.sock.getInputStream());						
					while(true)
					{
						msgPacket tmpMsg = (msgPacket)os.readObject();						
						Mutex.println("Rcvd a pkt.. from "+tmpMsg.nodeId+" with TS:"+tmpMsg.logicalTime);
						//processMsg(tmpMsg,"rcvd");
						synchronized(lockForSend1){
							Mutex.mQueue.add(tmpMsg);
							processMsg(tmpMsg,"update");
						}
						Thread.yield();
					}
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						return;
					}					
           	}  	
		
		};	    
		class serverThread implements Runnable {
			ServerSocket serverSock;
			Thread[] connArr;
			Integer nodId;
			serverThread(ServerSocket sock,Integer nId,Integer nNo){
				this.serverSock = sock;
				this.connArr = new Thread[nNo+1];		
				this.nodId = nId;
			}
			public void run()
			{
				try {				
					int i = 0;
		        	while(true){
		        		//Listens for a connection to be made to this socket and accepts it
		        		Socket sock = this.serverSock.accept();
					Mutex.println("Mutex: rcvd hi5 new conn...");
					//if (Mutex.oPortList.containsValue(sock.getLocalPort())){
		        			this.connArr[i] = new Thread(new rcvThread(sock,this.nodId));
		        			this.connArr[i].start();
		        			Thread.yield();				
		        			i=i+1;
						Mutex.tConn = Mutex.tConn + 1;
		        		//}
		            }
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		};
		class wThread implements Runnable {

			public void run()
			{
				try {		

					msgPacket tmpMsg = new msgPacket(Mutex.nodeId);
					while(true){
						//Mutex.println("Going for update!");
						processMsg(tmpMsg,"update");
						if(Mutex.mQueue.size() > 0 && Mutex.waitForCs == 0){						
						synchronized(lockForSend1){
							tmpMsg = null;	
							tmpMsg = Mutex.mQueue.poll();
							}
							if (tmpMsg == null)
								continue;
							else
								processMsg(tmpMsg,"rcvd");
						}
					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		};
		Thread insertThread = new Thread(new serverThread(this.serverSock,this.nId,this.nodeList.size()));
		insertThread.start();
		int i;
		Thread.sleep(4000);
		for(i=0;i<this.nodeList.size(); i++){
	    	    String[] tmpNode = this.nHash.get(i).split(":");
		    Socket cSock;
		    try {
		    		Mutex.println("Mutex: Initiated conn to:"+this.nHash.get(i));
				cSock = new Socket(tmpNode[0],Integer.valueOf(tmpNode[1]));
				Mutex.ooS.put(i,new ObjectOutputStream(cSock.getOutputStream()));
				Mutex.oPortList.put(i, cSock.getPort());
		    } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		    }
		}
		while(Mutex.tConn < this.nodeList.size()){
			 Mutex.println("Mutex.tConn:"+Mutex.tConn);
			Thread.sleep(500);
		}
		Thread wT = new Thread(new wThread());
		wT.start();
		
	}
	
	void processMsg(msgPacket tmpMsg, String action) throws IOException{		

	if(action == "send"){	
		if (tmpMsg.req == 1){
			synchronized(Mutex.lockForSend){
				Mutex.println("Mutex: Going to Send all req");				
				tmpMsg.req = 1;
				Mutex.msg.logicalTime = Mutex.msg.logicalTime + 1;
				tmpMsg.logicalTime = Mutex.msg.logicalTime;	
				Mutex.msgQueue.clear();	
			//}	
			int i;
			for(i=0;i<Mutex.nodeList.size();i++){
				Mutex.println("Mutex:Sending to"+i);
				Mutex.ooS.get(i).writeObject(tmpMsg);
				Mutex.ooS.get(i).flush();
				Mutex.ooS.get(i).reset();
			}
		}
			Mutex.println("Mutex: Sent all req");					
		}
		else if(tmpMsg.rel == 1){
			synchronized(this.lockForSend){
				Mutex.msg.logicalTime = Mutex.msg.logicalTime + 1;
				tmpMsg.logicalTime = Mutex.msg.logicalTime;
				//}
				int i;			
				for(i=0;i<Mutex.nodeList.size();i++){
					Mutex.ooS.get(i).writeObject(tmpMsg);
					Mutex.ooS.get(i).flush();
					Mutex.ooS.get(i).reset();
				}	
				//Mutex.msgQueue.clear();
				Mutex.println("Mutex: Setting Mutex.waitForCs to 0!");
				Mutex.waitForCs=0;
				Mutex.println("Sent release to all..");
			}
		}
	}
	else if(action == "update"){
		if(Mutex.waitForCs == 1)
			Mutex.println("Just for happened before relation");
	}
	else{
		//Mutex.println("wanted update but in rcvd block! type:rep -> "+ tmpMsg.reply + ", req -> "+tmpMsg.req+",rel -> "+tmpMsg.rel);
	     synchronized(Mutex.lockForSend){
	    	 Mutex.msg.logicalTime = Math.max(Mutex.msg.logicalTime,tmpMsg.logicalTime) + 1;
		 }
	    	 if(tmpMsg.reply == 1){			
	    		 Mutex.msgQueue.add(tmpMsg);
	    		 Mutex.println("Mutex:rcvd reply from "+tmpMsg.nodeId);
	    		 Mutex.println("Current size of Mutex.msgQueue:" +Mutex.msgQueue.size());
	    		 if ((Mutex.msgQueue.size() == Mutex.nodeList.size()) && (Mutex.reqQueue.peek().nodeId == Mutex.nodeId) && Mutex.waitForCs == 0){
			        Mutex.println("Mutex: Setting Mutex.waitForCs to 1!");
			        Mutex.waitForCs=1;
	    		 }
	    	 }
	    	 if (tmpMsg.req == 1){
	    		 Mutex.println("Mutex:rcvd req from "+tmpMsg.nodeId+" with TS:"+tmpMsg.logicalTime);
	    		 Mutex.reqQueue.add(tmpMsg);
	    		 Mutex.println("Current head of reqQueue: "+Mutex.reqQueue.peek().nodeId +" with TS:"+Mutex.reqQueue.peek().logicalTime);
	    		 if (Mutex.waitForCs==0){
			 	synchronized(Mutex.lockForSend){
	    				Mutex.println("Mutex: Mutex.waitForCs Status:"+Mutex.waitForCs);
	 				Mutex.println("Mutex: Sent reply msg to "+tmpMsg.nodeId+" at:"+Mutex.nodeList.indexOf(tmpMsg.nodeId));
	 				msgPacket tmpMsg1 = new msgPacket(Mutex.nodeId);
	 				tmpMsg1.reply = 1;
	 				Mutex.msg.logicalTime = Mutex.msg.logicalTime + 1;
	 		 		tmpMsg1.logicalTime = Mutex.msg.logicalTime;
	 				try {	 					
	 					Mutex.ooS.get(Mutex.nodeList.indexOf(tmpMsg.nodeId)).writeObject(tmpMsg1);
						Mutex.ooS.get(Mutex.nodeList.indexOf(tmpMsg.nodeId)).flush();
						Mutex.ooS.get(Mutex.nodeList.indexOf(tmpMsg.nodeId)).reset();
	 				} catch (IOException e) {
	 				// TODO Auto-generated catch block
	 				e.printStackTrace();
	 				}
				}
	    			 
	    		 }
	    	 }
	    	 if (tmpMsg.rel == 1){
	    		 Mutex.println("Mutex:rcvd release from "+tmpMsg.nodeId);
	    		 Mutex.reqQueue.poll();
	    		 if ((Mutex.msgQueue.size() > 0 && Mutex.reqQueue.size()> 0) && (Mutex.msgQueue.size() == Mutex.nodeList.size()) && (Mutex.reqQueue.peek().nodeId == Mutex.nodeId) && Mutex.waitForCs == 0){
	    			 Mutex.println("Mutex: Setting Mutex.waitForCs to 1!");
	    			 Mutex.waitForCs=1;
			 }
	    	 }		
	}	
	}
	
	
	void csEnter() throws IOException, InterruptedException{
		//broadcast request
		int i;
		msgPacket tmpMsg = new msgPacket(this.nId);
		tmpMsg.req = 1;
		if (this.lStarted == 0){
			startLamport();
			Mutex.println("Mutex.tConn"+Mutex.tConn+",Mutex.nodeList.size()"+Mutex.nodeList.size());
			Mutex.println("Mutex:Successfully started Lamport");
			this.lStarted = 1;
		}
		processMsg(tmpMsg,"send");
		while(true){
			processMsg(tmpMsg,"update");
			if(Mutex.waitForCs == 1){
				Mutex.println("Mutex: Returnin to tob to send...");
				break;
			}
		}
		
	}
	
	void csLeave() throws IOException{
		//broadcast request
			int i;	
			msgPacket tmpMsg;
			tmpMsg = new msgPacket(this.nId);
			tmpMsg.rel = 1;
			processMsg(tmpMsg,"send");
			/*while(Mutex.reqQueue.size() > 0){				
				tmpMsg = Mutex.reqQueue.poll();
				processMsg(tmpMsg,"rcvd");
			}*/
	}
}
