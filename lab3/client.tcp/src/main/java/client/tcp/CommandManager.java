package client.tcp;

import java.io.*;
import java.lang.*;
import java.nio.charset.Charset;
import java.util.Arrays;


public class CommandManager implements Const{

    // little bit more than 10MB
    private static final int MAX_CONTENT_SIZE = 10_500_000;

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private static final byte[] SERIALIZATION_ERROR = new byte[] {100};
    private static final byte[] CMD_ERROR = new byte[] {101};
    private static final byte[] INCORRECT_COMMAND = new byte[] {102};

    private static final byte[] INCORRECT_CONTENT_SIZE = new byte[] { 111 }; // 101

    private byte currentCommand;


//    int size;
//    byte type;
//    String[] data;
//
//    CommandManager(int size){
//        this.size = size;
//    }
//
//    CommandManager(byte type, String[] data){
//        this.type = type;
//        this.data = data;
//    }

    public boolean isContentSizeOk(int contentSize) {
        return contentSize > 0 && contentSize < MAX_CONTENT_SIZE;
    }

    public byte[] execute(String[] command) {
//        String[] command = cmd.split(" ");

        if (command.length == 0)
            return incorrectCommand();

        currentCommand = command[0].getBytes()[0];
        try {
            switch (currentCommand) {
                case CMD_PING:
                    return new byte[] {CMD_PING};
                case CMD_ECHO:
                    return formEchoQuery(command);
                case CMD_LOGIN:
                    return formLoginQuery(command);
                case CMD_LIST:
                    return new byte[] {CMD_LIST};
                case CMD_MSG:
                    return formMsgQuery(command);
                case CMD_FILE:
                    return formFileQuery(command);
                case CMD_RECEIVE_MSG:
                    return new byte[] {CMD_RECEIVE_MSG};
                case CMD_RECEIVE_FILE:
                    return new byte[] {CMD_RECEIVE_FILE};

                default:
                    return incorrectCommand();
            }
        } catch (Exception ex) {
            return SERIALIZATION_ERROR;
        }
    }

    public void decode(byte[] message){
        if (message.length == 0)
            incorrectCommand();

//        for debug
//        for (byte e: message
//             ) {
//            System.out.println(e);
//        }

        switch (currentCommand) {
            case CMD_PING:
                handlePing(message);
                break;
            case CMD_ECHO:
                handleEcho(message);
                break;
            case CMD_LOGIN:
                handleLogin(message);
                break;
            case CMD_LIST:
                handleList(message);
                break;
            case CMD_MSG:
                handleMsg(message);
                break;
            case CMD_FILE:
                handleFile(message);
                break;
            case CMD_RECEIVE_MSG:
                handleReceiveMessage(message);
                break;
            case CMD_RECEIVE_FILE:
                handleReceiveFile(message);
                break;

            default:
                incorrectCommand();
        }
    }


    private byte[] incorrectCommand() {
        return INCORRECT_COMMAND;
    }


    public byte[] incorrectContentSize() {
        return INCORRECT_CONTENT_SIZE;
    }


    private byte[] formEchoQuery(String[] command) {
        String[] cmd = Arrays.copyOfRange(command, 1, command.length);
        String joined = String.join(" ", cmd);
        byte[] result = new byte[joined.length() + 1];

        // copy bytes to new array (from, fromIndex, to, toIndex, count)
        // System.arraycopy(CMD_ECHO, 0, result, 0, 1);
        result[0] = CMD_ECHO;
        System.arraycopy(joined.getBytes(), 0, result, 1, joined.getBytes().length);

        return result;
    }

    private byte[] formLoginQuery(String[] command) throws IOException{
        if (command.length > 3)
            return CMD_ERROR;

        String[] cmd = Arrays.copyOfRange(command, 1, command.length);
        byte[] b = serialize(cmd);
        byte[] result = new byte[b.length + 1];

        result[0] = CMD_LOGIN;
        System.arraycopy(b, 0, result, 1, b.length);

        return result;
    }

    private byte[] formMsgQuery(String[] command) throws IOException{
        if (command.length > 3)
            return CMD_ERROR;

        String[] cmd = Arrays.copyOfRange(command, 1, command.length);
        byte[] b = serialize(cmd);
        byte[] result = new byte[b.length + 1];

        result[0] = CMD_MSG;
        System.arraycopy(b, 0, result, 1, b.length);

        return result;
    }

    private byte[] formFileQuery(String[] command) throws IOException{
        if (command.length > 4)
            return CMD_ERROR;

        Object[] obj = new Object[3];
        obj[0] = command[1];
        obj[1] = command[2];

        File file = new File("1.txt");

        try {
            byte[] b = getBytesFromFile(file);
            obj[2] = b;
        } catch (IOException e) {
            return CMD_ERROR;
        }

        byte[] tempByte = serialize(obj);
        byte[] result = new byte[tempByte.length + 1];

        result[0] = CMD_FILE;
        System.arraycopy(tempByte, 0, result, 1, tempByte.length);

        return result;
    }



    private void handlePing(byte[] message) {
        if (message[0] == CMD_PING_RESPONSE)
            System.out.println("ping success!");
        else
            System.out.println("ERROR: ping");
    }

    private void handleEcho(byte[] message) {
        try {
            String response = new String(message, CHARSET);

            System.out.println(response);
        } catch (Exception ex) {
            System.out.println("ERROR: echo");
        }
    }

    private void handleLogin(byte[] message) {
        System.out.println("login");
    }

    private void handleList(byte[] message) {
        System.out.println("list");
    }

    private void handleMsg(byte[] message) {
        System.out.println("msg");
    }

    private void handleFile(byte[] message) {
        System.out.println("file");
    }

    private void handleReceiveMessage(byte[] message) {
        System.out.println("receive message");
    }

    private void handleReceiveFile(byte[] message) {
        System.out.println("receive file");
    }

























    private byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(object);
            return byteStream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] data, Class<T> clazz) throws ClassNotFoundException, IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data);
             ObjectInputStream objectStream = new ObjectInputStream(stream)) {
            return (T) objectStream.readObject();
        }
    }



    // https://stackoverflow.com/questions/858980/file-to-byte-in-java
    // Returns the contents of the file in a byte array.
    public byte[] getBytesFromFile(File file) throws IOException {
        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
            throw new IOException("File is too large!");
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;

        InputStream is = new FileInputStream(file);
        try {
            while (offset < bytes.length
                    && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
        } finally {
            is.close();
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
        return bytes;
    }
}
