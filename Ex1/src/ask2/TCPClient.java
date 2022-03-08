package ask2;
/*
 * Simple example TCPClient
 *
 * @author K&R
 */

import java.io.*;
import java.net.*;

public class TCPClient {

    public static void main(String[] argv) throws Exception {
        String get_request;
        String put_request;
        String response;
        String host = "147.52.19.22";

        //BufferedReader inFromUser =
        //        new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket(host, 4333);

        DataOutputStream outToServer =
                new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new
                        InputStreamReader(clientSocket.getInputStream()));

        get_request = "GET /ask2/index.html HTTP/1.1\nHost: "+ host + "\nAccept-Language: en-us";
        put_request = "PUT /new.html HTTP/1.1\nContent-type: text/html\nContent-length: 46\n<html><p>The flame is gone<br>the fire remains.</p></html>";

        outToServer.writeBytes(put_request + '\n');

        response = inFromServer.readLine();
        System.out.println("FROM SERVER:");
        while (response != null){
            System.out.println(response);
            response = inFromServer.readLine();
        }

        clientSocket.close();

    }
}
