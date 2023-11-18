package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessageUtility {
    public static byte[] getMessage(SocketChannel socketChannel, short messageSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(messageSize); //메세지 크기 만큼 읽어옴
        socketChannel.read(buffer);
        buffer.flip();
        byte[] bytes = bufferToByte(buffer);
        return bytes;
    }

    public static short getMessageSize(SocketChannel client) throws IOException { //읽을 메세지가 있는 client소켓을 주면 메세지의 길이를 get
        short size = 0;
        ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
        int readLength = client.read(lengthBuffer);
        if(readLength == -1){
            System.out.println("FIN");
            return -1;
        }
        lengthBuffer.flip(); // 다시 처음부터 읽을 수 있도록
        if (readLength == 2) { //정상적으로 사이즈 정보를 읽어옴)
            size = lengthBuffer.getShort();
            System.out.println("messageSize = " + size);
        }
        return size;
    }

    public static byte[] bufferToByte(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return bytes;
    }
}
