package kek;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;

public class EchoClient {
    private SocketChannel client;
    private ByteBuffer buffer;
    private EchoClient instance;


    public static void main(String[] args) {
        EchoClient echoClient = new EchoClient();
        echoClient.startReadThread();
        echoClient.sendMessage("zdarova blyt");
    }

    public void stop() throws IOException {
        client.close();
        buffer = null;
    }

    private EchoClient() {
        try {
            client = SocketChannel.open(new InetSocketAddress("localhost", 3443));
            buffer = ByteBuffer.allocate(256);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendMessage(String msg) {
        buffer = ByteBuffer.wrap(msg.getBytes());
        String response = null;
        try {
            client.write(buffer);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }

    public void startReadThread() {
        System.out.println("startReadThread");
        new Thread(() -> {
            while (true) {
                try {
                    client.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String response = new String(buffer.array()).trim();
                System.out.println("[" + (LocalDateTime.now().toLocalTime().toString()) + "]" +
                        "i got = " + response);
                buffer.clear();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }
}