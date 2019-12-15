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
                        new ArrayList<PersonType>(){
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

                // ПИЦЦА
                if(message.getMessage().contains("pizza")){
                    // Запрашиваю ресурсы у сервера ( Я Ж ФИЛАСАФ))
                    MessResource messResource = null ;
                    MessageWrapper messageWrapperResource = null;
                    {
                        messResource = new MessResource(
                                new ArrayList<Integer>(){{
                                    add(resource_1);
                                    add(resource_2);
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
                            new ArrayList<PersonType>(){
                                {
                                    add(PersonType.RESOURCE);
                                }
                            }
                    );
                    str = person.readMessage();

                    // Получил ресурсы, готовлю
                    System.out.println("Я получил нужные мне ресурсы");
                    // готовлю
                    this.person.working(7);
                    // Возвращаю ресурсы серверу

                    // Отправляю заказ
                }

                // Говорим серверу что готовы принимать сообщения от
                person.send_I_Am_FreeFor(
                        new ArrayList<PersonType>(){
                            {
                                add(PersonType.DISPATCHER);
                            }
                        }
                );
                System.out.println("i send I_AM_FREE");


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


    private String readFromFile(String fileName){
        String result = "";
        try(FileReader reader = new FileReader(fileName))
        {
            // читаем посимвольно
            int c;
            while((c=reader.read())!=-1){
                //System.out.print((char)c);
                result += (char) c;
            }
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
        return result;
    }


}