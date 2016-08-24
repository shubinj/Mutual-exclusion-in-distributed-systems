import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Subject3 extends Thread
{
	ServerSocket serverSocket;
	static int myNodeId;
	static String myHostName;
	static int no_of_nodes;
	static String myMachine;
	static int myPort;
	static String[] machine = null; // To keep track of all Machine names in the network
	static int[] port = null;		// To keep track of Server ports in all machines
	static String[] configArray = null;
	static int no_of_pathlines=0;
	static int no_of_msg=0;
	static int msg_Delay=0;
	//static ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<String>(100);
	private static Object lockForSendMessage = new Object();
	private static Object lockForReceiveMessage = new Object();
	private static boolean[] isMessageToSend = null;
	private static String[] messageToSend = null;
	private static boolean[] isMessageReceived = null;
	private static String[] messageReceived = null;
	static Lamport lamportObj=null;
	static TOB TOBObj=null;
	static commChannel channelObj=null;
	static sendChannel sendObj=null;
	
	public Subject3()
	{
        try
        {
        	serverSocket = new ServerSocket(myPort);
        	serverSocket.setSoTimeout(60000);        	
        }        
        catch(Exception e)
        {
            e.printStackTrace();
        }
	}
	public synchronized static void startServer()
	{
		Subject3 server= new Subject3();
		server.start();
	}
	
	public synchronized void run()
    {
		while(true)
		{
			Socket socket = null;
			try
			{
				//println(myMachine + " listening on : "+myPort);
				socket = serverSocket.accept();
				//println("Client connected : "+socket.getInetAddress().getHostName());
			    new ServerThread(socket).start();
			}
			catch (SocketTimeoutException s)
			{
				//println("Socket timeout!!");
				break;
			}
			catch(Exception e)
	        {
	            e.printStackTrace();
	        }
		}
    }
	public static void println(String output)
	{		
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		Date date = new Date();
		System.out.println(dateFormat.format(date) + " T_" + Thread.currentThread().getName()+ " " + output);
	}
	
	public static boolean getSendMessageFlag(int nodeId)
	{
		synchronized(lockForSendMessage)
		{
			boolean x = isMessageToSend[nodeId];
			//Subject3.println("getSendMessageFlag[" + nodeId + "]=" + x);
			return x;
		}
	}	
	public static void setSendMessageFlag(int nodeId, boolean value)
	{
		synchronized(lockForSendMessage)
		{
			isMessageToSend[nodeId] = value;
			//Subject3.println("setSendMessageFlag[" + nodeId + "]=" + value);
		}
	}
	
	public static String getSendMessage(int nodeId)
	{
		synchronized(lockForSendMessage)
		{
			return(messageToSend[nodeId]);
		}
	}	
	public static void setSendMessage(int nodeId, String value)
	{
		synchronized(lockForSendMessage)
		{
			messageToSend[nodeId] = value;
			//Subject2.println("setSendMessage[" +nodeId +"]=" + value);
		}
	}
	
	public static boolean getReceivedMessageFlag(int nodeId)
	{
		synchronized(lockForReceiveMessage)
		{
			boolean x = isMessageReceived[nodeId];
			//Subject3.println("getReceivedMessageFlag[" + nodeId + "]=" + x);
			return x;
		}
	}	
	public static void setReceivedMessageFlag(int nodeId, boolean value)
	{
		synchronized(lockForReceiveMessage)
		{
			isMessageReceived[nodeId] = value;
			//Subject3.println("setReceivedMessageFlag[" + nodeId + "]=" + value);
		}
	}
	
	public static String getReceivedMessage(int nodeId)
	{
		synchronized(lockForReceiveMessage)
		{
			return messageReceived[nodeId];
		}
	}	
	public static void setReceivedMessage(int nodeId, String value)
	{
		synchronized(lockForReceiveMessage)
		{
			messageReceived[nodeId] = value;
			//Subject2.println("setReceivedMessage[" + nodeId +"]=" + value);
		}
	}		
	
	public synchronized static void printMyServerClient()
	{
		String s="", c="";
		for(int i=0;i<no_of_nodes;i++)
		{
			if(ServerThread.amIserverTo[i]==0)
				continue;
			s = s + i;
		}
		for(int i=0;i<no_of_nodes;i++)
		{
			if(ClientThread.amIclientTo[i]==0)
				continue;
			c = c + i;
		}
		println("I am server to : " + s);
		println("I am client to : " + c);
	}

	public static void main(String[] args) throws IOException, InterruptedException 
	{
		println("Program started!!");
		myNodeId = Integer.parseInt(args[0]);
		myHostName = InetAddress.getLocalHost().getHostName().substring(0,4);
		String configFileName=args[1];
		readConfigFile(configFileName);
		processConfigFile();
		myMachine = machine[myNodeId];
		myPort = port[myNodeId];
		startServer();
		sendClientrequests();
		Thread.sleep(10000);// In-order to achieve only no-of-nodes-1 connections are established.
		printMyServerClient();
		sendObj=new sendChannel();
		lamportObj=new Lamport(sendObj);
		TOBObj=new TOB(lamportObj,sendObj);
		channelObj=new commChannel(lamportObj,TOBObj);
		channelObj.start();
		TOBObj.start();
		Application appObj = new Application(TOBObj);
		appObj.startApplicationModule();
	}//end of main
	public static String parseMsgString(String msg,int location)
	{
		if(msg==null)
			return null;
		else
		{
			String[] tmp=msg.split("-");
			return(tmp[location]);
		}
	}
	public static int parseMsgInt(String msg,int location)
	{
		if(msg==null)
			return (-1);
		else
		{
			String[] tmp=msg.split("-");
			return(Integer.parseInt(tmp[location]));
		}
	}
	
	public synchronized static void sendClientrequests() throws InterruptedException
	{
		// TODO Auto-generated method stub
		for (int j=0;j<no_of_nodes;j++)
		{
			if(j>=myNodeId)
				continue;
			new ClientThread(j,machine[j],port[j]).start();
			//println("Client thread spawned to "+"node "+j+" machine name "+machine[j]+" port name "+port[j]);
			//Thread.sleep(1000); //-- Cause issues during initial connection
			// MUST in-order to differentiate s_thread connected_id for connection to same PC
		}		
	}
	
	public static int getNumberOfValidLines(String fileName) 
    {
		int noOfLines=0;
		try
		{
	        FileReader fr=new FileReader(fileName);
	        BufferedReader br=new BufferedReader(fr);
	        String aLine;
	        while((aLine=br.readLine())!= null)
	        {
	        	// If first character is "#" OR blank line, then ignore
	        	if(aLine.equals(""))
	        		continue;
	        	if(aLine.substring(0,1).equals("#"))
	        		continue;
	        	noOfLines++;
	        }
	        //println("No. of valid lines in config file : " + noOfLines);
	        br.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	    return noOfLines;
    }
	public static void readConfigFile(String fileName)
	{
		//Reading configuration file
		try
		{			
			int numberOfLines = getNumberOfValidLines(fileName);
			int j=0;
			configArray = new String[numberOfLines];
			FileReader fr=new FileReader(fileName);
	        BufferedReader br=new BufferedReader(fr);
		    String aLine;
		    while((aLine=br.readLine())!= null)
	        {
		    	// If first character is "#" OR blank line, then ignore
            	if(aLine.equals(""))
            		continue;
            	if(aLine.substring(0,1).equals("#"))
	        		continue;
	        	configArray[j] = aLine;
	        	j++;	        		
	        } 
	        br.close();
	        //println("number of valid lines"+numberOfLines);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}//end of readconfig
	
	public static void processConfigFile()
	{
		
		for(int j=0;j<configArray.length;j++)
		{
			if(j==0) // First line must contain number of nodes in the network
			{
				String tempStr1 = configArray[j].trim();
				tempStr1=tempStr1.replaceAll("\\s+","-");
				String[] temparray=tempStr1.split("-");
				no_of_nodes = Integer.parseInt(temparray[0]);
				no_of_msg=Integer.parseInt(temparray[1]);
				msg_Delay=Integer.parseInt(temparray[2]);
				machine = new String[no_of_nodes];
				port = new int[no_of_nodes];
				isMessageToSend = new boolean[no_of_nodes];
				messageToSend = new String[no_of_nodes];
				isMessageReceived = new boolean[no_of_nodes];
				messageReceived = new String[no_of_nodes];
				no_of_pathlines=configArray.length-(no_of_nodes+1);
				continue;
			}
			if(configArray[j].contains("dc")) // Store all machine names & it's port numbers
			{
				String tempStr1=configArray[j].trim();
				tempStr1=tempStr1.replaceAll("\\s+","-"); //removes tab space,one or more white-space
				String[] temparray=tempStr1.split("-");
				int nodeId = Integer.parseInt(temparray[0]);
				machine[nodeId] = temparray[1];
				port[nodeId] = Integer.parseInt(temparray[2]);						
			}	
		}
	}//end of processConfigFile	
	
}//end of class Subject2


class ServerThread extends Thread
{
	protected Socket socket;
	protected int connected_id;
	public static int[] amIserverTo = new int[Subject3.no_of_nodes];
	
    public ServerThread(Socket clientSocket)
    {
    	//Subject2.println("Server thread spawned for connection from :" + clientSocket.getInetAddress().getHostName().substring(0,4)); 
    	this.socket = clientSocket;
    	try
    	{
	    	DataInputStream dis = null;
			dis = new DataInputStream(this.socket.getInputStream());			
			String clientNodeId = dis.readUTF();
			this.connected_id = Integer.parseInt(clientNodeId);
			amIserverTo[this.connected_id] = 1;
			Subject3.println("Connected as server to " + this.connected_id);
    	}
    	catch (SocketTimeoutException e)
    	{
    		Subject3.println(" KILLED:SocketTimeoutException");
			amIserverTo[this.connected_id] = 0;
    		return;
    	}
		catch (IOException e)
    	{
			Subject3.println(" KILLED:IOException");
			amIserverTo[this.connected_id] = 0;
			return;
    	}
    	catch (Exception e)
        {
    		Subject3.println(e.getStackTrace().toString());
    		e.printStackTrace();
        }
    }
        
    public synchronized void run()
	{
    	Thread.currentThread().setName("s_thread_" + this.connected_id);
	    while(true)
	    {
	    	try
	    	{
	        	//Writing section
	    		if(Subject3.getSendMessageFlag(this.connected_id)==true)
		    	{
	    			String messageToSend = Subject3.getSendMessage(this.connected_id);
		    		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			        dos.writeUTF(messageToSend);
					dos.flush();
					Subject3.setSendMessageFlag(this.connected_id,false);
					Subject3.println(" WROTE:" + messageToSend);
		    	}
	    		//Reading section
	    		DataInputStream dis = null;
				dis = new DataInputStream(socket.getInputStream());			
				String str = null;
				socket.setSoTimeout(1000);
				str = dis.readUTF();
				Subject3.println(" READ:" + str);
				//------------------ BLOCK until the value has been read by other side -----------------------
				while(true)
				{
					if(Subject3.getReceivedMessageFlag(this.connected_id)==false)
						break;
				}
				//--------------------------------------------------------------------------------------------
				Subject3.setReceivedMessage(this.connected_id, str);
				Subject3.setReceivedMessageFlag(this.connected_id, true);
	    	}
	    	catch (SocketTimeoutException e)
	    	{
	    		continue;
	    	}
			catch (IOException e)
	    	{
				Subject3.println(" KILLED:IOException at actual communication");
				amIserverTo[this.connected_id] = 0;
				return;
	    	}
	    	catch (Exception e)
	        {
	    		Subject3.println(e.getStackTrace().toString());
	    		e.printStackTrace();
	        }
	    }//end of while	 	
	}//end of ServerThread run()    
 }//end of ServerThread class


class ClientThread extends Thread
{
	protected String serverName;
	protected int serverPort;
	protected Socket socket;
	protected int connected_id;
	public static int[] amIclientTo = new int[Subject3.no_of_nodes];
	public ClientThread(int nodeId, String serverName, int serverPort)
	{
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.connected_id = nodeId;
		//Subject2.println("Client Thread spawned to connect to " + nodeId); 
	}	
	
	public synchronized void run() 
	{
		Thread.currentThread().setName("c_thread_" + this.connected_id);
		try 
        {
        	socket = new Socket(this.serverName,this.serverPort);
        	amIclientTo[this.connected_id] = 1;
        	Subject3.println("Connected as client to " + this.connected_id);
        	// Send my node Id to Server
        	DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
	        dos.writeUTF(Integer.toString(Subject3.myNodeId));
			dos.flush();
        } 
        catch(IOException e)
        {
        	Subject3.println(" KILLED:IOException");
        	amIclientTo[this.connected_id] = 0;
        	//e.printStackTrace();
        	return;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return; // Added on 10/23/2015
        }
		while(true)
	    {
			try
	    	{
		    	//Writing section
	    		if(Subject3.getSendMessageFlag(this.connected_id)==true)
		    	{
	    			String messageToSend = Subject3.getSendMessage(this.connected_id);
		    		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			        dos.writeUTF(messageToSend);
					dos.flush();
					Subject3.setSendMessageFlag(this.connected_id,false);
					Subject3.println(" WROTE:" + messageToSend);
		    	}
	    		//Reading section
	    		DataInputStream dis = null;
				dis = new DataInputStream(socket.getInputStream());			
				String str = null;
				socket.setSoTimeout(1000);
				str = dis.readUTF();
				Subject3.println(" READ:" + str);
				//------------------ BLOCK until the value has been read by other side -----------------------
				while(true)
				{
					if(Subject3.getReceivedMessageFlag(this.connected_id)==false)
						break;
				}
				//--------------------------------------------------------------------------------------------
				Subject3.setReceivedMessage(this.connected_id, str);
				Subject3.setReceivedMessageFlag(this.connected_id, true);
	    	}
	    	catch (SocketTimeoutException e)
	    	{
	    		continue;
	    	}
			catch (IOException e)
	    	{
				Subject3.println(" KILLED:IOException at actual communication");
				amIclientTo[this.connected_id] = 0;
				return;
	    	}
	    	catch (Exception e)
	        {
	    		Subject3.println(e.getStackTrace().toString());
	    		e.printStackTrace();
	        }
	    }//end of while	 
	} //end of ClientThread run()	
} //end of ClientThread class


class Application
{
	TOB TOBObj;
	public Application(TOB TObj)
	{		
		this.TOBObj = TObj;
	}
	
	public void startApplicationModule() throws InterruptedException, IOException
	{	
		//----------------------------------------------------------------------------------
		File file = new File("log_"+Subject3.myNodeId+".txt");
		if(!file.exists())
			file.createNewFile();
		FileOutputStream fout = new FileOutputStream(file,true);
		DataOutputStream dout = new DataOutputStream(fout);
		BufferedWriter bout = new BufferedWriter(new OutputStreamWriter(dout));		
		int readLineCount=0;
		//------------------------------------------------------------------------------------
		
		/*Random rnumber = new Random();
		int x=rnumber.nextInt(Subject3.no_of_nodes-0+1)+0;
		//int x=(Subject3.no_of_nodes-1)-Subject3.myNodeId;
		int delay=4000*x;
		Subject3.println("Thread sleeps for :"+delay+" before sending the msg");
		Thread.sleep(delay);
		for(int i=0;i<Subject3.no_of_msg;i++)
		{
			String msg=i+":MSG^" + Subject3.myNodeId;
			TOBObj.tobSend(msg);
			Thread.sleep(Subject3.msg_Delay);
		}
		while(true)
		{
			String rcvdMessage = TOBObj.tobReceive();
			//Subject3.println("ApplicationModule :"+rcvdMessage);
			bout.write(rcvdMessage);
			bout.newLine();
			readLineCount++;
			if(readLineCount==Subject3.no_of_nodes*Subject3.no_of_msg)
				break;
		}	
		bout.flush();
		bout.close();
	}

} // End of class 'Application'


class TOB extends Thread
{
	Lamport lamportObj;
	sendChannel sendObj;
	static ArrayBlockingQueue<String> mySendQueue=new ArrayBlockingQueue<String>(Subject3.no_of_msg+10); 
	static ArrayBlockingQueue<String> myReceiveQueue=new ArrayBlockingQueue<String>(Subject3.no_of_nodes*Subject3.no_of_msg+50);
	// Note : +10/+50 for safety purpose only
	
	public TOB(Lamport lObj,sendChannel sendObj)
	{
		this.lamportObj = lObj;
		this.sendObj=sendObj;
	}
	public void tobSend(String m) throws InterruptedException
	{		
		mySendQueue.add(m);		
	}
	public String tobReceive()
	{
		while(true)
		{
			String m = myReceiveQueue.poll();
			if(m!=null)
				return m;
		}
	}
	public void TOBreceiveMsgHandler(int nodeid,String rcv)
	{
		// give it to tobreceive();
		String[] temp=rcv.split("-");
		String queue=temp[0];
		myReceiveQueue.add(queue);
	}
	public synchronized void run()
	{		
		try
		{
			while(true)
			{
				if(mySendQueue.size()!=0)
				{
					String currentMessage = mySendQueue.poll();
					String send=currentMessage+"-"+"TOB";
					lamportObj.csEnter();
					Subject3.println("I am in critical section:" + currentMessage);
					//Broadcast to all nodes (including myself)
					myReceiveQueue.add(currentMessage); //Add my broadcast message to ReceiveQueue
					for(int i=0;i<Subject3.no_of_nodes;i++)
					{
						if(i==Subject3.myNodeId)
							continue;
						sendObj.SendMessage(i,send);
					}
					Thread.sleep(1000);
					lamportObj.csExit();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

class Lamport extends Thread
{
	static int lamportClock=0; // To hold the Lamport clock for this node
	static int requestMessageClock=0; // To hold the clock in Request message
	static boolean isCriticalSectionExecuting=false; // To tell if CS is being executed by this node
	static boolean isConditionSatisfy=false; // For Lamport RcvThread to tell csEnter() if it can grant permission
	static int noOfRepliesRcvd=0; // To hold of no. of replies received
	//-------------------------------------- PRIORITY QUEUE -------------------------------------------------------	
	static PriorityQueue<String> requestQueue = new PriorityQueue<String>(new Comparator<String>(){
	public int compare(String m1, String m2)
	{
		
		return (Subject3.parseMsgInt(m1, 0)==Subject3.parseMsgInt(m2,0)) ? (Integer.valueOf(Subject3.parseMsgInt(m1,1)).compareTo(Subject3.parseMsgInt(m2,1)))
                    : ((Subject3.parseMsgInt(m1,0)<Subject3.parseMsgInt(m2,0)) ? -1 : 1);
	}
	});
	//-------------------------------------- PRIORITY QUEUE -------------------------------------------------------
	private static Object lockForLamportClock = new Object();
	private static Object lockForCondition = new Object();
	private static Object lockForReplyCount = new Object();
	private static Object lockForRequestQueue = new Object();
	
	sendChannel sendObj;
	public Lamport(sendChannel sendObj)
	{
		this.sendObj=sendObj;
	}
	
	public static int getLamportClock()
	{
		synchronized(lockForLamportClock)
		{
			return lamportClock;
		}
	}	
	public static void setLamportClock(int value)
	{
		synchronized(lockForLamportClock)
		{
			lamportClock=value;
		}
	}	
	public static boolean getCondition()
	{
		synchronized(lockForCondition)
		{
			return(isConditionSatisfy);
		}
	}	
	public static void setCondition(boolean value)
	{
		synchronized(lockForCondition)
		{
			isConditionSatisfy=value;
		}
	}	
	public static int getReplyCount()
	{
		synchronized(lockForReplyCount)
		{
			return(noOfRepliesRcvd);
		}
	}	
	public static void setReplyCount(int value)
	{
		synchronized(lockForReplyCount)
		{
			noOfRepliesRcvd=value;
		}
	}
	public static String getHeadInRequestQueue()
	{
		synchronized(lockForRequestQueue)
		{
			return(requestQueue.peek());
		}
	}
	public static void removeHeadInRequestQueue()
	{
		synchronized(lockForRequestQueue)
		{
			requestQueue.remove();
			return;
		}
	}	
	public static void addToRequestQueue(String value)
	{
		synchronized(lockForRequestQueue)
		{
			requestQueue.add(value);
		}
	}
	
	public void csEnter() throws InterruptedException
	{
		// Increment the timestamp and add to request queue
		int currentLamportClock=getLamportClock();
		currentLamportClock++;
		setLamportClock(currentLamportClock);
		String request = currentLamportClock+"-"+Subject3.myNodeId+"-"+"REQUEST"+"-"+"MUTEX";
		requestMessageClock=currentLamportClock;
		addToRequestQueue(request);
		//broadcast the request
		for(int i=0;i<Subject3.no_of_nodes;i++)
		{
			if(i==Subject3.myNodeId)
				continue;
			sendObj.SendMessage(i,request);
		}
		// check for critical section entry
		while(true)
		{
			if(getCondition()==true)
			{
				setCondition(false);
				Subject3.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  CS ENTER <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				isCriticalSectionExecuting=true;
				break;
			}
		}
	}
	
	public void csExit()
	{
		isCriticalSectionExecuting=false;
		// send release msg to all other nodes
		int currentLamportClock=getLamportClock();
		currentLamportClock++;
		setLamportClock(currentLamportClock);
		String release=currentLamportClock+"-"+Subject3.myNodeId+"-"+"RELEASE"+"-"+"MUTEX";
		for(int i=0;i<Subject3.no_of_nodes;i++)
		{
			if(i==Subject3.myNodeId)
				continue;
			sendObj.SendMessage(i,release);	
		}
		// Remove request from my queue
		String pop=getHeadInRequestQueue();
		requestQueue.remove();
		Subject3.println("Removed:"+pop);
		// Reset the values
		setReplyCount(0);
		requestMessageClock=0;
		Subject3.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  CS EXIT <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	}
	
	public void updateclock(int x)
	{
		int currentclock=getLamportClock();
		if(x>currentclock)
			setLamportClock(x+1);
		else
			setLamportClock(currentclock+1);
	}
	
	public void MutexreceiveMsgHandler(int nodeid,String rcv)
	{
		if(nodeid!=Subject3.myNodeId)
		{
			if(rcv!=null)
			{
				if(rcv.contains("REQUEST"))
				{
					addToRequestQueue(rcv);
					// send reply message
					updateclock(Subject3.parseMsgInt(rcv,0));
					int clock=getLamportClock();
					clock++;
					setLamportClock(clock);
					String replyMsg=clock+"-"+Subject3.myNodeId+"-"+"REPLY"+"-"+"MUTEX";
					int toReplyId=Subject3.parseMsgInt(rcv, 1);
					sendObj.SendMessage(toReplyId,replyMsg);
				}
				else if(rcv.contains("REPLY"))
				{
					updateclock(Subject3.parseMsgInt(rcv,0));
					if(Subject3.parseMsgInt(rcv,0)> requestMessageClock)//check the timestamp
						setReplyCount(getReplyCount()+1);
					if(getReplyCount()==Subject3.no_of_nodes-1)
					{
						Subject3.println("I have received n-1 messages of greater timestamp");
						String pop=getHeadInRequestQueue();
						if(Subject3.parseMsgInt(pop,1)==Subject3.myNodeId)
						{
							Subject3.println("I am in the top of priority queue");
							setCondition(true);
						}
						else
							Subject3.println("I am not in the top of priority queue");
					}
					else
						Subject3.println("I have received :"+getReplyCount()+" replies from other nodes");						
				}
				else if(rcv.contains("RELEASE"))
				{
					//Subject3.println("Received: "+rcv);
					updateclock(Subject3.parseMsgInt(rcv,0));
					String pop=getHeadInRequestQueue();
					if(Subject3.parseMsgInt(rcv,1)==Subject3.parseMsgInt(pop,1))
					removeHeadInRequestQueue();
					Subject3.println("Removed:"+pop);
					String pop1=getHeadInRequestQueue();
					if(Subject3.parseMsgInt(pop1,1)==Subject3.myNodeId)
					{
						Subject3.println("I am in the top of priority queue");
						if(getReplyCount()==Subject3.no_of_nodes-1)
							setCondition(true);
						else
							Subject3.println("I have received only:"+getReplyCount()+" replies from other nodes");
					}
					else
						Subject3.println("I am not in the top of priority queue");
				}
				else
					System.out.println("Invalid message received:" + rcv);
			}//null if
		}//nodeidif
		
	}		
}//end of Lamport class

class sendChannel
{
	public void SendMessage(int nodeid,String msg)
	{
		while(true)
		{
			if(Subject3.getSendMessageFlag(nodeid)==false)
				break;
		}
		//--------------------------------------------------------------------------------------------
		Subject3.setSendMessage(nodeid,msg);
		Subject3.setSendMessageFlag(nodeid, true);
	}
	
}

class commChannel extends Thread
{
	Lamport Lobj;
	TOB Tobj;
	public commChannel(Lamport Lobj, TOB Tobj)
	{
		this.Lobj=Lobj;
		this.Tobj=Tobj;
	}
	
	public synchronized void run()
	{
		while(true)
		{
			for(int i=0;i<Subject3.no_of_nodes;i++)
			{
				if(i==Subject3.myNodeId)
					continue;
				if(Subject3.getReceivedMessageFlag(i)==true)
				{
					String rcv = Subject3.getReceivedMessage(i);
					if(rcv==null)
						continue;
					if(rcv.contains("MUTEX"))
					{
						Subject3.setReceivedMessageFlag(i,false);
						Lobj.MutexreceiveMsgHandler(i,rcv);	
					}
					else if(rcv.contains("TOB"))
					{
						Subject3.setReceivedMessageFlag(i,false);
						Tobj.TOBreceiveMsgHandler(i,rcv);	
					}
					else
						System.out.println("Invalid message received:" + rcv);
				}//
			}// end of for loop
		}// end of while
			
	}//end of run
	
	
}// end of commChannel
