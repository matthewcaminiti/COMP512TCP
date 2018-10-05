package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPTestClient
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public static String ANSI_GREEN = "\u001B[32m";
    public static String ANSI_RED = "\u001B[31m";
    public static String ANSI_RESET = "\u001B[0m";

    public void startConnection(String ip, int port) throws Exception
    {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String msg) throws Exception
    {
        out.println(msg);
        String resp = in.readLine();
        //String resp = "";
        //while((resp = in.readLine()) == "");
        return resp;
    }

    public void stopConnection() throws Exception
    {
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args) throws Exception
    {
        TCPTestClient client = new TCPTestClient();
        client.startConnection(args[0], Integer.parseInt(args[1]));

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        while(true){

            System.out.print((char)27 + "[32;1m\n>] " + (char)27 + "[0m");
            String input = stdin.readLine().trim();
            boolean f_Add = false, f_QueryPrice = false, f_QuerySeat = false;
            boolean r_Add = false, r_QueryPrice = false, r_QueryRoom = false;
            boolean c_Add = false, c_QueryPrice = false, c_QueryCar = false;
            boolean u_Add = false, u_Reserve = false, u_Bill = false;
            boolean bundle = false;

            int f_id = 1;
            int f_seats = 50;
            int f_price = 100;

            String r_loc = "italy";
            int r_num = 50;
            int r_price = 100;

            String c_loc = "italy";
            int c_num = 50;
            int c_price = 100;

            int u_id = 123;

            switch(input){
                case "FlightTest":
                    int xid = 1;
                    String resp = client.sendMessage("AddFlight," + xid + "," + f_id + "," + f_seats + "," + f_price);
                    if(resp.equals("Response: Adding a new flight [xid=" + xid + "]" + "aaa" + "-Flight Number: " + f_id + "aaa" + "-Flight Seats: " + f_seats + "aaa" + "-Flight Price: " + f_price + "aaa" + " Flight added")){
                        f_Add = true;
                        if(client.sendMessage("QueryFlight," + xid + "," + f_id).equals("Response: Number of seats available: " + f_seats)){
                            f_QuerySeat = true;
                        }
                        if(client.sendMessage("QueryFlightPrice," + xid + "," + f_id).equals("Response: Querying a flight price [xid=" + xid + "]" + "aaa" + "-Flight Number: " + f_id + "aaa" + "Price of a seat: " + f_price)){
                            f_QueryPrice = true;
                        }
                    }else{
                        f_QueryPrice = false;
                        f_QuerySeat = false;
                    }
                    String colorf_Add = f_Add ?ANSI_GREEN + f_Add + ANSI_RESET : ANSI_RED + f_Add + ANSI_RESET;
                    String colorf_Qs = f_QuerySeat ?ANSI_GREEN + f_QuerySeat + ANSI_RESET : ANSI_RED + f_QuerySeat + ANSI_RESET;
                    String colorf_Qp = f_QueryPrice ?ANSI_GREEN + f_QueryPrice + ANSI_RESET : ANSI_RED + f_QueryPrice + ANSI_RESET;
                    System.out.println("Fligh Test Results:\nAdding a Flight: " + colorf_Add + "\nQuerying number of Seats: " + colorf_Qs + "\nQuerying Price of Seats: " + colorf_Qp);
                    break;
                case "RoomTest":
                    xid = 1;
                    resp = client.sendMessage("AddRooms," + xid + "," + r_loc + "," + r_num + "," + r_price);
                    if(resp.equals("Response: Adding new rooms [xid=" + xid + "]" + "aaa" + "-Room Location: " + r_loc + "aaa" + "-Number of Rooms: " + r_num + "aaa" + "-Room Price: " + r_price + "aaa" + "Rooms added")){
                        r_Add = true;
                        if(client.sendMessage("QueryRooms, " + xid + "," + r_loc).equals("Response: Querying rooms location [xid=" + xid + "]" + "aaa" + "-Room Location: " + r_loc + "aaa" + "Number of rooms at this location: " + r_num)){
                            r_QueryRoom = true;
                        }
                        if(client.sendMessage("QueryRoomsPrice," + xid + "," + r_loc).equals("Response: Querying rooms price [xid=" + xid + "]" + "aaa" + "-Room Location: " + r_loc + "aaa" + "Price of rooms at this location: " + r_price)){
                            r_QueryPrice = true;
                        }
                    }
                    String colorr_Add = r_Add ?ANSI_GREEN + r_Add + ANSI_RESET : ANSI_RED + r_Add + ANSI_RESET;
                    String colorr_Qr = r_QueryRoom ?ANSI_GREEN + r_QueryRoom + ANSI_RESET : ANSI_RED + r_QueryRoom + ANSI_RESET;
                    String colorr_Qp = r_QueryPrice ?ANSI_GREEN + r_QueryPrice + ANSI_RESET : ANSI_RED + r_QueryPrice + ANSI_RESET;
                    System.out.println("Room Test Results:\nAdding a Room Location: " + colorr_Add + "\nQuerying number of Rooms: " + colorr_Qr + "\nQuerying Price of Rooms: " + colorr_Qp);
                    break;
                case "CarTest":
                    xid = 1;
                    resp = client.sendMessage("AddCars," + xid + "," + c_loc + "," + c_num + "," + c_price);
                    if(resp.equals("Response: Adding new cars [xid=" + xid + "]" + "aaa" + "-Car Location: " + c_loc  + "aaa" +  "-Number of Cars: " + c_num + "aaa" + "-Car Price: " + c_price + "aaa" + "Cars added")){
                        c_Add = true;
                        if(client.sendMessage("QueryCars, " + xid + "," + c_loc).equals("Response: Querying cars location [xid=" + xid + "]" + "aaa" + "-Car Location: " + c_loc + "aaa" + "Number of cars at this location: " + c_num)){
                            c_QueryCar = true;
                        }
                        if(client.sendMessage("QueryCarsPrice," + xid + "," + c_loc).equals("Response: Querying cars price [xid=" + xid + "]" + "aaa" + "-Car Location: " + c_loc + "aaa" + "Price of cars at this location: " + c_price)){
                            c_QueryPrice = true;
                        }
                    }
                    String colorc_Add = c_Add ?ANSI_GREEN + c_Add + ANSI_RESET : ANSI_RED + c_Add + ANSI_RESET;
                    String colorc_Qc = c_QueryCar ?ANSI_GREEN + c_QueryCar + ANSI_RESET : ANSI_RED + c_QueryCar + ANSI_RESET;
                    String colorc_Qp = c_QueryPrice ?ANSI_GREEN + c_QueryPrice + ANSI_RESET : ANSI_RED + c_QueryPrice + ANSI_RESET;
                    System.out.println("Car Test Results:\nAdding a Car Location: " + colorc_Add + "\nQuerying number of Cars: " + colorc_Qc + "\nQuerying Price of Cars: " + colorc_Qp);
                    break;
                case "CustomerTest":
                    xid = 1;
                    if(client.sendMessage("AddCustomerID," + xid + "," + u_id).equals("Response: Adding a new customer [xid=" + xid + "]" + "aaa" + "-Customer ID: " + u_id + "aaa" + "Add customer ID: " + u_id)){
                        u_Add = true;
                    }
                    if(client.sendMessage("ReserveRoom," + xid + "," + u_id + "," + r_loc).equals("Response: Reserving a room at a location [xid=" + xid + "]" + "aaa" + "-Customer ID: " + u_id + "aaa" + "-Room Location: " + r_loc + "aaa" + "Room Reserved")){
                        u_Reserve = true;
                    }
                    if(client.sendMessage("QueryCustomer," + xid + "," + u_id).equals("Response:aaaBill for customer:aaa1 room-" + r_loc + " $" + r_price + "aaaaaaaaa")){
                        u_Bill = true;
                    }
                    String coloru_Add = u_Add ?ANSI_GREEN + u_Add + ANSI_RESET : ANSI_RED + u_Add + ANSI_RESET;
                    String coloru_R = u_Reserve ?ANSI_GREEN + u_Reserve + ANSI_RESET : ANSI_RED + u_Reserve + ANSI_RESET;
                    String coloru_B = u_Bill ?ANSI_GREEN + u_Bill + ANSI_RESET : ANSI_RED + u_Bill + ANSI_RESET;
                    System.out.println("Customer Test Results:\nAdding a Customer: " + coloru_Add + "\nMaking a Reservation: " + coloru_R + "\nQuerying Customer: " + coloru_B);
                    break;
                case "BundleTest":
                    xid = 1;
                    if(client.sendMessage("Bundle," + xid + "," + u_id + "," + f_id + "," + r_loc + ",true,true").equals("Response: " + "Reserving car succeeded.aaa" + "Reserving room succeeded.aaa" + "Reserving flight succeeded.aaa")){
                        bundle = true;
                    }
                    String colorb_ = bundle ?ANSI_GREEN + bundle + ANSI_RESET : ANSI_RED + bundle + ANSI_RESET;
                    System.out.println("Bundle Test Results:\nAll Reservations succesful: " + colorb_);
                break;
            }
        }
    }
}