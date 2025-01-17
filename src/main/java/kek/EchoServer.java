package kek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import kek.message.*;
import kek.person.Person;
import kek.person.PersonType;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Semaphore;

public class EchoServer implements Runnable {

    public static void main(String[] args) {
        EchoServer echoServer = new EchoServer();
        echoServer.run();
    }

    static class Attachment {
        /**
         * Буфер для чтения, в момент проксирования становится буфером для
         * записи для ключа хранимого в peer
         * <p>
         * ВАЖНО: При парсинге Socks4 заголовком мы предполагаем что размер
         * буфера, больше чем размер нормального заголовка, у браузера Mozilla
         * Firefox, размер заголовка равен 12 байт 1 версия + 1 команда + 2 порт +
         * 4 ip + 3 id (MOZ) + 1 \0
         */

        ByteBuffer in;
        /**
         * Буфер для записи, в момент проксирования равен буферу для чтения для
         * ключа хранимого в peer
         */
        ByteBuffer out;
        /**
         * Куда проксируем
         */
        SelectionKey peer;

    }

    Selector selector = null;
    ServerSocketChannel serverSocket_1;
    ServerSocketChannel serverSocket_2;
    SelectionKey selectionKey_1;
    SelectionKey selectionKey_2;

    ByteBuffer buffer;

    Thread_SendMessage threadSendMessage;
    Semaphore semaphore_clients_list = new Semaphore(1);

    private int numMessage = 0;

    private static final String POISON_PILL = "POISON_PILL";

    List<Client> clients_list = new ArrayList<>();

    HashMap<SocketChannel, Client> clients_map = new HashMap<SocketChannel, Client>();

    // Очередь запросов от покупателей
    List<Pair<Client, MessageWrapper>> buyerQueueList = new LinkedList<>();
    List<Pair<Client, MessageWrapper>> dispatcherQueueList = new LinkedList<>();
    List<Pair<Client, MessageWrapper>> cookQueueList = new LinkedList<>();
    List<Pair<Client, MessageWrapper>> courierQueueList = new LinkedList<>();


    HashMap<PersonType, List<Pair<Client, MessageWrapper>>> requestQueueHashMap
            = new HashMap<PersonType, List<Pair<Client, MessageWrapper>>>() {
        {
            put(PersonType.BUYER, buyerQueueList);
            put(PersonType.DISPATCHER, dispatcherQueueList);
            put(PersonType.COOK, cookQueueList);
            put(PersonType.COURIER, courierQueueList);
        }
    };

    Boolean freeResources[];
    List<Pair<Client, MessageWrapper>> needResource = new ArrayList<>();

