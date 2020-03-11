package client.tcp;

public interface Const {
    byte CMD_PING = 1;
    byte CMD_PING_RESPONSE = 2;
    byte CMD_ECHO = 3;
    byte CMD_LOGIN = 5;
    byte CMD_LIST = 10;
    byte CMD_MSG = 15;
    byte CMD_FILE = 20;
    byte CMD_RECEIVE_MSG = 25;
    byte CMD_RECEIVE_FILE = 30;
    byte CMD_EXIT = 127;

    byte[] CMD_LOGIN_OK_NEW = new byte[] { 6 };
    byte[] CMD_LOGIN_OK = new byte[] { 7 };
    byte[] CMD_MSG_SENT = new byte[] { 16 };
    byte[] CMD_FILE_SENT = new byte[] { 21 };
    byte[] CMD_RECEIVE_MSG_EMPTY = new byte[] { 26 };
    byte[] CMD_RECEIVE_FILE_EMPTY = new byte[] { 31 };

    // Errors
    byte SERVER_ERROR = 100;
    byte INCORRECT_CONTENT_SIZE = 101;
    byte SERIALIZATION_ERROR =  102;
    byte INCORRECT_COMMAND =  103;
    byte WRONG_PARAMS = 104;

    byte LOGIN_WRONG_PASSWORD = 110;
    byte LOGIN_FIRST = 112;
    byte FAILED_SENDING = 113;
}
