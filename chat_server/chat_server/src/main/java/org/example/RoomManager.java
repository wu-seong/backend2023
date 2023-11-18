package org.example;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RoomManager {
    private ConcurrentLinkedQueue<Room> rooms = new ConcurrentLinkedQueue<>();
    private final IdGenerator idGenerator = new IdGenerator();

    public Room createRoom(User user, String title){

        Room room = new Room();

        room.setId(idGenerator.getNewId());
        ArrayList<User> members = new ArrayList<>();
        members.add(user);
        room.setMembers(members);
        room.setName(title);

        rooms.add(room);
        user.setParticipatingRoom(room);
        return room;
    }
    public boolean joinRoom(User user, int roomId){
        for (Room room: rooms) {
            if(room.getId().equals(roomId)){
                room.getMembers().add(user);
                user.setParticipatingRoom(room);
                return true;
            }
        }
        return false; //해당하는 RoomId가 없음
    }
    public boolean removeRoom(Room room){
        if(!room.getMembers().isEmpty()){ //남아있는 멤버가 있으면 없앨 수 없음
            return false;
        }
        idGenerator.releaseId(room.getId());
        return rooms.remove(room);
    }
    public Room leaveRoom(User user){
        Room leaveRoom = null;
        for (Room room:rooms) {
            if(room.equals(user.getParticipatingRoom())){
                leaveRoom = room;
                break;
            }
        };
        leaveRoom.getMembers().remove(user);
        user.setParticipatingRoom(null);
        return leaveRoom;
    }
    public Room findRoom(int roomId){
        for (Room room :rooms) {
            if(room.getId().equals(roomId)){
                return room;
            }
        }
        return null;
    }
    public ConcurrentLinkedQueue<Room> getRooms() {
        return rooms;
    }
}
