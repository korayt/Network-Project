import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class Protocol {

    private byte payloadPhase;
    private byte payloadType;
    private String payloadMessage;
    private int fileHash;
    private int hashFromFile;
    private static final String IMAGE_FOLDER_PATH = "/Users/koray/Downloads/NetworkProject/PS2_codes/client-master/Images/output.jpg";

    public int getFileHash() {
        return fileHash;
    }

    public int getHashFromFile() {
        return hashFromFile;
    }

    public byte getPayloadPhase() {
        return payloadPhase;
    }

    public byte getPayloadType() {
        return payloadType;
    }

    public String getPayloadMessage() {
        return payloadMessage;
    }

    public void interpretPayload(DataInputStream is) {
        try {
            this.payloadPhase = is.readByte();
            this.payloadType = is.readByte();
            if (payloadPhase == 1) {
                byte payloadSocketType = is.readByte();
                if (payloadType == 2 || payloadType == 3 || payloadType == 4) {
                    this.payloadMessage = readMessage(is);
                    return;
                }
                if (payloadSocketType == 1) {
                    if (payloadType == 0) {
                        byte[] bytearray = readData(is);
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytearray);
                        BufferedImage apodFile = ImageIO.read(bais);
                        ImageIO.write(apodFile, "jpg", new File(IMAGE_FOLDER_PATH));
                        bais.close();
                        JFrame editorFrame = new JFrame("Astronomy Picture of the Day");
                        ImageIcon imageIcon = new ImageIcon(apodFile);
                        JLabel jLabel = new JLabel();
                        jLabel.setIcon(imageIcon);
                        editorFrame.getContentPane().add(jLabel, BorderLayout.CENTER);
                        editorFrame.pack();
                        editorFrame.setLocationRelativeTo(null);
                        editorFrame.setVisible(true);
                        hashFromFile = Arrays.hashCode(bytearray);
                    } else if (payloadType == 1) {
                        byte[] bytearray = readData(is);
                        String decoded = new String(bytearray, "UTF-8");
                        interpretJSON(decoded);
                        hashFromFile = Arrays.hashCode(bytearray);
                    }
                } else if (payloadSocketType == 0) {
                    if (payloadType == 0 || payloadType == 1) {
                        fileHash = Integer.parseInt(readMessage(is));
                    }
                } else {
                    this.payloadMessage = readMessage(is);
                }
            } else {
                this.payloadMessage = readMessage(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void interpretJSON(String str) {
        JSONParser parse = new JSONParser();
        try {
            JSONObject insightData = (JSONObject)parse.parse(str);
            JSONArray sols = (JSONArray) insightData.get("sol_keys");
            Random rand = new Random();
            int solPick = rand.nextInt(sols.size());
            String sol = (String) sols.get(solPick);
            JSONObject solObject  = (JSONObject) insightData.get(sol);
            JSONObject pressureObject  = (JSONObject) solObject.get("PRE");
            Number av = (Number) pressureObject.get("av");
            Number ct = (Number) pressureObject.get("ct");
            Number mn = (Number) pressureObject.get("mn");
            Number mx = (Number) pressureObject.get("mx");
            System.out.println("AV: "+ av +" CT: "+ct+" MN: "+mn+" MX: "+mx);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String readMessage(DataInputStream is) {
        int length = 0;
        StringBuilder dataString = null;
        try {
            length = is.readInt();

            byte[] messageByte = new byte[length];
            boolean end = false;
            dataString = new StringBuilder(length);
            int totalBytesRead = 0;
            while (!end) {
                int currentBytesRead = is.read(messageByte);
                totalBytesRead = currentBytesRead + totalBytesRead;
                if (totalBytesRead <= length) {
                    dataString
                            .append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                } else {
                    dataString
                            .append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead,
                                    StandardCharsets.UTF_8));
                }
                if (dataString.length() >= length) {
                    end = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dataString == null)
            return "Fatal Error Can't read message";
        return dataString.toString();
    }

    private byte[] readData(DataInputStream is) {
        int length = 0;
        byte[] messageByte = null;
        try {
            length = is.readInt();
            messageByte = new byte[length];
            boolean end = false;
            int totalBytesRead = 0;
            while (totalBytesRead != length) {
                messageByte[totalBytesRead] = is.readByte();
                totalBytesRead++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messageByte;
    }

    public void sendPayload(byte phase, byte type, String message, DataOutputStream dos) {
        byte[] byteData = message.getBytes();
        try {
            dos.writeByte(phase);
            dos.writeByte(type);
            dos.writeInt(byteData.length);
            dos.write(byteData);
            dos.flush();
        } catch (IOException e) {
            System.err.println("Socket closed because due to inactivity");
            System.exit(0);
        }

    }

    public void sendPayload(byte phase, byte type, int token, String message, DataOutputStream dos){

        byte[] byteData = message.getBytes();
        try {
            dos.writeByte(phase);
            dos.writeByte(type);
            dos.writeInt(token);
            dos.writeInt(byteData.length);
            dos.write(byteData);
            dos.flush();
        } catch (IOException e) {
            System.err.println("Socket closed because due to inactivity");
            System.exit(0);
        }

    }

}
