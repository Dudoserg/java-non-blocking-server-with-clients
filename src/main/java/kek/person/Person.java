package kek.person;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.*;
import lombok.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Person {

    protected PersonType personType;
    protected String personName;

    @JsonIgnore
    protected SocketChannel socketChannel;
    //private ByteBuffer buffer;

    @JsonIgnore
    protected EchoClient instance;

    protected Integer port;


    public Person(PersonType personType, String personName) {
        this.personType = personType;
        this.personName = personName;
    }


    public Person(PersonType personType) {
        this.personType = personType;
    }

    @JsonIgnore
    public void confirmConnectToServer() {
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
                        .person(this)
                        .build();

        // Отправляем серверу сообщение с подтверждением о приеме
        try {
            this.sendMessConfirm(messageToServer);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    //    public String sendMessConfirm(String msg) {
//        buffer = ByteBuffer.wrap(msg.getBytes());
//        return send();
//    }
    @JsonIgnore
    public String sendMessConfirm(MessConfirm obj)
            throws JsonProcessingException {

        String tmp = obj.serialize();

        // Заворачиваем Сообщение в обертку
        MessageWrapper messageWrapper = MessageWrapper
                .builder()
                .str(tmp)
                .messageType(MessageType.MESSAGE_CONFIRM)
                .build();

        // Сериализуем и отправляем сообщение на сервер
        String msg = messageWrapper.serialize();


        return send(msg);
    }

    @JsonIgnore
    public String sendMessageFreeForPersonType(MessageFreeForPersonType obj)
            throws JsonProcessingException {

        String tmp = obj.serialize();

        // Заворачиваем Сообщение в обертку
        MessageWrapper messageWrapper = MessageWrapper
                .builder()
                .str(tmp)
                .messageType(MessageType.MESSAGE_FREE_FOR_PERSONTYPE)

                .build();



        // Сериализуем и отправляем сообщение на сервер
        String msg = messageWrapper.serialize();


        return send(msg);
    }

//    @JsonIgnore
//    public String sendMessage(Message obj, Integer toPort, Person toPerson,
//                              PersonType toPersonType) throws JsonProcessingException {
//
//        String tmp = obj.serialize();
//
//        // Заворачиваем Сообщение в обертку
//        MessageWrapper messageWrapper =
//                MessageWrapper.builder()
//                        .str(tmp)
//                        .messageType(MessageType.MESSAGE)
////                        .fromPerson(this)
////                        .fromPort(this.port)
////                        .fromPersonType(this.personType)
////                        .toPort(toPort)
////                        .toPerson(toPerson)
////                        .toPersonType(toPersonType)
//                        .build();
//        FROM_TO from_to =
//                FROM_TO.builder()
//                        .fromPerson(this)
//                        .fromPort(this.port)
//                        .fromPersonType(this.personType)
//                        .toPort(toPort)
//                        .toPerson(toPerson)
//                        .toPersonType(toPersonType)
//                        .build();
//        // От кого кому
//        messageWrapper.history_List.add(from_to);
//        // Сериализуем и отправляем сообщение на сервер
//        String msg = messageWrapper.serialize();
//
//
//        return send(msg);
//    }

    @JsonIgnore
    public String send(String msg) {
        ByteBuffer buffer = ByteBuffer.allocate(16384);
        buffer.clear();
        buffer.put(msg.getBytes());
        // НА начало буфера переходим
        buffer.flip();
        String response = null;
        try {
            this.socketChannel.write(buffer);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @JsonIgnore
    public String readMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(16384);
        try {
            buffer.clear();
            this.socketChannel.read(buffer);
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


    @JsonIgnore
    public void send_I_Am_FreeFor(List<PersonType> list) throws JsonProcessingException {
        MessageFreeForPersonType message = new MessageFreeForPersonType(list,port, this);
        System.out.println("\n\nЖду какого-либо сообщения\t от " + message);

        sendMessageFreeForPersonType(message);

    }

//    public void startReadThread() {
//        System.out.println("startReadThread");
//        new Thread(() -> {
//            while (true) {
//                try {
//                    socketChannel.read(buffer);
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
//        socketChannel.close();
//        buffer = null;
//    }

    public void working(int timeSleep) throws InterruptedException {
        System.out.println();
        for (int i = timeSleep; i > 0; i--) {
            System.out.print("\r");
            System.out.print("\r                                                    ");
            System.out.print("\r");
            System.out.print("Осталось : " + i + " сек.");
            Thread.sleep(1000);
        }
        System.out.println("Закончил");
    }

}
