package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;

public class Buyer implements Runnable {


    private Person person;


    public static void main(String[] args) throws JsonProcessingException {
        Buyer buyer = new Buyer();
        buyer.run();
    }


    public Buyer() {
        try {
            person = new Person(PersonType.DISPATCHER, "BATYA");
            person.socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 3443));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {

        person.confirmConnectToServer();

        String str = ")))";
        // Начинается рабочий цикл
        try {
            while (true) {
                str = person.readMessage();

                System.out.println("[" + (LocalDateTime.now().toLocalTime().toString()) + "]" +
                        "i got = " + str);
                System.out.println("i sleep");
                Thread.sleep(10000);
                System.out.println("i wake up");

                person.send_I_Am_Free();
                System.out.println("i send I_AM_FREE");

            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}