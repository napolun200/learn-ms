//package com.gfm.io;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class BioEchoServer {
//
//    public static void main(String[] args) throws IOException {
//        ServerSocket socket = new ServerSocket(9999);
//        ExecutorService executorService = Executors.newFixedThreadPool(6);
//        while (true){
//            Socket clientSocket = socket.accept();
//            System.out.println("accepted connection from: "+clientSocket);
//            executorService.execute(()->{
//                try(BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))){
//                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(),true);
//                    while (true){
//                        writer.println(reader.readLine());
//                        writer.flush();
//                    }
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
//            });
//        }
//    }
//}
