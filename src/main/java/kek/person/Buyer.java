package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.FROM_TO;
import kek.message.Message;
import kek.message.MessageType;
import kek.message.MessageWrapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Scanner;

public class Buyer implements Runnable {


    private Person person;
    private Integer _1000 = 1000;

    public static void main(String[] args) throws JsonProcessingException {
        Buyer buyer = new Buyer();
        buyer.run();
    }


    public Buyer() {
        try {
            person = new Person(PersonType.BUYER, "BATYA");
            person.set_1000(this._1000);
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
        boolean flagWork = true;
        try {
            while (flagWork) {
                System.out.println("\n\n");
                Thread.sleep(_1000);

                Message message = null;
                MessageWrapper messageWrapper = null;
                {
                    message = Message.builder("pizza" + num).build();

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
                System.out.println("Звоню , и заказываю " + message.getMessage());
                Thread.sleep(_1000);
                // Отправляем сообщение
                str = messageWrapper.serialize();
                person.send(str);


                // Говорим серверу, что готовы принимать сообщения от ДИСПЕТЧЕРА И КУОБЕОА
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>() {
                            {
                                add(PersonType.DISPATCHER);
                                add(PersonType.COURIER);
                            }
                        }
                );
                str = person.readMessage();
                Thread.sleep(_1000);


                MessageWrapper messageWrapperFrom = MessageWrapper.deserialize(str);
                Message messageFrom = messageWrapperFrom.getMessage();

                switch (messageWrapperFrom.getFromPerson_last().getPersonType()) {
                    case DISPATCHER: {
                        System.out.println("Получил ответ от " +
                                messageWrapperFrom.getFromPerson_last().getPersonType());
                        Thread.sleep(_1000);
                        System.out.println(" они не могут сделать мне мой заказ ((");
                        Thread.sleep(_1000);
                        System.out.println(" Думаю что бы такого заказать");
                        person.working(3);
                        break;
                    }
                    case COURIER: {
                        System.out.println("Получил ответ от " +
                                messageWrapperFrom.getFromPerson_last().getPersonType());
                        Thread.sleep(_1000);
                        System.out.println("i got  " + messageFrom.getMessage());
                        Thread.sleep(_1000);

                        System.out.println("Начинаю есть свой заказ");
                        this.person.working(10);
                        System.out.println("Съел =) ");
                        Thread.sleep(_1000);
                        System.out.println("Ну все, BYE BYE");
                        person.working(3);
                        flagWork = false;
                        break;
                    }
                }
            }

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