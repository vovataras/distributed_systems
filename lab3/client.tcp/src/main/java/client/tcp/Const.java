package client.tcp;

public interface Const {
    static final byte CMD_PING = 1;
    static final byte CMD_PING_RESPONSE = 2;
    static final byte CMD_ECHO = 3;
    static final byte CMD_LOGIN = 5;
    static final byte CMD_LIST = 10;
    static final byte CMD_MSG = 15;
    static final byte CMD_FILE = 20;
    static final byte CMD_RECEIVE_MSG = 25;
    static final byte CMD_RECEIVE_FILE = 30;
    static final byte CMD_EXIT = 127;

    static final byte[] CMD_LOGIN_OK_NEW = new byte[] { 6 };
    static final byte[] CMD_LOGIN_OK = new byte[] { 7 };
    static final byte[] CMD_MSG_SENT = new byte[] { 16 };
    static final byte[] CMD_FILE_SENT = new byte[] { 21 };
    static final byte[] CMD_RECEIVE_MSG_EMPTY = new byte[] { 26 };
    static final byte[] CMD_RECEIVE_FILE_EMPTY = new byte[] { 31 };

    // Errors
    static final byte SERVER_ERROR = 100;
    static final byte INCORRECT_CONTENT_SIZE = 101;
    static final byte SERIALIZATION_ERROR =  102;
    static final byte INCORRECT_COMMAND =  103;
    static final byte WRONG_PARAMS = 104;

    static final byte LOGIN_WRONG_PASSWORD = 110;
    static final byte LOGIN_FIRST = 112;
    static final byte FAILED_SENDING = 113;
}
