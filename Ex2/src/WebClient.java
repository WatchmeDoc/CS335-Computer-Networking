import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WebClient {

    public static void main(String[] args) {
        int port = 0;
        String host = "localhost";
        String request = null;
        String filePath = null;
        // Handle command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p": // This server's Port
                    i++;
                    port = Integer.parseInt(args[i]);
                    break;
                case "-ip": // This server's IP (optional)
                    i++;
                    host = args[i];
                    break;
                case "--request":
                    i++;
                    request = args[i];
                    i++;
                    filePath = args[i];
                    break;
                default:
                    System.out.println("Unknown argument provided: " + args[i]);
                    System.exit(0);
                    break;
            }
        }
        // Simple error checking
        if (port <= 0) {
            System.out.println("Invalid port provided.");
            System.exit(1);
        } else if (request == null) {
            System.out.println("No request provided");
            System.exit(2);
        }
        
        try {
            Socket clientSocket = new Socket(host, port);
            DataOutputStream outToServer =
                    new DataOutputStream(clientSocket.getOutputStream());

            BufferedReader inFromServer =
                    new BufferedReader(new
                            InputStreamReader(clientSocket.getInputStream()));

            // Send proper request and close socket
            if (request.equals("PUT")) {
                // PUT <filename>
                put_request(filePath, inFromServer, outToServer);
            } else if (request.equals("GET")) {
                // GET <filename>
                get_request(filePath, inFromServer, outToServer);
            }

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Socket exception occurred:");
            e.printStackTrace();
        }

    }

    public static void put_request(String filePath, BufferedReader inFromServer, DataOutputStream outToServer) throws IOException {
        // Cut file name from provided path
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        String body = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        // Write file contents to the request
        outToServer.writeBytes("PUT " + fileName + "\r\n" + body + WebServer.REQUEST_SUFFIX + "\r\n");
        String response = inFromServer.readLine();
        // Read and print server's response
        System.out.println("FROM SERVER:");
        while (!response.equals(WebServer.REQUEST_SUFFIX)) {
            System.out.println(response);
            response = inFromServer.readLine();
        }
    }

    public static void get_request(String filePath, BufferedReader inFromServer, DataOutputStream outToServer) throws IOException {
        // Cut file name from provided path
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        outToServer.writeBytes("GET " + fileName + "\r\n" + WebServer.REQUEST_SUFFIX + "\r\n");
        // Read server response.
        String initResponse = inFromServer.readLine(); // Should be ACK or NAK
        System.out.println("FROM SERVER:");
        String response = inFromServer.readLine(); // Body
        StringBuilder wholeResponse = new StringBuilder();
        // Read the response and hold its contents:
        while (!response.equals(WebServer.REQUEST_SUFFIX)) {
            wholeResponse.append(response).append("\r\n");
            System.out.println(response);
            response = inFromServer.readLine();
        }
        if (initResponse.equals("ACK")) { // If response was ACK then write to file the received contents.
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(wholeResponse.toString());
            writer.close();
        }


    }
}
