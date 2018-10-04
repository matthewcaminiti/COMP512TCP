package server.tcp;



import java.io.*;
import java.net.*;

public class TCPServer
{
    private ServerSocket serverSocket;
    
    public void start(int port) throws Exception
    {
        serverSocket = new ServerSocket(port);
        while(true){
            new TCPServerHandler(serverSocket.accept()).start();
        }
    }
    
    public void stop() throws Exception
    {
        serverSocket.close();
    }
    
    private static class TCPServerHandler extends Thread
    {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        
        public TCPServerHandler(Socket socket)
        {
            this.clientSocket = socket;
        }
        
        public void run()
        {
            try{
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                
                String inputLine;
                while((inputLine = in.readLine()) != null){
                    if(".".equals(inputLine)){
                        out.println("bye");
                        break;
                    }
                    out.println(inputLine);
                }
                
                
                in.close();
                out.close();
                clientSocket.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) throws Exception
    {
        TCPServer server = new TCPServer();
        server.start(5555);
    }
    
}