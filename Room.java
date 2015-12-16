import java.io.*;
import java.net.*;
import java.util.*;

class Room {

    public  String              name;
    private Set<Person>         visitors = new HashSet<Person>();

    public Room(String name) {
        this.name = name;
    }

    boolean newentry(Person visitor) {
        return !visitors.contains(visitor);
    }

    public void addVisitor(Person visitor) {
	visitors.add(visitor);
    }

    public boolean removeVisitor(Person visitor) {
	return visitors.remove(visitor);
    }
; 
    public String list() {
        String s = "@Room: " + name;
	for (Person p : visitors) s = s + " " + p.name;
        return(s);
    }

    public boolean empty() {
     return visitors.isEmpty();
    }
}
