package server.tcp;

import server.lockmanager.*;
import server.common.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;

public class MiddlewareServer
{
    private ServerSocket serverSocket;
    
    public ResourceManager rm;
    public String s_name;
    
    public static int timeoutMs = 30000;
    
    public static boolean carRMDown = false;
    public static boolean flightRMDown = false;
    public static boolean roomRMDown = false;
    
    public static int portnum = 5523;
    
    
    public static Socket flightSocket;
    public static String fip;
    public static PrintWriter f_out;
    public static BufferedReader f_in;
    
    public static Socket roomSocket;
    public static String rip;
    public static PrintWriter r_out;
    public static BufferedReader r_in;
    
    public static Socket carSocket;
    public static String cip;
    public static PrintWriter c_out;
    public static BufferedReader c_in;
    
    public static TransactionManager tm;
    public static LockManager lm;
    
    public static String twoPCState;
    
    private static File committedTrans = null;
    private static File stagedTrans = null;
    
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
        fip = f_ip;
        f_out = new PrintWriter(flightSocket.getOutputStream(), true);
        f_in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
        System.out.println("Connected to Flight Server /" + f_ip + ":" + portnum);
        
        roomSocket = new Socket(r_ip, portnum);
        rip = r_ip;
        r_out = new PrintWriter(roomSocket.getOutputStream(), true);
        r_in = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
        System.out.println("Connected to Room Server /" + r_ip + ":" + portnum);
        
        carSocket = new Socket(c_ip, portnum);
        cip = c_ip;
        c_out = new PrintWriter(carSocket.getOutputStream(), true);
        c_in = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
        System.out.println("Connected to Car Server /" + c_ip + ":" + portnum);
        
        tm = new TransactionManager();
        lm = new LockManager();
        
        committedTrans = new File("../committedTransMW.txt");
        stagedTrans = new File("../stagedTransMW.txt");
        
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
        
        private boolean in2PC = false;
        private int[] votes = new int[3];
        
