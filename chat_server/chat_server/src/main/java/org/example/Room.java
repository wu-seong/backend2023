package org.example;

import java.util.ArrayList;

public class Room {
    private Integer id;
    private String name;
    private ArrayList<User> members = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ArrayList<User> getMembers() {
        return members;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMembers(ArrayList<User> members) {
        this.members = members;
    }
}


