import java.net.SocketException;
import java.util.Scanner;

public class Main
{
    public static void main(String args[]){
        ConnectionToServer connectionToServer = new ConnectionToServer(ConnectionToServer.DEFAULT_SERVER_ADDRESS, ConnectionToServer.DEFAULT_SERVER_PORT);
        connectionToServer.Connect();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username please");
        String message = scanner.nextLine();
        while (!message.equals("QUIT"))
        {
            String res = connectionToServer.SendForAnswer(message);
            System.out.println(res);
            if ("Success: Closing Connection...".equals(res))
                break;
            if ("Failed: You've entered the wrong password too many times".equals(res))
                break;
            message = scanner.nextLine();
        }
        connectionToServer.Disconnect();
    }
}
