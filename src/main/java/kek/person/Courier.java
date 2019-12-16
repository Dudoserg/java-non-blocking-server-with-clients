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

public class Courier implements Runnable {


    private Person person;
    private Integer _1000 = 600;

    public static void main(String[] args) throws JsonProcessingException {
        Courier courier = new Courier();
        courier.run();
    }


    public Courier() {
        try {
            person = new Person(PersonType.COURIER, "Courier 228");
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

        person.confirmConnectToServer();

        System.out.println("\tport = " + this.person.getPort());

        String str = ")))";
        // Начинается рабочий цикл
        try {
            while (true) {
                System.out.println("\n\n\nЯ свободен");
                Thread.sleep(_1000);
                // Говорим серверу что готовы принимать сообщения от
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>() {
                            {
                                add(PersonType.COOK);
                            }
                        }
                );
                str = person.readMessage();

                MessageWrapper messageWrapperFromCook = MessageWrapper.deserialize(str);
                Message messageFromCook = messageWrapperFromCook.getMessage();

                System.out.println("i got message from " + messageWrapperFromCook.getFromPerson_last().getPersonType() +
                        " with # " + messageWrapperFromCook.getFromPerson_last().getPort());
                Thread.sleep(_1000);

                Integer portBuyer = 0;
                String order = messageFromCook.getMessage();


                // Ищем покупателя в истории, и берем его порт
                for (FROM_TO currentFROM_TO : messageWrapperFromCook.history_List) {
                    if (currentFROM_TO.getFromPersonType().equals(PersonType.BUYER))
                        portBuyer = currentFROM_TO.getFromPort();
                }


                System.out.println("Еду к покупателю # " + portBuyer);
                Thread.sleep(_1000);
                this.person.working(5);
                System.out.println("Приехал");
                Thread.sleep(_1000);


                // Передаю готовый заказ покупателю
                System.out.println("Передаю ему заказ");
                Thread.sleep(_1000);
                Message message_Order = null;
                MessageWrapper messageWrapper_Order = null;
                {
                    message_Order = Message.builder(order).build();

                    messageWrapper_Order = MessageWrapper.builder()
                            .str(message_Order.serialize())
                            .messageType(MessageType.MESSAGE)
                            // Сохраняем историю передачи сообщения, чтобы знать
                            // от какого покупателя, и каким диспетчером обслужено
                            .history_List(messageWrapperFromCook.history_List)
                            .build();
                    // Шлем КОНКРЕТНОМУ покупателю
                    FROM_TO from_to =
                            FROM_TO.builder()
                                    .fromPerson(this.person)
                                    .fromPort(this.person.port)
                                    .fromPersonType(this.person.personType)
                                    .toPort(portBuyer)
                                    .toPerson(null)
                                    .toPersonType(PersonType.BUYER)
                                    .build();
                    // Добавляем в историю передачи сообщения, то что заказ отправлен от нас
                    messageWrapper_Order.history_List.add(from_to);
                }
                person.send(messageWrapper_Order.serialize());
                System.out.println("Заказ отдан");
                Thread.sleep(_1000);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        catch (InterruptedException e) {
//            e.printStackTrace();
//        }

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