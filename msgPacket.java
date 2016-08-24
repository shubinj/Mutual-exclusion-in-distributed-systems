import java.io.Serializable;


public class msgPacket implements Serializable{
	int logicalTime = 0;
	int nodeId = 0;
	int req = 0;
	int reply = 0;
	int rel = 0;
        String msg = "";	
    msgPacket(Integer nId){
          this.nodeId = nId;
    }

}
           
