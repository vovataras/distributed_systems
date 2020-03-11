package client.tcp;

import java.io.*;
import java.lang.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class CommandManager implements Const{

    // little bit more than 10MB
    private static final int MAX_CONTENT_SIZE = 10_500_000;

    private static final Charset CHARSET = StandardCharsets.UTF_8;

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

    public synchronized void sendRequest(byte[] request, DataOutputStream outputStream) throws IOException {
        if (request == null) {
            System.out.println("ERROR: Unable to send request to the server.");
        } else {
            // Sending Request to the server.
            outputStream.writeInt(request.length);
            outputStream.write(request);
        }
    }

    public synchronized byte[] receiveResponse(DataInputStream readStream) throws IOException {
        // read the size of reply and response message itself
        int contentSize = readStream.readInt();
        byte[] response = new byte[contentSize];
        readStream.readFully(response);

        return response;
    }



    private void checkError(byte error) {

        switch (error) {
            case SERVER_ERROR:
                System.out.println("100 SERVER ERROR: Internal Server Error occurred. " +
                        "That means that it’s not your fault (or you just sent something " +
                        "THAT bad that server was not expecting).\n");
                break;
            case INCORRECT_CONTENT_SIZE:
                System.out.println("101 INCORRECT CONTENT SIZE: " +
                        "The transfer size is below 0 or above 10MB.\n");
                break;
            case SERIALIZATION_ERROR:
                System.out.println("102 SERIALIZATION ERROR: Server failed to deserialize content.\n");
                break;
            case INCORRECT_COMMAND:
                System.out.println("103 INCORRECT COMMAND: Server did not understand the command.\n");
                break;
            case WRONG_PARAMS:
                System.out.println("104 WRONG PARAMS: Incorrect number or content of parameters. " +
                        "Server expected something different.\n");
                break;
            case LOGIN_WRONG_PASSWORD:
                System.out.println("110 LOGIN WRONG PASSWORD: The specified password is incorrect " +
                        "(this login was used before with another password).\n");
                break;
            case LOGIN_FIRST:
                System.out.println("112 LOGIN FIRST: This command requires login first.\n");
                break;
            case FAILED_SENDING:
                System.out.println("113 FAILED SENDING: Failed to send the message or file. " +
                        "Either receiver does not exist or his pending message quota exceeded.\n");
                break;

            default:
                System.out.println("Unexpected error...\n");
        }
    }

    public synchronized byte[] execute(String[] command) {

        if (command.length == 0){
            checkError(INCORRECT_COMMAND);
            return new byte[0];
        }

        byte currentCommand = command[0].getBytes()[0];

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
                    return new byte[] {INCORRECT_COMMAND};
            }
        } catch (Exception ex) {
            return new byte[] {SERIALIZATION_ERROR};
        }
    }

    public synchronized void decode(byte currentCommand, byte[] message){
        if (message.length == 0) {
            incorrectCommand();
            return;
        }

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



    private void incorrectCommand() {
        checkError(INCORRECT_COMMAND);
    }

    public void incorrectContentSize() {
        checkError(INCORRECT_CONTENT_SIZE);
    }


    // TODO: review START
    private byte[] formEchoQuery(String[] command) {
        String[] cmd = Arrays.copyOfRange(command, 1, command.length);
        String joined = String.join(" ", cmd);
        byte[] result = new byte[joined.length() + 1];

        // copy bytes to new array (from, fromIndex, to, toIndex, count)
        result[0] = CMD_ECHO;
        System.arraycopy(joined.getBytes(), 0, result, 1, joined.getBytes().length);

        return result;
    }

    private byte[] formLoginQuery(String[] command) throws IOException{
        if (command.length > 3) {
            checkError(WRONG_PARAMS);
            return new byte[0];
        }

        String[] cmd = Arrays.copyOfRange(command, 1, command.length);
        byte[] b = serialize(cmd);
        byte[] result = new byte[b.length + 1];

        result[0] = CMD_LOGIN;
        System.arraycopy(b, 0, result, 1, b.length);

        return result;
    }

    private byte[] formMsgQuery(String[] command) throws IOException{
        if (command.length < 3){
            checkError(WRONG_PARAMS);
            return new byte[0];
        }

        String[] cmd = new String[2];
        cmd[0] = command[1];
        cmd[1] = String.join(" ", Arrays.copyOfRange(command, 2, command.length));

        byte[] b = serialize(cmd);
        byte[] result = new byte[b.length + 1];

        result[0] = CMD_MSG;
        System.arraycopy(b, 0, result, 1, b.length);

        return result;
    }

    private byte[] formFileQuery(String[] command) throws IOException{
        if (command.length > 4) {
            checkError(WRONG_PARAMS);
            return new byte[0];
        }

        Object[] obj = new Object[3];
        obj[0] = command[1];
        obj[1] = command[2];

        File file = new File(command[3]);
        System.out.println(file.isFile());

        // TODO: check the file size
        try {
//            System.out.println("Success!");
            byte[] b = getBytesFromFile(file);
            if (!isContentSizeOk(b.length)){
                incorrectContentSize();
                return new byte[0];
            }
            obj[2] = b;

        } catch (IOException e) {
            return new byte[] {WRONG_PARAMS};
        }

        byte[] tempByte = serialize(obj);
        byte[] result = new byte[tempByte.length + 1];

        result[0] = CMD_FILE;
        System.arraycopy(tempByte, 0, result, 1, tempByte.length);

        return result;
    }
    // TODO: review END





    private void handlePing(byte[] message) {
        if (message[0] == CMD_PING_RESPONSE)
            System.out.println("ping success!\n");
        else
            System.out.println("ERROR ping: Failed to test connection.\n");
    }

    private void handleEcho(byte[] message) {
        try {
            String response = new String(message, CHARSET);
            System.out.println(response + "\n");
        } catch (Exception ex) {
            System.out.println("ERROR echo: Failed to get response.\n");
        }
    }

    private void handleLogin(byte[] message) {
        if (Arrays.equals(message, CMD_LOGIN_OK_NEW))
            System.out.println("Registration ok.\n");
        else if (Arrays.equals(message, CMD_LOGIN_OK))
            System.out.println("Login ok.\n");
        else
            checkError(message[0]);
    }

    private void handleList(byte[] message) {
        if (message.length > 1){
            try {
                String[] users = deserialize(message, String[].class);
                System.out.println("Number of users on the server: " + users.length + ".");
                if (users.length > 0){
                    System.out.println("Users:");
                    for (String user: users) {
                        System.out.println("  "+ user);
                    }
                    System.out.println();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        } else {
            checkError(message[0]);
        }
    }

    private void handleMsg(byte[] message) {
        if (Arrays.equals(message, CMD_MSG_SENT))
            System.out.println("Message successfully sent.\n");
        else
            checkError(message[0]);
    }

    private void handleFile(byte[] message) {
        if (Arrays.equals(message, CMD_FILE_SENT))
            System.out.println("File successfully sent.\n");
        else {
            checkError(message[0]);
        }
    }



    // TODO: review START
    private void handleReceiveMessage(byte[] message) {
        if(message.length == 1){
            if (!Arrays.equals(message, CMD_RECEIVE_MSG_EMPTY)) {
                checkError(message[0]);
            }
//            else {
//                System.out.println("No messages.");
//            }
        } else {
            try {
                String[] msg = deserialize(message, String[].class);
                System.out.println("\nYou have new message!");
                System.out.println("Login of user: " + msg[0]);
                System.out.println("Message: " + msg[1]);
                System.out.println();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleReceiveFile(byte[] message) {
        if(message.length == 1){
            if (!Arrays.equals(message, CMD_RECEIVE_FILE_EMPTY))
                System.out.println("ERROR: receive file");
        } else {
            try {
                Object[] msg = deserialize(message, Object[].class);
                System.out.println("You have new file!");
                System.out.println("Login of user: " + msg[0]);
                System.out.println("Filename: " + msg[1] + "\n");

                File file = new File("./receivedFiles/"+ msg[1]);

                writeByte((byte[])msg[2], file);

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }



    public String[] getActiveUsers(byte[] response) {
        if (response.length > 1){
            try {
                return deserialize(response, String[].class);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }

        return new String[0];
    }
    // TODO: review END





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
    private byte[] getBytesFromFile(File file) throws IOException {
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
        int numRead;

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
//        System.out.println("File encoded!!!");
        return bytes;
    }



    // https://www.geeksforgeeks.org/convert-byte-array-to-file-using-java/
    // Method which write the bytes into a file
    private void writeByte(byte[] bytes, File file)
    {
        try {
            // Initialize a pointer
            // in file using OutputStream
            OutputStream os = new FileOutputStream(file);

            // Starts writing the bytes in it
            os.write(bytes);
            System.out.println("Successfully file received.");

            // Close the file
            os.close();
        }

        catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}
