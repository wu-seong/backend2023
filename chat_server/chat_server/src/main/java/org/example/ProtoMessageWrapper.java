package org.example;

public class ProtoMessageWrapper {
    private User user;
    private byte[] message;

    public User getUser() {
        return user;
    }

    public ProtoMessageWrapper(User user, byte[] message) {
        this.user = user;
        this.message = message;
    }

    public byte[] getMessage() {
        return message;
    }
}