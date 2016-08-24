import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class tob {
	int totalNodes = 0;
	ArrayList<Integer> nodeList = new ArrayList<Integer>();    
    Hashtable<Integer,String> nHash = new Hashtable<Integer,String>();
    File file = null;
	int numMsg;
	int minDelay;
	int stop=0;
	int nId = 0;
	ServerSocket serverSock;
	 public static Object lockForSend = new Object();
	public static Hashtable<Integer,ObjectOutputStream> ooS = new Hashtable<Integer,ObjectOutputStream>();
	public static int tNodes;
	Hashtable<Integer,Integer> oPortList = new Hashtable<Integer,Integer>();
	public static ArrayBlockingQueue<msgPacket> rcvQ = new ArrayBlockingQueue<msgPacket>(100);
	public static ArrayBlockingQueue<msgPacket> repQ = new ArrayBlockingQueue<msgPacket>(100);
	public static ArrayBlockingQueue<msgPacket> msgQ = new ArrayBlockingQueue<msgPacket>(100);
	public static Mutex csObj;
	

	tob(String fileName, String nodeId){
		try {
			this.parseConfig(fileName);
			this.nId = Integer.parseInt(nodeId);			
			int i;
			String[] tmpNode = this.nHash.get(this.nId).split(":");
     		this.serverSock = new ServerSocket(Integer.valueOf(tmpNode[1]));
			try {
				startTob();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.csObj = new Mutex(fileName, nodeId, this.serverSock);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	public static void println(String output)
	{
	      DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	      Date date = new Date();
	      System.out.println(dateFormat.format(date)+ " " + output);
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
	        int count = 0;

		while ((newLine = bRead.readLine()) != null)
		{ 
			if(newLine.isEmpty())
				continue;
			if (newLine.startsWith("#"))
				continue;
			String[] tmpList = newLine.split("\\s+");
			if (firstLine == 0){
				tob.tNodes = Integer.parseInt(tmpList[0]);
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
				nodeList.add(Integer.parseInt(tmpNL[0]));
				this.totalNodes= this.totalNodes+1;	
			}				
			if (grpHash == 2){					
				newLine = newLine.split("#")[0];
				tmpList = newLine.split("\\s+");
				int i;
				String tmpLine=null;					
				for(i=0;i< tmpList.length;i++)
				{
					if (tmpList[i].startsWith("#"))
						continue;
					int idx= Integer.parseInt(tmpList[i]);									}
				count=count+1;							
			}		

		}	
		
	}
	void startTob() throws InterruptedException{
		class rcvThread implements Runnable{
			//int isActive;		
			Socket sock;
			Integer nodId;
			Integer isValid=1;
    		public rcvThread(Socket sock,Integer nId) throws IOException{
    			this.sock = sock;
    			this.nodId = nId; 
			tob.println("tob: rcvd new connection!");
    			//not fully covered.
    			if (Mutex.oPortList.containsKey(nId) == false)
    				Mutex.oPortList.put(nId, sock.getPort());
    			else
    				this.isValid = 0;
    		}    		

    		public void run()
    		{	  				
				try {
					ObjectInputStream os;
					this.sock.setKeepAlive(true);
					os = new ObjectInputStream(this.sock.getInputStream());						
					while(true)
					{						
						msgPacket tmpMsg = (msgPacket)os.readObject();	
						//synchronized(tob.lockForSend){
						if (tmpMsg.reply == 1){
							tob.println("tob: rcvd reply from "+tmpMsg.nodeId);
							tob.repQ.add(tmpMsg);
						}
						else{
							tob.println("tob: rcvd from "+tmpMsg.nodeId);
							tob.rcvQ.add(tmpMsg);
						}
						sendMsg(tmpMsg,"update");
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
				this.connArr = new Thread[nNo];		
				this.nodId = nId;
			}
			public void run()
			{
				try {				
					int i = 0;
		        	while(true && i < tob.tNodes){
		        		//Listens for a connection to be made to this socket and accepts it
		        		this.connArr[i] = new Thread(new rcvThread(this.serverSock.accept(),this.nodId));
		        		this.connArr[i].start();
             		    Thread.yield();				
                 		i=i+1;
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
				msgPacket tmpMsg = new msgPacket(Mutex.nodeId);
				while(true){
					//Mutex.println("Going for update!");
					sendMsg(tmpMsg,"update");
					if(tob.msgQ.size() > 0){						
						tmpMsg = null;	
						//synchronized(tob.lockForSend){
							tmpMsg = tob.msgQ.poll();
						//}
						if (tmpMsg == null)
							continue;
						else
							sendMsg(tmpMsg,"send");
						}
					
				}
			}	
		};
		
		Thread insertThread = new Thread(new serverThread(this.serverSock,this.nId,this.nodeList.size()));
		insertThread.start();
		Thread.sleep(4000);
		int i;
		for(i=0;i<this.nodeList.size(); i++){
	        String[] tmpNode = this.nHash.get(i).split(":");
	        Socket clientSock;
			try {
				clientSock = new Socket(tmpNode[0],Integer.valueOf(tmpNode[1]));
				this.ooS.put(i,new ObjectOutputStream(clientSock.getOutputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Thread wT = new Thread(new wThread());
		wT.start();
		
	}
	
	public void sendMsg(msgPacket tmpMsg, String action){
		int i;
		if (action == "send"){
			try {
				tob.repQ.clear();
				tob.csObj.csEnter();
				tob.println("tob:Going to enter mutex to send:"+tmpMsg.msg);
				for(i=0;i<tob.tNodes;i++){
					ObjectOutputStream c = tob.ooS.get(i);
					c.writeObject(tmpMsg);
					c.flush();
					c.reset();
				}
				tob.println("tob:Sent to all");
				while(tob.repQ.size() != tob.tNodes){
				}
				tob.csObj.csLeave();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
	                        e.printStackTrace();
	                }
		}
		else if (action == "add"){
			//synchronized(tob.lockForSend){
				tob.msgQ.add(tmpMsg);
			//}
		}
		else{
			if(tob.msgQ.size() > 0 && tob.rcvQ.size() > 0){
				//tob.println("Just for happened before relation");
			}
		}
	}
	
	void tobSend(String msg){
		tob.tNodes = this.totalNodes;
		String msg1=msg;		
		msgPacket mObj = new msgPacket(this.nId);
		mObj.msg = msg1;
		tob.println("tob:Going to add "+msg1);
		sendMsg(mObj,"add");
	}
	
	String tobReceive() throws IOException{
		String nodeMsg="";		
		Socket sock;
		int rcvdMsg = 0;
		msgPacket tM = null;
		tob.println("tob:Going to pop rcvd msg!");
		while(rcvdMsg == 0){			
				sendMsg(tM,"update");
				if (tob.rcvQ.size() > 0){
				//synchronized(tob.lockForSend){
					tM = tob.rcvQ.poll();
				//}
				if (tM != null){
					nodeMsg = tM.msg+" "+Integer.toString(tM.nodeId);
					msgPacket mObj = new msgPacket(this.nId);
					mObj.reply = 1;
					tob.ooS.get(nodeList.indexOf(tM.nodeId)).writeObject(mObj);
					tob.ooS.get(nodeList.indexOf(tM.nodeId)).flush();
					tob.ooS.get(nodeList.indexOf(tM.nodeId)).reset();
					rcvdMsg = 1;
				}
				}
		}
		tob.println("tob:Rcvd from "+tM.nodeId);		
		return nodeMsg;
	}
}
