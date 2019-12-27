package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

/**
 * ChatClient
 */
public class ChatClient {
    private final String serverName;
    private final int serverPort;
    private Socket socket;
    private InputStream serverIn;
    private OutputStream serverOut;
    private BufferedReader bufferedIn;

    private ArrayList<UserStatusListener> userStatusListener = new ArrayList<>();
    private ArrayList<MessageListener> messageListener = new ArrayList<>();

    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 8818);
        client.addUserStatusListener(new UserStatusListener(){
            @Override
            public void online(String login) {
                System.out.println("ONLINE: " + login);
            }

            @Override
            public void offline(String login) {
                System.out.println("OFFLINE: " + login);
            }
        });

        client.addMessageListener(new MessageListener(){
            @Override
            public void onMessage(String fromLogin, String msg) {
                System.out.println("You got a message from: " + fromLogin + ": " + msg);
            }
        });

        if(!client.connect()){
            System.err.println("Connection failed.");
        } else {
            System.out.println("Connection success!");
            if (client.login("guest", "guest")) {
                System.out.println("Login successful");

                client.msg("austin", "Hello World!");
            } else {
                System.err.println("Login error");
            }
        }
        //client.logoff();
    }

    private void msg(String sendTo, String msg) throws IOException {
        String cmd = "msg " + sendTo + " " + msg + "\n";
        serverOut.write(msg.getBytes());
    }

    private boolean login(String login, String password) throws IOException {
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());

        String response = bufferedIn.readLine();
        System.out.println(response);
        if ("login success".equalsIgnoreCase(response)){
            startMessageReader();
            return true;
        } else {
            return false;
        }
    }

    private void logoff() throws IOException {
        String cmd = "logoff\n";
        serverOut.write(cmd.getBytes());
    }

    private void startMessageReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    readMessageLoop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    protected void readMessageLoop() throws IOException {
        String line;
        while ((line = bufferedIn.readLine()) != null){
            String[] token = StringUtils.split(line);
            if (token !=null && token.length > 0){
                String cmd = token[1];
                if ("online".equalsIgnoreCase(cmd)){
                    handleOnline(token);
                } else if ("offline".equalsIgnoreCase(cmd)){
                    handleOffline(token);
                } else if ("msg".equalsIgnoreCase(token[0])){
                    String[] tokenMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokenMsg);
                }
            }
        }
    }

    private void handleMessage(String[] token) {
        String login = token[1].replaceAll(":", "");
        String msg = token[2];

        for(MessageListener listener : messageListener){
            listener.onMessage(login, msg);
        }
    }

    private void handleOffline(String[] token) {
        String login = token[2];
        for (UserStatusListener listener : userStatusListener){
            listener.offline(login);
        }
    }

    private void handleOnline(String[] token) {
        String login = token[2];
        for (UserStatusListener listener : userStatusListener){
            listener.online(login);
        }
    }

    private boolean connect() {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListener.add(listener);
    }
    
    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListener.remove(listener);
    }

    public void addMessageListener(MessageListener listener){
        messageListener.add(listener);
    }

    public void removeMessageListener(MessageListener listener){
        messageListener.remove(listener);
    }
}