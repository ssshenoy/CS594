import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.concurrent.*;

class Broadcast implements Runnable {

    private String serverName;
    private int portNumber;
    private int clientId;

    Broadcast(String serverName, int portNumber, int clientId) {
	this.serverName = serverName;
        this.portNumber = portNumber;
        this.clientId = clientId;
    }

    public void run() 
    {
        int maxClients = 10;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = null;
        int myId = clientId + 2 * maxClients;

	while (true) {

	// Client's output thread establishes server connection and identity 
         try {
          clientSocket = new Socket(serverName, portNumber);
         } catch (Exception be)
	     { 
               System.out.println("Broadcast Cannot reach server ... exiting");
	       System.exit(0);
             }
 
        
         try {
          clientSocket.setSoTimeout(5000);
          DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
          BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

          outToServer.writeBytes("" + myId + "\n");
          myId = Integer.parseInt(inFromServer.readLine());

         // receive message to be output to user from server. Blank message means output is done.
          String s = inFromServer.readLine();
          while (!s.equals(new String("")))
	     { 
	      System.out.println(s);
              s = inFromServer.readLine();
	     }
            
	 clientSocket.close();

         try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { return; }
	 } catch (Exception E) { 
	   System.out.println("Server not responding ... exiting");
           System.exit(0); 
	 }
	}
    }
}
