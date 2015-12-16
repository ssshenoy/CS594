import java.io.*;
import java.net.*;
import java.util.*;
// Server code
class Server {


   public static void main(String argv[]) throws Exception
    {
        int msgBufSize = 1000;
        int maxClients = 100;

	// create array of messages and pointer to next message into it
	Message msgBuf[] = new Message[msgBufSize];
        int msgBufPtr = 0;

        // create array of clients
        Person personList[] = new Person[maxClients];
        int clientId = maxClients-1;
        int newclientId = -1;

        String msgBack;
        int clientCrashes = 0;

        // Lists for active rooms and client names
        Map<String, Room> roomList = new HashMap<String, Room>();
        Map<String, Person> nameList = new HashMap<String, Person>();

	// Create server socket 
        ServerSocket serverSocket;
        try {
         serverSocket = new ServerSocket(Integer.parseInt(argv[0]));
	}
        catch (BindException be)
	    {
		System.out.println("Invalid port or port already in use");
		return;
            }

	while (true) {

	// receive a new client transaction        
	  Socket connectionSocket;

          try 
	  {
	  clientId = maxClients-1;
          connectionSocket = serverSocket.accept();
          connectionSocket.setSoTimeout(5000);
          BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

	  // Dialogue to establish identify of client connection ... input or output thread
          // and client identity to associate with this client's state stored in server
	  clientId = Integer.parseInt(inFromClient.readLine());
    
          // this is an output thread 
	  if (clientId >= 2*maxClients)
	   {
            clientId -= maxClients;
           }
	  if (clientId >= maxClients)
	   {
            outToClient.writeBytes(clientId + "\n");
	    clientId -= maxClients; 
            personList[clientId].logtime(System.currentTimeMillis());

           // broadcast to it all the new messages for the room the client is currently in or
           // mail messages they have received if they are in mail reading mode
            switch (personList[clientId].mode) 
            { 
	    case 1: 
	     Room currentRoom = personList[clientId].currentRoom;
	     for (int i=personList[clientId].nextMsg(currentRoom); i<msgBufPtr; i++) 
	      {
	       if (msgBuf[i].room == currentRoom)
	       {
	        outToClient.writeBytes(msgBuf[i].room.name + ":" + msgBuf[i].sender + ":" + msgBuf[i].content + "\n");
 
	   // register number of last message broadcast to this client for this room
	        personList[clientId].update(currentRoom, i+1); 
               }
              }
             break; 
	    
	    case 3:  // broadcast mail messages to this client from their mailbox 
	      for (Mail mail : personList[clientId].mailBox.toArray(new Mail[0]))
	       {
                outToClient.writeBytes("Mail From " + mail.sender + ": " + mail.content + "\n");
	        personList[clientId].mailBox.remove(mail);
	       }
	     break;             
	    }
             outToClient.writeBytes("" + "\n");
	   }
 
 	  else           
          // this is a client thread that will communicate user messages to server 
          {
	   if (clientId < 0) 
           { 
	    clientId = newclientId = (++newclientId) % maxClients;
            personList[clientId] = new Person(clientId);
            personList[clientId].mode = 0;
            personList[clientId].name = "";
           }

           outToClient.writeBytes(clientId + "\n");
 
	   msgBack = " " + personList[clientId].name + ":cmd>";

          // process commands in command mode 
           if (personList[clientId].mode == 0) 
	       {
		String cmd[] = inFromClient.readLine().split(" ");
                switch (cmd[0])
		{
		 case "n":     // name command
		     Person p = nameList.get(cmd[1]);
                     if (p != null) { // duplicate name
			 msgBack = "Invalid name " + "cmd>";
                     }
                     else // associate name with this client
			 {
                          personList[clientId].name = cmd[1];
                          nameList.put(cmd[1], personList[clientId]);
                         }
                     break; 

                case "s":      // list rooms and occupants
                     msgBack = "";
		     for (String n : nameList.keySet()) msgBack = msgBack + n + " ";
		     for (Room r : roomList.values()) msgBack = msgBack + r.list();
		     msgBack = msgBack + "; " + personList[clientId].name + ":cmd>";; 
	             break;

                case "l":      // leave a room
		    {
		      Room r = roomList.get(cmd[1]);
		      if (r != null) 
                      {
		       // System message to others in room about departure 
        	       msgBuf[msgBufPtr] = new Message((personList[clientId].name + " has left room"), "SYSTEM", r);
                       msgBufPtr = (++msgBufPtr) % msgBufSize;
                       // remove this client from room
                       r.removeVisitor(personList[clientId]);
                       personList[clientId].leave(r);
                       personList[clientId].currentRoom = null;
                       // if the room is empty as a result delete the room
                       if (r.empty()) roomList.remove(r.name);
                      }
		      else msgBack = "Invalid room!" + " " + personList[clientId].name + ":cmd>";
                      break;
                    }

                case "e":      // create, enter or re-enter a room
		    { 
			Room r = roomList.get(cmd[1]);
			// if room does not exist create it
		        if (r == null) 
			{
			 r = new Room(cmd[1]);
			 roomList.put(cmd[1], r);
		        }

                        // first time this client is entering this room so add System message
	                if (r.newentry(personList[clientId]))
			{
                         msgBuf[msgBufPtr] = new Message((personList[clientId].name + " has joined room"), "SYSTEM", r);
                         msgBufPtr = (++msgBufPtr) % msgBufSize;
                        }
                        // update state to reflect entry or re-entry		
                        r.addVisitor(personList[clientId]);
                        personList[clientId].enter(r, msgBufPtr);
                        personList[clientId].mode = 1;
			personList[clientId].currentRoom = r;
                        personList[clientId].setup();
		        msgBack = cmd[1] + ":" + "msg>";
		        break;
		    }

		case "m": // mail mode
		 if (cmd.length > 1) // send
		  {
		      // process list of recepients
                      personList[clientId].recepients.clear();
                      for (int i=1; i<cmd.length; i++)
		      personList[clientId].recepients.add(cmd[i]);
                      personList[clientId].mode = 2;
                  }
                  else // receive
		  {
		      personList[clientId].mode = 3;
                  }               
                  msgBack = "Mail:"; 
		 break;

		case "q": 
                { // quit session
                 Iterator<Room> iter = roomList.values().iterator();
                 while (iter.hasNext()) 
                  {
		        Room r = iter.next();
                        personList[clientId].leave(r);
                        r.removeVisitor(personList[clientId]);
	                msgBuf[msgBufPtr] = new Message((personList[clientId].name + " has left chat"), "SYSTEM", r);
                        msgBufPtr = (++msgBufPtr) % msgBufSize;
                        if (r.empty()) iter.remove(); 
		  } 
                  nameList.remove(personList[clientId].name);
                  personList[clientId].name = "";
                 }
                 msgBack = "";
		 break;

                default:     		
                 msgBack = "Invalid cmd!" + " " + personList[clientId].name + ":cmd>";
		}
	       }
           
           else 
           { // in message mode
	    String s;
	    switch (s = inFromClient.readLine())
	    {
	    case "": // leave message mode
		  personList[clientId].mode = 0;
                  msgBack = personList[clientId].name + ":cmd>";
                  break;
             
	    case "\t":
	        if (personList[clientId].mode == 1) // switch rooms
		{
		  personList[clientId].currentRoom = personList[clientId].next(); 
                  msgBack = personList[clientId].currentRoom.name + ":" + personList[clientId].name + ":" + "msg>";
                }
                break;

            case "?":
	        if (personList[clientId].mode == 1) // display who is member of this room currently
		  msgBack = personList[clientId].currentRoom.list() + ":" + personList[clientId].name + ":" + "msg>";
	        break;

            default:  // store message in buffer or mail in receiver's mailbox
                if (personList[clientId].mode == 1)
		{
         	  msgBuf[msgBufPtr] = new Message(s, personList[clientId].name, personList[clientId].currentRoom);
                  msgBufPtr = (++msgBufPtr) % msgBufSize;
		  msgBack = personList[clientId].currentRoom.name + ":" + personList[clientId].name + ":" + "msg>";
		}
        
                else if (personList[clientId].mode == 2)     // read and store mail messages in mailbox of each recepient
		 for (String recepient : personList[clientId].recepients)
		    if (nameList.get(recepient) == null) 
                    {
                     msgBack =  recepient + "Invalid name!" + " " + personList[clientId].name + ":cmd>";
                     personList[clientId].mode = 0;
                    }
                    else
		    {
		     nameList.get(recepient).mailBox.add(new Mail(s, personList[clientId].name));
                     msgBack = "Mail:";
                    } 
	      }
	   }
	   outToClient.writeBytes((personList[clientId].mailBox.isEmpty() ? "" : "!") + msgBack + "\n");
	  }	    
            
          }
          catch (SocketException ie)
	      	 {
		     clientCrashes++;
                 }
          finally 
                {	     // search for crashed clients and force them to exit
	   	 if (clientCrashes > 0) 
                  for (int i = 0; i <= newclientId; i++) 
                  { 
		   if (personList[i].name.compareTo("")==0) continue;
		   Room r=null;
		   if ((System.currentTimeMillis() - personList[i].lasttime()) > 10000) 
                   {
		    clientCrashes--;
                    System.out.println("terminating client");
                    Iterator<Room> iter = roomList.values().iterator();
                    while (iter.hasNext()) 
                     {
		        r = iter.next();
                        personList[i].leave(r);
                        r.removeVisitor(personList[i]);
	                msgBuf[msgBufPtr] = new Message((personList[i].name + " has left chat"), "SYSTEM", r);
                        msgBufPtr = (++msgBufPtr) % msgBufSize;
			if (r!=null) if (r.empty()) iter.remove(); 
	             } 
                    nameList.remove(personList[i].name);
                    personList[i].name = "";
		   }
 		  }
		} 
	}
    }
}
