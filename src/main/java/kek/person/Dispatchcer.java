package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.Message;
import kek.message.MessageWrapper;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Dispatchcer implements Runnable {


    private Person person;


    public static void main(String[] args) throws JsonProcessingException {
        Dispatchcer dispatchcer = new Dispatchcer();
        dispatchcer.run();
    }


    public Dispatchcer() {
        try {
            person = new Person(PersonType.DISPATCHER, "BATYA");
            person.socketChannel =
                    SocketChannel.open(new InetSocketAddress("localhost", 3443));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {

        System.out.println(this.person.getPersonName() + " start");
        //Scanner in = new Scanner(System.in);
        //System.out.print("Input a number for start working работы крч: ");
        //int num = in.nextInt();

        person.confirmConnectToServer();

        System.out.println("\tport = " + this.person.getPort());

        String str = ")))";
        // Начинается рабочий цикл
        try {
            while (true) {

                // Говорим серверу, что готовы принимать сообщения от
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>() {
                            {
                                add(PersonType.BUYER);
                                add(PersonType.COOK);
                                add(PersonType.COURIER);
                            }
                        }
                );

                str = person.readMessage();
                MessageWrapper messageWrapperFrom = MessageWrapper.deserialize(str);
                System.out.println("i got message from " +
                        messageWrapperFrom.getFromPerson().getPersonType());
                switch (messageWrapperFrom.getFromPerson().getPersonType()) {
                    case BUYER: {
                        Message messageFromBuyer = messageWrapperFrom.getMessage();
                        System.out.println("Он хочет : " + messageFromBuyer.getMessage());

                        // Передаем сообщение повару
                        System.out.println("Передаю сообщение повару");
                        this.person.working(5);


                        // Шлем ответ имеено данному покупателю
                        Message messageTo = new Message();

                        messageTo.setMessage(messageFromBuyer.getMessage());

                        // Отправляем сообщение ЛЮБОМУ ПОВАРУ
                        this.person.sendMessage(
                                messageTo,
                                null,
                                null, messageWrapperFrom.getFromPersonType()
                        );
                        System.out.println("Сообщение передано");

//                        // Шлем ответ имеено данному покупателю
//                        Message messageTo = new Message();
//
//                        messageTo.setMessage("========================" +
//                                "========================\n" +
//                                messageFromBuyer.getMessage().toUpperCase() +
//                                " ZAKAZ GOTOV ;-)\n" +
//                                "========================" +
//                                "========================");
//
//                        // Отправляем сообщение КОНКРЕТНОМУ покупателю
//                        this.person.sendMessage(
//                                messageTo,
//                                messageWrapperFrom.getFromPort(),
//                                messageWrapperFrom.getFromPerson(),
//                                messageWrapperFrom.getFromPersonType()
//                        );
                        break;
                    }
                    case COURIER: {
                        break;
                    }
                    case COOK: {
                        break;
                    }
                }

                // Говорим серверу, что готовы принимать сообщения от
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>() {
                            {
                                add(PersonType.BUYER);
                                add(PersonType.COOK);
                                add(PersonType.COURIER);
                            }
                        }
                );
                System.out.println("i send I_AM_FREE");

//                str = person.readMessage();
////
////                System.out.println("[" + (LocalDateTime.now().toLocalTime().toString()) + "]" +
////                        "i got = " + str);
////                System.out.println("i sleep");
////                Thread.sleep(10000);
////                System.out.println("i wake up");
////
////                person.send_I_Am_FreeFor();
////                System.out.println("i send I_AM_FREE");

            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private String readFromFile(String fileName) {
        String result = "";
        try (FileReader reader = new FileReader(fileName)) {
            // читаем посимвольно
            int c;
            while ((c = reader.read()) != -1) {
                //System.out.print((char)c);
                result += (char) c;
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return result;
    }
}