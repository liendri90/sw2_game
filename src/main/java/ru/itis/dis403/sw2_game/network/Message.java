package ru.itis.dis403.sw2_game.network;


import java.io.Serializable;

public class Message implements Serializable {
    private MessageType type;
    private String sender;
    private String data;
    private Object payload;

    public Message(MessageType type, String sender) {
        this.type = type;
        this.sender = sender;
    }

    public Message(MessageType type, String sender, String data) {
        this.type = type;
        this.sender = sender;
        this.data = data;
    }

    public Message(MessageType type, String sender, String data, Object payload) {
        this.type = type;
        this.sender = sender;
        this.data = data;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public String getSender() { return sender; }
    public String getData() { return data; }
    public Object getPayload() { return payload; }

    public void setType(MessageType type) { this.type = type; }
    public void setSender(String sender) { this.sender = sender; }
    public void setData(String data) { this.data = data; }
    public void setPayload(Object payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}