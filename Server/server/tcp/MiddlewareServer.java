package server.tcp;

import server.lockmanager.*;
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
    
    public static TransactionManager tm;
    public static LockManager lm;
    
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
        
        tm = new TransactionManager();
        lm = new LockManager();
        
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
        
        private TransactionObject to;
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
                    boolean isStarted = false;
                    while((inputLine = in.readLine()) != null){
                        //CLIENT COMMAND HANDLING
                        arguments = parse(inputLine);
                        try{
                            String resp = "";
                            String resp_f = "";
                            String resp_c = "";
                            String resp_r = "";
                            Command cmd = Command.fromString((String)arguments.elementAt(0));
                            if(isStarted && (!cmd.equals(Command.Quit) || !cmd.equals(Command.Shutdown))){
                                if(to.getXId() != Integer.parseInt(arguments.elementAt(1))){
                                    //xid does NOT exist!
                                    out.println("xid does NOT exist!");
                                    break;
                                }
                            }
                            if(!isStarted && !cmd.equals(Command.Start)){
                                out.println("Need to start a transaction (Start)");
                                continue;
                            }
                            switch(cmd){
                                    // case Help:
                                    // {
                                    
                                    // }
                                    case AddFlight:
                                    //NEED WRITE LOCK FOR FLIGHT
                                    lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                    innerExecute(inputLine);
                                    // out.println("Flight Added");
                                    break;

                                    case AddCars:
                                    //NEED WRITE LOCK FOR CAR
                                    lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CAR", inputLine);
                                    innerExecute(inputLine);
                                    // out.println("Cars Added");
                                    break;

                                    case AddRooms:
                                    //NEED WRITE LOCK FOR ROOM
                                    lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "ROOM", inputLine);
                                    innerExecute(inputLine);
                                    // out.println("Rooms Added");
                                    break;

                                    case AddCustomer:
                                    //NEED WRITE LOCK FOR CUSTOMER
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    innerExecute(inputLine);
                                    // out.println("Customer Added");
                                    break;

                                    case AddCustomerID:
                                    //NEED WRITE LOCK FOR CUSTOMER
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    innerExecute(inputLine);
                                    // out.println("Customer Added");
                                    break;

                                    case DeleteFlight:
                                    //NEED WRITE LOCK FOR FLIGHT
                                    lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                    innerExecute(inputLine);
                                    // out.println("Flight Deleted");
                                    break;

                                    case DeleteCars:
                                    //NEED WRITE LOCK FOR CAR
                                    lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CAR", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case DeleteRooms:
                                    //NEED WRITE LOCK FOR ROOM
                                    lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "ROOM", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case DeleteCustomer:
                                    //NEED WRITE LOCK FOR CUSTOMER
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryFlight:
                                    //NEED READ LOCK FOR FLIGHT
                                    lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryCars:
                                    //NEED READ LOCK FOR CAR
                                    lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CAR", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryRooms:
                                    //NEED READ LOCK FOR ROOM
                                    lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "ROOM", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryCustomer:
                                    //NEED READ LOCK FOR CUSTOMER
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryFlightPrice:
                                    //NEED READ LOCK FOR FLIGHT
                                    lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryCarsPrice:
                                    //NEED READ LOCK FOR CAR
                                    lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CAR", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case QueryRoomsPrice:
                                    //NEED READ LOCK FOR ROOM
                                    lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "ROOM", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case ReserveFlight:
                                    //NEED WRITE LOCK FOR FLIGHT, CUSTOMER
                                    lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case ReserveCar:
                                    //NEED WRITE LOCK FOR CAR, CUSTOMER
                                    lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "CAR", inputLine);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case ReserveRoom:
                                    //NEED WRITE LOCK FOR ROOM, CUSTOMER
                                    lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                    lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                    tm.newOperation(to.getXId(), "ROOM", inputLine);
                                    tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                    // out.println("Flight Added");
                                    innerExecute(inputLine);
                                    break;

                                    case Bundle:
                                    {
                                        //NEED WRITE LOCK FOR FLIGHT, ROOM, CAR, CUSTOMER
                                        lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                        lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                        lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                        lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                        tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                        tm.newOperation(to.getXId(), "ROOM", inputLine);
                                        tm.newOperation(to.getXId(), "CAR", inputLine);
                                        tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                        // out.println("Flight Added");
                                        innerExecute(inputLine);
                                        break;
                                    }
                                    case Quit:
                                    {
                                        break;
                                    }
                                    case Start:
                                    {
                                        this.to = tm.start();
                                        out.println("Your transaction ID: " + to.getXId());
                                        isStarted = true;
                                        break;
                                    }
                                    case Commit:
                                    {
                                        String[] operationHistory = tm.getOperationHistory(to.getXId());
                                        String temp = "";
                                        for(int i=0; i < operationHistory.length; i++){
                                            innerExecute(operationHistory[i]);
                                            temp += operationHistory[i] + "...";
                                        }
                                        lm.UnlockAll(to.getXId());
                                        tm.commit(to.getXId());
                                        this.to.setXId(-1);
                                        out.println("Commited transaction [" + to.getXId() + "]");
                                        break;
                                    }
                                    case Abort:
                                    {
                                        String[] operationHistory = tm.getOperationHistory(to.getXId());
                                        for(int i=0; i < operationHistory.length; i++){
                                            Vector<String> args = parse(operationHistory[i]);
                                            Command usedCmd = Command.fromString((String)args.elementAt(0));
                                            switch(usedCmd){
                                                case AddFlight:{
                                                    innerExecute("DeleteFlight," +  to.getXId() + "," + args.elementAt(2));
                                                    break;
                                                }
                                                case AddCars:{
                                                    innerExecute("DeleteCars," + to.getXId() + "," + args.elementAt(2));
                                                    break;
                                                }
                                                case AddRooms:{
                                                    innerExecute("DeleteRooms," + to.getXId() + "," + args.elementAt(2));
                                                    break;
                                                }
                                                case AddCustomer:{
                                                    //NEED TO FIND AUTO GENERATED ID TO DELETE!!
                                                    //innerExecute("DeleteCustomer," + to.getXId() + "," + args.elementAt(2));
                                                    break;
                                                }
                                                case AddCustomerID:{
                                                    innerExecute("DeleteCustomer," + to.getXId() + "," + args.elementAt(2));
                                                    break;
                                                }
                                                case DeleteFlight:{
                                                    
                                                    break;
                                                }
                                                case DeleteCars:{

                                                    break;
                                                }
                                                case DeleteRooms:{

                                                    break;
                                                }
                                                case DeleteCustomer:{

                                                    break;
                                                }
                                                case ReserveFlight:{

                                                    break;
                                                }
                                                case ReserveCar:{

                                                    break;
                                                }
                                                case ReserveRoom:{

                                                    break;
                                                }
                                                case Bundle:{

                                                    break;
                                                }
                                                default:
                                                //was either a read or inconsequential
                                                break;
                                            }
                                        }
                                        break;
                                    }
                                    case Shutdown:
                                    {
                                        f_out.println("Quit");
                                        c_out.println("Quit");
                                        r_out.println("Quit");
                                        System.exit(0);
                                        break;
                                    }
                                    default:
                                    {
                                        out.println("Command handling error. (Unhandled input)");
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

                public String innerExecute(String inputLine){
                    //CLIENT COMMAND HANDLING
                    Vector<String> arguments = parse(inputLine);
                    String ret = "";
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
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case AddCars:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case AddRooms:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                ret = resp;
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
                                    ret = resp_r;
                                    out.println("Response: " + resp_r);
                                }else{
                                    ret = "Failed to Add Customer";
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
                                    ret = resp_r;
                                    out.println("Response: " + resp_r);
                                }else{
                                    ret = "Failed to Add Customer";
                                    out.println("Failed to Add Customer");
                                }
                                break;
        
                                case DeleteFlight:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case DeleteCars:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case DeleteRooms:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                ret = resp;
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
                                    ret = resp_r;
                                    out.println("Response: " + resp_r);
                                }else{
                                    ret = "Failed to Delete Customer";
                                    out.println("Failed to Delete Customer");
                                }
                                break;
        
                                case QueryFlight:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case QueryCars:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case QueryRooms:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                ret = resp;
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
                                    ret = "Bill for customer:aaa" + resp_r + "aaa" + resp_f + "aaa" + resp_c;
                                    out.println("Response:aaaBill for customer:aaa" + resp_r + "aaa" + resp_f + "aaa" + resp_c);
                                }else{
                                    ret = "Failed to Query Customer";
                                    out.println("Failed to Query Customer");
                                }
                                break;
        
                                case QueryFlightPrice:
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case QueryCarsPrice:
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
        
                                case QueryRoomsPrice:
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
                                case ReserveFlight:
                                
                                f_out.println(inputLine);
                                resp = f_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
                                case ReserveCar:
                                
                                c_out.println(inputLine);
                                resp = c_in.readLine();
                                ret = resp;
                                out.println("Response: " + resp);
                                break;
                                case ReserveRoom:
                                
                                r_out.println(inputLine);
                                resp = r_in.readLine();
                                ret = resp;
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
                                    // boolean ret = true;
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
                                    ret = resp;
                                    out.println("Response: " + resp);
                                    break;
                                }
                                default:
                                {
                                    out.println("Command handling error. (Unhandled input)");
                                }
                                return ret;
                            }
                        }catch(Exception e){
                            out.println("Command handling error.");
                            e.printStackTrace();
                            return ret;
                        }
                        return ret;
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