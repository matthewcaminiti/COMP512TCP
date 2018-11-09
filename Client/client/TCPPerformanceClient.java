package client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import client.TCPClient;

public class TCPPerformanceClient extends TCPClient{

    private int transactionsPerSec;
    private int transactionTime;
    private int numTransactions;

    public TCPPerformanceClient(int rate, int num){
        super();

        // Messages/second
        transactionsPerSec = rate;
        // Time/transaction (to be used in sleep)
        transactionTime = 1000/transactionsPerSec;
        numTransactions = num;
    }

    public String startTransaction(){
        String response = "";
        try{
            response = this.sendMessage("Start");
        }catch (Exception e){
            System.out.println("Error");
        }
        
        String[] responseParts = response.split(" ");
        String xid = responseParts[responseParts.length - 1];
        return xid;
    }

    public static Iterator<String> getTransactionCommands(String xid, int customerID, int locationCounter){
        List<String> commands = new ArrayList<String>();
        
        String command1 = "AddCustomerID,"+xid+","+customerID;
        String command2 = "AddRooms,"+xid+",Loc-"+locationCounter+",1,100";
        String command3 = "AddCars,"+xid+",Loc-"+locationCounter+",1,50";
        String command4 = "QueryRoomsPrice,"+xid+",Loc-"+locationCounter;
        String command5 = "ReserveCar,"+xid+","+customerID+",Loc-"+locationCounter;
        String command6 = "Commit";

        commands.add(command1);
        commands.add(command2);
        commands.add(command3);
        commands.add(command4);
        commands.add(command5);
        commands.add(command6);

        return commands.iterator();
    }

public static void main(String[] args) throws Exception
    {
        TCPPerformanceClient client = new TCPPerformanceClient(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        client.startConnection(args[0], Integer.parseInt(args[1]));

        Date time = new Date();
        long totalTime = 0;

        for(int i = 0; i < 100; i++){
            long start = time.getTime();
            String xid = client.startTransaction();
            Iterator<String> commands = getTransactionCommands(xid,i,i);
            while(commands.hasNext()){
                String response = "";
                try{
                    response = client.sendMessage(commands.next());
                }catch (Exception e){
                    System.out.println("Error");
                }
                System.out.println(response.replaceAll("a{3}", "\n"));
                if(response.equals("Break")){
                    break;
                }
            }

            long end = time.getTime();
            long diff = end - start;

            System.out.println("Transaction took " + String.valueOf(diff) + " milliseconds to complete...");

            if(diff < client.transactionTime){
                try{
                    client.wait(diff);
                }catch(Exception e){
                    System.out.println("Couldn't wait...");
                }
                totalTime += client.transactionTime;
            }

        }

        System.out.println("Total Time: " + String.valueOf(totalTime));
    }


}