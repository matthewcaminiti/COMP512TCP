package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPClient
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws Exception
    {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String msg) throws Exception
    {
        out.println(msg);
        String resp = in.readLine();
        //String resp = "";
        //while((resp = in.readLine()) == "");
        return resp;
    }

    public void stopConnection() throws Exception
    {
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args) throws Exception
    {
        TCPClient client = new TCPClient();
        client.startConnection(args[0], Integer.parseInt(args[1]));

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        while(true){

            System.out.print((char)27 + "[32;1m\n>] " + (char)27 + "[0m");
            String input = stdin.readLine().trim();
            
            //System.out.println("Sent message: \"" + input + "\"");
            String response = client.sendMessage(input);
            
            System.out.println(response.replaceAll("a{3}", "\n"));
            if(response.equals("cunt")){
                break;
            }
        }
        stdin.close();
    }
}