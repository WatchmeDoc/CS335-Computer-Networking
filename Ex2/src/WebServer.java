import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class WebServer {
    private final int port;
    private final HashMap<Integer, ServerListNode> serverHashMap;
    private String host;
    private int nextServerID;
    private ServerSocket listenSocket;
    private int serverID;
    // Modify this to set the sleeptime frequency
    private static final int SLEEPTIME = 10000;


    public WebServer(int port, String host, int serverID) {
        this.port = port;
        this.host = host;
        this.listenSocket = null;
        this.serverID = serverID;
        this.serverHashMap = new HashMap<>();
        this.addToServerList(port, serverID, host, 0);
    }

    /***
     * Opens a listening socket to the configured port.
     * @throws IOException if an I/O exception occurs when opening the server socket.
     */
    public void open_listenSocket() throws IOException {
        this.listenSocket = new ServerSocket(port);

        System.out.println("Server ready at port: " + this.port + " with ip: " + this.host);
    }

    /***
     * Connect current server with the other Web Servers.
     * @throws IOException if clientSocket throws one.
     */
    public void connectToServer(String host, int port) throws IOException {
        Socket clientSocket = new Socket(host, port);
        DataOutputStream outToServer =
                new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new
                        InputStreamReader(clientSocket.getInputStream()));
        // JOIN <new server ID> <new server port> <new server host>
        String request = "JOIN " + this.serverID + " " + this.port + " " + this.host;
        outToServer.writeBytes(request + '\n');
        String response = inFromServer.readLine();
        if (response.equals("ACK")) {
            this.setNextServerID(Integer.parseInt(inFromServer.readLine()));
            this.updateServerList(inFromServer.readLine().split(","));
            System.out.println("Server list updated successfully. New list:");
            System.out.println(this.getServerListString());

        } else {
            System.out.println("Server insertion failed. Response from other server(s):");
            while (response != null) {
                System.out.println(response);
                response = inFromServer.readLine();
            }
        }


        clientSocket.close();
    }

    public void updateServerList(String[] list) {

        for (String s : list) {
            StringTokenizer serverNode = new StringTokenizer(s);
            int id = Integer.parseInt(serverNode.nextToken());
            int currPort = Integer.parseInt(serverNode.nextToken());
            String currHost = serverNode.nextToken();
            int nextServerID = Integer.parseInt(serverNode.nextToken());

            this.addToServerList(currPort, id, currHost, nextServerID);
        }
        System.out.println("List updated:");
        System.out.println(this.serverHashMap);
    }

    /***
     * Send a message to the next server in the grid, if such server exists.
     * @param msg The message to deliver to the next server.
     * @throws IOException If socket fails to close.
     */
    public void echoToNext(String msg) throws IOException {
        ServerListNode next = this.getServerWithID(this.nextServerID);
        if (next != null) { // essentially if there is no next server
            Socket clientSocket = new Socket(next.host, next.port);
            DataOutputStream outToServer =
                    new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer =
                    new BufferedReader(new
                            InputStreamReader(clientSocket.getInputStream()));
            outToServer.writeBytes(msg + "\r\n");

            String response = inFromServer.readLine();

            while (response != null){
                System.out.println("Other Server: " + response);
                response = inFromServer.readLine();
            }
            clientSocket.close();
        }

    }

    /***
     * Start running the server as configured.
     * @throws IOException if clientSocket throws one.
     */
    public void run() throws IOException {
        // Nurse to check the next server's health.
        Thread nurse = new Thread(new HealthWorker(SLEEPTIME));
        nurse.start();
        while (true) {
            // Standby and listen to your socket
            System.out.println("Server waiting for new requests.");
            Socket connectionSocket = listenSocket.accept();

            BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient =
                    new DataOutputStream(connectionSocket.getOutputStream());
            // Assign a worker (thread) to handle the request and keep on listening
            Thread worker = new Thread(new RequestHandler(this, connectionSocket, inFromClient, outToClient));
            worker.start();

        }
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    void setHost(String host) {
        this.host = host;
    }

    private ServerListNode getServerWithID(int id) {
        return this.serverHashMap.get(id);
    }

    /***
     * Getter for the adjacent serverID.
     * @return nextServerID if it exists, otherwise return this server's ID.
     */
    public int getNextServerID() {
        if (nextServerID > 0)
            return this.nextServerID;
        else
            return this.serverID;
    }

    void setNextServerID(int nextServerID) {
        this.nextServerID = nextServerID;
        this.serverHashMap.get(this.serverID).nextServerID = nextServerID;
    }

    public ServerSocket getListenSocket() {
        return listenSocket;
    }

    public int getServerID() {
        return serverID;
    }

    public void setServerID(int serverID) {
        this.serverID = serverID;
    }

    /***
     * Adds server to server hash map
     * @param port server's port
     * @param id server's ID
     * @param host server's host
     * @return true if there is no server with such ID, otherwise false
     */
    public boolean addToServerList(int port, int id, String host, int nextServerID) {
        if (this.serverHashMap.containsKey(id)) {
            return false;
        }
        this.serverHashMap.put(id, new ServerListNode(port, host, nextServerID));
        return true;
    }

    public void removeFromServerList(int id) {
        if (this.serverHashMap.containsKey(id)) {
            // 1. remove him from the map
            ServerListNode deadServer = this.serverHashMap.remove(id);
            if(id == this.nextServerID){ // if its the next server from our server, update the list.
                this.setNextServerID(deadServer.nextServerID);
            }
        }
    }

    public void updateHashMapNextId(int id, int nextServerID){
        this.serverHashMap.get(id).nextServerID = nextServerID;
    }
    /***
     * Get Server List as a string
     * @return a string containing all servers connected to the grid
     */
    public String getServerListString() {
        return this.serverHashMap.keySet().stream()
                .map(key -> key + " " + this.serverHashMap.get(key).toString())
                .collect(Collectors.joining(", "));
    }

    /***
     * Wrapper class that only serves as a packaging method
     */
    private static class ServerListNode {

        int port;
        String host;
        int nextServerID;

        ServerListNode(int port, String host, int nextServerID) {
            this.port = port;
            this.host = host;
            this.nextServerID = nextServerID;
        }

        @Override
        public String toString() {
            return this.port + " " + this.host + " " + this.nextServerID;
        }


    }

    /***
     * A health worker class which frequently checks if the next server is alive or not.
     */
    private class HealthWorker implements Runnable {
        private final int sleepTime;
        HealthWorker(){
            this.sleepTime = 5000;
        }
        HealthWorker(int sleepTime){
            this.sleepTime = sleepTime;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    // HEALTHCHECK
                    echoToNext("HEALTHCHECK");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    System.out.println("WHOOOO DARES TO WAKE ME UP?!");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("Next server is dead.");
                    int deadServerID = nextServerID;
                    removeFromServerList(nextServerID);
                    try {
                        // DEATH <deadServerID> <senderID> <sender's new next server>
                        echoToNext("DEATH " + deadServerID + " " + serverID + " " + nextServerID);
                    } catch (IOException ioException) {
                        System.out.println("Failed to close socket.");
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }
}

class RequestHandler implements Runnable {

    StringTokenizer tokenizedRequestLine;
    DataOutputStream outputStream;
    Socket socket;
    WebServer server;

    RequestHandler(WebServer server, Socket socket, BufferedReader fromClient, DataOutputStream toClient) throws IOException {
        String requestMessageLine = fromClient.readLine();
        this.tokenizedRequestLine = new StringTokenizer(requestMessageLine);
        this.outputStream = toClient;
        this.socket = socket;
        this.server = server;
    }

    /***
     * Main thread function. Handles the request provided to the constructor.
     */
    @Override
    public void run() {
        String request = this.tokenizedRequestLine.nextToken();
        System.out.println("Handling request " + request);

        // if there is a JOIN request from another server:
        switch (request) {
            case "JOIN": { // JOIN <new server ID> <new server port> <new server host>
                // 1. Get server info from the request.
                int id = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                int port = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                String host = this.tokenizedRequestLine.nextToken();
                String response = String.valueOf(this.server.getNextServerID());


                // 2. Try to add server to the HashMap. If ID is not unique then respond NAK.
                if (this.server.addToServerList(port, id, host, this.server.getNextServerID())) {
                    // 3. Respond ACK and tell him his adjacent server id, along with the server list.

                    // 4. Change this server's next server
                    this.server.setNextServerID(id);
                    String serverList = this.server.getServerListString();

                    try {
                        this.ACKNOWLEDGE(response + "\r\n" + serverList);
                        // 5. Inform other servers for the insertion of a new one
                        // UPDATE_LIST <sender ID> <server list>
                        this.server.echoToNext("UPDATE_LIST " + this.server.getServerID() + " " + serverList);
                    } catch (IOException e) {
                        System.out.println("Failed to close socket.");
                        e.printStackTrace();
                    }
                } else {
                    try {
                        this.NEGATIVE_ACK("Server ID already exists!");
                    } catch (IOException e) {
                        System.out.println("Failed to close socket.");
                        e.printStackTrace();
                    }
                }

                break;
            }
            case "UPDATE_LIST": { // UPDATE_LIST <sender ID> <server list>
                int senderID = Integer.parseInt(this.tokenizedRequestLine.nextToken());


                if (senderID != this.server.getServerID()) { // ... and our server didn't send the message

                    try {
                        this.ACKNOWLEDGE(null);
                        StringBuilder list = new StringBuilder();
                        while (this.tokenizedRequestLine.hasMoreTokens()) {
                            list.append(this.tokenizedRequestLine.nextToken()).append(" ");
                        }

                        this.server.updateServerList(list.toString().split(","));
                        this.server.echoToNext("UPDATE_LIST " + senderID + " " + list.toString());
                    } catch (IOException e) {
                        System.out.println("Failed to close socket.");
                        e.printStackTrace();
                    }
                }
                break;
            }
            case "HEALTHCHECK": // HEALTHCHECK
                try {
                    this.ACKNOWLEDGE(null);
                } catch (IOException e) {
                    System.out.println("Failed to close socket.");
                    e.printStackTrace();
                }

                break;
            case "DEATH": // DEATH <deadServerID> <senderID> <sender's new next server>
                int deadServerID = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                int senderID = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                int nextSenderServerID = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                if(senderID != this.server.getServerID()){
                    this.server.updateHashMapNextId(senderID, nextSenderServerID);
                    this.server.removeFromServerList(deadServerID);
                    System.out.println("New hashmap:");
                    System.out.println(this.server.getServerListString());

                    try {
                        this.ACKNOWLEDGE(null);
                        // DEATH <deadServerID> <senderID> <sender's new next server>
                        this.server.echoToNext("DEATH " + deadServerID + " " + senderID + " " + nextSenderServerID);
                    } catch (IOException e) {
                        System.out.println("Failed to close socket.");
                        e.printStackTrace();
                    }
                }

        }


    }

    /***
     * Send an ACK message to the output stream, along with a message.
     * @param s the message for the client. If s is null, it just sends an ACK.
     * @throws IOException if socket fails to close.
     */
    private void ACKNOWLEDGE(String s) throws IOException {
        this.outputStream.writeBytes("ACK\r\n");
        if (s != null) {
            this.outputStream.writeBytes(s + "\r\n");
        }

        socket.close();
    }

    /***
     * Send an NAK message to the output stream, along with a message.
     * @param s the message for the client. If s is null, it just sends an NAK.
     * @throws IOException if socket fails to close.
     */
    private void NEGATIVE_ACK(String s) throws IOException {
        this.outputStream.writeBytes("NAK\r\n");
        if (s != null) {
            this.outputStream.writeBytes(s + "\r\n");
        }
        socket.close();
    }
}