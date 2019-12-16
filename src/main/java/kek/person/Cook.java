package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.*;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Cook implements Runnable {


    private Person person;
    private Integer _1000 = 1000;

    public static void main(String[] args) throws JsonProcessingException {
        Cook cook = new Cook();
        cook.run();
    }


    public Cook() {
        try {
            person = new Person(PersonType.COOK, "CookOLD");
            person.set_1000(this._1000);
            person.socketChannel =
                    SocketChannel.open(new InetSocketAddress("localhost", 3443));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        List<Integer> needResourcesList = new ArrayList<>();

        System.out.println("Введите ресурсы, которые необходимы данному повару");
        Scanner in = new Scanner(System.in);
        int num = 0;
        while (true) {
            System.out.print("Введите номер следующего ресурса ( 0 для выхода): ");
            num = in.nextInt();
            if( num != 0)
                needResourcesList.add(num);
            else
                break;
        }

        System.out.println("Мне нужны ресурсы # " +
                needResourcesList.stream()
                        .map(Objects::toString)
                        .collect(Collectors.joining(", "))
        );


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
                Message message = messageWrapperFrom.getMessage();

                System.out.println("i got message from " +
                        messageWrapperFrom.getFromPerson_last().getPersonType());
                Thread.sleep(_1000);


                String order = message.getMessage();
                // ПИЦЦА
                if (order.contains("pizza")) {
                    // Запрашиваю ресурсы у сервера ( Я Ж ФИЛАСАФ))
                    MessResource messResource = null;
                    MessageWrapper messageWrapperResource = null;
                    {
                        messResource = new MessResource(
                                new ArrayList<Integer>() {{
                                    addAll(needResourcesList);
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
                    System.out.println("Запрашиваю ресурсы у сервера");
                    Thread.sleep(_1000);
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
                    System.out.println("Получил от сервера необходимые ресурсы");
                    Thread.sleep(_1000);

                    // готовлю
                    System.out.println("\n\t\t\tНачинаю готовить");
                    Thread.sleep(_1000);
                    this.person.working(10);
                    System.out.println("\t\t\tПриготовил\n");
                    Thread.sleep(_1000);


                    // Возвращаю ресурсы серверу
                    System.out.println("Возвращаю ресурсы серверу");
                    Thread.sleep(_1000);
                    MessResource messResourceReturn = null;
                    MessageWrapper messageWrapperResourceReturn = null;
                    {
                        messResourceReturn = new MessResource(
                                new ArrayList<Integer>() {{
                                    addAll(needResourcesList);
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
                    Thread.sleep(_1000);


                    // Отправляю заказ
                    System.out.println("Передаю готовый заказ курьеру");
                    Thread.sleep(_1000);
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

                    System.out.println("Сообщение курьеру отправлено");
                    Thread.sleep(_1000);
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