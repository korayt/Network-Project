import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class Protocol {

    private byte payloadPhase;
    private byte payloadType;
    private String payloadMessage;

    public byte getPayloadPhase() {
        return payloadPhase;
    }

    public byte getPayloadType() {
        return payloadType;
    }

    public String getPayloadMessage() {
        return payloadMessage;
    }

    public void interpretPayload(DataInputStream is, SocketAddress localSocketAddress,Authenticator authenticator) throws IOException {
            this.payloadPhase = is.readByte();
            this.payloadType = is.readByte();
            if (payloadPhase == 1){
                int token = is.readInt();
                authenticator.checkToken(token,localSocketAddress);
                if (!authenticator.isAuthenticated()){
                    return;
                }
            }
            int length = is.readInt();
            byte[] messageByte = new byte[length];
            boolean end = false;
            StringBuilder dataString = new StringBuilder(length);
            int totalBytesRead = 0;
            while(!end) {
                int currentBytesRead = is.read(messageByte);
                totalBytesRead = currentBytesRead + totalBytesRead;
                if(totalBytesRead <= length) {
                    dataString
                            .append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                } else {
                    dataString
                            .append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead,
                                    StandardCharsets.UTF_8));
                }
                if(dataString.length()>=length) {
                    end = true;
                }
            }
            this.payloadMessage = dataString.toString();
    }

    public void sendPayload(byte phase, byte type, String message, DataOutputStream dos) throws IOException {
        byte[] byteData = message.getBytes();
        dos.writeByte(phase);
        dos.writeByte(type);
        dos.writeInt(byteData.length);
        dos.write(byteData);
        dos.flush();
    }

    public void sendPayload(byte phase, byte type, byte socketType, String message, DataOutputStream dos) throws IOException {
        byte[] byteData = message.getBytes();
        dos.writeByte(phase);
        dos.writeByte(type);
        dos.writeByte(socketType);
        dos.writeInt(byteData.length);
        dos.write(byteData);
        dos.flush();
    }

    public void sendPayload(byte phase, byte type, byte socketType, byte[] message, DataOutputStream dos) throws IOException {
        dos.writeByte(phase);
        dos.writeByte(type);
        dos.writeByte(socketType);
        dos.writeInt(message.length);
        dos.write(message);
        dos.flush();
    }

}
