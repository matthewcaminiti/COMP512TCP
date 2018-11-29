package server.lockmanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.rmi.RemoteException;
import java.util.*;


import javax.transaction.InvalidTransactionException;


import server.common.*;
import server.lockmanager.TransactionObject;

public class TransactionManager
{
    //maintain a list of active transactions
    private LinkedList<TransactionObject> transactionList;
    
    //maintain a list of each Transaction's commands
    private HashMap<Integer, LinkedList<String>> operationHistory;
    
    //maintain a lis to of each response to parse output for undoing
    private HashMap<Integer, LinkedList<String>> responseHistory;
    
    //maintain a list of every resource manager's data
    private HashMap<Integer, HashMap<String, LinkedList<String>>> dataHistory;
    
    //transaction counter for cleanliness when debugging/testing
    private int transactionCounter = 0;
    
    //keep track of which RM's are involved in a transaction
    private HashMap<Integer,LinkedList<String>> rmNeeded; //key-value, key being xid, values being linkedlist of needed RM's
    
    
    private File twoPCLog = null;
    
    BufferedReader tpcbr = null;
    BufferedWriter tpcbw = null;
    
    public TransactionManager(){
        transactionList = new LinkedList<TransactionObject>();
        transactionCounter = 0;
        rmNeeded = new HashMap<Integer, LinkedList<String>>();
        operationHistory = new HashMap<Integer, LinkedList<String>>();
        responseHistory = new HashMap<Integer, LinkedList<String>>();
        dataHistory = new HashMap<Integer, HashMap<String, LinkedList<String>>>();
    }
    
    public TransactionObject start() throws RemoteException 
    {
        while(true){
            if(rmNeeded.containsKey(transactionCounter)){
                transactionCounter++;
            }else{
                break;
            }
        }
        rmNeeded.put(transactionCounter, new LinkedList<String>());
        
        return new TransactionObject(transactionCounter);
    }
    
    //implement 1-phase commit: tell the appropriate RM's that they should commit/abort the transaction
    
    
    public boolean commit(int transactionId) throws RemoteException, InvalidTransactionException
    {
        try{
            if(transactionList.size() > 0){
                transactionList.remove(transactionList.get(transactionId));
            }
        }catch(Exception e){
            return false;
        }
        return true;
    }
    
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException
    {
        
    }
    
    public boolean shutdown() throws RemoteException
    {
        return false;
    }
    
    public String newOperation(int transactionId, String rmName, String command)
    {
        if(operationHistory == null || operationHistory.size() == 0){
            LinkedList<String> temp = new LinkedList<String>();
            temp.add(command);
            operationHistory.put(transactionId, temp);
            return "new opHistory";
        }else{
            if(operationHistory.get(transactionId) == null || operationHistory.get(transactionId).size() == 0){
                LinkedList<String> temp = new LinkedList<String>();
                temp.add(command);
                operationHistory.put(transactionId, temp);
                return "exisiting opHistory";
            }
            LinkedList<String> temp = new LinkedList<String>();
            temp = operationHistory.get(transactionId);
            temp.add(command);
            operationHistory.replace(transactionId, operationHistory.get(transactionId), temp);
            return "exisiting opHistory";
        }
    }
    
    public String newResponse(int transactionId, String response)
    {
        if(operationHistory == null || operationHistory.size() == 0){
            LinkedList<String> temp = new LinkedList<String>();
            temp.add(response);
            responseHistory.put(transactionId, temp);
            return "new opHistory";
        }else{
            if(responseHistory.get(transactionId) == null || responseHistory.get(transactionId).size() == 0){
                LinkedList<String> temp = new LinkedList<String>();
                temp.add(response);
                responseHistory.put(transactionId, temp);
                return "exisiting opHistory";
            }
            LinkedList<String> temp = new LinkedList<String>();
            temp = responseHistory.get(transactionId);
            temp.add(response);
            responseHistory.replace(transactionId, responseHistory.get(transactionId), temp);
            return "exisiting opHistory";
        }
    }
    
