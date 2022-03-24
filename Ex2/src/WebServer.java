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


    public WebServer(int port, String host, int serverID) {
        this.port = port;
        this.host = host;
        this.listenSocket = null;
        this.serverID = serverID;
        this.serverHashMap = new HashMap<>();
        this.addToServerList(port, serverID, host);
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
        String request = "JOIN " + this.serverID + " " + this.port + " " + this.host;
        outToServer.writeBytes(request + '\n');
        String response = inFromServer.readLine();
        System.out.println("FROM SERVER:");
        while (response != null) {
            System.out.println(response);
            response = inFromServer.readLine();
        }
        // TODO add server list
        clientSocket.close();
    }

    /***
     * Start running the server as configured.
     * @throws IOException if clientSocket throws one.
     */
    public void run() throws IOException {
        // Standby and listen to your socket
        while (true) {
            System.out.println("Server waiting for new requests.");
            Socket connectionSocket;
            connectionSocket = listenSocket.accept();

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
    public boolean addToServerList(int port, int id, String host) {
        if (this.serverHashMap.containsKey(id)) {
            return false;
        }
        this.serverHashMap.put(id, new ServerListNode(port, host));
        return true;
    }

    /***
     * Get Server List as a string
     * @return a string containing all servers connected to the grid
     */
    public String getServerListString() {
        return this.serverHashMap.keySet().stream()
                .map(key -> key + "=" + this.serverHashMap.get(key).toString())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    /***
     * Wrapper class that only serves as a packaging method
     */
    private class ServerListNode {
        int port;
        String host;

        ServerListNode(int port, String host) {
            this.port = port;
            this.host = host;
        }
        @Override
        public String toString(){
            return this.port + " " + this.host;
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

    @Override
    public void run() {
        String request = this.tokenizedRequestLine.nextToken();
        System.out.println("Handling request " + request);

        // if there is a JOIN request from another server:
        if (request.equals("JOIN")) {
            // 1. Get server info from the request.
            int id = Integer.parseInt(this.tokenizedRequestLine.nextToken());
            int port = Integer.parseInt(this.tokenizedRequestLine.nextToken());
            String host = this.tokenizedRequestLine.nextToken();
            String serverList = this.server.getServerListString();
            // 2. Try to add server to the HashMap. If ID is not unique then respond NAK.
            if (this.server.addToServerList(port, id, host)) {
                // 3. Respond ACK and tell him his adjacent server id, along with the server list.

                String response;
                response = String.valueOf(this.server.getNextServerID());
                try {
                    this.ACKNOWLEDGE(response + " " + serverList);

                } catch (IOException e) {
                    System.out.println("Failed to close socket.");
                    e.printStackTrace();
                }
                // 4. Change this server's next server
                this.server.setNextServerID(id);

            } else {
                try {
                    this.NEGATIVE_ACK("Server ID already exists!");
                } catch (IOException e) {
                    System.out.println("Failed to close socket.");
                    e.printStackTrace();
                }
            }

        }


    }

    private void ACKNOWLEDGE(String s) throws IOException {
        this.outputStream.writeBytes("ACK\r\n");
        this.outputStream.writeBytes(s + "\r\n");
        socket.close();
    }

    private void NEGATIVE_ACK(String s) throws IOException {
        this.outputStream.writeBytes("NAK\r\n");
        this.outputStream.writeBytes(s + "\r\n");
        socket.close();
    }
}