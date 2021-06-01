import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Yahya Hassanzadeh on 20/09/2017.
 */

public class ConnectionToServer {
    public static final String DEFAULT_SERVER_ADDRESS = "localhost";
    public static final int DEFAULT_SERVER_PORT = 4444;
    private Socket s;
    protected DataInputStream is;
    protected DataOutputStream dos;
    private Socket fileSocket;
    protected DataInputStream fis;
    protected DataOutputStream fdos;
    private Protocol protocol;
    protected String serverAddress;
    private Authenticator authenticator;
    private byte phase;
    private byte type;
    protected int serverPort;

    /**
     * @param address IP address of the server, if you are running the server on the same computer as client, put the address as "localhost"
     * @param port    port number of the server
     */
    public ConnectionToServer(String address, int port) {
        protocol = new Protocol();
        serverAddress = address;
        serverPort = port;
        authenticator = new Authenticator();
    }

    /**
     * Establishes a socket connection to the server that is identified by the serverAddress and the serverPort
     */
    public void Connect() {
        try {
            s = new Socket(serverAddress, serverPort);
            is = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            dos = new DataOutputStream(s.getOutputStream());
            System.out.println("Successfully connected to " + serverAddress + " on port " + serverPort);
        } catch (IOException e) {
            System.err.println("Error: no server has been found on " + serverAddress + "/" + serverPort);
        }
    }
    public void connectFileSocket(String serverAddress, int serverPort){
        try {
            fileSocket = new Socket(serverAddress, serverPort);
            fileSocket.setSoTimeout(5 * 1000);
            fis = new DataInputStream(new BufferedInputStream(fileSocket.getInputStream()));
            fdos = new DataOutputStream(fileSocket.getOutputStream());
            System.out.println("Successfully connected to " + serverAddress + " on port " + serverPort);
        } catch (IOException e) {
            System.err.println("Error: no server has been found on " + serverAddress + "/" + serverPort);
        }
    }

    /**
     * sends the message String to the server and retrives the answer
     *
     * @param message input message string to the server
     * @return the received server answer
     */
    public String SendForAnswer(String message) {
        String response = new String();

        if (phase == 0){
            protocol.sendPayload((byte) 0, (byte) 0, message, dos);
            protocol.interpretPayload(is);
            phase = protocol.getPayloadPhase();
            type = protocol.getPayloadType();
            if (phase == 0 && type == 3) {
                String[] str = protocol.getPayloadMessage().split("/");
                authenticator.setToken(Integer.parseInt(str[0]));
                connectFileSocket(ConnectionToServer.DEFAULT_SERVER_ADDRESS, Integer.parseInt(str[1]));
                phase = 1;
                response = str[2];
            } else {
                response = protocol.getPayloadMessage();
            }
        }else if(phase == 1){
            String[] str = message.split("/");
            if (str[0].equalsIgnoreCase("apod")) {
                if (str.length<2)
                    protocol.sendPayload((byte) 1, (byte) 0, authenticator.getToken(), "current", dos);
                else
                    protocol.sendPayload((byte) 1, (byte) 0, authenticator.getToken(), str[1], dos);
                protocol.interpretPayload(is);
                response = protocol.getPayloadMessage();
                type = protocol.getPayloadType();
                if (type!=2){
                    protocol.interpretPayload(fis);
                    if (protocol.getFileHash() == protocol.getHashFromFile()){
                        protocol.sendPayload((byte) 1, (byte) 3, authenticator.getToken(), "Success", dos);
                    }else{
                        protocol.sendPayload((byte) 1, (byte) 2, authenticator.getToken(), "Failed", dos);
                    }
                    protocol.interpretPayload(is);
                    response = protocol.getPayloadMessage();
                }
            }else if (str[0].equalsIgnoreCase("insight")) {
                protocol.sendPayload((byte) 1, (byte) 1, authenticator.getToken(), "insight api query", dos);;
                protocol.interpretPayload(is);
                response = protocol.getPayloadMessage();
                type = protocol.getPayloadType();
                if (type!=2){
                    protocol.interpretPayload(fis);
                    if (protocol.getFileHash() == protocol.getHashFromFile()){
                        protocol.sendPayload((byte) 1, (byte) 3, authenticator.getToken(), "Success", dos);
                    }else{
                        protocol.sendPayload((byte) 1, (byte) 2, authenticator.getToken(), "Failed", dos);
                    }
                    protocol.interpretPayload(is);
                    response = protocol.getPayloadMessage();
                }
            }else if (str[0].equalsIgnoreCase("close")){
                protocol.sendPayload((byte) 1, (byte) 4, authenticator.getToken(), "close connection", dos);
                protocol.interpretPayload(is);
                response = protocol.getPayloadMessage();
            } else {
                response = "Failed: Illegal query";
            }
        }

        return response;
    }


    /**
     * Disconnects the socket and closes the buffers
     */
    public void Disconnect() {
        try {
            if (protocol.getPayloadPhase() == 1){
                fis.close();
                fdos.close();
                fileSocket.close();
            }
            is.close();
            dos.close();
            s.close();
            System.out.println("Connection Closed.");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
