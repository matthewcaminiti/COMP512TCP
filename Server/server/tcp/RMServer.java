package server.tcp;

import server.common.*;

import java.util.*;
import java.io.*;
import java.net.*;

public class RMServer
{
    private ServerSocket serverSocket;
    public ResourceManager rm;
    public String s_name;
    
    public static void main(String[] args) throws Exception
    {
        RMServer server = new RMServer();
        server.rm = new ResourceManager(args[0]);
        server.setName(args[0]);
        server.start(Integer.parseInt(args[0]));
    }
    
    public void start(int port) throws Exception
    {
        serverSocket = new ServerSocket(port);
        while(true){
            new RMServerHandler(serverSocket.accept(), rm).start();
        }
    }
    
    public void stop() throws Exception
    {
        serverSocket.close();
    }
    
    private static class RMServerHandler extends Thread
    {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private ResourceManager m_resourceManager;
        
        public RMServerHandler(Socket socket, ResourceManager rm)
        {
            this.clientSocket = socket;
            this.m_resourceManager = rm;
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
                        try{
                            execute(cmd, arguments);
                        }catch (Exception e){
                            System.out.println("Execute crapped out.");
                            out.println("Execute crapped out.");
                            e.printStackTrace();
                        }
                        
                    }catch (Exception e){
                        System.out.println("Unknown command: " + inputLine);
                        out.println("Unknown command: " + inputLine);
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
        
        public void execute(Command cmd, Vector<String> arguments) throws Exception
        {
            switch (cmd)
            {
                case Help:
                {
                    if (arguments.size() == 1) {
                        System.out.println(Command.description());
                    } else if (arguments.size() == 2) {
                        Command l_cmd = Command.fromString((String)arguments.elementAt(1));
                        System.out.println(l_cmd.toString());
                    } else {
                        System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
                    }
                    break;
                }
                case AddFlight: {
                    checkArgumentsCount(5, arguments.size());
                    
                    System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));
                    System.out.println("-Flight Seats: " + arguments.elementAt(3));
                    System.out.println("-Flight Price: " + arguments.elementAt(4));
                    
                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));
                    int flightSeats = toInt(arguments.elementAt(3));
                    int flightPrice = toInt(arguments.elementAt(4));
                    
