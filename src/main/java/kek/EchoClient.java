package kek;

import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.MessConfirm;
import kek.message.MessageType;
import kek.message.MessageWrapper;
import kek.person.Person;
import kek.person.PersonType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;

public class EchoClient implements Runnable {

    private SocketChannel client;
    //private ByteBuffer buffer;

    private EchoClient instance;

    private Integer port;
    private Person person;



    public static void main(String[] args) throws JsonProcessingException {
        EchoClient echoClient = new EchoClient();
        echoClient.init();
        echoClient.run();
    }


    public EchoClient() {
        try {
            client = SocketChannel.open(new InetSocketAddress("localhost", 3443));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Инициализируемся
    public void init() {
        this.person = new Person(PersonType.DISPATCHER, "BATYA");
    }

    @Override
    public void run() {

//        try(FileReader reader = new FileReader("burger.txt"))
//        {
//            // читаем посимвольно
//            int c;
//            while((c=reader.read())!=-1){
//
//                System.out.print((char)c);
//            }
//        }
//        catch(IOException ex){
//
//            System.out.println(ex.getMessage());
//        }
        String str;

        // Читаем сообщение от сервера ( тут к нам придет айдишник(порт))
        str = this.readMessage();
        MessConfirm messageFromServer = null;
        try {
            // Т.к. мы тут точно знаем что будет подтверждение, то достаем именно его
            MessageWrapper messageWrapper = MessageWrapper.deserialize(str);
            messageFromServer = messageWrapper.getMessConfirm();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // Сохраняем назначенный им порт
        this.port = messageFromServer.getPort();

        // Готовим сообщение с подтверждением о приеме
        // Указываем
        //      Сообщение: ОК
        //      Полученный от сервара айдишник (порт)
        //      И информацию о Персоне
        MessConfirm messageToServer =
                MessConfirm.builder("OK", this.port)
                        .person(this.person)
                        .build();

        // Отправляем серверу сообщение с подтверждением о приеме
        try {
            this.sendMessage(messageToServer);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println();



        // Начинается рабочий цикл
        while (true){
            str = this.readMessage();
            System.out.println("[" + (LocalDateTime.now().toLocalTime().toString()) + "]" +
                    "i got = " + str);
        }



    }

//    public String sendMessage(String msg) {
//        buffer = ByteBuffer.wrap(msg.getBytes());
//        return send();
//    }

    public String sendMessage(MessConfirm obj) throws JsonProcessingException {

        String tmp = obj.serialize();

        // Заворачиваем Сообщение в обертку
        MessageWrapper messageWrapper = MessageWrapper
                .builder()
                .str(tmp)
                .messageType(MessageType.MESSAGE_CONFIRM)
                .fromPerson(this.person)
                .fromPort(this.port)
                .build();

        // Сериализуем и отправляем сообщение на сервер
        String msg = messageWrapper.serialize();


        return send(msg);
    }

    private String send(String msg) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        buffer.put(msg.getBytes());
        // НА начало буфера переходим
        buffer.flip();
        String response = null;
        try {
            client.write(buffer);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public String readMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            buffer.clear();
            client.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String response = new String(buffer.array()).trim();
        buffer.clear();
        buffer.flip();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

//    public void startReadThread() {
//        System.out.println("startReadThread");
//        new Thread(() -> {
//            while (true) {
//                try {
//                    client.read(buffer);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                String response = new String(buffer.array()).trim();
//                System.out.println("[" + (LocalDateTime.now().toLocalTime().toString()) + "]" +
//                        "i got = " + response);
//                buffer.clear();
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }).start();
//    }
//
//    public void stop() throws IOException {
//        client.close();
//        buffer = null;
//    }

}