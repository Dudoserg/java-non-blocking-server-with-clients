package kek.person;


import com.fasterxml.jackson.core.JsonProcessingException;
import kek.message.Message;
import kek.message.MessageWrapper;

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
                        messageWrapperFrom.getFromPerson().getPersonType());
                Message message = messageWrapperFrom.getMessage();

                // ПИЦЦА
                if(message.getMessage().contains("pizza")){

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