                    if (m_resourceManager.addFlight(id, flightNum, flightSeats, flightPrice)) {
                        out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Flight Number: " + arguments.elementAt(2) + "aaa" + "-Flight Seats: " + arguments.elementAt(3) + "aaa" + "-Flight Price: " + arguments.elementAt(4) + "aaa" + " Flight added");
                        System.out.println("Flight added");
                    } else {
                        out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Flight Number: " + arguments.elementAt(2) + "aaa" + "-Flight Seats: " + arguments.elementAt(3) + "aaa" + "-Flight Price: " + arguments.elementAt(4) + "aaa" + " Flight could not be added");
                        System.out.println("Flight could not be added");
                    }
                    break;
                }
                case AddCars: {
                    checkArgumentsCount(5, arguments.size());
                    
                    System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));
                    System.out.println("-Number of Cars: " + arguments.elementAt(3));
                    System.out.println("-Car Price: " + arguments.elementAt(4));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    int numCars = toInt(arguments.elementAt(3));
                    int price = toInt(arguments.elementAt(4));
                    
                    if (m_resourceManager.addCars(id, location, numCars, price)) {
                        out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2)  + "aaa" +  "-Number of Cars: " + arguments.elementAt(3) + "aaa" + "-Car Price: " + arguments.elementAt(4) + "aaa" + "Cars added");
                        System.out.println("Cars added");
                    } else {
                        out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2)  + "aaa" +  "-Number of Cars: " + arguments.elementAt(3) + "aaa" + "-Car Price: " + arguments.elementAt(4) + "aaa" + "Cars could not be added");
                        System.out.println("Cars could not be added");
                    }
                    break;
                }
                case AddRooms: {
                    checkArgumentsCount(5, arguments.size());
                    
                    System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Room Location: " + arguments.elementAt(2));
                    System.out.println("-Number of Rooms: " + arguments.elementAt(3));
                    System.out.println("-Room Price: " + arguments.elementAt(4));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    int numRooms = toInt(arguments.elementAt(3));
                    int price = toInt(arguments.elementAt(4));
                    
                    if (m_resourceManager.addRooms(id, location, numRooms, price)) {
                        out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Room Location: " + arguments.elementAt(2) + "aaa" + "-Number of Rooms: " + arguments.elementAt(3) + "aaa" + "-Room Price: " + arguments.elementAt(4) + "aaa" + "Rooms added");
                        System.out.println("Rooms added");
                    } else {
                        out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Room Location: " + arguments.elementAt(2) + "aaa" + "-Number of Rooms: " + arguments.elementAt(3) + "aaa" + "-Room Price: " + arguments.elementAt(4) + "aaa" + "Rooms could not be added");
                        System.out.println("Rooms could not be added");
                    }
                    break;
                }
                case AddCustomer: {
                    checkArgumentsCount(2, arguments.size());
                    
                    System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
                    
                    int id = toInt(arguments.elementAt(1));
                    int customer = m_resourceManager.newCustomer(id);
                    out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]" + "aaa" + "Add customer ID: " + customer);
                    System.out.println("Add customer ID: " + customer);
                    break;
                }
                case AddCustomerID: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    
                    if (m_resourceManager.newCustomer(id, customerID)) {
                        out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "Add customer ID: " + customerID);
                        System.out.println("Add customer ID: " + customerID);
                    } else {
                        out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "Customer could not be added");
                        System.out.println("Customer could not be added");
                    }
                    break;
                }
                case DeleteFlight: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));
                    
                    if (m_resourceManager.deleteFlight(id, flightNum)) {
                        out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Flight Number: " + arguments.elementAt(2) + "aaa" + "Flight Deleted");
                        System.out.println("Flight Deleted");
                    } else {
                        out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Flight Number: " + arguments.elementAt(2) + "aaa" + "Flight could not be Deleted");
                        System.out.println("Flight could not be deleted");
                    }
                    break;
                }
                case DeleteCars: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    
                    if (m_resourceManager.deleteCars(id, location)) {
                        out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2) + "aaa" + "Cars Deleted");
                        System.out.println("Cars Deleted");
                    } else {
                        out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2) + "aaa" + "Cars could not be Deleted");
                        System.out.println("Cars could not be deleted");
                    }
                    break;
                }
                case DeleteRooms: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    
                    if (m_resourceManager.deleteRooms(id, location)) {
                        out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2) + "aaa" + "Rooms Deleted");
                        System.out.println("Rooms Deleted");
                    } else {
                        out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2) + "aaa" + "Rooms could not be Deleted");
                        System.out.println("Rooms could not be deleted");
                    }
                    break;
                }
                case DeleteCustomer: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    
                    if (m_resourceManager.deleteCustomer(id, customerID)) {
                        out.println("Customer Deleted");
                        System.out.println("Customer Deleted");
                    } else {
                        out.println("Customer could not be Deleted");
                        System.out.println("Customer could not be deleted");
                    }
                    break;
                }
                case QueryFlight: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));
                    
                    int seats = m_resourceManager.queryFlight(id, flightNum);
                    out.println("Number of seats available: " + seats);
                    System.out.println("Number of seats available: " + seats);
                    break;
                }
                case QueryCars: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    
                    int numCars = m_resourceManager.queryCars(id, location);
                    out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2) + "aaa" + "Number of cars at this location: " + numCars);
                    System.out.println("Number of cars at this location: " + numCars);
                    break;
                }
                case QueryRooms: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Room Location: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    
                    int numRoom = m_resourceManager.queryRooms(id, location);
                    out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Room Location: " + arguments.elementAt(2) + "aaa" + "Number of rooms at this location: " + numRoom);
                    System.out.println("Number of rooms at this location: " + numRoom);
                    break;
                }
                case QueryCustomer: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    
                    String bill = m_resourceManager.queryCustomerInfo(id, customerID);
                    //out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + bill.replaceAll("\n", "aaa"));
                    out.println(bill);
                    System.out.print(bill);
                    break;               
                }
                case QueryFlightPrice: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));
                    
                    int price = m_resourceManager.queryFlightPrice(id, flightNum);
                    out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Flight Number: " + arguments.elementAt(2) + "aaa" + "Price of a seat: " + price);
                    System.out.println("Price of a seat: " + price);
                    break;
                }
                case QueryCarsPrice: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    
                    int price = m_resourceManager.queryCarsPrice(id, location);
                    out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Car Location: " + arguments.elementAt(2) + "aaa" + "Price of cars at this location: " + price);
                    System.out.println("Price of cars at this location: " + price);
                    break;
                }
                case QueryRoomsPrice: {
                    checkArgumentsCount(3, arguments.size());
                    
                    System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Room Location: " + arguments.elementAt(2));
                    
                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    
                    int price = m_resourceManager.queryRoomsPrice(id, location);
                    out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Room Location: " + arguments.elementAt(2) + "aaa" + "Price of rooms at this location: " + price);
                    System.out.println("Price of rooms at this location: " + price);
                    break;
                }
                case ReserveFlight: {
                    checkArgumentsCount(4, arguments.size());
                    
                    System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    System.out.println("-Flight Number: " + arguments.elementAt(3));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    int flightNum = toInt(arguments.elementAt(3));
                    
                    if (m_resourceManager.reserveFlight(id, customerID, flightNum)) {
                        out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "-Flight Number: " + arguments.elementAt(3) + "aaa" + "Flight Reserved");
                        System.out.println("Flight Reserved");
                    } else {
                        out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "-Flight Number: " + arguments.elementAt(3) + "aaa" + "Flight could not be Reserved");
                        System.out.println("Flight could not be reserved");
                    }
                    break;
                }
                case ReserveCar: {
                    checkArgumentsCount(4, arguments.size());
                    
                    System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    System.out.println("-Car Location: " + arguments.elementAt(3));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    String location = arguments.elementAt(3);
                    
                    if (m_resourceManager.reserveCar(id, customerID, location)) {
                        out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "-Car Location: " + arguments.elementAt(3) + "aaa" + "Car Reserved");
                        System.out.println("Car Reserved");
                    } else {
                        out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "-Car Location: " + arguments.elementAt(3) + "aaa" + "Car could not be Reserved");
                        System.out.println("Car could not be reserved");
                    }
                    break;
                }
                case ReserveRoom: {
                    checkArgumentsCount(4, arguments.size());
                    
                    System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    System.out.println("-Room Location: " + arguments.elementAt(3));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    String location = arguments.elementAt(3);
                    
                    if (m_resourceManager.reserveRoom(id, customerID, location)) {
                        out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "-Room Location: " + arguments.elementAt(3) + "aaa" + "Room Reserved");
                        System.out.println("Room Reserved");
                    } else {
                        out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]" + "aaa" + "-Customer ID: " + arguments.elementAt(2) + "aaa" + "-Room Location: " + arguments.elementAt(3) + "aaa" + "Room could not be Reserved");
                        System.out.println("Room could not be reserved");
                    }
                    break;
                }
                case Bundle: {
                    if (arguments.size() < 7) {
                        System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
                        break;
                    }
                    
                    System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    for (int i = 0; i < arguments.size() - 6; ++i)
                    {
                        System.out.println("-Flight Number: " + arguments.elementAt(3+i));
                    }
                    System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size()-3));
                    System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
                    System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));
                    
                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    Vector<String> flightNumbers = new Vector<String>();
                    for (int i = 0; i < arguments.size() - 6; ++i)
                    {
                        flightNumbers.addElement(arguments.elementAt(3+i));
                    }
                    String location = arguments.elementAt(arguments.size()-3);
                    boolean car = toBoolean(arguments.elementAt(arguments.size()-2));
                    boolean room = toBoolean(arguments.elementAt(arguments.size()-1));
                    
                    if (m_resourceManager.bundle(id, customerID, flightNumbers, location, car, room)) {
                        out.println("Bundle Reserved");
                        System.out.println("Bundle Reserved");
                    } else {
                        out.println("Bundle could not be Reserved");
                        System.out.println("Bundle could not be reserved");
                    }
                    break;
                }
                case Quit:
				//checkArgumentsCount(1, arguments.size());
                
				System.out.println("Quitting client");
                System.exit(0);
                break;
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
    }

    public void setName(String name){
        s_name = name;
    }

    public String getName(){
        return s_name;
    }
    
    
    
}