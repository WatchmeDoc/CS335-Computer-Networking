package ask1;
/*
 * Simple example TCPServer
 *
 * @author K&R
 */
import java.io.*;
import java.net.*;
import java.util.Random;

public class TCPServer
{

    public static void main(String[] argv) throws Exception
    {
        String clientSentence;
        int port = 4333;
        Random rand = new Random();
        int r_index;
        String[] responses = {"I like ", "I don't like "};


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
            while (clientSentence != null){
                r_index = rand.nextInt(2);
                outToClient.writeBytes(responses[r_index] + clientSentence + "\n");
                if(clientSentence.endsWith("$"))
                    break;
                clientSentence = inFromClient.readLine();
            }
            connectionSocket.close();
        }
    }
}