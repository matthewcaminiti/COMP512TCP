package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.*;

import com.sun.corba.se.impl.orbutil.threadpool.TimeoutException;

public class TCPClient
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean transStarted = false;
    static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    private ExecutorService readExecutor = Executors.newCachedThreadPool();
    private Callable<String> waitForInput = new Callable<String>(){
        public String call(){
            try{
                return stdin.readLine().trim();
            }catch (Exception e){
                //TODO: handle
                return "";
            }
        }
    };

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

        while(true){

            System.out.print((char)27 + "[32;1m\n>] " + (char)27 + "[0m");
            String input;

            if(client.transStarted){
                Future<String> awaitingInput = client.readExecutor.submit(client.waitForInput);
                try{
                    input = awaitingInput.get(1, TimeUnit.MINUTES);
                    if(input == "Shutdown") System.exit(0);
                }catch (Exception e){
                    input = "Timeout";
                    client.transStarted = false;
                }
            }else{
                input = stdin.readLine().trim();
                if(input.equals("Start")){
                    client.transStarted = true;
                }else{
                    System.out.println("Must start a transaction (Start) before proceeding...");
                    continue;
                }
            }
            String response = client.sendMessage(input);
            if(input == "Shutdown" || response == null) System.exit(0);
            System.out.println(response.replaceAll("a{3}", "\n"));
        }
        //stdin.close();
    }
}