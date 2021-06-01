import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ServerThread extends Thread {
    protected DataInputStream is;
    public DataOutputStream dos;
    protected DataInputStream fis;
    public DataOutputStream fdos;
    protected Socket s;
    private String line = new String();
    private String lines = new String();
    private Protocol protocol;
    private Authenticator authenticator;
    private ServerSocket fileServerSocket;
    private Socket fileSocket;
    private static final byte APOD = 0;
    private static final byte Insight = 1;
    private static final byte Fail = 2;
    private static final byte Success = 3;
    private static final byte Close = 4;
    private static final byte Command_Socket = 0;
    private static final byte File_socket = 1;

    private boolean run;

    /**
     * Creates a server thread on the input socket
     *
     * @param s input socket to create a thread on
     */
    public ServerThread(Socket s, ServerSocket fileServerSocket) {
        this.fileServerSocket = fileServerSocket;
        this.run = true;
        this.s = s;
        this.protocol = new Protocol();
        this.authenticator = new Authenticator();
    }

    /**
     * The server thread, echos the client until it receives the QUIT string from the client
     */
    public void run() {
        try {
            is = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            dos = new DataOutputStream(s.getOutputStream());
            s.setSoTimeout(15*1000);
        } catch (IOException e) {
            System.err.println("Server Thread. Run. IO error in server thread");
        }

        try {

            while(run) {
                protocol.interpretPayload(is,s.getRemoteSocketAddress(),authenticator);
                byte phase = protocol.getPayloadPhase();
                byte type = protocol.getPayloadType();
                String message = protocol.getPayloadMessage();

                if (phase == 0) {
                    if (type == 0) {
                        if (authenticator.getUsername().equals("")) {
                            if (authenticator.checkUsername(message))
                                protocol.sendPayload((byte) 0, (byte) 1, "Please enter your password ", dos);
                            else
                                protocol.sendPayload((byte) 0, (byte) 2, "Failed: Username not found", dos);
                        } else if (!authenticator.isAuthorized()) {
                            if (authenticator.getWrongPassword() < 3) {
                                authenticator.authenticate(message,s.getRemoteSocketAddress());
                                if (authenticator.isAuthorized()){
                                    protocol.sendPayload((byte) 0, (byte) 3, authenticator.getToken() + "/" + Server.DEFAULT_FILE_PORT + "/Success: User Authenticated\nEnter query", dos);
                                    boolean notConnected = true;
                                    while (notConnected) {
                                        fileSocket = connectDataSocket(fileServerSocket);
                                        if (fileSocket!=null){
                                            fileSocket.setSoTimeout(15*1000);
                                            notConnected = false;
                                        }
                                    }
                                    fis = new DataInputStream(new BufferedInputStream(fileSocket.getInputStream()));
                                    fdos = new DataOutputStream(fileSocket.getOutputStream());
                                }
                                else
                                    if (authenticator.getWrongPassword() < 3)
                                        protocol.sendPayload((byte) 0, (byte) 2, "Failed: Incorrect password", dos);
                                    else {
                                        protocol.sendPayload((byte) 0, (byte) 2, "Failed: You've entered the wrong password too many times", dos);
                                        run = false;
                                    }
                            }
                        }
                    }
                } else if (phase == 1) {
                    if(!authenticator.isAuthenticated()){
                        protocol.sendPayload((byte) 1, Fail, Command_Socket, "You are not authorized", dos);
                    }else{
                        if(type == 0){
                            getAPOD(message);
                        }else if (type == 1) {
                            getMarsWeather();
                        }else if (type == 2){
                                protocol.sendPayload((byte) 1, Fail, Command_Socket, "Failed: Incorrect image sent, you can re-enter your query", dos);
                        }else if (type == 3){
                            protocol.sendPayload((byte) 1, Success, Command_Socket, "Success: Correct File Sent", dos);
                        }else if (type == 4) {
                            protocol.sendPayload((byte) 1, Close, Command_Socket, "Success: Closing Connection...", dos);
                            run = false;
                        }
                    }
                }
            }

        } catch (IOException e) {
            line = this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run. IO Error/ Client " + line + " terminated abruptly");
        } catch (NullPointerException e) {
            line = this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run.Client " + line + " Closed");
        } finally {
            try {
                System.out.println("Closing the connection");
                if (is != null) {
                    is.close();
                    System.err.println(" Socket Input Stream Closed");
                }
                if (dos != null) {
                    dos.close();
                    System.err.println("Socket Out Closed");
                }
                if (s != null) {
                    s.close();
                    System.err.println("Socket Closed");
                }
                if (fis != null) {
                    fis.close();
                    System.err.println("File Socket Input Stream Closed");
                }
                if (fdos != null) {
                    fdos.close();
                    System.err.println("File Socket Out Closed");
                }
                if (fileSocket != null) {
                    fileSocket.close();
                    System.err.println("File Socket Closed");
                }

            } catch (IOException ie) {
                System.err.println("Socket Close Error");
            }
        }//end finally
    }


    private void getAPOD(String date) {
        Pattern p = Pattern.compile("(?<=url\":\").*?(?=\")");
        BufferedImage image;
        try {
            URL apodAPI;
            if("current".equals(date))
                apodAPI = new URL("https://api.nasa.gov/planetary/apod?&api_key=6Io3jtp4bh4PuQaVVxiQIzvcvB95xdBO6mSYhL2l");
            else
                apodAPI = new URL("https://api.nasa.gov/planetary/apod?date="+date+"&api_key=6Io3jtp4bh4PuQaVVxiQIzvcvB95xdBO6mSYhL2l");
            URLConnection apod = apodAPI.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(apod.getInputStream()));
            String inputLine = in.readLine();
            Matcher m = p.matcher(inputLine);
            if (m.find()) {
                URL url =new URL(m.group());
                image = ImageIO.read(url);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] bytes = baos.toByteArray();
                protocol.sendPayload((byte) 1, APOD, Command_Socket,String.valueOf(Arrays.hashCode(bytes)), dos);
                protocol.sendPayload((byte) 1, APOD, File_socket,bytes, fdos);
            }
            in.close();
        } catch (IOException e) {
            try {
                System.err.println("Failed to fetch data from APOD api");
                protocol.sendPayload((byte) 1, Fail, Command_Socket, "Failed: Illegal format for APOD", dos);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void getMarsWeather() {
        try {
            URL insightAPI = new URL("https://api.nasa.gov/insight_weather/?api_key=6Io3jtp4bh4PuQaVVxiQIzvcvB95xdBO6mSYhL2l&feedtype=json&ver=1.0");
            URLConnection insight = insightAPI.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(insight.getInputStream()));
            StringBuilder data = new StringBuilder();
            String nextLine;
            while ((nextLine = in.readLine()) != null)
                data.append(nextLine);
            JSONParser parse = new JSONParser();
            JSONObject jobj = (JSONObject)parse.parse(data.toString());
            byte[] bytes = jobj.toString().getBytes("utf-8");
            protocol.sendPayload((byte) 1, Insight, Command_Socket,String.valueOf(Arrays.hashCode(bytes)), dos);
            protocol.sendPayload((byte) 1, Insight, File_socket, bytes, fdos);
            in.close();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static Socket connectDataSocket(ServerSocket serverSocket){
        Socket s;
        try
        {
            s = serverSocket.accept();
            System.out.println("A connection was established with a client on the address of " + s.getRemoteSocketAddress());
            return s;
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("Server Class.Connection establishment error inside listen and accept function");
        }
        return null;
    }


}
