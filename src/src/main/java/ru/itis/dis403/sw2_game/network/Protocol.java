package ru.itis.dis403.sw2_game.network;

import java.io.*;

public class Protocol {

    public static byte[] serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(message);
            objectStream.flush();
            return byteStream.toByteArray();
        }
    }

    public static Message deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
            return (Message) objectStream.readObject();
        }
    }

    public static byte[] createTextMessage(String text) {
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String parseTextMessage(byte[] data) {
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }
}