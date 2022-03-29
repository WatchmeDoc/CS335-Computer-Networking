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
    // Modify this to set the sleeptime frequency
    private static final int SLEEPTIME = 20000;
    public static String REQUEST_SUFFIX = "$$";
    private final int port;
    private final HashMap<Integer, ServerListNode> serverHashMap;
    private final HashMap<String, String> files;
    private final String host;
    private final int serverID;
    private int nextServerID;
    private ServerSocket listenSocket;
    private int requestID;

    public WebServer(int port, String host, int serverID) {
        this.port = port;
        this.host = host;
        this.listenSocket = null;
        this.serverID = serverID;
        this.serverHashMap = new HashMap<>();
        this.addToServerList(port, serverID, host, 0);
        this.files = new HashMap<>();
        this.requestID = 1;
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
        this.sendRequest(request, outToServer);
        String response = inFromServer.readLine();
        if (response.equals("ACK")) {
            this.setNextServerID(Integer.parseInt(inFromServer.readLine()));
            this.updateServerList(inFromServer.readLine().split(","));
            System.out.println("Server list updated successfully. New list:");
            System.out.println(this.getServerListString());

        } else {
            System.out.println("Server insertion failed. Response from other server(s):");
            while (!response.equals(REQUEST_SUFFIX)) {
                System.out.println(response);
                response = inFromServer.readLine();
            }
        }


        clientSocket.close();
    }

    /***
     * Send a request to the output stream including the request SUFFIX.
     * @param request The request to be delivered to the output stream
     * @param output The output stream
     * @throws IOException If I/O exception occurs.
     */
    public void sendRequest(String request, DataOutputStream output) throws IOException {
        output.writeBytes(request + "\r\n" + REQUEST_SUFFIX + "\r\n");
    }


    public void updateServerList(String[] list) {
        this.serverHashMap.clear();
        for (String s : list) {
            StringTokenizer serverNode = new StringTokenizer(s);
            int id = Integer.parseInt(serverNode.nextToken());
            int currPort = Integer.parseInt(serverNode.nextToken());
            String currHost = serverNode.nextToken();
            int nextServerID = Integer.parseInt(serverNode.nextToken());

            this.addToServerList(currPort, id, currHost, nextServerID);
        }
    }

    /***
     * Send a message to the next server in the grid, if such server exists.
     * @param msg The message to deliver to the next server.
     * @throws IOException If socket fails to close.
     */
    public String echoToNext(String msg) throws IOException {
        ServerListNode next = this.getServerWithID(this.nextServerID);
        if (next != null) { // essentially if there is no next server
            Socket clientSocket = new Socket(next.host, next.port);
            DataOutputStream outToServer =
                    new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer =
                    new BufferedReader(new
                            InputStreamReader(clientSocket.getInputStream()));
            this.sendRequest(msg, outToServer);

            StringBuilder response = new StringBuilder();
            String line = inFromServer.readLine();

            while (!line.equals(REQUEST_SUFFIX)) {
                response.append(line).append('\n');
                line = inFromServer.readLine();
            }
            clientSocket.close();
            return response.toString();
        }
        return null;
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
            System.out.println("Server waiting for request no." + this.requestID);
            Socket connectionSocket = listenSocket.accept();
            this.requestID += 1;
            BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient =
                    new DataOutputStream(connectionSocket.getOutputStream());
            // Assign a worker (thread) to handle the request and keep on listening
            Thread worker = new Thread(new RequestHandler(this, connectionSocket, inFromClient, outToClient, this.requestID));
            worker.start();


        }
    }

    /***
     * Getter for the server hashmap
     * @param id a key value for the server hashmap
     * @return ServerListNode with serverID == id
     */
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

    /***
     * Change next serverID while updating the hashmap as well
     * @param nextServerID new adjacent server's ID.
     */
    void setNextServerID(int nextServerID) {
        this.nextServerID = nextServerID;
        this.serverHashMap.get(this.serverID).nextServerID = nextServerID;
    }

    /***
     * Getter for server's ID parameter.
     * @return server ID.
     */
    public int getServerID() {
        return serverID;
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

    /***
     * Remove a server from the server list. Used when someone detects a dead server.
     * @param id The dead server's ID
     */
    public void removeFromServerList(int id) {
        if (this.serverHashMap.containsKey(id)) {
            // 1. remove him from the map
            ServerListNode deadServer = this.serverHashMap.remove(id);
            if (id == this.nextServerID) { // if its the next server from our server, update the list.
                this.setNextServerID(deadServer.nextServerID);
            }
        }
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
     * Add file to the file hashmap.
     * @param key the file's name.
     * @param value the file's contents.
     */
    public void addFile(String key, String value) {
        this.files.put(key, value);
    }

    /***
     * Check if the file hashmap contains a specific file.
     * @param key the file's name
     * @return true if found, else false.
     */
    public boolean containsFile(String key) {
        return this.files.containsKey(key);
    }

    /***
     * Get file from file hashmap
     * @param key file's name
     * @return the file with the matching filename. If file doesn't exist, returns null.
     */
    public String getFile(String key) {
        return this.files.get(key);
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

        HealthWorker(int sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(sleepTime);
                    // HEALTHCHECK
                    System.out.println("\nDoctor: I'll check on the next server.");
                    String response = echoToNext("HEALTHCHECK");
                    System.out.println("Doctor: Server responded \"" + response.trim() + "\"\n");
                } catch (InterruptedException e) {
                    System.out.println("WHOOOO DARES TO WAKE ME UP?!");
                    e.printStackTrace();
                } catch (IOException e) { // If the adjacent server is dead.
                    System.out.println("\nDoctor: Next server is dead.\n");
                    removeFromServerList(nextServerID);
                    try {
                        // UPDATE_LIST <sender ID> <server list>
                        echoToNext("UPDATE_LIST " + serverID + " " + getServerListString());
                    } catch (IOException ioException) {
                        System.out.println("Doctor: Failed to close socket.");
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }
}

class RequestHandler implements Runnable {

    StringTokenizer tokenizedRequestLine;
    BufferedReader fromClient;
    DataOutputStream outputStream;
    Socket socket;
    WebServer server;
    int requestID;

    RequestHandler(WebServer server, Socket socket, BufferedReader fromClient, DataOutputStream toClient, int id) throws IOException {
        String requestMessageLine = fromClient.readLine();
        this.fromClient = fromClient;
        this.tokenizedRequestLine = new StringTokenizer(requestMessageLine);
        this.outputStream = toClient;
        this.socket = socket;
        this.server = server;
        this.requestID = id;
    }

    /***
     * Main thread function. Handles the request provided to the constructor.
     */
    @Override
    public void run() {
        String request = this.tokenizedRequestLine.nextToken();
        System.out.println(this.requestID + ": Handling request " + request);

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
                        System.out.println(this.requestID + ": Failed to close socket.");
                        e.printStackTrace();
                    }
                } else {
                    try {
                        this.NEGATIVE_ACK("Server ID already exists!");
                    } catch (IOException e) {
                        System.out.println(this.requestID + ": Failed to close socket.");
                        e.printStackTrace();
                    }
                }

                break;
            }
            case "UPDATE_LIST": { // UPDATE_LIST <sender ID> <server list>
                int senderID = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                if (senderID != this.server.getServerID()) { // ... and our server didn't send the message

                    try {

                        StringBuilder list = new StringBuilder();
                        while (this.tokenizedRequestLine.hasMoreTokens()) { // read list
                            list.append(this.tokenizedRequestLine.nextToken()).append(" ");
                        }
                        // update our server list
                        this.server.updateServerList(list.toString().split(","));
                        // let the other servers know
                        this.server.echoToNext("UPDATE_LIST " + senderID + " " + list.toString());
                    } catch (IOException e) {
                        System.out.println(this.requestID + ": Failed to close socket.");
                        e.printStackTrace();
                    }
                }
                try {
                    this.ACKNOWLEDGE(null);
                } catch (IOException e) {
                    System.out.println(this.requestID + ": Failed to close socket.");
                    e.printStackTrace();
                }
                break;
            }
            case "PUT": { // PUT <filename>\n<TEXT>$$ (client request)
                String filename = this.tokenizedRequestLine.nextToken();
                try {
                    // Save file and let the others know
                    String text = this.saveFile(filename);
                    this.server.echoToNext("UPDATE_FILES " + this.server.getServerID() + " " + filename + "\n" + text);
                    this.ACKNOWLEDGE(null);
                } catch (IOException e) {
                    System.out.println(this.requestID + ": Client connection closed before parsing the whole message.");
                    e.printStackTrace();
                    try {
                        this.NEGATIVE_ACK(null);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                break;
            }
            case "GET": { // GET <filename>
                String filename = this.tokenizedRequestLine.nextToken();
                int senderID = -1;
                if (this.tokenizedRequestLine.hasMoreTokens()) {
                    senderID = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                }
                System.out.println(this.requestID + ": Searching for file " + filename);
                if (senderID != this.server.getServerID()) { // If our server didn't send the request
                    if (senderID == -1) { // If client sent the request, the search starts from our server.
                        senderID = this.server.getServerID();
                    }
                    if (this.server.containsFile(filename)) { // if we found the file
                        try {
                            System.out.println(this.requestID + ": File found on this server!");
                            this.ACKNOWLEDGE(this.server.getFile(filename));
                        } catch (IOException e) {
                            System.out.println(this.requestID + ": Failed to send message to client.");
                            e.printStackTrace();
                        }
                    } else { // if we didn't find the file on our server, ask the other guys.
                        try {
                            System.out.println(this.requestID + ": File not found on this server! I'll look on the next one.");
                            this.server.sendRequest(this.server.echoToNext("GET " + filename + " " + senderID).trim(), outputStream);
                        } catch (IOException e) {
                            System.out.println(this.requestID + ": Could not connect with the next server.");
                            e.printStackTrace();
                        }
                    }
                } else { // if no one found the file, tell everyone that the file doesn't exist on the grid.
                    try {
                        System.out.println(this.requestID + ": File not found on the grid.");
                        this.NEGATIVE_ACK("File not found.");
                    } catch (IOException e) {
                        System.out.println(this.requestID + ": Failed to close socket.");
                        e.printStackTrace();
                    }
                }
                break;
            }
            case "UPDATE_FILES": { // UPDATE_FILES <sender id> <filename>\n<text>&&
                int senderId = Integer.parseInt(this.tokenizedRequestLine.nextToken());
                String filename = this.tokenizedRequestLine.nextToken();
                System.out.println(this.requestID + ": Sender ID: " + senderId);
                try {
                    if (senderId != this.server.getServerID()) {
                        // If our server didn't send this request, update the saved file's contents and inform the others
                        String text = this.saveFile(filename);
                        this.server.echoToNext("UPDATE_FILES " + senderId + " " + filename + "\n" + text);
                    }
                    this.ACKNOWLEDGE(null);
                } catch (IOException e) {
                    System.out.println(this.requestID + ": Client connection closed before parsing the whole message.");
                    e.printStackTrace();
                    try {
                        this.NEGATIVE_ACK(null);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                break;

            }
            case "HEALTHCHECK": { // HEALTHCHECK - do nothing but reply ACK.
                try {
                    this.ACKNOWLEDGE(null);
                } catch (IOException e) {
                    System.out.println(this.requestID + ": Failed to close socket.");
                    e.printStackTrace();
                }

                break;
            }
            default:
                try {
                    this.NEGATIVE_ACK("Unknown request.");
                } catch (IOException e) {
                    System.out.println(this.requestID + ": Failed to close socket.");
                    e.printStackTrace();
                }

        }
        System.out.println("Request #" + this.requestID + " finished successfully.");

    }

    /***
     * Send an ACK message to the output stream, along with a message.
     * @param s the message for the client. If s is null, it just sends an ACK.
     * @throws IOException if socket fails to close.
     */
    private void ACKNOWLEDGE(String s) throws IOException {
        this.outputStream.writeBytes("ACK\r\n");
        if (s != null) {
            System.out.println("ACK " + s);
            this.outputStream.writeBytes(s + "\r\n");
        }
        this.outputStream.writeBytes(WebServer.REQUEST_SUFFIX + "\r\n");
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
            System.out.println("NAK " + s);
            this.outputStream.writeBytes(s + "\r\n");
        }
        this.outputStream.writeBytes(WebServer.REQUEST_SUFFIX + "\r\n");
        socket.close();
    }

    /***
     * Read client text and save file to the server hashmap.
     * @param filename file's name
     * @return input text from client
     * @throws IOException if client socket closes.
     */
    private String saveFile(String filename) throws IOException {
        StringBuilder text = new StringBuilder();
        String line = this.fromClient.readLine();
        while (!line.equals(WebServer.REQUEST_SUFFIX)) {
            text.append(line).append("\n");
            line = this.fromClient.readLine();
        }
        this.server.addFile(filename, text.toString());
        System.out.println("Saving file " + filename + " with contents:");
        System.out.println(text);
        return text.toString();

    }
}