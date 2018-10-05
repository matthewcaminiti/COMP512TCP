package server.tcp;

import server.common.*;

import java.util.*;
import java.io.*;
import java.net.*;

public class MiddlewareServer
{
    private ServerSocket serverSocket;
    
    public ResourceManager rm;
    public String s_name;
    
    public static int portnum = 5545;
    
    
    public static Socket flightSocket;
    public static PrintWriter f_out;
    public static BufferedReader f_in;
    
    public static Socket roomSocket;
    public static PrintWriter r_out;
    public static BufferedReader r_in;
    
    public static Socket carSocket;
    public static PrintWriter c_out;
    public static BufferedReader c_in;

    public static void main(String[] args) throws Exception
    {
        //arg[0] = flight ip
        //arg[1] = car ip
        //arg[2] = room ip
        MiddlewareServer server = new MiddlewareServer();
        server.rm = new ResourceManager("Middleware");
        server.setName("Middleware");
        server.start(portnum, args[0].split("@")[1], args[1].split("@")[1], args[2].split("@")[1]);
    }
    
    public void start(int port, String f_ip, String c_ip, String r_ip) throws Exception
    {
        serverSocket = new ServerSocket(port);

        flightSocket = new Socket(f_ip, portnum);
        f_out = new PrintWriter(flightSocket.getOutputStream(), true);
        f_in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
        
        roomSocket = new Socket(r_ip, portnum);
        r_out = new PrintWriter(roomSocket.getOutputStream(), true);
        r_in = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
        
        carSocket = new Socket(c_ip, portnum);
        c_out = new PrintWriter(carSocket.getOutputStream(), true);
        c_in = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));

        while(true){
            new MiddlewareServerHandler(serverSocket.accept()).start();
        }
    }
    
    public void stop() throws Exception
    {
        serverSocket.close();
    }
    
    
    
    private static class MiddlewareServerHandler extends Thread
    {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        
        //private ResourceManager m_resourceManager;
        
        public MiddlewareServerHandler(Socket socket) throws Exception
        {
            this.clientSocket = socket;
        }
        
        public void run()
        {
            try{
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Vector<String> arguments = new Vector<String>();
                String inputLine;
                while((inputLine = in.readLine()) != null){ 
                    //CLIENT COMMAND HANDLING
                    arguments = parse(inputLine);
                    try{
                        Command cmd = Command.fromString((String)arguments.elementAt(0));
                        switch(cmd){
                            case Help:
                            {
                                
                            }
                            case AddFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case AddCars:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case AddRooms:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case AddCustomer:
                            {
                                int id = Integer.parseInt(arguments.elementAt(1));
                                int cid = Integer.parseInt(String.valueOf(id) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) + String.valueOf(Math.round(Math.random() * 100 + 1)));
                                inputLine = "AddCustomerID," + id + "," + cid;
                                r_out.println(inputLine);
                                String resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                String resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                String resp_c = c_in.readLine();
                                if(resp_r == resp_f && resp_f == resp_c){
                                    out.println("Response: " + resp_r.replaceAll("a{3}", "\n"));
                                }else{
                                    out.println("Failed to Add Customer");
                                }
                            }
                            case AddCustomerID:
                            {
                                r_out.println(inputLine);
                                String resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                String resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                String resp_c = c_in.readLine();
                                if(resp_r == resp_f && resp_f == resp_c){
                                    out.println("Response: " + resp_r.replaceAll("a{3}", "\n"));
                                }else{
                                    out.println("Failed to Add Customer");
                                }
                            }
                            case DeleteFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case DeleteCars:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case DeleteRooms:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case DeleteCustomer:
                            {
                                r_out.println(inputLine);
                                String resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                String resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                String resp_c = c_in.readLine();
                                if(resp_r == resp_f && resp_f == resp_c){
                                    out.println("Response: " + resp_r.replaceAll("a{3}", "\n"));
                                }else{
                                    out.println("Failed to Delete Customer");
                                }
                            }
                            case QueryFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryCars:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryRooms:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryCustomer:
                            {
                                r_out.println(inputLine);
                                String resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                String resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                String resp_c = c_in.readLine();
                                if(resp_r == resp_f && resp_f == resp_c){
                                    out.println("Response: " + resp_r.replaceAll("a{3}", "\n"));
                                }else{
                                    out.println("Failed to Delete Customer");
                                }
                            }
                            case QueryFlightPrice:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryCarsPrice:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryRoomsPrice:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case ReserveFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case ReserveCar:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case ReserveRoom:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case Bundle:
                            {
                                
                            }
                            case Quit:
                            {
                                
                            }
                        }
                    }catch(Exception e){
                        out.println("Command handling error.");
                        e.printStackTrace();
                    }
                    
                    //out.println("Success!");
                }
                in.close();
                out.close();
                clientSocket.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    public static Vector<String> parse(String command)
    {
        Vector<String> arguments = new Vector<String>();
        StringTokenizer tokenizer = new StringTokenizer(command,",");
        String argument = "";
        while (tokenizer.hasMoreTokens())
        {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }
    
    public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException
    {
        if (expected != actual)
        {
            throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
        }
    }
    
    public static int toInt(String string) throws NumberFormatException
    {
        return (new Integer(string)).intValue();
    }
    
    public static boolean toBoolean(String string)// throws Exception
    {
        return (new Boolean(string)).booleanValue();
    }
    
    public void setName(String name){
        s_name = name;
    }
    
    public String getName(){
        return s_name;
    }
    
    
    
}