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
    
    public static void main(String[] args) throws Exception
    {
        //arg[0] = flight ip
        //arg[1] = car ip
        //arg[2] = room ip
        MiddlewareServer server = new MiddlewareServer();
        server.rm = new ResourceManager(args[0]);
        server.setName(args[0]);
        server.start(5555, args[0], args[1], args[2]);
    }
    
    public void start(int port, String f_ip, String c_ip, String r_ip) throws Exception
    {
        serverSocket = new ServerSocket(port);
        while(true){
            new MiddlewareServerHandler(serverSocket.accept(), rm, f_ip, c_ip, r_ip).start();
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
        
        private ServerSocket flightSocket;
        private PrintWriter f_out;
        private BufferedReader f_in;
        
        private ServerSocket roomSocket;
        private PrintWriter r_out;
        private BufferedReader r_in;
        
        private ServerSocket carSocket;
        private PrintWriter c_out;
        private BufferedReader c_in;
        
        private ResourceManager m_resourceManager;
        
        public MiddlewareServerHandler(Socket socket, ResourceManager rm, String flight_ip, String car_ip, String room_ip)
        {
            this.clientSocket = socket;
            this.m_resourceManager = rm;
            
            flightSocket = new Socket(flight_ip, 5555);
            f_out = new PrintWriter(flightSocket.getOutputStream(), true);
            f_in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
            
            roomSocket = new Socket(room_ip, 5555);
            r_out = new PrintWriter(roomSocket.getOutputStream(), true);
            r_in = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
            
            carSocket = new Socket(car_ip, 5555);
            c_out = new PrintWriter(carSocket.getOutputStream(), true);
            c_in = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
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
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case AddCars:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case AddRooms:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case AddCustomer:
                            {
                                
                            }
                            case AddCustomerID:
                            {
                                
                            }
                            case DeleteFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case DeleteCars:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case DeleteRooms:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case DeleteCustomer:
                            {
                                
                            }
                            case QueryFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryCars:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryRooms:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryCustomer:
                            {
                                
                            }
                            case QueryFlightPrice:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryCarsPrice:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case QueryRoomsPrice:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case ReserveFlight:
                            {
                                f_out.println(inputLine);
                                String resp = f_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case ReserveCar:
                            {
                                c_out.println(inputLine);
                                String resp = c_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case ReserveRoom:
                            {
                                r_out.println(inputLine);
                                String resp = r_in.readLine();
                                System.out.println("Response: " + resp.replaceAll("a{3}", "\n"));
                            }
                            case Bundle:
                            {
                                
                            }
                            case Quit:
                            {
                                
                            }
                        }
                    }catch(Exception e){
                        System.out.println("Command handling error.");
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