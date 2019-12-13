import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Класс реализующий простой неблокирующий Socks 4 Proxy Сервер Реализуюший
 * только команду connect
 *
 * @author dgreen
 * @date 19.09.2009
 */
public class Socks4Proxy implements Runnable {
    int bufferSize = 8192;
    /**
     * Порт
     */
    int port;
    /**
     * Хост
     */
    String host;

    /**
     * Дополнительная информация цепляемая к каждому ключу {@link SelectionKey}
     *
     * @author dgreen
     * @date 19.09.2009
     */
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

    /**
     * так выглядит ответ ОК или Сервис предоставлен
     */
    static final byte[] OK = new byte[]{0x00, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    /**
     * Сердце неблокирующего сервера, практически не меняется от приложения к
     * приложению, разве что при использование неблокирующего сервера в
     * многопоточном приложение, и работе с ключами из других потоков, надо
     * будет добавить некий KeyChangeRequest, но нам в этом приложение это без
     * надобности
     */
    public void run() {
        try {
            // Создаём Selector
            Selector selector = SelectorProvider.provider().openSelector();
            // Открываем серверный канал
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            // Убираем блокировку
            serverChannel.configureBlocking(false);
            // Вешаемся на порт
            serverChannel.socket().bind(new InetSocketAddress(host, port));
            // Регистрация в селекторе
            serverChannel.register(selector, serverChannel.validOps());
            // Основной цикл работу неблокирующего сервер
            // Этот цикл будет одинаковым для практически любого неблокирующего
            // сервера
            while (selector.select() > -1) {
                // Получаем ключи на которых произошли события в момент
                // последней выборки
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        // Обработка всех возможнных событий ключа
                        try {
                            if (key.isAcceptable()) {
                                // Принимаем соединение
                                accept(key);
                            } else if (key.isConnectable()) {
                                // Устанавливаем соединение
                                connect(key);
                            } else if (key.isReadable()) {
                                // Читаем данные
                                ByteBuffer buffer = ByteBuffer.allocate(256);
                                answerWithEcho(buffer,key);
                                read(key);
                            } else if (key.isWritable()) {
                                // Пишем данные
                                write(key);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            close(key);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Функция принимает соединение, регистрирует ключ с интересуемым действием
     * чтение данных (OP_READ)
     *
     * @param key ключ на котором произошло событие
     * @throws IOException
     * @throws ClosedChannelException
     */
    private void accept(SelectionKey key) throws IOException, ClosedChannelException {
        // Приняли
        SocketChannel newChannel = ((ServerSocketChannel) key.channel()).accept();
        // Неблокирующий
        newChannel.configureBlocking(false);
        // Регистрируем в селекторе
        newChannel.register(key.selector(), SelectionKey.OP_READ);
    }

    private static void answerWithEcho(ByteBuffer buffer, SelectionKey key)
            throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        client.read(buffer);
        System.out.println(new String(buffer.array()));
        String str = new String(buffer.array());
        if (new String(buffer.array()).trim().equals("DUDOS")) {
            client.close();
            System.out.println("Not accepting client messages anymore");
        }

        buffer.flip();
        client.write(buffer);
        buffer.clear();
    }

    /**
     * Читаем данные доступные в данный момент. Функция бывает в двух состояних -
     * чтение заголовка запроса и непосредственного проксирование
     *
     * @param key ключ на котором произошло событие
     * @throws IOException
     * @throws UnknownHostException
     * @throws ClosedChannelException
     */

    private void read(SelectionKey key) throws IOException, UnknownHostException, ClosedChannelException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());
        if (attachment == null) {
            // Лениво инициализируем буферы
            key.attach(attachment = new Attachment());
            attachment.in = ByteBuffer.allocate(bufferSize);
        }
        if (channel.read(attachment.in) < 1) {
            // -1 - разрыв 0 - нету места в буфере, такое может быть только если
            // заголовок превысил размер буфера
            close(key);
        } else if (attachment.peer == null) {
            // если нету второго конца :) стало быть мы читаем заголовок
            readHeader(key, attachment);
        } else {
            // ну а если мы проксируем, то добавляем ко второму концу интерес
            // записать
            attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_WRITE);
            // а у первого убираем интерес прочитать, т.к пока не записали
            // текущие данные, читать ничего не будем
            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
            // готовим буфер для записи
            attachment.in.flip();
        }
    }


    private void readHeader(SelectionKey key, Attachment attachment) throws IllegalStateException, IOException,
            UnknownHostException, ClosedChannelException {
        byte[] ar = attachment.in.array();
        if (ar[attachment.in.position() - 1] == 0) {
            // Если последний байт \0 это конец ID пользователя.
            if (ar[0] != 4 && ar[1] != 1 || attachment.in.position() < 8) {
                // Простенькая проверка на версию протокола и на валидность
                // команды,
                // Мы поддерживаем только conect
                throw new IllegalStateException("Bad Request");
            } else {
                // Создаём соединение
                SocketChannel peer = SocketChannel.open();
                peer.configureBlocking(false);
                // Получаем из пакета адрес и порт
                byte[] addr = new byte[]{ar[4], ar[5], ar[6], ar[7]};
                int p = (((0xFF & ar[2]) << 8) + (0xFF & ar[3]));
                // Начинаем устанавливать соединение
                peer.connect(new InetSocketAddress(InetAddress.getByAddress(addr), p));
                // Регистрация в селекторе
                SelectionKey peerKey = peer.register(key.selector(), SelectionKey.OP_CONNECT);
                // Глушим запрашивающее соединение
                key.interestOps(0);
                // Обмен ключами :)
                attachment.peer = peerKey;
                Attachment peerAttachemtn = new Attachment();
                peerAttachemtn.peer = key;
                peerKey.attach(peerAttachemtn);
                // Очищаем буфер с заголовками
                attachment.in.clear();
            }
        }
    }

    /**
     * Запись данных из буфера
     *
     * @param key
     * @throws IOException
     */
    private void write(SelectionKey key) throws IOException {
        // Закрывать сокет надо только записав все данные
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());
        if (channel.write(attachment.out) == -1) {
            close(key);
        } else if (attachment.out.remaining() == 0) {
            if (attachment.peer == null) {
                // Дописали что было в буфере и закрываемся
                close(key);
            } else {
                // если всё записано, чистим буфер
                attachment.out.clear();
                // Добавялем ко второму концу интерес на чтение
                attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_READ);
                // А у своего убираем интерес на запись
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
            }
        }
    }

