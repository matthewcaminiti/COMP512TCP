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
    
    public static String mwIP = "lab1-11.cs.mcgill.ca";
    public static int mwPort;
    
    public static void main(String[] args) throws Exception
    {
        RMServer server = new RMServer();
        server.rm = new ResourceManager(args[0]);
        server.setName(args[1]);
        server.start(Integer.parseInt(args[0]), args[1]);
    }
    
    public void start(int port, String name) throws Exception
    {
        mwPort = port;
        serverSocket = new ServerSocket(port);
        while(true){
            new RMServerHandler(serverSocket.accept(), rm, name).start();
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
        private String s_name;
        private Boolean in2PC;
        
        private File committedTrans = null;
        private File stagedTrans = null;
        private File twoPCLog = null;
        
        BufferedWriter cfbw = null;
        BufferedWriter sfbw = null;
        
        private String tpcState = "";
        
        public RMServerHandler(Socket socket, ResourceManager rm, String name)
        {
            this.clientSocket = socket;
            this.m_resourceManager = rm;
            this.s_name = name;
            this.committedTrans = new File("../committedTrans" + name + ".txt");
            this.stagedTrans = new File("../stagedTrans" + name + ".txt");
            this.twoPCLog = new File("../twoPCLog" + name + ".txt");
            this.in2PC = false;
        }
        
        public void run()
        {
            try{
                //CHECK IF TRANSACTIONS HAVE BEEN COMMITTED
                if(!committedTrans.createNewFile()){
                    //if already exists committed transaction file
                    System.out.println("Found committedTrans" + s_name + ".txt");
                    FileReader fr = new FileReader(committedTrans);
                    //Committed Transaction file will contain each transaction in chronological order (of commits)
                    // CT file will not have Transaction ID except for within Commands
                    // Should be able to safely re-execute each command
                    BufferedReader fbr = new BufferedReader(fr);
                    String line;
                    int currXid = -1;
                    Vector<String> arguments = new Vector<String>();
                    PrintWriter old_out = out;
                    out = new PrintWriter(System.out);
                    while((line = fbr.readLine()) != null){
                        //line will contain command
                        
                        arguments = parse(line);
                        try{
                            Command cmd = Command.fromString((String)arguments.elementAt(0));
                            try{
                                execute(cmd, arguments);
                            }catch (Exception e){
                                System.out.println("Execute crapped out.");
                                e.printStackTrace();
                            }
                            
                        }catch (Exception e){
                            System.out.println("Unknown command: " + line);
                            e.printStackTrace();
                        }
                        
                        System.out.println(line);
                    }
                    out = old_out;
                    
                }else{
                    //created new empty committedTrans.txt
                    System.out.println("Created new committedTrans" + s_name + ".txt");
                }
                
            }catch(Exception e){
                
            }
            
            try{
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Vector<String> arguments = new Vector<String>();
                String inputLine;
                
                try{
                    if(!stagedTrans.createNewFile()){
                        //if already exists committed transaction file
                        System.out.println("Found stagedTrans" + s_name + ".txt");
                        FileWriter fw = new FileWriter(stagedTrans, true);

                        FileReader fr = new FileReader(stagedTrans);
                        BufferedReader test = new BufferedReader(fr);
                        String line = "";
                        
                        arguments = new Vector<String>();
                        PrintWriter old_out = out;
                        out = new PrintWriter(System.out);
                        while((line = test.readLine()) != null){
                            arguments = parse(line);
                            try{
                                Command cmd = Command.fromString((String)arguments.elementAt(0));
                                try{
                                    execute(cmd, arguments);
                                }catch (Exception e){
                                    System.out.println("Execute crapped out.");
                                    e.printStackTrace();
                                }
                                
                            }catch (Exception e){
                                System.out.println("Unknown command: " + line);
                                e.printStackTrace();
                            }
                        }
                        out = old_out;
                        arguments = new Vector<String>();
                    }else{
                        //created new staged trans
                        System.out.println("Created new stagedTrans" + s_name + ".txt");
                    }
                }catch(Exception e){
                    System.out.println("Error when accessing stagedTrans. : ");
                    e.printStackTrace();
                }
                Vector<String> commandArgs = new Vector<String>();
                boolean checkWithMiddleware = false;
                try{
                    twoPCLog.createNewFile();
                    FileReader fr = new FileReader(twoPCLog);
                    BufferedReader br = new BufferedReader(fr);
                    
                    String lastLine = "";
                    String line = "";
                    while((line = br.readLine()) != null){
                        lastLine = line;
                    }
                    tpcState = lastLine;

                    if(tpcState != "none" && tpcState.length() > 3) {
                        in2PC = true;
                        //out.println("PING");
                    }

                    System.out.println("On reboot, found last 2PC state was: " + tpcState);
                    
                    Vector<String> logArgs = parse(lastLine);
                    commandArgs.add(logArgs.elementAt(2));
                    commandArgs.add(logArgs.elementAt(1));

                    if(lastLine.contains("Commit")){
                        execute(Command.Commit, commandArgs);
                    }else if(lastLine.contains("Abort")){
                        execute(Command.Abort, commandArgs);
                    }else{
                        System.out.println("setting checkWithMiddleWare flag");
                        checkWithMiddleware = true;
                    }

                }catch (Exception e){
                    //i/o error
                }
                
                //---------------------- ON REBOOT: BEFORE ENTERING 2PC -> ASK COORDINATOR WHAT'S UP -----------
                // if(checkWithMiddleware){
                //     System.out.println("Transaction state unknown when server crashed - ask Middleware");
                //     Socket mwSocket = new Socket(mwIP, mwPort);
                //     PrintWriter mw_out = new PrintWriter(clientSocket.getOutputStream(), true);
                //     BufferedReader mw_in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    
                //         //no commit/abort record

                //         //------- MESSAGE MW TO FIND TRANSACTION STATUS -----
                //         mw_out.println("QueryTransaction," + tpcState.split(",")[1] + "," + s_name);
                //         String resp = mw_in.readLine();

                //         if(resp.equals("Committed")){
                //             execute(Command.Commit, commandArgs);
                //         }else if (resp.equals("Aborted")){
                //             execute(Command.Abort, commandArgs);
                //         }else if (resp.equals("Staged")){
                //             execute(Command.Prepare, commandArgs);
                //         }else{
                //             System.out.println("Transaction "+tpcState.split(",")[1]+" is in an unknown state.");
                //         }
                //         // ---------------------------------------------------

                // }
                
                while(in2PC){
                    switch(tpcState.split(",")[0]){
                        case "receivedVoteReq":{

                            if(m_resourceManager.getCrashStatus() == 1){
                                System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 1");
                                System.out.println("    - After receiving vote request, but before sending answer...");
                                System.exit(1);
                            }
                            
                            FileReader fr = new FileReader(committedTrans);
                            BufferedReader br = new BufferedReader(fr);
                            
                            String line = "";
                            String lastLine = "";
                            while((line = br.readLine()) != null){
                                lastLine = line;
                            }
                            //line is now last operation executed
                            br.close();
                            
                            System.out.println("Upon crash recovery, determined last 2PC log: " + tpcState);
                            
                            String decision = "";
                            if(lastLine.split(",")[0] == "Abort"){
                                decision = "NO";
                                //out.println("NO");
                            }else{
                                decision = "YES";
                            }
                            in2PC = true;
                            
                            tpcState = "madeDecision," + tpcState.split(",")[1] + "," + decision;
                            
                            System.out.println("Made decision [" + decision + "]");
                            
                            FileWriter tpcLog = new FileWriter(twoPCLog, true);
                            BufferedWriter bw = new BufferedWriter(tpcLog);
                            
                            bw.write("madeDecision," + tpcState.split(",")[1] + "," + decision);
                            bw.newLine();
                            bw.close();
                            
                            //out.println("YES");
                            //System.out.println("Sent decision [" + decision + "] to MW.");
                            break;
                        }
                        case "madeDecision":{
                            if(m_resourceManager.getCrashStatus() == 2){
                                System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 2");
                                System.out.println("    - After deciding which answer to send...");
                                System.exit(1);
                            }
                            
                            FileReader fr = new FileReader(twoPCLog);
                            BufferedReader br = new BufferedReader(fr);
                            
                            String line = "";
                            String lastLine = "";
                            while((line = br.readLine()) != null){
                                lastLine = line;
                            }
                            br.close();
                            
                            out.println(lastLine.split(",")[2]); //send decision to middleware
                            System.out.println("Sent decision [" + lastLine.split(",")[2] + "] to MW.");
                            
                            FileWriter tpcLog = new FileWriter(twoPCLog, true);
                            BufferedWriter bw = new BufferedWriter(tpcLog);
                            
                            bw.write("sentDecision," + lastLine.split(",")[1] + "," + lastLine.split(",")[2]);
                            bw.newLine();
                            tpcState = "sentDecision," + lastLine.split(",")[1] + "," + lastLine.split(",")[2];
                            bw.close();
                            
                            break;
                        }
                        case "sentDecision":{
                            
                            // mwSocket = new Socket(mwIP, mwPort);
                            // mw_out = new PrintWriter(clientSocket.getOutputStream(), true);
                            // mw_in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            
                            
                            if(m_resourceManager.getCrashStatus() == 3){
                                System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 3");
                                System.out.println("    - After sending answer...");
                                System.exit(1);
                            }
                            
                            String masterDecision = in.readLine(); //WAITS TO RECEIVE ABORT OR COMMIT FROM MIDDLEWARE
                            System.out.println("Received decision to: " + masterDecision);
                            
                            FileReader fr = new FileReader(twoPCLog);
                            BufferedReader br = new BufferedReader(fr);
                            
                            String line = "";
                            String lastLine = "";
                            while((line = br.readLine()) != null){
                                lastLine = line;
                            }
                            br.close();
                            
                            FileWriter tpcLog = new FileWriter(twoPCLog, true);
                            BufferedWriter bw = new BufferedWriter(tpcLog);
                            
                            bw.write("receivedDecision," + lastLine.split(",")[1] + "," + masterDecision);
                            bw.newLine();
                            tpcState = "receivedDecision," + lastLine.split(",")[1] + "," + masterDecision;
                            bw.close();
                            
                            if(m_resourceManager.getCrashStatus() == 4){
                                System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 4");
                                System.out.println("    - After receiving decision but before committing/aborting...");
                                System.exit(1);
                            }
                            
                            if(masterDecision.split(",")[0].equals("Commit")){
                                //decision is to commit
                                Vector<String> asd = new Vector<String>();
                                asd.add("Commit");
                                asd.add(masterDecision.split(",")[1]);
                                System.out.println("Performed Commit," + masterDecision.split(",")[1]);
                                execute(Command.Commit, asd);
                                in2PC = false;
                            }else {
                                //decision is to abort
                                Vector<String> asd = new Vector<String>();
                                asd.add("Abort");
                                asd.add(masterDecision.split(",")[1]);
                                System.out.println("Performed Abort," + masterDecision.split(",")[1]);
                                execute(Command.Abort, asd);
                                in2PC = false;
                            }
                            
                            break;
                        }
                        case "receivedDecision":{
                            FileReader fr = new FileReader(twoPCLog);
                            BufferedReader br = new BufferedReader(fr);
                            
                            String line = "";
                            String lastLine = "";
                            while((line = br.readLine()) != null){
                                lastLine = line;
                            }
                            br.close();
                            
                            if(lastLine.split(",")[2].equals("Commit")){
                                //decision is to commit
                                Vector<String> asd = new Vector<String>();
                                asd.add("Commit");
                                asd.add(lastLine.split(",")[1]);
                                execute(Command.Commit, asd);
                                in2PC = false;
                            }else {
                                //decision is to abort
                                Vector<String> asd = new Vector<String>();
                                asd.add("Abort");
                                asd.add(lastLine.split(",")[1]);
                                execute(Command.Commit, asd);
                                in2PC = false;
                            }
                            FileWriter tpcLog = new FileWriter(twoPCLog, true);
                            BufferedWriter bw = new BufferedWriter(tpcLog);
                            
                            bw.write("none");
                            bw.newLine();
                            tpcState = "none";
                            bw.close();
                            
                            break;
                        }
                    }
                }

                //System.out.println("Waiting for input from MW...");
                try{
                    while((inputLine = in.readLine()) != null){ 
                        //CLIENT COMMAND HANDLING
                        arguments = parse(inputLine);
                        try{
                            Command cmd = Command.fromString((String)arguments.elementAt(0));
                            try{
                                execute(cmd, arguments);
                                if(!inputLine.contains("GetData") && !inputLine.contains("Quit") && !inputLine.contains("Query") && !inputLine.contains("Crash")){
                                    if(!stagedTrans.createNewFile()){
                                        //if already exists committed transaction file
                                        System.out.println("Found stagedTrans" + s_name + ".txt");
                                        FileWriter fw = new FileWriter(stagedTrans, true);
                                        FileReader fr = new FileReader(stagedTrans);
                                        BufferedReader test = new BufferedReader(fr);
                                        String line = "";
                                        while((line = test.readLine()) != null){
                                            System.out.println(line);
                                        }
                                        sfbw = new BufferedWriter(fw);
                                    }else{
                                        //created new staged trans
                                        System.out.println("Created new stagedTrans" + s_name + ".txt");
                                        FileWriter fw = new FileWriter(stagedTrans, true);
                                        sfbw = new BufferedWriter(fw);
                                    }
                                    if(!inputLine.contains("Prepare")) sfbw.write(inputLine);
                                    if(!inputLine.contains("Prepare")) sfbw.newLine();
                                    sfbw.close();
                                    System.out.println("Wrote to stagedTrans and closed");
                                }
                            }catch (Exception e){
                                System.out.println("Execute crapped out.");
                                out.println("Execute crapped out.");
                                //e.printStackTrace();
                            }
                            
                        }catch (Exception e){
                            System.out.println("Unknown command: " + inputLine);
                            out.println("Unknown command: " + inputLine);
                            e.printStackTrace();
                        }
                        //out.println("Success!");
                    }
                }catch(Exception e){
                    // IF REACHED HERE, MW CRASHED ---- 
                FileReader fr = new FileReader(twoPCLog);
                BufferedReader br = new BufferedReader(fr);
                    
                String lastLine = "";
                String line = "";
                while((line = br.readLine()) != null){
                    lastLine = line;
                }
                switch(lastLine.split(",")[0]){
                    case "receivedVoteReq":{
                        //shouldnt hit here
                        break;
                    }
                    case "madeDecision":{
                        if(lastLine.split(",")[2].equals("YES")){
                            System.out.println("Blocking indefinitely because voted: YES...");
                            while(true){
                                Thread.sleep(500);
                            } //block indefinitely
                        }else{
                            Date startTime = new Date();
                            while(((System.currentTimeMillis() - startTime.getTime()) < 30000)){
                                //wait for 
                                System.out.println("Waiting on MW...");
                                Thread.sleep(500);
                            }
                            System.out.println("Executing Abort," +  lastLine.split(",")[1] + " From Timeout");
                            Vector<String> asd = new Vector<String>();
                            asd.add("Abort");
                            asd.add(lastLine.split(",")[1]);
                            execute(Command.Abort, asd);
                        }
                        break;
                    }
                    case "sentDecision":{
                        if(lastLine.split(",")[2].equals("YES")){
                            System.out.println("Blocking indefinitely because voted: YES...");
                            while(true){
                                Thread.sleep(500);
                            } //block indefinitely
                        }else{
                            Date startTime = new Date();
                            while(((System.currentTimeMillis() - startTime.getTime()) < 30000)){
                                //wait for 
                                System.out.println("Waiting on MW...");
                                Thread.sleep(500);
                            }
                            System.out.println("Executing Abort," +  lastLine.split(",")[1] + " From Timeout");
                            Vector<String> asd = new Vector<String>();
                            asd.add("Abort");
                            asd.add(lastLine.split(",")[1]);
                            execute(Command.Abort, asd);
                        }
                        break;
                    }
                    case "receivedDecision":{

                        break;
                    }
                }
                
                }
                
                
                sfbw.close();
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
                case CrashRMServer:{
                    int mode = Integer.parseInt(arguments.elementAt(2).trim());
                    System.out.println("Setting CRASH MODE to: " + mode);
                    m_resourceManager.crashResourceManager(mode);
                    break;
                }
                case ResetRMCrash:{
                    m_resourceManager.crashResourceManager(0);
                    break;
                }
                case GetCrashStatus:{
                    out.println("Crash status of RM server [ " + s_name + "] is: " + String.valueOf(m_resourceManager.getCrashStatus()));
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
                case GetData:{
                    System.out.println("Reached getDATA...");
                    out.println(m_resourceManager.getObjectData(Integer.parseInt(arguments.elementAt(1)), arguments.elementAt(2), arguments.elementAt(3)));
                    break;
                }
                case RemoveReservation:{
                    out.println(m_resourceManager.removeReservation(Integer.parseInt(arguments.elementAt(1)), arguments.elementAt(2), arguments.elementAt(3), arguments.elementAt(4)));
                    break;
                }
                case Quit:{
                    //checkArgumentsCount(1, arguments.size());
                    if(sfbw != null) sfbw.close();
                    if(cfbw != null) cfbw.close();
                    System.out.println("Quitting client");
                    System.exit(0);
                    break;
                }
                case Commit:{
                    FileWriter fw = new FileWriter(committedTrans, true);
                    cfbw = new BufferedWriter(fw);
                    
                    FileReader fr = new FileReader(stagedTrans);
                    BufferedReader reader = new BufferedReader(fr);
                    
                    File temp = new File("tempTrans" + arguments.elementAt(1));
                    FileWriter tempFw = new FileWriter(temp, true);
                    BufferedWriter tempWriter = new BufferedWriter(tempFw);
                    
                    String line = "";
                    while((line = reader.readLine()) != null){
                        Vector<String> args = parse(line);
                        if(args.elementAt(1).equals(arguments.elementAt(1))){ //if it is the XID of the Committed Transaction
                            cfbw.write(line);
                            cfbw.newLine();
                        }else{
                            tempWriter.write(line);
                            tempWriter.newLine();
                        }
                    }
                    reader.close();
                    tempWriter.close();
                    stagedTrans.delete();
                    if(temp.renameTo(stagedTrans)){
                        //succesfully renamed tempFile
                    }
                    cfbw.close();

                    System.out.println("Committed: " + arguments.elementAt(1));

                    break;
                }
                case Abort:{
                    FileReader fr = new FileReader(stagedTrans);
                    BufferedReader reader = new BufferedReader(fr);
                    
                    File temp = new File("tempTrans" + arguments.elementAt(1));
                    FileWriter tempFw = new FileWriter(temp, true);
                    BufferedWriter tempWriter = new BufferedWriter(tempFw);
                    
                    String line = "";
                    while((line = reader.readLine()) != null){
                        Vector<String> args = parse(line);
                        if(args.elementAt(1).equals(arguments.elementAt(1))){ //if it is the XID of the Aborted Transaction
                            //ignore because we are 'erasing' the operations of this transaction
                        }else{
                            tempWriter.write(line);
                            tempWriter.newLine();
                        }
                    }
                    reader.close();
                    tempWriter.write("Abort," + arguments.elementAt(1));
                    tempWriter.newLine();
                    tempWriter.close();
                    stagedTrans.delete();
                    if(temp.renameTo(stagedTrans)){
                        //succesfully renamed tempFile
                    }
                    //System.out.println("Aborted: " +arguments.elementAt(1));
                    out.println("Aborted: " + arguments.elementAt(1));
                    break;
                }
                case Prepare:{
                    FileWriter tpcLog = new FileWriter(twoPCLog, true);
                    BufferedWriter bw = new BufferedWriter(tpcLog);
                    
                    bw.write("receivedVoteReq," + arguments.elementAt(1));
                    bw.newLine();
                    bw.close();
                    in2PC = true;
                    
                    System.out.println("Crash mode: " + m_resourceManager.getCrashStatus());
                    if(m_resourceManager.getCrashStatus() == 1){
                        System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 1");
                        System.out.println("    - After receiving vote request, but before sending answer...");
                        System.exit(1);
                    }
                    
                    FileReader fr = new FileReader(committedTrans);
                    BufferedReader br = new BufferedReader(fr);
                    
                    String line = "";
                    String lastLine = "";
                    while((line = br.readLine()) != null){
                        lastLine = line;
                    }
                    //line is now last operation executed
                    br.close();

                    String decision = "";
                    if(lastLine.contains("Abort")){
                        decision = "NO";
                    }else{
                        decision = "YES";
                    }
                    
                    
                    tpcLog = new FileWriter(twoPCLog, true);
                    bw = new BufferedWriter(tpcLog);
                    
                    bw.write("madeDecision," + arguments.elementAt(1) + "," + decision);
                    bw.newLine();
                    bw.close();
                    
                    System.out.println("Made Decision: " + decision);
                    
                    if(m_resourceManager.getCrashStatus() == 2){
                        System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 2");
                        System.out.println("    - After deciding which answer to send...");
                        System.exit(1);
                    }
                    
                    out.println(decision);
                    
                    //IF DECISION == "NO" THEN START TIMER

                    tpcLog = new FileWriter(twoPCLog, true);
                    bw = new BufferedWriter(tpcLog);
                    
                    bw.write("sentDecision," + arguments.elementAt(1) + "," + decision);
                    bw.newLine();
                    bw.close();
                    
                    System.out.println("Sent Decision to MW: " + decision);
                    
                    if(m_resourceManager.getCrashStatus() == 3){
                        System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 3");
                        System.out.println("    - After sending answer...");
                        System.exit(1);
                    }
                    
                    String masterDecision = in.readLine(); //waits for commit/abort -- Commit,xid
                    
                    if(masterDecision == null){
                        if(Boolean.parseBoolean(decision)){
                            //block indefinitely
                            while(true);
                        }else{
                            Date startTime = new Date();
                            while(((System.currentTimeMillis() - startTime.getTime()) < 30000)){
                                //wait for MW
                                System.out.println("Waiting on MW");
                                Thread.sleep(500);
                            }
                            masterDecision = "Abort," + arguments.elementAt(1);
                        }
                    }

                    tpcLog = new FileWriter(twoPCLog, true);
                    bw = new BufferedWriter(tpcLog);
                    
                    bw.write("receivedDecision," + arguments.elementAt(1) + "," + masterDecision.split(",")[0]);
                    bw.newLine();
                    bw.close();
                    
                    System.out.println("Received decision [" + masterDecision + "] from MW");
                    
                    if(m_resourceManager.getCrashStatus() == 4){
                        System.out.println("Resource manager server (name: " + this.s_name + ") about to crash with mode: 4");
                        System.out.println("    - After receiving decision but before committing/aborting...");
                        System.exit(1);
                    }
                    
                    Command cmdexec = masterDecision.split(",")[0].equals("Commit") ? Command.Commit : Command.Abort;
                    Vector<String> asd = new Vector<String>();
                    asd.add(masterDecision.split(",")[0]);
                    asd.add(masterDecision.split(",")[1]);
                    //EXECUTE COMMIT/ABORT
                    System.out.println("Performed: " + masterDecision.split(",")[0]);
                    execute(cmdexec, asd);
                    
                    
                    break;
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
    }
    
    public void setName(String name){
        s_name = name;
    }
    
    public String getName(){
        return s_name;
    }
    
    
    
}