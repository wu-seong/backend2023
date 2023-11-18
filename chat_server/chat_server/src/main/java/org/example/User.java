package org.example;

import java.nio.channels.SocketChannel;

public class User {
    SocketChannel socketChannel;

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    String name;
    Room participatingRoom;

    public void setParticipatingRoom(Room participatingRoom) {
        this.participatingRoom = participatingRoom;
    }

    public String getName() {
        return name;
    }

    public Room getParticipatingRoom() {
        return participatingRoom;
    }

    public boolean isParticipating(){
        return participatingRoom != null;
    }
    public void setName(String name) {
        this.name = name;
    }
}
