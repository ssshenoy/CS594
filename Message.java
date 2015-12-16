import java.io.*;
import java.net.*;
import java.util.*;

class Message extends Mail {

    public Room room;

    public Message(String content, String sender, Room room) {
        super(content, sender);
        this.room = room;
    }
}
