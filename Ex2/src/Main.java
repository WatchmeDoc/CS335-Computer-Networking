import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        int port = 0;
        String host = "localhost";
        int connecting_port = 0;
        String connecting_host = "localhost";
        int id = -1;
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
                case "-id": // This server's ID
                    i++;
                    id = Integer.parseInt(args[i]);
                    break;
                case "--connect": // Server who will add this one into the grid (optional only if the grid is new)
                    i++;
                    connecting_port = Integer.parseInt(args[i]);
                    i++;
                    connecting_host = args[i];
                    break;
                default:
                    System.out.println("Unknown argument provided: " + args[i]);
                    System.exit(0);
            }
        }
        if (id <= 0) {
            System.out.println("Invalid ID provided. Please a positive integer.");
            System.exit(1);
        } else if (port <= 0) {
            System.out.println("Invalid port provided.");
            System.exit(1);
        }
        WebServer server = new WebServer(port, host, id);
        // Try to create Listening Socket
        try {
            server.open_listenSocket();
        } catch (IOException e) {
            System.out.println("Failed to create server socket.");
            System.exit(2);
        }

        // If you have to connect to another server
        if (connecting_port != 0) {
            System.out.println("Connect to server with port: " + connecting_port + ".");
            try {
                server.connectToServer(connecting_host, connecting_port);
            } catch (IOException e) {
                System.out.println("Could not connect to server with ip: " + host + " and port: " + connecting_port);
                System.exit(3);
            }
        }
        // Run server and catch any exceptions.
        try {
            server.run();
        } catch (IOException e) {
            System.out.println("IO exception occurred while waiting into Listening Socket.");
            System.exit(4);
        }

    }
}
