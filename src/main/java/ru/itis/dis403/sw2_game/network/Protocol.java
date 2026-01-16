package ru.itis.dis403.sw2_game.network;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class Protocol {

    public static byte[] serializeMessage(Message message) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(message);
        objectStream.flush();
        return byteStream.toByteArray();
    }

    public static Message deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        return (Message) objectStream.readObject();
    }

    public static byte[] createTextMessage(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public static String parseTextMessage(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }
}