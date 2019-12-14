package baeldung;

import baeldung.person.PersonType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.channels.SocketChannel;

@Data
@Getter
@Setter
public class Client {
    private SocketChannel socketChannel;
    private Integer port;
    private String localAddress;
    private String remoteAddress;
    // Уведомлен ли клиент о своем id
    // Для этого при подключении клиента сервер отправляет OK-port
    // Клиент отвечает OK-port, в таком случае клиент уведомлен
    private boolean clientNotified = false;   // Если true, то клиент активен, и знает свой id ( port )

    //
    private PersonType personType;

    public Client(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.localAddress = this.socketChannel.getLocalAddress().toString();
        this.remoteAddress = this.socketChannel.getRemoteAddress().toString();
        this.port = this.socketChannel.socket().getPort();

        System.out.println();
        System.out.println("===========================================================================");
        System.out.println("Create new User");
        System.out.println("this.socketChannel.socket().getPort() " + this.socketChannel.socket().getPort());
        System.out.println("this.socketChannel.socket().getLocalPort() " + this.socketChannel.socket().getLocalPort());
        System.out.println("this.socketChannel.getRemoteAddress() " + this.socketChannel.getRemoteAddress());
        System.out.println("this.socketChannel.getLocalAddress()" + this.socketChannel.getLocalAddress());
//        System.out.println("this.socketChannel.socket().getLocalAddress()" + this.socketChannel.socket().getLocalAddress());
//        System.out.println("this.socketChannel.socket().getInetAddress()" + this.socketChannel.socket().getInetAddress());
//        System.out.println("this.socketChannel.socket().getReuseAddress()" + this.socketChannel.socket().getReuseAddress());
//        System.out.println("this.socketChannel.socket().getLocalSocketAddress()" + this.socketChannel.socket().getLocalSocketAddress());
//        System.out.println("this.socketChannel.socket().getRemoteSocketAddress()" + this.socketChannel.socket().getRemoteSocketAddress());
        System.out.println("===========================================================================");
        System.out.println();
    }

    public void close() throws IOException {
        this.socketChannel.close();
    }
}
