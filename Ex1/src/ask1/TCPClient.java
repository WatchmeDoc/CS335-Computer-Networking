package ask1;
/*
 * Simple example TCPClient
 *
 * @author K&R
 */

import java.io.*;
import java.net.*;

public class TCPClient {

    public static void main(String[] argv) throws Exception {
        String sentence;
        String response;

        //BufferedReader inFromUser =
        //        new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket("147.52.19.22", 4333);

        DataOutputStream outToServer =
                new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new
                        InputStreamReader(clientSocket.getInputStream()));

        sentence = "1111\n2222\n3333$";

        outToServer.writeBytes(sentence + '\n');

        response = inFromServer.readLine();
        while (response != null){
            System.out.println("FROM SERVER: " + response);
            response = inFromServer.readLine();
        }


        clientSocket.close();

    }
}
