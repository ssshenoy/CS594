import java.io.*;
import java.net.*;
import java.util.*;

class Person {

    public String name;
    public int id;
    public HashMap<Room, Integer> visits = new HashMap<Room, Integer>();
    public int mode;
    public Room currentRoom = null;
    public List<Mail> mailBox = new ArrayList<Mail>();
    public Set<String> recepients = new HashSet<String>();
    public long lastTimeLog = 0;

    private Iterator<Room> iter = null;

    public Person(int id) {
        this.id = id;
    }

    public void logtime (long t) {
	lastTimeLog = t;
    }

    public long lasttime() { 
        return lastTimeLog; 
    }

    public void enter(Room r, int at) {
        int atLocal = at;
	if (visits.containsKey(r)) 
        {
         atLocal = this.nextMsg(r);
         visits.remove(r);
        } 
        visits.put(r, atLocal);
    }
 
    public void leave(Room r) {
	visits.remove(r);
    }

    public int nextMsg(Room r) {
     return visits.get(r);
    }  

    public void update(Room r, int at) {
	//	visits.remove(r);
        visits.put(r, at);
    }
 
    public void setup() {
	iter = visits.keySet().iterator();
    }

    public Room next() {
	if ((iter == null) || (!iter.hasNext())) iter = visits.keySet().iterator(); 
	return iter.next();  
        }
}
