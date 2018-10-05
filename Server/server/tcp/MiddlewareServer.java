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
    
    public static int portnum = 5523;
    
    
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
        //args[0] = flight ip
        //args[1] = car ip
        //args[2] = room ip
        portnum = Integer.parseInt(args[3]);
        MiddlewareServer server = new MiddlewareServer();
        server.rm = new ResourceManager("Middleware");
        server.setName("Middleware");
        System.out.println("Flight ip: " + args[0].split("@")[1] + "/ Car ip: " + args[1].split("@")[1] + "/ Room ip: " + args[2].split("@")[1]);
        server.start(portnum, args[0].split("@")[1], args[1].split("@")[1], args[2].split("@")[1]);
    }
    
    public void start(int port, String f_ip, String c_ip, String r_ip) throws Exception
    {
        serverSocket = new ServerSocket(port);

        flightSocket = new Socket(f_ip, portnum);
        f_out = new PrintWriter(flightSocket.getOutputStream(), true);
        f_in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
        System.out.println("Connected to Flight Server /" + f_ip + ":" + portnum);
        
        roomSocket = new Socket(r_ip, portnum);
        r_out = new PrintWriter(roomSocket.getOutputStream(), true);
        r_in = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
        System.out.println("Connected to Room Server /" + r_ip + ":" + portnum);
        
        carSocket = new Socket(c_ip, portnum);
        c_out = new PrintWriter(carSocket.getOutputStream(), true);
        c_in = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
        System.out.println("Connected to Car Server /" + c_ip + ":" + portnum);

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
                        String resp = "";
                        String resp_f = "";
                        String resp_c = "";
                        String resp_r = "";
                        Command cmd = Command.fromString((String)arguments.elementAt(0));
                        switch(cmd){
                            // case Help:
                            // {
                                
                            // }
                            case AddFlight:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case AddCars:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case AddRooms:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case AddCustomer:
                                int id = Integer.parseInt(arguments.elementAt(1));
                                int cid = Integer.parseInt(String.valueOf(id) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) + String.valueOf(Math.round(Math.random() * 100 + 1)));
                                inputLine = "AddCustomerID," + id + "," + cid;
                                r_out.println(inputLine);
                                resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                resp_c = c_in.readLine();
                                if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                    out.println("Response: " + resp_r);
                                }else{
                                    out.println("Failed to Add Customer");
                                }
                                break;
                            case AddCustomerID:
                                r_out.println(inputLine);
                                resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                resp_c = c_in.readLine();
                                if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                    out.println("Response: " + resp_r);
                                }else{
                                    out.println("Failed to Add Customer");
                                }
                                break;
                            case DeleteFlight:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case DeleteCars:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case DeleteRooms:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case DeleteCustomer:
                                r_out.println(inputLine);
                                resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                resp_c = c_in.readLine();
                                if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                    out.println("Response: " + resp_r);
                                }else{
                                    out.println("Failed to Delete Customer");
                                }
                                break;
                            case QueryFlight:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case QueryCars:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case QueryRooms:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case QueryCustomer:
                                r_out.println(inputLine);
                                resp_r = r_in.readLine();
                                f_out.println(inputLine);
                                resp_f = f_in.readLine();
                                c_out.println(inputLine);
                                resp_c = c_in.readLine();
                                if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                    out.println("Response:aaaBill for customer:aaa" + resp_r + "aaa" + resp_f + "aaa" + resp_c);
                                }else{
                                    out.println("Failed to Query Customer");
                                }
                                break;
                            case QueryFlightPrice:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case QueryCarsPrice:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case QueryRoomsPrice:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case ReserveFlight:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case ReserveCar:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case ReserveRoom:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                out.println("Response: " + resp);
                                break;
                            case Bundle:
                            {
                                int xid = toInt(arguments.elementAt(1));
				                int customerID = toInt(arguments.elementAt(2));
				                Vector<String> flightNumbers = new Vector<String>();
				                for (int i = 0; i < arguments.size() - 6; ++i)
				                {
					                flightNumbers.addElement(arguments.elementAt(3+i));
				                }
                                String location = arguments.elementAt(arguments.size()-3);
                                boolean car = (new Boolean(arguments.elementAt(arguments.size()-2))).booleanValue();
                                boolean room = (new Boolean(arguments.elementAt(arguments.size()-1))).booleanValue();
                                boolean ret = true;
                                boolean csuccess = false, rsuccess = false, fsuccess = false;
                                if(car){ //reserve a car at the given location
                                    c_out.println("ReserveCar," + xid + "," + customerID + "," + location);
                                    csuccess = !c_in.readLine().contains("not");
                                }
                                if(room){//reserve a room at the given location
                                    r_out.println("ReserveRoom," + xid + "," + customerID + "," + location);
                                    rsuccess = !r_in.readLine().contains("not");
                                }
                                for (String flightNum : flightNumbers) {
                                    f_out.println("ReserveFlight," + xid + "," + customerID + "," + flightNum);
                                    fsuccess = !f_in.readLine().contains("not");
                                }
                                if(!csuccess){
                                    resp += "Reserving car failed.aaa";
                                }else{
                                    resp += "Reserving car succeeded.aaa";
                                }
                                if(!rsuccess){
                                    resp += "Reserving room failed.aaa";
                                }else{
                                    resp += "Reserving room succeeded.aaa";
                                }
                                if(!fsuccess){
                                    resp += flightNumbers.size() > 1 ? "Reserving flights failed.aaa"  : "Reserving flight failed.aaa";
                                }else{
                                    resp += flightNumbers.size() > 1 ? "Reserving flights succeeded.aaa"  : "Reserving flight succeeded.aaa";
                                }
                                out.println("Response: " + resp);
                                break;
                            }
                            case Quit:
                            {
                                 break;
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