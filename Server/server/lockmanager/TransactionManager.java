package server.lockmanager;

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

    //transaction counter for cleanliness when debugging/testing
    private int transactionCounter = 0;

    //keep track of which RM's are involved in a transaction
    private HashMap<Integer,LinkedList<String>> rmNeeded; //key-value, key being xid, values being linkedlist of needed RM's
    
    public TransactionManager(){
        transactionList = new LinkedList<TransactionObject>();
        transactionCounter = 0;
        rmNeeded = new HashMap<Integer, LinkedList<String>>();
        operationHistory = new HashMap<Integer, LinkedList<String>>();
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

    public void newOperation(int transactionId, String rmName, String command)
    {
        if(operationHistory == null || operationHistory.size() == 0){
            LinkedList<String> temp = new LinkedList<String>();
            temp.add(command);
            operationHistory.put(transactionId, temp);
        }else if(operationHistory.get(transactionId) == null){
            LinkedList<String> temp = new LinkedList<String>();
            temp.add(command);
            operationHistory.put(transactionId, temp);
        }else{
            operationHistory.get(transactionId).add(command);
        }
        
        LinkedList<String> foo;
        if(!(foo = rmNeeded.get(transactionId)).contains(rmName)){
            foo.add(rmName);
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
        String[] temp = new String[operationHistory.size()];
        for(int i=0; i < operationHistory.size(); i++){
            temp[i] = operationHistory.get(transactionId).get(i);
        }
        return temp;
    }
    //handle client disconnects by implementing time-to-live mechanism
}