    public EchoServer() {
        try {
            selector = Selector.open();
            serverSocket_1 = ServerSocketChannel.open();
            serverSocket_1.bind(new InetSocketAddress("localhost", 3443));
            serverSocket_1.configureBlocking(false);
            selectionKey_1 = serverSocket_1.register(selector, SelectionKey.OP_ACCEPT);

            serverSocket_2 = ServerSocketChannel.open();
            serverSocket_2.bind(new InetSocketAddress("192.168.1.2", 3443));
            serverSocket_2.configureBlocking(false);
            selectionKey_2 = serverSocket_2.register(selector, SelectionKey.OP_ACCEPT);


            buffer = ByteBuffer.allocate(16384);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.init();
    }

    public void init() {
        // Инициализируем 100 ресурсов, ну шоб с запасом
//        IntStream.range(0, 100).forEach(value -> freeResources.add(true));
        freeResources = new Boolean[100];
        for (int i = 0; i < freeResources.length; i++) {
            freeResources[i] = true;
        }
    }

    @Override
    public void run() {
        System.out.println("start server");
        threadSendMessage = new Thread_SendMessage();
        threadSendMessage.start();

        // GC мусор
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.gc();
//                System.out.println("GC from " + Thread.currentThread().getId() + "\t" + Thread.currentThread().getName());
                System.out.println(".");
            }
        }).start();

        // Обеспечение философов ресурсами
        new Thread(() -> {
            try {
                while (true) {

                    // Перебираем все запросы на ресурсы
                    Iterator<Pair<Client, MessageWrapper>> it = needResource.iterator();
                    boolean reservedResource[] = new boolean[freeResources.length];
                    while (it.hasNext()) {
                        Pair<Client, MessageWrapper> pair = it.next();
                        Client client = pair.getKey();
                        SocketChannel socketChannel = client.getSocketChannel();
                        MessageWrapper messageWrapper = pair.getValue();
                        MessResource messResource = messageWrapper.getMessResource();

                        // Если пользователь Запрашивает ресурсы
                        if (messResource.getResourceType().equals(ResourceType.NEED)) {
                            boolean allResourcesIsFree = true;
                            // Смотрим, чтобы все элементы были свободны, и не зарезервированы
                            for (int i = 0; i < messResource.getResource().size(); i++) {
                                int numNeedResource = messResource.getResource().get(i);
                                if (freeResources[numNeedResource] == true && reservedResource[numNeedResource] == false) {

                                } else {
                                    allResourcesIsFree = false;
                                    // Дальше уже можно не проверять, один из необходимых ресурсов занят
                                    break;
                                }
                            }

                            if (allResourcesIsFree) {
                                // Все необходимые ресурсы свободны
                                // Блокируем ресурсы
                                for (int q = 0; q < messResource.getResource().size(); q++) {
                                    int numNeedResource = messResource.getResource().get(q);
                                    this.freeResources[numNeedResource] = false;
                                }
                                // отправляем их юзеру
                                ByteBuffer byteBuffer = ByteBuffer.allocate(16384);

                                MessResource messResourceSend = new MessResource(
                                        new ArrayList<Integer>() {{
                                            this.addAll(messResource.getResource());
                                        }},
                                        ResourceType.SEND
                                );
                                MessageWrapper messageWrapperSend = MessageWrapper.builder()
                                        .str(messResourceSend.serialize())
                                        .messageType(MessageType.MESSAGE_RESOURCE)
                                        .build();
                                messageWrapperSend.init();
                                // пихаем туда сообщение
                                String str = null;
                                try {
                                    str = messageWrapperSend.serialize();
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                byteBuffer.clear();
                                byteBuffer.put(str.getBytes());
                                // НА начало буфера переходим
                                byteBuffer.flip();

                                try {
                                    // Пишем в него
                                    socketChannel.write(byteBuffer);
                                    System.out.println("to " + client.getPerson().getPersonType() + " #" + client.getPort() + " send = " + str);
                                    // Теперь данный клиент занят, и мы не можем ему послать новое сообщение, т.к. он его не успеет обработать
                                    client.setFree(false);
                                    System.out.println(client.getPerson().getPersonType() + " #" + client.getPort() + " free = false ");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                byteBuffer.flip();
                                // Удаляем данный запрос
                                it.remove();
                            } else {
                                // Резервируем ресурсы
                                for (int i = 0; i < messResource.getResource().size(); i++) {
                                    int numNeedResource = messResource.getResource().get(i);
                                    reservedResource[numNeedResource] = true;
                                }
                                //continue;
                            }
                        }
                    }

                    // Если пользователь отдает ресурс
                    // Делаем это в отдельном цикле, чтобы не нарушить очередность
                    // т.к. вернуть может 5й из списка, а получит ресурсы 10й, хотя их же ждет 1й в очереди

                    // Перебираем все запросы
                    it = needResource.iterator();
                    while (it.hasNext()) {
                        Pair<Client, MessageWrapper> pair = it.next();
                        Client client = pair.getKey();
                        SocketChannel socketChannel = client.getSocketChannel();
                        MessageWrapper messageWrapper = pair.getValue();
                        MessResource messResource = messageWrapper.getMessResource();

                        // Если пользователь ОСВОБОЖДАЕТ ресурсы
                        if (messResource.getResourceType().equals(ResourceType.RETURN)) {

                            for (int i = 0; i < messResource.getResource().size(); i++) {
                                if (this.freeResources[messResource.getResource().get(i)])
                                    System.out.println("Чета не то с ресурсами, мы освобождаем свободный)00");
                                this.freeResources[messResource.getResource().get(i)] = true;
                            }
                            // Удаляем сообщение об освобождении, т.к. уже освободили
                            it.remove();
                        }
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }).start();

        // Рассылка сообщений из очереди, если они имеются
        // И если есть кому отправлять)0
        new Thread(() -> {
            try {
                while (true) {

                    //System.out.println("check message");

                    semaphore_clients_list.acquire();

                    for (Map.Entry<PersonType, List<Pair<Client, MessageWrapper>>> entry : requestQueueHashMap.entrySet()) {
                        PersonType fromPersonType = entry.getKey();     // Тут ОТ КОГО ЗАПРОС (его тип)
                        // ОТ КОГО сообщение, САМО сообщение
                        List<Pair<Client, MessageWrapper>> requests = entry.getValue(); // ЗАПРОСЫ
                        // Смотрим сообщения от ПОКУПАТЕЛЕЙ

                        // итератор по сообщениеям от текущего типа клиентов
                        Iterator<Pair<Client, MessageWrapper>> it = requests.iterator();
                        while (it.hasNext()) {
                            Pair<Client, MessageWrapper> pair = it.next();

                            Client fromClient = pair.getKey();
                            MessageWrapper fromMessageWrapper = pair.getValue();
                            PersonType toPersonType = fromMessageWrapper.getToPersonType_last();

                            //Message message = null;
                            //try {
                            //    message = fromMessageWrapper.getMessage();
                            //} catch (JsonProcessingException e) {
                            //    e.printStackTrace();
                            //}

                            // Ищем свободного нужного ПОЛУЧАТЕЛЯ

                            for (Client toClient : clients_list) {

                                SocketChannel toSocketChannel = toClient.getSocketChannel();
                                boolean flag = true;
                                //if dispatcherChannel.isOpen()

                                // Клиент прошел регистрацию
                                flag = flag & toClient.isClientNotified();
                                if (!flag)
                                    continue;
                                // Клиент нужного нам типа
                                flag = flag & toClient.getPerson().getPersonType().equals(toPersonType);
                                if (!flag)
                                    continue;
                                // Клиент в данный момент свободен
                                flag = flag & toClient.isFree();
                                if (!flag)
                                    continue;
                                // ИМЕННО ДЛЯ НАС
                                flag = flag & toClient.isFree(fromPersonType);
                                if (!flag)
                                    continue;
                                // Если нужно отправить именно по порту, то добавим и его в проверку условия
                                if (fromMessageWrapper.getToPort_last() != null)
                                    flag = flag & (fromMessageWrapper.getToPort_last().equals(toClient.getPort()));

                                // проверяем, данный клиент является ли тем, кому надо послать сообщение
                                // и то, что он свободен в данный момент
                                if (flag) {
                                    // Если to свободен, шлем ему сообщение от from
                                    // Создаем буфер
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(16384);

                                    // пихаем туда сообщение
                                    String str = null;
                                    try {
                                        str = fromMessageWrapper.serialize();
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                    byteBuffer.clear();
                                    byteBuffer.put(str.getBytes());
                                    // НА начало буфера переходим
                                    byteBuffer.flip();

                                    try {
                                        // Пишем в него
                                        toSocketChannel.write(byteBuffer);
                                        System.out.println("to " + toPersonType + " #" + toClient.getPort() + " send = " + str);
                                        // Теперь данный клиент занят, и мы не можем ему послать новое сообщение, т.к. он его не успеет обработать
                                        toClient.setFree(false);
                                        System.out.println(toPersonType + " #" + toClient.getPort() + " free = false ");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    byteBuffer.flip();

                                    //!!!!!!!!!!!!!1
                                    // Удаляем сообщение, его уже передали диспетчеру
                                    it.remove();
                                    break;
                                }
                            }
                        }
                    }
                    // Спим
                    //System.out.println("end check message");

                    semaphore_clients_list.release();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }).start();

        // В данном потоке рассылка всем клиентам времени каждые 2 секунды

//        new Thread(() -> {
//            while (true) {
//                if (this.clients_list.size() > 0) {
//
//                    // Создаем буфер
//                    ByteBuffer byteBuffer = ByteBuffer.allocate(16384);
//                    String str = null;
//                    try {
//                        str = this.serialize();
//                    } catch (JsonProcessingException e) {
//                        e.printStackTrace();
//                    }
//                    // пихаем туда строку
//                    byteBuffer.clear();
//                    byteBuffer.put(str.getBytes());
//                    // НА начало буфера переходим
//                    byteBuffer.flip();
//                    // Итератор по клиентам
//                    Iterator<Client> it = clients_list.iterator();
//                    // ПРоходимся по всем клиентам
//                    while (it.hasNext()) {
//                        // очередной клиент
//                        Client client = it.next();
//                        SocketChannel socketChannel = client.getSocketChannel();
//
//                        // Если клиент прошел подтверждение регистрации
//                        // Если клиент не занят
//                        if (client.isClientNotified() && client.isFree()) {
//                            // Если клиент открыт
//                            if (socketChannel.isOpen() ) {
//                                // Отправляем сообщение клиенту
//                                try {
//                                    // Пишем в него
//                                    socketChannel.write(byteBuffer);
//                                    System.out.println("to socketChannel #" + client.getPort() + " send = " + str);
//                                    // Теперь данный клиент занят, и мы не можем ему послать новое сообщение, т.к. он его не успеет обработать
//                                    client.setFree(false);
//                                    System.out.println("socketChannel #" + client.getPort() + " free = false ");
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                byteBuffer.flip();
//                            } else {
//                                // Закрываем сокет клиента, ХЗ надо ли
//                                try {
//                                    client.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                // Удаляем клиента из clients_list, т.к. он закрыт
//                                it.remove();
//                                System.out.println("Клиент #" + client.getPort() + " закрыт, Удаляем из списка");
//                            }
////                        try {
////
////                        } catch (ClosedChannelException e) {
////                            e.printStackTrace();
////                        } catch (IOException e) {
////                            e.printStackTrace();
////                        }
//                        }
//                        byteBuffer.clear();
//                    }
//                }
//                try {
//                    Thread.sleep(1999);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();


        // Работа по принятию сообщений

        try {
            // Ждем очередного события
            while (selector.select() > -1) {
                // Ключи событий
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                // Итератор по этим ключам( в них вроде находятся клиенты наши )
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                threadSendMessage.not();

                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {

                        if(key.equals(selectionKey_1))
                            register(selector, serverSocket_1);
                        else if(key.equals(selectionKey_2))
                            register(selector, serverSocket_2);

                    } else if (key.isReadable()) {
                        // Проверяем, не отвалился ли клиент, если происходит отвал
                        // То сообщение будет пустым
                        ByteBuffer buff = ByteBuffer.allocate(32);
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        try {
                            socketChannel.read(buffer);
                        } catch (IOException e) {
                            Client client = clients_map.get(socketChannel);
                            // Закрываем клиента, и удаляем его из списка
                            System.out.println("Клиент #" + client.getPort() + " отвалился, закрываем");
                            client.close();
                            System.out.println("Клиент #" + client.getPort() + " закрыт, Удаляем из списка");


                            semaphore_clients_list.acquire();

                            // удаляем из списков
                            clients_map.remove(socketChannel);
                            clients_list.remove(client);

                            semaphore_clients_list.release();

                            // Удалим запрос этого пользователя, если он был
                            ////////////////!!!!!!!!!!!!!!!!!!!!!
                            ////////////////!!!!!!!!!!!!!!!!!!!!!

                            // закрываем
                            key.cancel();
                            socketChannel.close();
                            continue;
                        }
                        read(buffer, key);
                        //answerWithEcho(buffer, key);

                    } else if (key.isWritable()) {

                        //answerWithEcho(buffer, key);
                        System.out.println();

                    } else if (key.isConnectable()) {

                        //answerWithEcho(buffer, key);
                        System.out.println();

                    }

                    iter.remove();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }


    /// Регистрация клиента
    private void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException, InterruptedException {
        semaphore_clients_list.acquire();

        SocketChannel socketChannel = serverSocket.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);

        // Запоминаем  клиента При подключении
        Client myClient = new Client(socketChannel);


        clients_list.add(myClient);
        clients_map.put(socketChannel, myClient);

        // Отправляем ему OK
        ByteBuffer buffer = ByteBuffer.allocate(16384);

        MessConfirm messConfirm =
                MessConfirm.builder("OK", myClient.getPort())
                        .build();

        MessageWrapper messageWrapper =
                MessageWrapper.builder()
                        .str(messConfirm.serialize())
                        .messageType(MessageType.MESSAGE_CONFIRM)
                        .build();


        String str = messageWrapper.serialize();
        System.out.println();
        buffer.put(str.getBytes());
        buffer.flip();
        socketChannel.write(buffer);
        buffer.clear();
        semaphore_clients_list.release();
    }

    private boolean read(ByteBuffer buffer, SelectionKey key) throws IOException, InterruptedException {
        semaphore_clients_list.acquire();
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Client client = clients_map.get(socketChannel);

        socketChannel.read(buffer);
        String message_str = new String(buffer.array());

        System.out.println("i got " + message_str + "\n\tfrom socketChannel #" + client.getPort());

        MessageWrapper messageWrapper =
                MessageWrapper.deserialize(message_str);

        switch (messageWrapper.getMessageType()) {
            // Если клиент еще не до конца зарегестрирован, и пришло сообщение с подтверждением
            case MESSAGE_CONFIRM: {
                // Вытаскиваем сообщение о подтверждении
                MessConfirm messConfirm = messageWrapper.getMessConfirm();

                if (!client.isClientNotified()) {
                    // Если он правильно запомнил его порт
                    if (client.getPort().equals(messConfirm.getPort())) {
                        // ЗНАЧИТ
                        // Клиент уведомлен об его айди
                        client.setClientNotified(true);
                        // Запоминаем Тип клиента
                        client.setPerson(messConfirm.getPerson());
                        // Клиент готов к приему данных
                        client.setFree(false);
                        System.out.println("socketChannel #" + client.getPort() + " isClientNotified = true");
                    }
                }
                break;
            }
            case MESSAGE: {
                Person p = clients_map.get(socketChannel).getPerson();
                switch (p.getPersonType()) {
                    case BUYER: {
                        // Добавляем запрос от покупателя в очередь
                        this.buyerQueueList.add(new Pair<>(client, messageWrapper));
                        break;
                    }
                    case DISPATCHER: {
                        // Добавляем запрос от диспетчера в очередь
                        this.dispatcherQueueList.add(new Pair<>(client, messageWrapper));
                        break;
                    }
                    case COOK: {
                        this.cookQueueList.add(new Pair<>(client, messageWrapper));
                        break;
                    }
                    case COURIER: {
                        this.courierQueueList.add(new Pair<>(client, messageWrapper));
                        break;
                    }
                }
                break;
            }
            case MESSAGE_FREE_FOR_PERSONTYPE: {
                MessageFreeForPersonType message = messageWrapper.getMessageFreeForPersonType();
                // Клиент прислал сообщение о том, что он теперь не занят
                System.out.println("socketChannel #" + client.getPort() + message.toString());
                // Вот эти ребята могут посылать сообщение клиенту
                client.setFreeFor(message.getList());
                // Помечаем его свободным, чтобы можно было послать ему сообщение
                client.setFree(true);
                break;
            }
            case MESSAGE_RESOURCE: {
                MessResource message = messageWrapper.getMessResource();
                // Клиент прислал сообщение о том, что он теперь не занят
                System.out.println("socketChannel #" + client.getPort() + message.toString());

                this.needResource.add(new Pair<>(client, messageWrapper));

                break;
            }
        }
        buffer.clear();
        semaphore_clients_list.release();
        return true;
    }


    private boolean answerWithEcho(ByteBuffer buffer, SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        client.read(buffer);

        System.out.println("i got " + new String(buffer.array()));

        if (new String(buffer.array()).trim().equals(POISON_PILL)) {
            client.close();
            System.out.println("Not accepting socketChannel messages anymore");
        }

        buffer.flip();
        client.write(buffer);
        buffer.clear();
        return true;
    }

    //    Метод start () определен так, что эхо-сервер может быть
    //    запущен как отдельный процесс во время модульного тестирования.
    public static Process start() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = EchoServer.class.getCanonicalName();

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);

        return builder.start();
    }

    private void close(SelectionKey key) throws IOException {
        key.cancel();
        key.channel().close();
        SelectionKey peerKey = ((EchoServer.Attachment) key.attachment()).peer;
        if (peerKey != null) {
            ((EchoServer.Attachment) peerKey.attachment()).peer = null;
            if ((peerKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                ((EchoServer.Attachment) peerKey.attachment()).out.flip();
            }
            peerKey.interestOps(SelectionKey.OP_WRITE);
        }
    }


    // Jackson
    public static EchoServer deserialize(String serializedObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        EchoServer messageFromClient = objectMapper.readValue(serializedObject, EchoServer.class);
        return messageFromClient;
    }

    //Jackson
    public static String serialize(EchoServer message) throws JsonProcessingException {

        return new ObjectMapper().writeValueAsString(message);
    }

    //Jackson
    public String serialize() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

}
