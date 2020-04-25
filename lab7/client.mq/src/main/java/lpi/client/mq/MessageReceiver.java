package lpi.client.mq;

import javax.jms.MessageListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MapMessage;


class MessageReceiver implements MessageListener {
    @Override
    public void onMessage(Message message) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(message instanceof MapMessage) {
            MapMessage mapMsg = (MapMessage)message;
            try {
                String sender = mapMsg.getString("sender");
                String msg = mapMsg.getString("message");

                System.out.println("You have a new message!");
                System.out.println("From: " + sender + "");
                System.out.println("Message: " + msg + "\n");

            } catch (JMSException e) {
//                e.printStackTrace();
                System.out.println(e.getMessage() + "\n");
            }
        }
    }
}
