import java.io.*;
import java.net.*;
import java.util.concurrent.*;

// Client program

class Client {

    public static void main(String argv[]) throws Exception
    {
	String sentence;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = null;
        int myId = -1;
        int myRealId;
        String msgBack = "cmd>";
	
        while (true) {

	 // Session message printed on client screen	for dialogue with user  
	 String[] printMsg = msgBack.split("@");
         for (String s : printMsg) System.out.println(s);	 
	    
	 // Read user input
         sentence = inFromUser.readLine();

         // Try to connect with server
         try {
         clientSocket = new Socket(argv[0], Integer.parseInt(argv[1]));
         } catch (SocketException be)
	     { 
               System.out.println("Client Cannot reach server ... exiting");
	       System.exit(0);
             }

         // Set a timeout for the connection in case server crashes during dialogue
         clientSocket.setSoTimeout(5000);

         try {

          DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
          BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

	  // Dialogue with server to establish client identity
          outToServer.writeBytes("" + myId + "\n");
          myRealId = Integer.parseInt(inFromServer.readLine());
          if (myId == -1) 
	  // First time this client is talking to server
           {
             System.out.println("starting chat ...");
	     myId = myRealId;

	  // Spawn a thread for printing server output 
             new Thread(new Broadcast(argv[0], Integer.parseInt(argv[1]), myId)).start();
           }
 
          // Now the meat: pass on user command or message to server and receive reply
          outToServer.writeBytes(sentence + "\n");

          msgBack = inFromServer.readLine();

	  clientSocket.close();

          // Server does all the work but if the command was to quit the session client
          // needs to terminate
          if (msgBack.compareTo("") == 0)
	     {
	        System.out.println("exiting chat");
		System.exit(0);
             }
	 }

	  // Terminate gracefully if server has crashed 
         catch (InterruptedIOException ie) 
         {
	     System.out.println("Server not responding ... exiting");
	     clientSocket.close();
             System.exit(0); }
	 }
    }
}
