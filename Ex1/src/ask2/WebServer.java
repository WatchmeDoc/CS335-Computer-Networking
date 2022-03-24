package ask2;
/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross,
 * All Rights Reserved.
 **/

import java.io.*;
import java.net.*;
import java.util.*;

class WebServer {

    public static void main(String argv[]) throws Exception {

        String requestMessageLine;
        String fileName;

        ServerSocket listenSocket = new ServerSocket(4333);

        while (true) {
            System.out.println("Web server ready at port: 4333");
            Socket connectionSocket = listenSocket.accept();
            BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient =
                    new DataOutputStream(connectionSocket.getOutputStream());


            requestMessageLine = inFromClient.readLine();

            StringTokenizer tokenizedLine =
                    new StringTokenizer(requestMessageLine);
            String request = tokenizedLine.nextToken();
            if (request.equals("GET")) {
                System.out.println("Serving a GET request.");
                fileName = tokenizedLine.nextToken();

                if (fileName.startsWith("/") == true)
                    fileName = fileName.substring(1);

                File file = new File(fileName);
                int numOfBytes = (int) file.length();

                FileInputStream inFile = new FileInputStream(fileName);

                byte[] fileInBytes = new byte[numOfBytes];
                inFile.read(fileInBytes);

                outToClient.writeBytes("HTTP/1.1 200 Document Follows\r\n");

                if (fileName.endsWith(".jpg"))
                    outToClient.writeBytes("Content-Type: image/jpeg\r\n");
                if (fileName.endsWith(".gif"))
                    outToClient.writeBytes("Content-Type: image/gif\r\n");

                outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
                outToClient.writeBytes("\r\n");

                outToClient.write(fileInBytes, 0, numOfBytes);

            } else if (request.equals("PUT")) {
                fileName = tokenizedLine.nextToken();
                if (fileName.startsWith("/") == true)
                    fileName = fileName.substring(1);
                String http_ver = tokenizedLine.nextToken(); // HTTP Ver
                requestMessageLine = inFromClient.readLine();

                tokenizedLine =
                        new StringTokenizer(requestMessageLine);
                tokenizedLine.nextToken(); // Content Type
                tokenizedLine.nextToken(); // Type
                requestMessageLine = inFromClient.readLine();

                tokenizedLine =
                        new StringTokenizer(requestMessageLine);
                tokenizedLine.nextToken(); // Content Length
                tokenizedLine.nextToken(); // Length
                requestMessageLine = inFromClient.readLine();
                FileWriter myWriter = new FileWriter(fileName);
                myWriter.write(requestMessageLine + " ");
                myWriter.close();
                outToClient.writeBytes(http_ver + "201 Created\n");
                outToClient.writeBytes("Content Location: " + fileName);
                outToClient.writeBytes("\r\n");
                System.out.println("Created file " + fileName + "with contents:");
                System.out.println(requestMessageLine);
            } else System.out.println("Bad Request Message");
            connectionSocket.close();
        }

    }
}
