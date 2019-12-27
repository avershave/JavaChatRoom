package app;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Server
 */
public class Server extends Thread {
    private final int serverPort;
    private ArrayList<ServerWork> workerList = new ArrayList<>();
    
    public Server(int serverPort){
        this.serverPort = serverPort;
    }

    public ArrayList<ServerWork> getWorkerList(){
        return workerList;
    }
    
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while(true){
                Socket clientSocket = serverSocket.accept();
                ServerWork worker = new ServerWork(this, clientSocket);
                workerList.add(worker);
                worker.start();
            }
            } catch(IOException e){
                e.printStackTrace();
        }
    }

	public void removeWorker(ServerWork serverWork) {
        workerList.remove(serverWork);
	}
}