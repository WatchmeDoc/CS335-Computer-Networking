package ask1;
/*
 * Simple example TCPServer
 *
 * @author K&R
 */
import java.io.*;
import java.net.*;

public class TCPServer
{

    public static void main(String[] argv) throws Exception
    {
        String clientSentence;
        String capitalizedSentence;
        int port = 4333;

        ServerSocket welcomeSocket = new ServerSocket(port);

        while (true)
        {
            System.out.println("Server ready on "+port);

            Socket connectionSocket = welcomeSocket.accept();

            BufferedReader inFromClient =
                    new BufferedReader(
                            new InputStreamReader(connectionSocket.getInputStream()));

            DataOutputStream outToClient =
                    new DataOutputStream(connectionSocket.getOutputStream());

            clientSentence = inFromClient.readLine();

            capitalizedSentence = clientSentence.toUpperCase() + '\n';

            outToClient.writeBytes(capitalizedSentence);
        }
    }
}