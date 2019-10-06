package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

class ReceiveThread extends Thread implements Runnable {

    DatagramSocket socket;
    static int port = 6999;

    public ReceiveThread(DatagramSocket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            receiveData(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //метод, который принимает сообщение от клиента
    public synchronized void receiveData(final DatagramSocket serverSocket) throws IOException {
        byte[] receivebuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        serverSocket.receive(receivePacket); //получили пакет
        final InetAddress IP = receivePacket.getAddress();
        final int PORT = receivePacket.getPort();
        String clientData = new String(receivePacket.getData()).trim(); //конвертируем данные в строку
        System.out.println(serverSocket.getLocalPort()+" -- "+IP+" "+PORT+": " + clientData); //и выводим на консоль

        if(clientData.equals("getTCPConnection")){

            Thread TCPConnection = new Thread(new Runnable() {
                @Override
                public void run() {
                    int TCP_Port = 0;
                    port++;
                    ServerSocket socket = null;

                    while (TCP_Port == 0){
                        try {
                            TCP_Port = port;
                            socket = new ServerSocket(TCP_Port);
                        }catch (BindException ex){
                            port++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    byte[] sendbuffer = new byte[1024];
                    String msg = "TCP port: "+TCP_Port;
                    sendbuffer = msg.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, IP, PORT);
                    try {
                        serverSocket.send(sendPacket); //высылаем клиенту номер порта TCP
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Socket TCPSocket = null; //извлекаем таким образом Socket, с которым будем работать
                    try {
                        TCPSocket = socket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Scanner scanner = null; //поток ввода
                    PrintWriter writer = null;//поток вывода
                    try {
                        scanner = new Scanner(TCPSocket.getInputStream());
                        writer = new PrintWriter(TCPSocket.getOutputStream(), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    String str = scanner.nextLine();
                    writer.println("Server TCP: "+ str);

                    try {
                        TCPSocket.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            TCPConnection.start();
        }

        receiveData(serverSocket); //рекурсивно вызываем метод receiveData (вместо бесконечного цикла)
    }

}

public class Server {

    //int numberOfPorts; //количество портов UDP,переданое в аргументах
   // int portNumber = 8000; //номер 1-го порта (для следующих будем увеличивать на 1)
    List<Integer> UDP_Ports;
    List<DatagramSocket> sockets; //коллекция сокетов, которые будут слушать клиентов
    List <ReceiveThread> receiveThreads;

    public Server(List<Integer> UDP_Ports) throws SocketException {
        this.UDP_Ports = UDP_Ports;

        sockets = new ArrayList<DatagramSocket>();
        receiveThreads = new ArrayList<ReceiveThread>();

        for (int i = 0; i < UDP_Ports.size(); i++) {
            try {
                final DatagramSocket socket = new DatagramSocket(UDP_Ports.get(i)); //создаем сокет клиента
                sockets.add(socket);
            } catch (BindException e) {
                System.err.println("ERROR: port " + UDP_Ports.get(i) +" is already in use\n");
            }
        }

        System.out.println("Server is running...");
        System.out.println("Ports:");
        for (DatagramSocket socket: sockets) {
            System.out.println(socket.getLocalPort());
        }

        System.out.println('\n');

        for (DatagramSocket socket: sockets){
            receiveThreads.add(new ReceiveThread(socket));
        }

        for (ReceiveThread receiveThread: receiveThreads) {
            receiveThread.start();
        }


    }

    public static void main(String[] args){
        boolean isOK = true;
        Set<Integer> PortsSet = new HashSet<Integer>();
        for (int i = 0; i < args.length; i++) {
            try {
                int port = Integer.parseInt(args[i]);
                if(port>1024) {
                    PortsSet.add(port);
                }else throw new Exception();
            }catch (NumberFormatException e){
                isOK = false;
                System.err.println("ERROR: the port number must be an integer\n"+e);
            } catch (Exception e) {
                isOK = false;
                System.err.println("ERROR: the port number must be greater than 1024\n");
            }
        }

        List<Integer> UDP_Ports = new ArrayList<Integer>();
        UDP_Ports.addAll(PortsSet);

        if(isOK) {
            try {
                new Server(UDP_Ports);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

    }
}

