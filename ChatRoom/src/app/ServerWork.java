package app;

/**
 * ServerWork
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class ServerWork extends Thread {

    private final Socket clientSocket;
    private final Server server;

    private OutputStream outputStream;
    private String login = null;

    public ServerWork(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run(){
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            this.outputStream = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ( ( line = reader.readLine()) != null) {
                String[] token = StringUtils.split(line);
                if (token != null && token.length > 0){
                String cmd = token[0];
                if (("quit".equalsIgnoreCase(cmd)) || "logoff".equalsIgnoreCase(cmd)){
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)){
                    handleLogin(outputStream, token);
                } else if ("msg".equalsIgnoreCase(cmd)){
                    String[] tokenMsg = StringUtils.split(line, null, 3);
                    handleMsg(tokenMsg);
                } else {
                    String msg = "Unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                    }
                }
            }
                clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMsg(String[] token) throws IOException {
        String sendTo = token[1];
        String msg = token[2];

        ArrayList<ServerWork> workerList = server.getWorkerList();
        for(ServerWork worker : workerList){
            if (sendTo.equalsIgnoreCase(worker.getLogin())){
                String outMsg = login + ": " + msg + "\n";
                worker.send(outMsg);
            }
        }
    }

    public void handleLogoff() throws IOException {
        server.removeWorker(this);
        ArrayList<ServerWork> workerList = server.getWorkerList();
        String offlineMsg = "User " + login + " is offline.\n";
        for(ServerWork worker : workerList){
            if(!login.equals(worker.getLogin())){
                worker.send(offlineMsg);
            }
        }
        clientSocket.close();
    }

    public String getLogin(){
        return login;
    }
    
    private void handleLogin(OutputStream outputStream, String[] token){
            if (token.length == 3) {
            String login = token[1];
            String password = token[2];

            if((login.equals("guest") && password.equals("guest")) || (login.equals("austin") && password.equals("austin"))){
                String msg = "login success\n";
                try {
                    outputStream.write(msg.getBytes());
                    this.login = login;
                    System.out.println("User has logged in successfully: " + login);
                    
                    ArrayList<ServerWork> workerList = server.getWorkerList();
                    String onlineMsg = "User " + login + " is online.\n";
                    for(ServerWork worker : workerList){
                        if(!login.equals(worker.getLogin())){
                            worker.send(onlineMsg);
                        }
                    }

                    for(ServerWork worker : workerList){
                        if(!login.equals(worker.getLogin())){
                            if(worker.getLogin() != null){
                                String currentMsg = "User online " + worker.getLogin() + ".\n";
                                send(currentMsg);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String msg = "login error\n";
                try {
                    outputStream.write(msg.getBytes());
                    System.out.println("User tried logging in but failed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void send(String msg) throws IOException {
        if(login != null){
            outputStream.write(msg.getBytes());
        }
    }
}