package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

    BufferedReader clientRead = new BufferedReader(new InputStreamReader(System.in)); //создаем BufferedReader, который будет читать то, что мы напишем в консоли
    InetAddress IP;
    List<Integer> UDP_Ports;
    Thread clientThread;
    boolean isEnd = false;

    public Client(List <Integer> UDP_Ports, InetAddress IP) throws SocketException {
        final DatagramSocket clientSocket = new DatagramSocket(); //создаем сокет клиента
        clientSocket.setSoTimeout(1000);

        this.IP = IP;
        this.UDP_Ports = UDP_Ports;
        System.out.println(UDP_Ports);

        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!clientThread.isInterrupted()) {
                    try {
                        sendData(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        clientThread.start();

        if(isEnd)
            clientSocket.close(); //после выхода из цикла, закрываем сокет клиента
    }

    //метод, который отсылает сообщение серверу
    public synchronized void sendData(DatagramSocket clientSocket) throws IOException {
            byte[] sendbuffer = new byte[1024];
            //IP = InetAddress.getByName("localhost");
            System.out.print("\nClient" + ": ");
            String clientData = clientRead.readLine(); //записываем сообщение клиента в String
            sendbuffer = clientData.getBytes(); //и конвертируем эту строку в массив байтов
        for (int UDP_Port : UDP_Ports) {
            DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, IP, UDP_Port); //запаковываем наше сообщение в DatagramPacket, где передаем порт сервера
            clientSocket.send(sendPacket); //и отсылаем его серверу

            if (clientData.equalsIgnoreCase("/end")) //для завершения коммуникации нужно написать команду /end
            {
                System.out.println("Connection ended by client");
                isEnd = true;
                clientThread.interrupt();
                break;
            }
            else
                receiveData(clientSocket);

        }

    }

    //метод, который принимает сообщение от сервера
    public synchronized void receiveData(DatagramSocket clientSocket) throws IOException {
        try {
            byte[] receivebuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
            clientSocket.receive(receivePacket); //получили пакет
            String serverData = new String(receivePacket.getData()); //конвертируем данные в строку
            System.out.print("\nServer: " + serverData); //и выводим на консоль
            int TCP_Port = Integer.parseInt(serverData.replaceAll("[^0-9]", ""));
            connectTCP(TCP_Port);
        }catch (SocketTimeoutException e){
            System.err.println("No response");
        }
    }

    //метод для подключения к TCP и отправки сообщения
    public void connectTCP(int TCP_Port) throws IOException {
        Socket TCP_socket = new Socket(); //создаем новый сокет
        TCP_socket.connect(new InetSocketAddress(IP, TCP_Port));//подключаемся к серверу
        System.out.println("\nTCP connection ready");

        Scanner scanner = new Scanner(TCP_socket.getInputStream()); //считываем данные, которые выдаст нам сервер
        PrintWriter writer = new PrintWriter(TCP_socket.getOutputStream(), true); //отправляем данные серверу{
        System.out.print("\nYour message: ");
        Scanner scan = new Scanner(System.in);
        String string = scan.nextLine();
        writer.println(string); //отправляем сообщение серверу
        String str = scanner.nextLine();
        System.out.println(str);

        System.out.println("\nYou disconnected ");
    }


    public static void main(String[] args) throws SocketException {
        boolean isOK = true;
        List<Integer> UDP_Ports = new ArrayList<Integer>();
        InetAddress IP = null;
        try {
            IP = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            isOK = false;
            System.err.println("ERROR: unknown host exception\n"+e);
        }

        for (int i = 1; i < args.length; i++) {
            try {
                int port = Integer.parseInt(args[i]);
                if(port>0) {
                    UDP_Ports.add(port);
                }else throw new Exception();
            }catch (NumberFormatException e){
                isOK = false;
                System.err.println("ERROR: the port number must be an integer\n"+e);
            } catch (Exception e) {
                isOK = false;
                System.err.println("ERROR: the port number must be greater than 0\n");
            }
        }

        if(isOK)
            new Client(UDP_Ports, IP);
    }

}
