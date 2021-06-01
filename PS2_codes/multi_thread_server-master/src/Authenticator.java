import java.io.File;
import java.io.FileNotFoundException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Scanner;

public class Authenticator {

    private String username;
    private boolean authorized;
    private boolean authenticated;
    private short wrongPassword;
    private SocketAddress address;
    private int token;


    public Authenticator() {
        username = "";
        wrongPassword = 0;
    }

    public void checkToken(int clientToken, SocketAddress localSocketAddress) {
        authenticated = clientToken == this.token && address.equals(localSocketAddress);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public short getWrongPassword() {
        return wrongPassword;
    }

    public void authenticate(String input, SocketAddress localSocketAddress){
        if (checkPassword(input)){
            generateToken();
            address = localSocketAddress;
            authorized = true;
        }else{
            wrongPassword++;
        }
    }

    public boolean checkUsername(String input){
        try {
            File myObj = new File("/Users/koray/Downloads/NetworkProject/PS2_codes/multi_thread_server-master/Database/users.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] usernamepass = data.split(";");
                if (usernamepass[0].equals(input)){
                    username = input;
                    return true;
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkPassword(String input){
        try {
            File myObj = new File("/Users/koray/Downloads/NetworkProject/PS2_codes/multi_thread_server-master/Database/users.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] usernamepass = data.split(";");
                if (usernamepass[0].equals(username))
                    if (usernamepass[1].equals(input))
                        return true;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return false;
    }

    private void generateToken(){
        token = (username+"87").hashCode();
    }

    public int getToken() {
        return token;
    }

}