        public MiddlewareServerHandler(Socket socket) throws Exception
        {
            this.clientSocket = socket;
        }
        public void run()
        {
            try{
                if(!committedTrans.createNewFile()){
                    //if already exists committed transaction file
                    System.out.println("Found committedTransMW.txt");
                    FileReader fr = new FileReader(committedTrans);
                    // Committed Transaction file will contain each transaction in chronological order (of commits)
                    // CT file will not have Transaction ID except for withing Commands
                    BufferedReader fbr = new BufferedReader(fr);
                    String line;
                    int currXid = -1;
                    while((line = fbr.readLine()) != null){
                        //line will contain command
                        //innerExecute(line, false, false);
                        System.out.println(line);
                    }
                }else{
                    //created new empty committedTrans.txt
                    System.out.println("Created new committedTrans.txt");
                }
            }catch (Exception e){
                //TODO: handle I/O errors
            }
            try{
                if(!stagedTrans.createNewFile()){
                    //if already exists committed transaction file
                    System.out.println("Found stagedTransMW.txt");
                    FileReader fr = new FileReader(stagedTrans);
                    //Committed Transaction file will contain each transaction in chronological order (of commits)
                    // CT file will not have Transaction ID except for withing Commands
                    BufferedReader fbr = new BufferedReader(fr);
                    String line;
                    int currXid = -1;
                    while((line = fbr.readLine()) != null){
                        //line will contain command
                        //innerExecute(line, false, false);
                        System.out.println(line);
                    }
                }else{
                    //created new empty committedTrans.txt
                    System.out.println("Created new stagedTransMW.txt");
                }
            }catch (Exception e){
                //TODO: handle I/O errors
            }
            try{
                //Upon crash recovery, need to check if Middleware was in 2PC
                String twoPCState = tm.getMiddlewareState();
                if(!twoPCState.equals("none")){
                    in2PC = true; //flag that middleware was in 2PC when it crashed
                }
            }catch(Exception e){
                //TODO: handle I/O errors
            }
            try{
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Vector<String> arguments = new Vector<String>();
                String inputLine;
                boolean isStarted = false;
                
                //Perform 2PC before taking any more client commands
                while(in2PC){
                    
                    String twoPCState = tm.getMiddlewareState();
                    switch(twoPCState.split(",")[0]){
                        
                        case "beforeVote":{
                            //send vote request
                            if(tm.getCrashStatus() == 1){
                                System.out.println("Middleware server about to crash with mode: 1");
                                System.exit(1);
                            }
                            
                            f_out.println("Prepare," + twoPCState.split(",")[1]);
                            c_out.println("Prepare," + twoPCState.split(",")[1]);
                            r_out.println("Prepare," + twoPCState.split(",")[1]);
                            tm.sentVote(Integer.parseInt(twoPCState.split(",")[1]));
                            
                            if(tm.getCrashStatus() == 2){
                                System.out.println("Middleware server about to crash with mode: 2");
                                System.exit(1);
                            }
                            break;
                        }
                        
                        case "waitingFor":{
                            
                            Date startTime = new Date();
                            boolean allVotesReceived = false;
                            while(((System.currentTimeMillis() - startTime.getTime()) < timeoutMs) && !allVotesReceived){
                                //cycle through each RM to get its response
                                
                                if(twoPCState.contains("none")){
                                    //all votes received
                                    tm.receivedAllVotes(votes, Integer.parseInt(twoPCState.split(",")[1]));
                                    allVotesReceived = true;
                                    if(tm.getCrashStatus() == 4){
                                        System.out.println("Middleware server about to crash with mode: 4");
                                        System.exit(1);
                                    }
                                }else{
                                    if(twoPCState.contains("Flight")){
                                        String f_resp = f_in.readLine();
                                        tm.receivedVote("Flight", Integer.parseInt(twoPCState.split(",")[1]));
                                        if(tm.getCrashStatus() == 3){
                                            System.out.println("Middleware server about to crash with mode: 3");
                                            System.exit(1);
                                        }
                                        votes[0] = f_resp == "YES" ? 1 : 0;
                                    }
                                    
                                    if(twoPCState.contains("Room")){
                                        String r_resp = r_in.readLine();
                                        tm.receivedVote("Room", Integer.parseInt(twoPCState.split(",")[1]));
                                        votes[1] = r_resp == "YES" ? 1 : 0;
                                    }
                                    
                                    if(twoPCState.contains("Car")){
                                        String c_resp = c_in.readLine();
                                        tm.receivedVote("Car", Integer.parseInt(twoPCState.split(",")[1]));
                                        votes[2] = c_resp == "YES" ? 1 : 0;
                                    }
                                }
                            }
                            
                            if(!allVotesReceived){
                                if(twoPCState.contains("Car")){ carRMDown = true; }
                                if(twoPCState.contains("Room")){ roomRMDown = true; }
                                if(twoPCState.contains("Flight")){ flightRMDown = true; }
                                
                                //HANDLE SERVER(S) BEING DOWN... abort transaction
                                //Communicate this to the Resource Managers
                                
                            }
                            
                            break;
                        }
                        
                        case "receivedAllVotes":{
                            
                            tm.makeDecision(twoPCState.split(",")[1], Integer.parseInt(twoPCState.split(",")[2]));
                            
                            if(tm.getCrashStatus() == 5){
                                System.out.println("Middleware server about to crash with mode: 5");
                                System.exit(1);
                            }
                            
                            break;
                        }
                        
                        case "madeDecision":{
                            //send decisions
                            Boolean decision = Boolean.parseBoolean(twoPCState.split(",")[1]);
                            if(decision){
                                f_out.println("Commit," + twoPCState.split(",")[2]);
                                tm.sentDecision("Flight", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                System.out.println("Sent decision: " + decision + "to Flight RM");
                                
                                if(tm.getCrashStatus() == 6){
                                    System.out.println("Middleware server about to crash with mode: 6");
                                    System.exit(1);
                                }
                                
                                r_out.println("Commit," + twoPCState.split(",")[2]);
                                tm.sentDecision("Room", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                System.out.println("Sent decision: " + decision + "to Room RM");
                                
                                c_out.println("Commit," + twoPCState.split(",")[2]);
                                tm.sentDecision("Car", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                System.out.println("Sent decision: " + decision + "to Car RM");
                            }else{
                                f_out.println("Abort," + twoPCState.split(",")[2]);
                                tm.sentDecision("Flight", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                System.out.println("Sent decision: " + decision + "to Flight RM");
                                
                                if(tm.getCrashStatus() == 6){
                                    System.out.println("Middleware server about to crash with mode: 6");
                                    System.exit(1);
                                }
                                
                                r_out.println("Abort," + twoPCState.split(",")[2]);
                                tm.sentDecision("Room", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                System.out.println("Sent decision: " + decision + "to Room RM");
                                
                                c_out.println("Abort," + twoPCState.split(",")[2]);
                                tm.sentDecision("Car", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                System.out.println("Sent decision: " + decision + "to Car RM");
                            }
                            tm.allDecisionsSent(Integer.parseInt(twoPCState.split(",")[2]), decision);
                            
                            if(tm.getCrashStatus() == 7){
                                System.out.println("Middleware server about to crash with mode: 7");
                                System.exit(1);
                            }
                        }
                    }
                }
                while((inputLine = in.readLine()) != null){
                    //CLIENT COMMAND HANDLING
                    arguments = parse(inputLine);
                    try{
                        String resp = "";
                        String resp_f = "";
                        String resp_c = "";
                        String resp_r = "";
                        Command cmd = Command.fromString((String)arguments.elementAt(0));
                        
                        if(isStarted && !cmd.equals(Command.Shutdown) && !cmd.equals(Command.Timeout) && !cmd.equals(Command.CrashRM) && !cmd.equals(Command.Ping) && !cmd.equals(Command.CrashTM) && !cmd.equals(Command.ResetCrash)){
                            if(cmd.equals(Command.Commit) && arguments.size() == 1){
                                out.println("Please enter XID to commit");
                                continue;
                            }
                            if(to.getXId() != Integer.parseInt(arguments.elementAt(1))){
                                //xid does NOT exist!
                                out.println("xid does NOT exist!");
                                continue;
                            }
                        }else if(!isStarted && !cmd.equals(Command.Start) && !cmd.equals(Command.Shutdown) && !cmd.equals(Command.CrashRM) && !cmd.equals(Command.Ping) && !cmd.equals(Command.CrashTM) && !cmd.equals(Command.ResetCrash)){
                            out.println("Need to start a transaction (Start)");
                            continue;
                        }
                        if(!inputLine.contains("GetData") && !inputLine.contains("Quit") && !inputLine.contains("Query") && !inputLine.contains("Crash")){
                            // BufferedWriter sfbw = null;
                            // if(!stagedTrans.createNewFile()){
                                //     //if already exists committed transaction file
                                //     System.out.println("Found stagedTransMW.txt");
                                //     FileWriter fw = new FileWriter(stagedTrans, true);
                                //     FileReader fr = new FileReader(stagedTrans);
                                //     BufferedReader test = new BufferedReader(fr);
                                //     String line = "";
                                //     while((line = test.readLine()) != null){
                                    //         System.out.println(line);
                                    //     }
                                    //     sfbw = new BufferedWriter(fw);
                                    // }else{
                                        //     //created new staged trans
                                        //     System.out.println("Created new stagedTransMW.txt");
                                        //     FileWriter fw = new FileWriter(stagedTrans, true);
                                        //     sfbw = new BufferedWriter(fw);
                                        // }
                                        // if(!inputLine.contains("Prepare")) sfbw.write(inputLine);
                                        // if(!inputLine.contains("Prepare")) sfbw.newLine();
                                        // sfbw.close();
                                        // System.out.println("Wrote to stagedTrans and closed");
                                    }
                                    switch(cmd){
                                        // case Help:
                                        // {
                                            
                                            // }
                                            case AddFlight:
                                            //NEED WRITE LOCK FOR FLIGHT
                                            lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            // out.println("Flight Added");
                                            break;
                                            
                                            case AddCars:
                                            //NEED WRITE LOCK FOR CAR
                                            lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "CAR", inputLine);
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            // out.println("Cars Added");
                                            break;
                                            
                                            case AddRooms:
                                            //NEED WRITE LOCK FOR ROOM
                                            lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "ROOM", inputLine);
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            // out.println("Rooms Added");
                                            break;
                                            
                                            case AddCustomer:
                                            //NEED WRITE LOCK FOR CUSTOMER
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            // out.println("Customer Added");
                                            break;
                                            
                                            case AddCustomerID:
                                            //NEED WRITE LOCK FOR CUSTOMER
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            // out.println("Customer Added");
                                            break;
                                            
                                            case DeleteFlight:
                                            //NEED WRITE LOCK FOR FLIGHT
                                            lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            // out.println("Flight Deleted");
                                            break;
                                            
                                            case DeleteCars:
                                            //NEED WRITE LOCK FOR CAR
                                            lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "CAR", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case DeleteRooms:
                                            //NEED WRITE LOCK FOR ROOM
                                            lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "ROOM", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case DeleteCustomer:
                                            //NEED WRITE LOCK FOR CUSTOMER
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryFlight:
                                            //NEED READ LOCK FOR FLIGHT
                                            lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryCars:
                                            //NEED READ LOCK FOR CAR
                                            lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "CAR", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryRooms:
                                            //NEED READ LOCK FOR ROOM
                                            lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "ROOM", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryCustomer:
                                            //NEED READ LOCK FOR CUSTOMER
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryFlightPrice:
                                            //NEED READ LOCK FOR FLIGHT
                                            lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryCarsPrice:
                                            //NEED READ LOCK FOR CAR
                                            lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "CAR", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case QueryRoomsPrice:
                                            //NEED READ LOCK FOR ROOM
                                            lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_READ);
                                            tm.newOperation(to.getXId(), "ROOM", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case ReserveFlight:
                                            //NEED WRITE LOCK FOR FLIGHT, CUSTOMER
                                            lm.Lock(to.getXId(), "FLIGHT", TransactionLockObject.LockType.LOCK_WRITE);
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "FLIGHT", inputLine);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case ReserveCar:
                                            //NEED WRITE LOCK FOR CAR, CUSTOMER
                                            lm.Lock(to.getXId(), "CAR", TransactionLockObject.LockType.LOCK_WRITE);
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "CAR", inputLine);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
                                            break;
                                            
                                            case ReserveRoom:
                                            //NEED WRITE LOCK FOR ROOM, CUSTOMER
                                            lm.Lock(to.getXId(), "ROOM", TransactionLockObject.LockType.LOCK_WRITE);
                                            lm.Lock(to.getXId(), "CUSTOMER", TransactionLockObject.LockType.LOCK_WRITE);
                                            tm.newOperation(to.getXId(), "ROOM", inputLine);
                                            tm.newOperation(to.getXId(), "CUSTOMER", inputLine);
                                            // out.println("Flight Added");
                                            tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
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
                                                tm.newResponse(to.getXId(), innerExecute(inputLine, true, true));
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
                                                lm.UnlockAll(to.getXId());
                                                //tm.commit(to.getXId());
                                                tm.begin2PC(to.getXId());
                                                //before sending vote
                                                //-----
                                                if(tm.getCrashStatus() == 1){
                                                    System.out.println("Middleware server about to crash with mode: 1");
                                                    System.exit(1);
                                                }
                                                twoPCState = tm.getMiddlewareState();
                                                f_out.println("Prepare," + twoPCState.split(",")[1]);
                                                c_out.println("Prepare," + twoPCState.split(",")[1]);
                                                r_out.println("Prepare," + twoPCState.split(",")[1]);
                                                
                                                tm.sentVote(Integer.parseInt(twoPCState.split(",")[1]));
                                                
                                                if(tm.getCrashStatus() == 2){
                                                    System.out.println("Middleware server about to crash with mode: 2");
                                                    System.exit(1);
                                                }
                                                //-----
                                                boolean waitingForVotes = true;
                                                twoPCState = tm.getMiddlewareState();
                                                Date startTime = new Date();
                                                boolean fReceived = false, rReceived = false, cReceived = false;
                                                while(waitingForVotes && ((System.currentTimeMillis() - startTime.getTime()) < timeoutMs)){
                                                    if(twoPCState.contains("Flight")){
                                                        String f_resp = "";
                                                        if(!fReceived){
                                                            f_resp = f_in.readLine();
                                                        }
                                                        //----------------READLINE RETURNS NULL IF LOST CONNECTION!----------------------
                                                        if(f_resp == null){
                                                            try{
                                                                flightSocket = new Socket(fip, portnum);
                                                                f_out = new PrintWriter(flightSocket.getOutputStream(), true);
                                                                f_in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
                                                                System.out.println("Re-Connected to Flight Server /" + fip + ":" + portnum);
                                                                f_resp = f_in.readLine();
                                                                if(f_resp != null || f_resp.length() > 1){
                                                                    fReceived = true;
                                                                }
                                                            }catch(Exception e){
                                                                //should throw I/O when socket is closed, or not connected
                                                                System.out.println("FAILED Reconnecting to Flight Server...");
                                                            }
                                                        }else{
                                                            tm.receivedVote("Flight", Integer.parseInt(twoPCState.split(",")[1]));
                                                            //SEND SIGNAL TO FLIGHT RM
                                                            System.out.println("Received Flight Vote: " + f_resp);
                                                            String tempa = "";
                                                            for(int i=0; i < twoPCState.split(",").length; i++){
                                                                if(!twoPCState.split(",")[i].equals("Flight")){
                                                                    tempa += twoPCState.split(",")[i] + ",";
                                                                }
                                                            }
                                                            twoPCState = tempa.substring(0, tempa.length());
                                                            if(twoPCState.split(",").length == 2){
                                                                twoPCState = twoPCState.split(",")[0] + "," + twoPCState.split(",")[1] + ",none";
                                                            }
                                                            if(tm.getCrashStatus() == 3){
                                                                System.out.println("Middleware server about to crash with mode: 3");
                                                                System.exit(1);
                                                            }
                                                            fReceived = true;
                                                            votes[0] = f_resp.equals("YES") ? 1 : 0;
                                                        }
                                                    }
                                                    if(twoPCState.contains("Room")){
                                                        String r_resp = "";
                                                        if(!rReceived){
                                                            r_resp = r_in.readLine();
                                                        }
                                                        //----------------READLINE THROWS NULL IF LOST CONNECTION!----------------------
                                                        if(r_resp == null){
                                                            try{
                                                                roomSocket = new Socket(rip, portnum);
                                                                r_out = new PrintWriter(roomSocket.getOutputStream(), true);
                                                                r_in = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                                                                System.out.println("Re-Connected to Room Server /" + rip + ":" + portnum);
                                                                r_resp = r_in.readLine();
                                                                if(r_resp != null || r_resp.length() > 1){
                                                                    rReceived = true;
                                                                }
                                                            }catch(Exception e){
                                                                //should throw I/O when socket is closed, or not connected
                                                                System.out.println("FAILED Reconnecting to Room Server");
                                                            }
                                                        }else{
                                                            tm.receivedVote("Room", Integer.parseInt(twoPCState.split(",")[1]));
                                                            //SEND SIGNAL TO ROOM RM
                                                            System.out.println("Received Room Vote: " + r_resp);
                                                            String tempa = "";
                                                            for(int i=0; i < twoPCState.split(",").length; i++){
                                                                if(!twoPCState.split(",")[i].equals("Room")){
                                                                    tempa += twoPCState.split(",")[i] + ",";
                                                                }
                                                            }
                                                            twoPCState = tempa.substring(0, tempa.length());
                                                            if(twoPCState.split(",").length == 2){
                                                                twoPCState = twoPCState.split(",")[0] + "," + twoPCState.split(",")[1] + ",none";
                                                            }
                                                            rReceived = true;
                                                            votes[1] = r_resp.equals("YES") ? 1 : 0;
                                                        }
                                                        
                                                    }
                                                    if(twoPCState.contains("Car")){
                                                        String c_resp = "";
                                                        if(!cReceived){
                                                            c_resp = c_in.readLine();
                                                        }
                                                        //----------------READLINE THROWS NULL IF LOST CONNECTION!----------------------
                                                        if(c_resp == null){
                                                            try{
                                                                carSocket = new Socket(cip, portnum);
                                                                c_out = new PrintWriter(carSocket.getOutputStream(), true);
                                                                c_in = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
                                                                System.out.println("Re-Connected to Car Server /" + cip + ":" + portnum);
                                                                c_resp = c_in.readLine();
                                                                if(c_resp != null || c_resp.length() > 1){
                                                                    cReceived = true;
                                                                }
                                                            }catch(Exception e){
                                                                //should throw I/O when socket is closed, or not connected
                                                                System.out.println("FAILED Reconnecting to Car Server");
                                                            }
                                                        }else{
                                                            tm.receivedVote("Car", Integer.parseInt(twoPCState.split(",")[1]));
                                                            //SEND SIGNAL TO CAR RM
                                                            System.out.println("Received Car Vote: " + c_resp);
                                                            String tempa = "";
                                                            for(int i=0; i < twoPCState.split(",").length; i++){
                                                                if(!twoPCState.split(",")[i].equals("Car")){
                                                                    tempa += twoPCState.split(",")[i] + ",";
                                                                }
                                                            }
                                                            twoPCState = tempa.substring(0, tempa.length());
                                                            if(twoPCState.split(",").length == 2){
                                                                twoPCState = twoPCState.split(",")[0] + "," + twoPCState.split(",")[1] + ",none";
                                                            }
                                                            cReceived = true;
                                                            votes[2] = c_resp.equals("YES") ? 1 : 0;
                                                        }
                                                        
                                                    }
                                                    if(twoPCState.contains("none")){
                                                        //all votes received
                                                        tm.receivedAllVotes(votes, Integer.parseInt(twoPCState.split(",")[1]));
                                                        System.out.println("Received All Votes");
                                                        waitingForVotes = false;
                                                        if(tm.getCrashStatus() == 4){
                                                            System.out.println("Middleware server about to crash with mode: 4");
                                                            System.exit(1);
                                                        }
                                                    }
                                                    Thread.sleep(500);
                                                }
                                                //if timed out, at least one RM vote is unreceived
                                                if(!fReceived || !rReceived || !cReceived){
                                                    if(!fReceived) votes[0] = 0;
                                                    if(!rReceived) votes[1] = 0;
                                                    if(!cReceived) votes[2] = 0;
                                                    tm.receivedAllVotes(votes, Integer.parseInt(twoPCState.split(",")[1]));
                                                    System.out.println("At least one vote not received!");
                                                    waitingForVotes = false;
                                                    if(tm.getCrashStatus() == 4){
                                                        System.out.println("Middleware server about to crash with mode: 4");
                                                        System.exit(1);
                                                    }
                                                }
                                                
                                                
                                                //---- RECEIVED ALL . GOING TO MAKE DECISION
                                                twoPCState = tm.getMiddlewareState();
                                                System.out.println(twoPCState);
                                                tm.makeDecision(twoPCState.split(",")[1], Integer.parseInt(twoPCState.split(",")[2]));
                                                System.out.println("Made decision");
                                                if(tm.getCrashStatus() == 5){
                                                    System.out.println("Middleware server about to crash with mode: 5");
                                                    System.exit(1);
                                                }
                                                
                                                //---- MADE DECISION . GOING TO SEND DECISION
                                                twoPCState = tm.getMiddlewareState();
                                                System.out.println("Current MW 2PC state: " + twoPCState);
                                                Boolean decision = Boolean.parseBoolean(twoPCState.split(",")[1]);
                                                if(decision){
                                                    f_out.println("Commit," + twoPCState.split(",")[2]);
                                                    tm.sentDecision("Flight", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                                    System.out.println("Sent COMMIT to Flight RM");
                                                    
                                                    if(tm.getCrashStatus() == 6){
                                                        System.out.println("Middleware server about to crash with mode: 6");
                                                        System.exit(1);
                                                    }
                                                    
                                                    r_out.println("Commit," + twoPCState.split(",")[2]);
                                                    tm.sentDecision("Room", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                                    System.out.println("Sent COMMIT to Room RM");
                                                    
                                                    c_out.println("Commit," + twoPCState.split(",")[2]);
                                                    tm.sentDecision("Car", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                                    System.out.println("Sent COMMIT to Car RM");

                                                    //innerExecute("Commit," + twoPCState.split(",")[2], false, false);
                                                }else{
                                                    f_out.println("Abort," + twoPCState.split(",")[2]);
                                                    f_in.readLine();
                                                    tm.sentDecision("Flight", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                                    System.out.println("Sent ABORT to Flight RM");
                                                    
                                                    if(tm.getCrashStatus() == 6){
                                                        System.out.println("Middleware server about to crash with mode: 6");
                                                        System.exit(1);
                                                    }
                                                    
                                                    r_out.println("Abort," + twoPCState.split(",")[2]);
                                                    r_in.readLine();
                                                    tm.sentDecision("Room", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                                    System.out.println("Sent ABORT to Room RM");
                                                    
                                                    c_out.println("Abort," + twoPCState.split(",")[2]);
                                                    c_in.readLine();
                                                    tm.sentDecision("Car", decision, Integer.parseInt(twoPCState.split(",")[2]));
                                                    System.out.println("Sent ABORT to Car RM");

                                                    // innerExecute("Abort," + twoPCState.split(",")[2], false, false);

                                                    try{
                                                        String[] operationHistory = tm.getOperationHistory(Integer.parseInt(arguments.elementAt(1)));
                                                String[] responseHistory = tm.getResponseHistory(to.getXId());
                                                LinkedList<String> flightDataHistory = tm.getDataHistory(to.getXId(), "flight");
                                                LinkedList<String> roomDataHistory = tm.getDataHistory(to.getXId(), "room");
                                                LinkedList<String> carDataHistory = tm.getDataHistory(to.getXId(), "car");
                                                LinkedList<String> customerDataHistory = tm.getDataHistory(to.getXId(), "customer");
                                                
                                                //out.println("Aborted transaction [" + to.getXId() + "]");
                                                
                                                System.out.println("Have identified " + operationHistory.length + " operations to undo");
                                                for(int i = operationHistory.length-1 ; i >= 0 ; i--){
                                                    Vector<String> args = parse(operationHistory[i]);
                                                    Command usedCmd = Command.fromString((String)args.elementAt(0));
                                                    switch(usedCmd){
                                                        case AddFlight:{
                                                            innerExecute("DeleteFlight," +  to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Flight");
                                                            break;
                                                        }
                                                        case AddCars:{
                                                            innerExecute("DeleteCars," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Cars");
                                                            break;
                                                        }
                                                        case AddRooms:{
                                                            innerExecute("DeleteRooms," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Rooms");
                                                            break;
                                                        }
                                                        case AddCustomer:{
                                                            innerExecute("DeleteCustomer," + to.getXId() + "," + customerDataHistory.removeLast(), false, true);
                                                            break;
                                                        }
                                                        case AddCustomerID:{
                                                            innerExecute("DeleteCustomer," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            break;
                                                        }
                                                        case DeleteFlight:{
                                                            String addFlight = flightDataHistory.removeLast();
                                                            String[] argumentz = addFlight.split(",");
                                                            innerExecute("AddFlight," + to.getXId() + "," + argumentz[1] + "," + argumentz[2] + "," + argumentz[3], false, true);
                                                            break;
                                                        }
                                                        case DeleteCars:{
                                                            String addCars = carDataHistory.removeLast();
                                                            String[] argumentz = addCars.split(",");
                                                            innerExecute("AddCars," + to.getXId() + "," + argumentz[3] + "," + argumentz[2] + "," + argumentz[4], false, true);
                                                            break;
                                                        }
                                                        case DeleteRooms:{
                                                            String addRooms = roomDataHistory.removeLast();
                                                            String[] argumentz = addRooms.split(",");
                                                            innerExecute("AddRoomss," + to.getXId() + "," + argumentz[3] + "," + argumentz[2] + "," + argumentz[4], false, true);
                                                            break;
                                                        }
                                                        case DeleteCustomer:{
                                                            String addCustomer = customerDataHistory.removeLast();
                                                            String[] argumentz = addCustomer.split(",");
                                                            innerExecute("AddCustomerID," + to.getXId() + "," + argumentz[1], false, true);
                                                            break;
                                                        }
                                                        case ReserveFlight:{
                                                            if(flightDataHistory.size() != 0){
                                                                String addFlight = flightDataHistory.removeLast();
                                                                String[] argumentz = addFlight.split(",");
                                                                innerExecute("AddFlight," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case ReserveCar:{
                                                            //TODO: CLEANUP
                                                            if(carDataHistory.size() != 0){
                                                                String addCar = carDataHistory.removeLast();
                                                                String[] argumentz = addCar.split(",");
                                                                innerExecute("AddCars," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case ReserveRoom:{
                                                            //TODO: CLEANUP
                                                            if(carDataHistory.size() != 0){
                                                                String addCar = carDataHistory.removeLast();
                                                                String[] argumentz = addCar.split(",");
                                                                innerExecute("AddCars," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case Bundle:{
                                                            //TODO: COPY PASTA ALL RESERVE UNDO's AND CLEAN
                                                            break;
                                                        }
                                                        default:
                                                        //was either a read or inconsequential
                                                        break;
                                                    }
                                                }
                                                

                                                    }catch(Exception e){

                                                        System.out.println("Error attempting abort at end of 2PC");
                                                    }
                                                }
                                                
                                                if(tm.getCrashStatus() == 7){
                                                    System.out.println("Middleware server about to crash with mode: 7");
                                                    System.exit(1);
                                                }
                                                //---- DECISIONS SENT
                                                
                                                tm.allDecisionsSent(Integer.parseInt(twoPCState.split(",")[2]), decision);
                                                isStarted = false;
                                                if(decision){
                                                    System.out.println("Committed transaction [" + twoPCState.split(",")[2] + "]");
                                                    out.println("Commited transaction [" + twoPCState.split(",")[2] + "]");
                                                }else{
                                                    System.out.println("Aborted transaction [" + twoPCState.split(",")[2] + "]");
                                                    out.println("Aborted transaction [" + twoPCState.split(",")[2] + "]");
                                                }
                                                break; 
                                            }
                                            
                                            case Abort:
                                            {
                                                //tm.commit(to.getXId());
                                                isStarted = false;
                                                
                                                String[] operationHistory = tm.getOperationHistory(Integer.parseInt(arguments.elementAt(1)));
                                                String[] responseHistory = tm.getResponseHistory(to.getXId());
                                                LinkedList<String> flightDataHistory = tm.getDataHistory(to.getXId(), "flight");
                                                LinkedList<String> roomDataHistory = tm.getDataHistory(to.getXId(), "room");
                                                LinkedList<String> carDataHistory = tm.getDataHistory(to.getXId(), "car");
                                                LinkedList<String> customerDataHistory = tm.getDataHistory(to.getXId(), "customer");
                                                
                                                out.println("Aborted transaction [" + to.getXId() + "]");
                                                
                                                System.out.println("Have identified " + operationHistory.length + " operations to undo");
                                                for(int i = operationHistory.length-1 ; i >= 0 ; i--){
                                                    Vector<String> args = parse(operationHistory[i]);
                                                    Command usedCmd = Command.fromString((String)args.elementAt(0));
                                                    switch(usedCmd){
                                                        case AddFlight:{
                                                            innerExecute("DeleteFlight," +  to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Flight");
                                                            break;
                                                        }
                                                        case AddCars:{
                                                            innerExecute("DeleteCars," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Cars");
                                                            break;
                                                        }
                                                        case AddRooms:{
                                                            innerExecute("DeleteRooms," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Rooms");
                                                            break;
                                                        }
                                                        case AddCustomer:{
                                                            innerExecute("DeleteCustomer," + to.getXId() + "," + customerDataHistory.removeLast(), false, true);
                                                            break;
                                                        }
                                                        case AddCustomerID:{
                                                            innerExecute("DeleteCustomer," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            break;
                                                        }
                                                        case DeleteFlight:{
                                                            String addFlight = flightDataHistory.removeLast();
                                                            String[] argumentz = addFlight.split(",");
                                                            innerExecute("AddFlight," + to.getXId() + "," + argumentz[1] + "," + argumentz[2] + "," + argumentz[3], false, true);
                                                            break;
                                                        }
                                                        case DeleteCars:{
                                                            String addCars = carDataHistory.removeLast();
                                                            String[] argumentz = addCars.split(",");
                                                            innerExecute("AddCars," + to.getXId() + "," + argumentz[3] + "," + argumentz[2] + "," + argumentz[4], false, true);
                                                            break;
                                                        }
                                                        case DeleteRooms:{
                                                            String addRooms = roomDataHistory.removeLast();
                                                            String[] argumentz = addRooms.split(",");
                                                            innerExecute("AddRoomss," + to.getXId() + "," + argumentz[3] + "," + argumentz[2] + "," + argumentz[4], false, true);
                                                            break;
                                                        }
                                                        case DeleteCustomer:{
                                                            String addCustomer = customerDataHistory.removeLast();
                                                            String[] argumentz = addCustomer.split(",");
                                                            innerExecute("AddCustomerID," + to.getXId() + "," + argumentz[1], false, true);
                                                            break;
                                                        }
                                                        case ReserveFlight:{
                                                            if(flightDataHistory.size() != 0){
                                                                String addFlight = flightDataHistory.removeLast();
                                                                String[] argumentz = addFlight.split(",");
                                                                innerExecute("AddFlight," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case ReserveCar:{
                                                            //TODO: CLEANUP
                                                            if(carDataHistory.size() != 0){
                                                                String addCar = carDataHistory.removeLast();
                                                                String[] argumentz = addCar.split(",");
                                                                innerExecute("AddCars," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case ReserveRoom:{
                                                            //TODO: CLEANUP
                                                            if(carDataHistory.size() != 0){
                                                                String addCar = carDataHistory.removeLast();
                                                                String[] argumentz = addCar.split(",");
                                                                innerExecute("AddCars," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case Bundle:{
                                                            //TODO: COPY PASTA ALL RESERVE UNDO's AND CLEAN
                                                            break;
                                                        }
                                                        default:
                                                        //was either a read or inconsequential
                                                        break;
                                                    }
                                                }
                                                
                                                lm.UnlockAll(to.getXId());
                                                innerExecute(inputLine, false, false);
                                                
                                                
                                                
                                                break;
                                            }
                                            
                                            case Timeout:
                                            {
                                                //tm.commit(to.getXId());
                                                isStarted = false;
                                                
                                                String[] operationHistory = tm.getOperationHistory(Integer.parseInt(arguments.elementAt(1)));
                                                String[] responseHistory = tm.getResponseHistory(to.getXId());
                                                LinkedList<String> flightDataHistory = tm.getDataHistory(to.getXId(), "flight");
                                                LinkedList<String> roomDataHistory = tm.getDataHistory(to.getXId(), "room");
                                                LinkedList<String> carDataHistory = tm.getDataHistory(to.getXId(), "car");
                                                LinkedList<String> customerDataHistory = tm.getDataHistory(to.getXId(), "customer");
                                                
                                                out.println("Aborted transaction DUE TO TIMEOUT [" + to.getXId() + "]");
                                                
                                                System.out.println("Have identified " + operationHistory.length + " operations to undo");
                                                for(int i = operationHistory.length-1 ; i >= 0 ; i--){
                                                    Vector<String> args = parse(operationHistory[i]);
                                                    Command usedCmd = Command.fromString((String)args.elementAt(0));
                                                    switch(usedCmd){
                                                        case AddFlight:{
                                                            innerExecute("DeleteFlight," +  to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Flight");
                                                            break;
                                                        }
                                                        case AddCars:{
                                                            innerExecute("DeleteCars," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Cars");
                                                            break;
                                                        }
                                                        case AddRooms:{
                                                            innerExecute("DeleteRooms," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            System.out.println("Deleting Rooms");
                                                            break;
                                                        }
                                                        case AddCustomer:{
                                                            innerExecute("DeleteCustomer," + to.getXId() + "," + customerDataHistory.removeLast(), false, true);
                                                            break;
                                                        }
                                                        case AddCustomerID:{
                                                            innerExecute("DeleteCustomer," + to.getXId() + "," + args.elementAt(2), false, true);
                                                            break;
                                                        }
                                                        case DeleteFlight:{
                                                            String addFlight = flightDataHistory.removeLast();
                                                            String[] argumentz = addFlight.split(",");
                                                            innerExecute("AddFlight," + to.getXId() + "," + argumentz[1] + "," + argumentz[2] + "," + argumentz[3], false, true);
                                                            break;
                                                        }
                                                        case DeleteCars:{
                                                            String addCars = carDataHistory.removeLast();
                                                            String[] argumentz = addCars.split(",");
                                                            innerExecute("AddCars," + to.getXId() + "," + argumentz[3] + "," + argumentz[2] + "," + argumentz[4], false, true);
                                                            break;
                                                        }
                                                        case DeleteRooms:{
                                                            String addRooms = roomDataHistory.removeLast();
                                                            String[] argumentz = addRooms.split(",");
                                                            innerExecute("AddRooms," + to.getXId() + "," + argumentz[3] + "," + argumentz[2] + "," + argumentz[4], false, true);
                                                            break;
                                                        }
                                                        case DeleteCustomer:{
                                                            String addCustomer = customerDataHistory.removeLast();
                                                            String[] argumentz = addCustomer.split(",");
                                                            innerExecute("AddCustomerID," + to.getXId() + "," + argumentz[1], false, true);
                                                            break;
                                                        }
                                                        case ReserveFlight:{
                                                            if(flightDataHistory.size() != 0){
                                                                String addFlight = flightDataHistory.removeLast();
                                                                String[] argumentz = addFlight.split(",");
                                                                innerExecute("AddFlight," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case ReserveCar:{
                                                            //TODO: CLEANUP
                                                            if(carDataHistory.size() != 0){
                                                                String addCar = carDataHistory.removeLast();
                                                                String[] argumentz = addCar.split(",");
                                                                innerExecute("AddCars," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case ReserveRoom:{
                                                            //TODO: CLEANUP
                                                            if(carDataHistory.size() != 0){
                                                                String addCar = carDataHistory.removeLast();
                                                                String[] argumentz = addCar.split(",");
                                                                innerExecute("AddCars," + to.getXId() + "," + argumentz[1] + ",1," + argumentz[3], false, true);
                                                                String fixCustomer = customerDataHistory.removeLast();
                                                                String[] custArgs = fixCustomer.split(",");
                                                                innerExecute("RemoveReservation," + to.getXId() + "," + custArgs[1] + "," + argumentz[1] + ",FLIGHT", false, true);
                                                            }
                                                            break;
                                                        }
                                                        case Bundle:{
                                                            //TODO: COPY PASTA ALL RESERVE UNDO's AND CLEAN
                                                            break;
                                                        }
                                                        default:
                                                        //was either a read or inconsequential
                                                        break;
                                                    }
                                                    
                                                }
                                                lm.UnlockAll(to.getXId());
                                                //innerExecute(inputLine, false, false);
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
                                            case CrashRM:
                                            {
                                                int mode = Integer.valueOf(arguments.elementAt(2).trim());
                                                String server_name = arguments.elementAt(1).trim();
                                                String cmdString = "CrashRMServer, " + server_name + "," + mode;
                                                if(server_name.equals("Flight")){
                                                    f_out.println(cmdString);
                                                    out.println("Set Flight RM crash mode to " + mode + ".");
                                                }else if(server_name.equals("Car")){
                                                    c_out.println(cmdString);
                                                    out.println("Set Car RM crash mode to + " + mode + ".");
                                                }else if(server_name.equals("Room")){
                                                    r_out.println(cmdString);
                                                    out.println("Set Room RM crash mode to + " + mode + ".");
                                                }else{
                                                    System.out.println("Incorrect RM specified [" + arguments.elementAt(1).trim() + "]");
                                                    System.out.println("RM name must be Flight | Car | Room");
                                                }
                                                break;
                                            }
                                            case CrashTM:
                                            {
                                                int mode = Integer.valueOf(arguments.elementAt(1).trim());
                                                tm.crashMiddleware(mode);
                                                out.println("Setup TM crash mode: " + mode);
                                                break;
                                            }
                                            case ResetCrash:
                                            {
                                                String server_name = arguments.elementAt(1).trim();
                                                if(server_name.equals("Flight")){
                                                    f_out.write("ResetRMCrash");
                                                }else if(server_name.equals("Car")){
                                                    c_out.write("ResetRMCrash");
                                                }else if(server_name.equals("Room")){
                                                    r_out.write("ResetRMCrash");
                                                }else if (server_name.equals("Middleware")){
                                                    tm.resetCrashes();
                                                }else{
                                                    System.out.println("Incorrect server specified [" + server_name + "]");
                                                    System.out.println("Server name must be Flight | Car | Room | Middleware");
                                                }
                                                out.println("Reset crashes");
                                                break;
                                            }
                                            case GetCrashStatus:
                                            {
                                                String server_name = arguments.elementAt(1).trim();
                                                resp = "";
                                                if(server_name.equals("Flight")){
                                                    f_out.write("GetCrashStatus");
                                                    resp = f_in.readLine();
                                                }else if(server_name.equals("Car")){
                                                    c_out.write("GetCrashStatus");
                                                    resp = c_in.readLine();
                                                }else if(server_name.equals("Room")){
                                                    r_out.write("GetCrashStatus");
                                                    resp = r_in.readLine();
                                                }else if (server_name.equals("Middleware")){
                                                    resp = "Crash status of middleware server: " + String.valueOf(tm.getCrashStatus());
                                                }else{
                                                    resp = "Incorrect server specified [" + server_name + "]\n" + 
                                                    "Server name must be Flight | Car | Room | Middleware";
                                                }
                                                
                                                System.out.println(resp);
                                                break;
                                            }
                                            case GetTransState:{
                                                if(arguments.elementAt(2).equals("Flight")){
                                                    f_out.write(tm.getTransactionState(Integer.parseInt(arguments.elementAt(1))));
                                                }else if(arguments.elementAt(2).equals("Car")){
                                                    c_out.write(tm.getTransactionState(Integer.parseInt(arguments.elementAt(1))));
                                                }else if(arguments.elementAt(2).equals("Room")){
                                                    r_out.write(tm.getTransactionState(Integer.parseInt(arguments.elementAt(1))));
                                                }
                                                break;
                                            }
                                            case Ping:{
                                                if(arguments.elementAt(1).equals("Flight")){
                                                    flightSocket = new Socket(fip, portnum);
                                                    f_out = new PrintWriter(flightSocket.getOutputStream(), true);
                                                    f_in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
                                                }else if(arguments.elementAt(1).equals("Car")){
                                                    carSocket = new Socket(cip, portnum);
                                                    c_out = new PrintWriter(carSocket.getOutputStream(), true);
                                                    c_in = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
                                                }else if(arguments.elementAt(1).equals("Room")){
                                                    roomSocket = new Socket(rip, portnum);
                                                    r_out = new PrintWriter(roomSocket.getOutputStream(), true);
                                                    r_in = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                                                }
                                                out.println("Succesfully ping'd " + arguments.elementAt(1));
                                                break;
                                            }
                                            default:
                                            {
                                                out.println("Command handling error. (Unhandled input, couldn't identify command)");
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
                        public String getPreviousData(PrintWriter outStream, BufferedReader inStream, String objType, int xid, String objId){
                            String ret = "";
                            outStream.println("GetData," + xid + "," +  objType + "," +  objId);
                            try{
                                ret = inStream.readLine();
                            }catch (Exception e){
                                //TODO: handle IOException
                            }
                            return ret;
                        }
                        public String innerExecute(String inputLine, boolean showPrints, boolean logInTM){
                            //CLIENT COMMAND HANDLING
                            Vector<String> arguments = parse(inputLine);
                            String ret = "";
                            try{
                                String resp = "";
                                String resp_f = "";
                                String resp_c = "";
                                String resp_r = "";
                                Command cmd = Command.fromString((String)arguments.elementAt(0));
                                int xID = Integer.parseInt(arguments.elementAt(1));
                                switch(cmd){
                                    // case Help:
                                    // {
                                        
                                        // }
                                        case AddFlight:
                                        if(logInTM) tm.newData(xID, "FLIGHT", getPreviousData(f_out, f_in, "FLIGHT", xID, arguments.elementAt(2)));
                                        f_out.println(inputLine);
                                        resp = f_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case AddCars:
                                        if(logInTM) tm.newData(xID, "CAR", getPreviousData(c_out, c_in, "CAR", xID, arguments.elementAt(2)));
                                        c_out.println(inputLine);
                                        resp = c_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case AddRooms:
                                        if(logInTM) tm.newData(xID, "ROOM", getPreviousData(r_out, r_in, "ROOM", xID, arguments.elementAt(2)));
                                        r_out.println(inputLine);
                                        resp = r_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case AddCustomer:
                                        int id = Integer.parseInt(arguments.elementAt(1));
                                        int cid = Integer.parseInt(String.valueOf(id) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) + String.valueOf(Math.round(Math.random() * 100 + 1)));
                                        if(logInTM) tm.newData(xID, "CUSTOMER", cid + "");
                                        inputLine = "AddCustomerID," + id + "," + cid;
                                        r_out.println(inputLine);
                                        resp_r = r_in.readLine();
                                        f_out.println(inputLine);
                                        resp_f = f_in.readLine();
                                        c_out.println(inputLine);
                                        resp_c = c_in.readLine();
                                        if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                            ret = resp_r;
                                            if(showPrints) out.println("Response: " + resp_r);
                                        }else{
                                            ret = "Failed to Add Customer";
                                            out.println("Failed to Add Customer");
                                        }
                                        break;
                                        
                                        case AddCustomerID:
                                        if(logInTM) tm.newData(xID, "CUSTOMER", arguments.elementAt(2));
                                        r_out.println(inputLine);
                                        resp_r = r_in.readLine();
                                        f_out.println(inputLine);
                                        resp_f = f_in.readLine();
                                        c_out.println(inputLine);
                                        resp_c = c_in.readLine();
                                        if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                            ret = resp_r;
                                            if(showPrints) out.println("Response: " + resp_r);
                                        }else{
                                            ret = "Failed to Add Customer";
                                            out.println("Failed to Add Customer");
                                        }
                                        break;
                                        
                                        case DeleteFlight:
                                        if(logInTM) tm.newData(xID, "FLIGHT", getPreviousData(f_out, f_in, "FLIGHT", xID, arguments.elementAt(2)));
                                        f_out.println(inputLine);
                                        resp = f_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case DeleteCars:
                                        if(logInTM) tm.newData(xID, "CAR", getPreviousData(c_out, c_in, "CAR", xID, arguments.elementAt(2)));
                                        c_out.println(inputLine);
                                        resp = c_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case DeleteRooms:
                                        if(logInTM) tm.newData(xID, "ROOM", getPreviousData(r_out, r_in, "ROOM", xID, arguments.elementAt(2)));
                                        r_out.println(inputLine);
                                        resp = r_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case DeleteCustomer:
                                        if(logInTM) tm.newData(xID, "CUSTOMER", getPreviousData(r_out, r_in, "CUSTOMER", xID, arguments.elementAt(2)));
                                        r_out.println(inputLine);
                                        resp_r = r_in.readLine();
                                        f_out.println(inputLine);
                                        resp_f = f_in.readLine();
                                        c_out.println(inputLine);
                                        resp_c = c_in.readLine();
                                        if(!resp_r.contains("not") && !resp_c.contains("not") && !resp_f.contains("not")){
                                            ret = resp_r;
                                            if(showPrints) out.println("Response: " + resp_r);
                                        }else{
                                            ret = "Failed to Delete Customer";
                                            out.println("Failed to Delete Customer");
                                        }
                                        break;
                                        
                                        case QueryFlight:
                                        f_out.println(inputLine);
                                        resp = f_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case QueryCars:
                                        c_out.println(inputLine);
                                        resp = c_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case QueryRooms:
                                        r_out.println(inputLine);
                                        resp = r_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
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
                                            if(showPrints) out.println("Response:aaaBill for customer:aaa" + resp_r + "aaa" + resp_f + "aaa" + resp_c);
                                        }else{
                                            ret = "Failed to Query Customer";
                                            out.println("Failed to Query Customer");
                                        }
                                        break;
                                        
                                        case QueryFlightPrice:
                                        f_out.println(inputLine);
                                        resp = f_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case QueryCarsPrice:
                                        c_out.println(inputLine);
                                        resp = c_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case QueryRoomsPrice:
                                        r_out.println(inputLine);
                                        resp = r_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case ReserveFlight:
                                        if(logInTM) tm.newData(xID, "FLIGHT", getPreviousData(f_out, f_in, "FLIGHT", xID, arguments.elementAt(2)));
                                        if(logInTM) tm.newData(xID, "CUSTOMER", getPreviousData(f_out, f_in, "CUSTOMER", xID, arguments.elementAt(2)));
                                        f_out.println(inputLine);
                                        resp = f_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case ReserveCar:
                                        if(logInTM) tm.newData(xID, "CAR", getPreviousData(c_out, c_in, "CAR", xID, arguments.elementAt(2)));
                                        if(logInTM) tm.newData(xID, "CUSTOMER", getPreviousData(c_out, c_in, "CUSTOMER", xID, arguments.elementAt(2)));
                                        c_out.println(inputLine);
                                        resp = c_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
                                        break;
                                        
                                        case ReserveRoom:
                                        if(logInTM) tm.newData(xID, "ROOM", getPreviousData(c_out, c_in, "ROOM", xID, arguments.elementAt(2)));
                                        if(logInTM) tm.newData(xID, "CUSTOMER", getPreviousData(c_out, c_in, "CUSTOMER", xID, arguments.elementAt(2)));
                                        r_out.println(inputLine);
                                        resp = r_in.readLine();
                                        ret = resp;
                                        if(showPrints) out.println("Response: " + resp);
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
                                        case RemoveReservation:{
                                            switch(arguments.elementAt(4).toLowerCase()){
                                                case "flight":
                                                f_out.println("RemoveReservation," + toInt(arguments.elementAt(1)) + "," + toInt(arguments.elementAt(2)) + "," + arguments.elementAt(3) + "," + arguments.elementAt(4));
                                                resp = f_in.readLine();
                                                ret = resp;
                                                out.println("Response: " + resp);
                                                break;
                                                case "room":
                                                r_out.println("RemoveReservation," + toInt(arguments.elementAt(1)) + "," + toInt(arguments.elementAt(2)) + "," + arguments.elementAt(3) + "," + arguments.elementAt(4));
                                                resp = r_in.readLine();
                                                ret = resp;
                                                out.println("Response: " + resp);
                                                break;
                                                case "car":
                                                c_out.println("RemoveReservation," + toInt(arguments.elementAt(1)) + "," + toInt(arguments.elementAt(2)) + "," + arguments.elementAt(3) + "," + arguments.elementAt(4));
                                                resp = c_in.readLine();
                                                ret = resp;
                                                out.println("Response: " + resp);
                                                break;
                                            }
                                            break;
                                        }
                                        case Commit:{
                                            f_out.println(inputLine);
                                            String fresp = f_in.readLine();
                                            c_out.println(inputLine);
                                            String cresp = c_in.readLine();
                                            r_out.println(inputLine);
                                            String rresp = r_in.readLine();
                                            if(showPrints) out.println("Committed transaction ID: " + arguments.elementAt(1));
                                            break;
                                        }
                                        
                                        case Abort:{
                                            f_out.println(inputLine);
                                            String fresp = f_in.readLine();
                                            c_out.println(inputLine);
                                            String cresp = c_in.readLine();
                                            r_out.println(inputLine);
                                            String rresp = r_in.readLine();
                                            if(showPrints) out.println("Aborted transaction ID: " + arguments.elementAt(1));
                                            break;
                                        }
                                        
                                        case QueryTransaction:{
                                            
                                            int xid = Integer.valueOf(arguments.elementAt(1).trim());
                                            String server = arguments.elementAt(2).trim();
                                            PrintWriter out;
                                            if(server.equals("Flight")){
                                                out = f_out;
                                            }else if(server.equals("Car")){
                                                out = c_out;
                                            }else if(server.equals("Room")){
                                                out = r_out;
                                            }else{
                                                break;
                                            }
                                            
                                            if(!tm.transactionExist(xid)){
                                                out.println("Transaction with xid " + xid + "does not exist");
                                                break;
                                            }
                                            
                                            String status = getTransactionStatus(xid);
                                            
                                            out.println(status + "," + xid);
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
                        
                        public static String getTransactionStatus(int xid){
                            boolean committed = tm.isCommitted(xid);
                            boolean staged = tm.isStaged(xid);
                            boolean aborted = tm.isAborted(xid);
                            
                            if(committed && !staged && !aborted){
                                return "Committed";
                            }else if(!committed && staged && !aborted){
                                return "Staged";
                            }else if(!committed && !staged && aborted){
                                return "Aborted";
                            }
                            
                            return "Error";
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