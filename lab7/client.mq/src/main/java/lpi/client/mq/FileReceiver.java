package lpi.client.mq;

import lpi.server.mq.FileInfo;
import javax.jms.MessageListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.io.File;
import java.io.IOException;


class FileReceiver implements MessageListener {
    @Override
    public void onMessage(Message message) {
        String folderPath = "./receivedFiles";

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(message instanceof ObjectMessage) {
            ObjectMessage objMsg = (ObjectMessage)message;
            try {
                FileInfo fileInfo = (FileInfo) objMsg.getObject();

                System.out.println("You have a new file!");
                System.out.println("From: " + fileInfo.getSender() + "");
                System.out.println("Name of file: " + fileInfo.getFilename() + "\n");

                // check if there is a folder to save the files
                File folder = new File(folderPath);
                if (!folder.exists()) {
                    folder.mkdir();
                }

                fileInfo.saveFileTo(folder);

            } catch (JMSException | IOException e) {
//                e.printStackTrace();
                System.out.println(e.getMessage() + "\n");
            }
        }
    }
}