    public void newData(int transactionId, String rm, String data){
        rm = rm.toLowerCase();
        if(dataHistory == null || dataHistory.size() == 0){ //fully empty dataHistory
            LinkedList<String> tempLL = new LinkedList<String>();
            HashMap<String, LinkedList<String>> tempMap = new HashMap<String, LinkedList<String>>();
            tempLL.add(data);
            tempMap.put(rm, tempLL);
            dataHistory.put(transactionId, tempMap);
        }else{
            //some data exists
            if(dataHistory.get(transactionId) == null || dataHistory.get(transactionId).size() == 0){
                //no data exists for current transaction
                LinkedList<String> tempLL = new LinkedList<String>();
                HashMap<String, LinkedList<String>> tempMap = new HashMap<String, LinkedList<String>>();
                tempLL.add(data);
                tempMap.put(rm, tempLL);
                dataHistory.put(transactionId, tempMap);
            }else{
                //some data exists for the current transaction
                if(dataHistory.get(transactionId).get(rm) == null || dataHistory.get(transactionId).get(rm).size() == 0){
                    //no data exists for the specified rm
                    LinkedList<String> tempLL = new LinkedList<String>();
                    HashMap<String, LinkedList<String>> tempMap = new HashMap<String, LinkedList<String>>();
                    tempLL.add(data);
                    tempMap.put(rm, tempLL);
                    dataHistory.get(transactionId).put(rm, tempLL);
                }else{
                    //data exists for the specified rm
                    LinkedList<String> tempLL = dataHistory.get(transactionId).get(rm);
                    tempLL.add(data);
                    dataHistory.get(transactionId).replace(rm, dataHistory.get(transactionId).get(rm), tempLL);
                }
            }
        }
    }
    public LinkedList<String> getUsedRms(int transactionId)
    {
        return rmNeeded.get(transactionId);
    }
    
    public boolean transactionExist(int transactionId)
    {
        return rmNeeded.containsKey(transactionId);
    }
    
    public String[] getOperationHistory(int transactionId)
    {
        String[] temp = new String[operationHistory.get(transactionId).size()];
        for(int i=0; i < operationHistory.get(transactionId).size(); i++){
            temp[i] = operationHistory.get(transactionId).get(i);
        }
        return temp;
    }
    
    public String[] getResponseHistory(int transactionId)
    {
        String[] temp = new String[responseHistory.get(transactionId).size()];
        for(int i=0; i < responseHistory.get(transactionId).size(); i++){
            temp[i] = responseHistory.get(transactionId).get(i);
        }
        return temp;
    }
    
    public LinkedList<String> getDataHistory(int transactionId, String rm){
        rm = rm.toLowerCase();
        return dataHistory.get(transactionId).get(rm);
    }
    
    public int sizeOperationHistory(int transactionId){
        return operationHistory.get(transactionId).size();
    }
    //handle client disconnects by implementing time-to-live mechanism
    
    //implement 2PC
    
    public void twoPCCommit(){
        try{
            twoPCLog = new File("../twoPCLog.txt");
            if(!twoPCLog.createNewFile()){
                System.out.println("Found twoPCLog.txt");
                FileReader fr = new FileReader(twoPCLog);
                BufferedReader fbr = new BufferedReader(fr);
                String line;
                String lastState = "none";
                while((line = fbr.readLine()) != null){
                    //line will contain states
                    System.out.println(line);
                    lastState = line;
                }
                fbr.close();
                return lastState;
            }else{
                //created new empty committedTrans.txt
                System.out.println("Created new twoPCLog.txt");
            }
        }catch (Exception e){
            //TODO: handle I/O errors
        }
    }
    public String getMiddlewareState(int xid){
        try{
            twoPCLog = new File("../twoPCLog.txt");
            if(!twoPCLog.createNewFile()){
                FileWriter fw = new FileWriter(twoPCLog, true);
                BufferedWriter br = new BufferedWriter(fw);
                
                br.write("beforeVote,0");
                br.newLine();
                br.close();
                return lastState;
            }else{
                //created new empty committedTrans.txt
                FileWriter fw = new FileWriter(twoPCLog, true);
                BufferedWriter br = new BufferedWriter(fw);
                
                br.write("sentVote,0");
                br.newLine();
                br.close();
                System.out.println("Created new twoPCLog.txt");
            }
        }catch (Exception e){
            //TODO: handle I/O errors
        }
        return "none";
    }
    
    public void sentVote(){
        FileWriter fw = new FileWriter(twoPCLog, true);
        BufferedWriter br = new BufferedWriter(fw);
        
        br.write("sentVote,0");
        br.newLine();
        br.close();
        return;
    }
}