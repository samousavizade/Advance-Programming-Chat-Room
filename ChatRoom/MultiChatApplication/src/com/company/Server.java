package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;


public class Server extends Thread
{
    private final int port;
    private HashSet<ServerWorker> workers = new HashSet<>();

    public Server(int port)
    {
        this.port = port;
    }

    @Override
    public void run()
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true)
            {

                System.out.println("About To Accept ClientAPI Connection ... ");
                Socket clientSocket = serverSocket.accept();
                System.out.println("ClientAPI " + clientSocket + "Accepted !");

                ServerWorker worker = new ServerWorker(this, clientSocket);
                workers.add(worker);
                worker.start();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public ServerWorker findUserByName(String name)
    {
        for (ServerWorker worker : workers)
            if (worker.getUserName().equalsIgnoreCase(name))
                return worker;

        return null;
    }


    public HashSet<ServerWorker> getWorkers()
    {
        return workers;
    }
}
