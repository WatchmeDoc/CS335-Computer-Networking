import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class WebClient {

    // TODO: REFACTOR THIS FUNCTION AND MAKE CLIENT BETTER
    public static void main(String[] args) throws Exception{
        String put_request;
        String response;
        String host = "localhost";

        //BufferedReader inFromUser =
        //        new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket(host, 4333);

        DataOutputStream outToServer =
                new DataOutputStream(clientSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new
                        InputStreamReader(clientSocket.getInputStream()));

        put_request = "PUT pasok.txt\nRETURN FROM WHENCE THOU CAME'ST\nFor that is thy place of belonging.";
        String get_request = "GET pasok.txt";
        outToServer.writeBytes(get_request + "\r\n" + WebServer.REQUEST_SUFFIX + "\r\n");

        response = inFromServer.readLine();
        System.out.println("FROM SERVER:");
        while (!response.equals(WebServer.REQUEST_SUFFIX)){
            System.out.println(response);
            response = inFromServer.readLine();
        }

        clientSocket.close();
    }
}
