
/**
 * ServerWork
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ServerWork extends Thread {

    private final Socket clientSocket;

    public ServerWork(Socket clientSocket) {
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
            OutputStream outputStream = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ( ( line = reader.readLine()) != null) {
                if ("quit".equalsIgnoreCase(line)){
                    break;
                }
                String msg = "You typed: " + line + "\n";
                outputStream.write(msg.getBytes());
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}