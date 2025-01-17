package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.FROM_TO;
import kek.message.Message;
import kek.message.MessageType;
import kek.message.MessageWrapper;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Dispatchcer implements Runnable {


    private Person person;
    private Integer _1000 = 500;


    public static void main(String[] args) throws JsonProcessingException {
        Dispatchcer dispatchcer = new Dispatchcer();
        dispatchcer.run();
    }


    public Dispatchcer() {
        try {
            person = new Person(PersonType.DISPATCHER, "BATYA");
            person.set_1000(this._1000);
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
                System.out.println("\n\n\nЯ свободен");
                Thread.sleep(_1000);
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
                        messageWrapperFrom.getFromPerson_last().getPersonType());
                Thread.sleep(_1000);

                // Смотрим от кого пришло сообщение
                switch (messageWrapperFrom.getFromPerson_last().getPersonType()) {
                    case BUYER: {
                        Message messageFromBuyer = messageWrapperFrom.getMessage();
                        System.out.println("Покупатель хочет : " + messageFromBuyer.getMessage());
                        Thread.sleep(_1000);

                        // Передаем сообщение повару
                        System.out.println("Передаю сообщение повару");
                        this.person.working(3);
                        // Шлем ответ Любому повару
                        Message messageTo = null;
                        MessageWrapper messageWrapperToCook = null;
                        {
                            messageTo = Message.builder(messageFromBuyer.getMessage())
                                    .build();

                            messageWrapperToCook = MessageWrapper.builder()
                                    .str(messageTo.serialize())
                                    .messageType(MessageType.MESSAGE)
                                    .history_List(messageWrapperFrom.history_List)
                                    .build();
                            // Шлем ответ Любому повару
                            FROM_TO from_to =
                                    FROM_TO.builder()
                                            .fromPerson(this.person)
                                            .fromPort(this.person.port)
                                            .fromPersonType(this.person.personType)
                                            .toPort(null)
                                            .toPerson(null)
                                            .toPersonType(PersonType.COOK)
                                            .build();
                            messageWrapperToCook.history_List.add(from_to);
                        }
                        this.person.send(messageWrapperToCook.serialize());

                        System.out.println("Сообщение Повару Отправлено");
                        Thread.sleep(_1000);
                        break;
                    }
                    case COURIER: {
                        break;
                    }
                    case COOK: {
                        break;
                    }
                }
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