    /**
     * Завершаем соединение
     *
     * @param key ключ на котором произошло событие
     * @throws IOException
     */
    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());
        // Завершаем соединение
        channel.finishConnect();
        // Создаём буфер и отвечаем OK
        attachment.in = ByteBuffer.allocate(bufferSize);
        attachment.in.put(OK).flip();
        attachment.out = ((Attachment) attachment.peer.attachment()).in;
        ((Attachment) attachment.peer.attachment()).out = attachment.in;
        // Ставим второму концу флаги на на запись и на чтение
        // как только она запишет OK, переключит второй конец на чтение и все
        // будут счастливы
        attachment.peer.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        key.interestOps(0);
    }

    /**
     * No Comments
     *
     * @param key
     * @throws IOException
     */
    private void close(SelectionKey key) throws IOException {
        key.cancel();
        key.channel().close();
        SelectionKey peerKey = ((Attachment) key.attachment()).peer;
        if (peerKey != null) {
            ((Attachment) peerKey.attachment()).peer = null;
            if ((peerKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                ((Attachment) peerKey.attachment()).out.flip();
            }
            peerKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    public static void main(String[] args) {
        Socks4Proxy server = new Socks4Proxy();
        server.host = "localhost";
        server.port = 3443;
        server.run();
    }
}