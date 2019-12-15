package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.Message;
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



            Message message = new Message();
            message.setMessage("pizza" + num);
            message.setPort(this.person.port);
            message.setPerson(this.person);
            System.out.println("Звоню , и заказываю " + message.getMessage());
            person.sendMessage(message, null, null, PersonType.DISPATCHER);
            System.out.println("Жду ответа");

            // Говорим серверу, что готовы принимать сообщения от
            person.send_I_Am_FreeFor(
                    new ArrayList<PersonType>(){
                        {
                            add(PersonType.DISPATCHER);
                        }
                    }
            );

            str = person.readMessage();
//            System.out.println(str);

            MessageWrapper messageWrapper = MessageWrapper.deserialize(str);
            Message fromMessage = messageWrapper.getMessage();
            switch (messageWrapper.getFromPerson().getPersonType()){
                case DISPATCHER:{
                    System.out.println("Получил ответ от " +
                            messageWrapper.getFromPerson().getPersonType());
                    System.out.println("i got  " + fromMessage.getMessage());

                    System.out.println("Начинаю есть свой заказ");
                    this.person.working(10);
                    System.out.println("Съел");

                    break;
                }
                case COURIER:{
                    System.out.println("Получил ответ от " +
                            messageWrapper.getFromPerson().getPersonType());
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