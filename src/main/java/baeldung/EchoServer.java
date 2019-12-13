package baeldung;

import sun.reflect.generics.scope.Scope;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EchoServer implements Runnable {
    static class Attachment {
        /**
         * Буфер для чтения, в момент проксирования становится буфером для
         * записи для ключа хранимого в peer
         * <p>
         * ВАЖНО: При парсинге Socks4 заголовком мы предполагаем что размер
         * буфера, больше чем размер нормального заголовка, у браузера Mozilla
         * Firefox, размер заголовка равен 12 байт 1 версия + 1 команда + 2 порт +
         * 4 ip + 3 id (MOZ) + 1 \0
         */

        ByteBuffer in;
        /**
         * Буфер для записи, в момент проксирования равен буферу для чтения для
         * ключа хранимого в peer
         */
        ByteBuffer out;
        /**
         * Куда проксируем
         */
        SelectionKey peer;

    }

    private static final String POISON_PILL = "POISON_PILL";

    List<SocketChannel> channelList = new ArrayList<>();

    public static void main(String[] args) {
        EchoServer echoServer = new EchoServer();
        echoServer.run();
    }

    Selector selector = null;
    ServerSocketChannel serverSocket;
    ByteBuffer buffer;

    public EchoServer() {

        try {
            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress("localhost", 3443));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            buffer = ByteBuffer.allocate(256);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // В данном потоке рассылка всем клиентам времени каждые 2 секунды
        new Thread(() -> {
            while (true) {
                if (this.channelList.size() > 0) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    String str = LocalDateTime.now().toLocalTime().toString();
                    byteBuffer.put(str.getBytes());
                    byteBuffer.flip();
                    Iterator<SocketChannel> it = channelList.iterator();
                    while (it.hasNext()) {
                        SocketChannel socketChannel = it.next();
                        if (socketChannel.isOpen()) {
                            try {
                                socketChannel.write(byteBuffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            byteBuffer.flip();
                        } else {
                            it.remove();
                            System.out.println("Клиент закрыт, Удаляем из списка");
                        }
//                        try {
//
//                        } catch (ClosedChannelException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }

                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try {
            while (selector.select() > -1) {

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        register(selector, serverSocket);
                    }

                    if (key.isReadable()) {
                        // Проверяем, не отвалился ли клиент, если происходит отвал
                            // То сообщение будет пустым
                        ByteBuffer buff = ByteBuffer.allocate(32);
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        SocketChannel client = (SocketChannel) key.channel();
                        try {
                            client.read(buffer);
                        } catch (IOException e) {
                            // Закрываем клиента, и удаляем его из списка
                            System.out.println("Клиент отвалился, закрываем");
                            // удаляем из списка
                            channelList.remove(socketChannel);
                            // закрываем
                            key.cancel();
                            socketChannel.close();
                            continue;
                        }

                        answerWithEcho(buffer, key);
                    } else if (key.isWritable()) {
                        //answerWithEcho(buffer, key);
                        System.out.println();
                    } else if (key.isConnectable()) {
                        //answerWithEcho(buffer, key);
                        System.out.println();
                    }

                    iter.remove();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }


    private boolean answerWithEcho(ByteBuffer buffer, SelectionKey key)
            throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(1024);
        SocketChannel socketChannel = (SocketChannel) key.channel();


        SocketChannel client = (SocketChannel) key.channel();

        client.read(buffer);

        System.out.println("i got " + new String(buffer.array()));

        if (new String(buffer.array()).trim().equals(POISON_PILL)) {
            client.close();
            System.out.println("Not accepting client messages anymore");
        }

        buffer.flip();
        client.write(buffer);
        buffer.clear();
        return true;
    }

    private void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException {

        SocketChannel client = serverSocket.accept();
        channelList.add(client);
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put("OK".getBytes());
        buffer.flip();
        client.write(buffer);
        buffer.clear();
    }

    public static Process start() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = EchoServer.class.getCanonicalName();

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);

        return builder.start();
    }

    private void close(SelectionKey key) throws IOException {
        key.cancel();
        key.channel().close();
        SelectionKey peerKey = ((EchoServer.Attachment) key.attachment()).peer;
        if (peerKey != null) {
            ((EchoServer.Attachment) peerKey.attachment()).peer = null;
            if ((peerKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                ((EchoServer.Attachment) peerKey.attachment()).out.flip();
            }
            peerKey.interestOps(SelectionKey.OP_WRITE);
        }
    }
}
