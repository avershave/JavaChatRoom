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
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

public class ServerWork extends Thread {

    private final Socket clientSocket;
    private final Server server;

    private OutputStream outputStream;
    private String login = null;
    private HashSet<String> topicSet = new HashSet<>();

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
                    } else if ("join".equalsIgnoreCase(cmd)){
                        handleJoin(token);
                    } else if ("leave".equalsIgnoreCase(cmd)){
                        handleLeave(token);
                    } else {
                        String msg = "Unknown " + cmd + "\n";
                        outputStream.write(msg.getBytes());
                    }
                }
            }
            clientSocket.close();
        }

    private void handleLeave(String[] token) throws IOException {
        if (login != null){
            if (token.length > 1){
                String topic = token[1];
                topicSet.remove(topic);
                System.out.println(login + " left " + topic + ".");
                ArrayList<ServerWork> workerList = server.getWorkerList();
                for (ServerWork worker : workerList){
                    String leaveMsg = login + " has left " + topic + ".\n";
                    worker.send(leaveMsg);
                }
            }
        } else {
            String errorMsg = "Please log in to use join.\n";
            outputStream.write(errorMsg.getBytes());
        }
    }

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] token) throws IOException {
        if (login != null){
            if (token.length > 1){
                String topic = token[1];
                topicSet.add(topic);
                System.out.println(topic + " has been created by " + login + ".");
                ArrayList<ServerWork> workerList = server.getWorkerList();
                for (ServerWork worker : workerList){
                    String joinMsg = login + " has joined " + topic + ".\n";
                    worker.send(joinMsg);
                }
            }
        } else {
            String errorMsg = "Please log in to use join.\n";
            outputStream.write(errorMsg.getBytes());
        }
	}

    private void handleMsg(String[] token) throws IOException {
        String sendTo = token[1];
        String msg = token[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        ArrayList<ServerWork> workerList = server.getWorkerList();
        for(ServerWork worker : workerList){
            if (isTopic){
                if (worker.isMemberOfTopic(sendTo)) {
                    String outMsg = sendTo + ": " + login + ": " + msg + "\n";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())){
                    String outMsg = "msg " + login + ": " + msg + "\n";
                    worker.send(outMsg);
                }
            }
        }
    }

    public void handleLogoff() throws IOException {
        server.removeWorker(this);
        ArrayList<ServerWork> workerList = server.getWorkerList();
        if(login != null){
            String offlineMsg = "User offline " + login + "\n";
            for(ServerWork worker : workerList){
                if(!login.equals(worker.getLogin())){
                    worker.send(offlineMsg);
                }
            }
        }
        clientSocket.close();
        System.out.println(login + " has logged off.");
    }

    public String getLogin(){
        return login;
    }
    
    private void handleLogin(OutputStream outputStream, String[] token) throws IOException {
            if (token.length == 3) {
            String login = token[1];
            String password = token[2];

            if((login.equals("guest") && password.equals("guest")) || (login.equals("austin") && password.equals("austin"))){
                String msg = "login success\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User has logged in successfully: " + login);
                
                ArrayList<ServerWork> workerList = server.getWorkerList();
                String onlineMsg = "User online " + login + "\n";
                for(ServerWork worker : workerList){
                    if(!login.equals(worker.getLogin())){
                        worker.send(onlineMsg);
                    }
                }

                for(ServerWork worker : workerList){
                    if(!login.equals(worker.getLogin())){
                        if(worker.getLogin() != null){
                            String currentMsg = "User online " + worker.getLogin() + "\n";
                            send(currentMsg);
                        }
                    }
                }
            } else {
                String msg = "login error\n";
                outputStream.write(msg.getBytes());
                System.out.println("User tried logging in but failed.");
            }
        }
    }

    private void send(String msg) throws IOException {
        if(login != null){
            outputStream.write(msg.getBytes());
        }
    }
}