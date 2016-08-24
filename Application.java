import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;


public class Application {
	int totalNodes = 0;
	ArrayList<Integer> nodeList = new ArrayList<Integer>();    
    Hashtable<Integer,String> nHash = new Hashtable<Integer,String>();
    public static tob tobObj;
	public static File file1 = null;
	int numMsg;
	int minDelay;
	public static int stop=0;
	int nId ;
	public static int minPerA = 1;
	public static int maxPerA = 10000;
	
	Application(String nodeId, String fileName){
		try {
			
			this.nId = Integer.parseInt(nodeId);
			this.parseConfig(fileName);
			File fName = new File(fileName);
			FileReader configFile = new FileReader(fileName);
			String tmpF = fName.getName();
			String tmpN = tmpF.split("\\.(?=[^\\.]+$)")[0];
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	//To parseConfig.	
	void parseConfig(String fileName) throws IOException{
		Application.tobObj = new tob(fileName,Integer.toString(this.nId));
		File fName = new File(fileName);
		FileReader configFile = new FileReader(fileName);
		//FileWriter tmpFW = new FileWriter(Application.file1);
		BufferedReader bRead = new BufferedReader(configFile);
		String newLine = null;
		int grpHash=1;
		int firstLine=0;
		
		String tmpF = fName.getName();
		String tmpN = tmpF.split("\\.(?=[^\\.]+$)")[0];
		Application.file1 = new File(tmpN+"-"+this.nId+".out");
	        int count = 0;

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

			if (grpHash == 1 && firstLine == 1){
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
	
	void startApp() throws InterruptedException{
	 int i;
		class serverThread implements Runnable {
			ServerSocket serverSock;
			Thread[] connArr;
			Integer nodId;
			
			public void run()
			{
				int i = 0;
				while(true)
					{
						if(Application.stop == 1){
							break;
						}
						//Listens for a connection to be made to this socket and accepts it
						try {
							BufferedWriter output = new BufferedWriter(new FileWriter(Application.file1,true));
							String rcvObj = Application.tobObj.tobReceive();
							output.write(rcvObj+"\n");
							output.flush();
							output.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
			}	
		};
		serverThread sObj = new serverThread();
		Thread tmpT = new Thread(sObj);
		tmpT.start();	
		Thread.sleep(4000);
		int currMsg = 0;
		Random currSend = new Random();
		
		while(currMsg < this.numMsg){
			int tmpNo = currSend.nextInt(Application.maxPerA - Application.minPerA + 1) + Application.minPerA;
			System.out.println("App: Going to send "+tmpNo);
			this.tobObj.tobSend(Integer.toString(tmpNo));
			currMsg = currMsg + 1;
			Thread.sleep(this.minDelay);			
		}
		
	}
	public static void main(String args[]) throws IOException,InterruptedException
	{
	    Application obj = new Application(args[0],args[1]);
	    Thread.sleep(10000);
	    obj.startApp();
	}	
}
