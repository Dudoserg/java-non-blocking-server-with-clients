package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.FROM_TO;
import kek.message.Message;
import kek.message.MessageType;
import kek.message.MessageWrapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;

public class Buyer implements Runnable {


    private Person person;


    public static void main(String[] args) throws JsonProcessingException {
        Buyer buyer = new Buyer();
        buyer.run();
    }


    public Buyer() {
        try {
            person = new Person(PersonType.BUYER, "BATYA");
            person.socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 3443));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {

        System.out.println(this.person.getPersonName() + " start");
        Scanner in = new Scanner(System.in);
        System.out.print("Input a number: ");
        int num = in.nextInt();

        person.confirmConnectToServer();

        System.out.println("\tport = " + this.person.getPort());

        String str = ")))";
        // Начинается рабочий цикл
        try {
            //while (true) {


            Message message = null;


            MessageWrapper messageWrapper = null;
            {
                message = Message.builder("pizza" + num).build();
                System.out.println("Звоню , и заказываю " + message.getMessage());

                messageWrapper = MessageWrapper.builder()
                        .str(message.serialize())
                        .messageType(MessageType.MESSAGE)
                        .build();
                FROM_TO from_to =
                        FROM_TO.builder()
                                .fromPerson(person)
                                .fromPort(person.port)
                                .fromPersonType(person.personType)
                                .toPort(null)
                                .toPerson(null)
                                .toPersonType(PersonType.DISPATCHER)
                                .build();
                messageWrapper.init();
                messageWrapper.history_List.add(from_to);
            }
            // Отправляем сообщение
            str = messageWrapper.serialize();
            person.send(str);


            // Говорим серверу, что готовы принимать сообщения от ДИСПЕТЧЕРА
            person.send_I_Am_FreeFor(
                    new ArrayList<PersonType>() {
                        {
                            add(PersonType.DISPATCHER);
                        }
                    }
            );
            str = person.readMessage();



            messageWrapper = MessageWrapper.deserialize(str);
            Message fromMessage = messageWrapper.getMessage();

            switch (messageWrapper.getFromPerson_last().getPersonType()) {
                case DISPATCHER: {
                    System.out.println("Получил ответ от " +
                            messageWrapper.getFromPerson_last().getPersonType());
                    System.out.println("i got  " + fromMessage.getMessage());

                    System.out.println("Начинаю есть свой заказ");
                    this.person.working(10);
                    System.out.println("Съел");

                    break;
                }
                case COURIER: {
                    System.out.println("Получил ответ от " +
                            messageWrapper.getFromPerson_last().getPersonType());
                    break;
                }
            }
            System.out.println("Bye Bye");
            this.person.working(2);

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

            //}
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }


}