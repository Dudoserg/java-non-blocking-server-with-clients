package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.*;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Cook implements Runnable {


    private Person person;


    public static void main(String[] args) throws JsonProcessingException {
        Cook cook = new Cook();
        cook.run();
    }


    public Cook() {
        try {
            person = new Person(PersonType.COOK, "CookOLD");
            person.socketChannel =
                    SocketChannel.open(new InetSocketAddress("localhost", 3443));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        int resource_1 = 1;
        int resource_2 = 2;


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

                // Говорим серверу что готовы принимать сообщения от
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>() {
                            {
                                add(PersonType.DISPATCHER);
                            }
                        }
                );

                str = person.readMessage();

                MessageWrapper messageWrapperFrom = MessageWrapper.deserialize(str);

                System.out.println("i got message from " +
                        messageWrapperFrom.getFromPerson_last().getPersonType());

                Message message = messageWrapperFrom.getMessage();
                String order = message.getMessage();
                // ПИЦЦА
                if (order.contains("pizza")) {
                    // Запрашиваю ресурсы у сервера ( Я Ж ФИЛАСАФ))
                    MessResource messResource = null;
                    MessageWrapper messageWrapperResource = null;
                    {
                        messResource = new MessResource(
                                new ArrayList<Integer>() {{
                                    add(resource_1);
                                    add(resource_2);
                                    add(7);
                                }},
                                ResourceType.NEED
                        );
                        messageWrapperResource = MessageWrapper.builder()
                                .str(messResource.serialize())
                                .messageType(MessageType.MESSAGE_RESOURCE)
                                .build();
                        // Шлем СЕРВЕРУ
                        FROM_TO from_to =
                                FROM_TO.builder()
                                        .fromPerson(this.person)
                                        .fromPort(this.person.port)
                                        .fromPersonType(this.person.personType)
                                        .toPort(null)
                                        .toPerson(null)
                                        .toPersonType(null)
                                        .build();
                        messageWrapperResource.init();
                        messageWrapperResource.history_List.add(from_to);
                    }
                    person.send(messageWrapperResource.serialize());

                    // Говорим серваку, что мы можем принять только ресурсы от самого сервера
                    person.send_I_Am_FreeFor(
                            new ArrayList<PersonType>() {
                                {
                                    add(PersonType.RESOURCE);
                                }
                            }
                    );
                    str = person.readMessage();


                    // Проверяем что ресурсы пришли нужные


                    // Получил ресурсы, готовлю
                    System.out.println("Я получил нужные мне ресурсы");
                    // готовлю
                    System.out.println("Готовлю");
                    this.person.working(2);
                    System.out.println("Приготовил");


                    // Возвращаю ресурсы серверу
                    System.out.println("Возвращаю ресурсы серверу");
                    this.person.working(2);
                    MessResource messResourceReturn = null;
                    MessageWrapper messageWrapperResourceReturn = null;
                    {
                        messResourceReturn = new MessResource(
                                new ArrayList<Integer>() {{
                                    add(resource_1);
                                    add(resource_2);
                                    add(7);
                                }},
                                ResourceType.RETURN
                        );
                        messageWrapperResourceReturn = MessageWrapper.builder()
                                .str(messResourceReturn.serialize())
                                .messageType(MessageType.MESSAGE_RESOURCE)
                                .build();
                        // Шлем СЕРВЕРУ
                        FROM_TO from_to =
                                FROM_TO.builder()
                                        .fromPerson(this.person)
                                        .fromPort(this.person.port)
                                        .fromPersonType(this.person.personType)
                                        .toPort(null)
                                        .toPerson(null)
                                        .toPersonType(null)
                                        .build();
                        messageWrapperResourceReturn.init();
                        messageWrapperResourceReturn.history_List.add(from_to);
                    }
                    person.send(messageWrapperResourceReturn.serialize());
                    System.out.println("Ресурсы отправлены на сервер");


                    // Отправляю заказ
                    Message message_Gotoviy = null;
                    MessageWrapper messageWrapper_Gotoviy = null;
                    {
                        message_Gotoviy = Message.builder(order).build();

                        messageWrapper_Gotoviy = MessageWrapper.builder()
                                .str(message_Gotoviy.serialize())
                                .messageType(MessageType.MESSAGE)
                                // Сохраняем историю передачи сообщения, чтобы знать
                                // от какого покупателя, и каким диспетчером обслужено
                                .history_List(messageWrapperFrom.history_List)
                                .build();
                        // Шлем ЛЮБОМУ КУРЬЕРУ
                        FROM_TO from_to =
                                FROM_TO.builder()
                                        .fromPerson(this.person)
                                        .fromPort(this.person.port)
                                        .fromPersonType(this.person.personType)
                                        .toPort(null)
                                        .toPerson(null)
                                        .toPersonType(PersonType.COURIER)
                                        .build();
                        // Добавляем в историю передачи сообщения, то что заказ отправлен от нас
                        messageWrapper_Gotoviy.history_List.add(from_to);
                    }
                    person.send(messageWrapper_Gotoviy.serialize());
                }

                // Говорим серверу что готовы принимать сообщения от
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>() {
                            {
                                add(PersonType.DISPATCHER);
                            }
                        }
                );


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