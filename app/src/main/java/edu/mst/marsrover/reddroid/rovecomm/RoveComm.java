package edu.mst.marsrover.reddroid.rovecomm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class RoveComm {

    // DataID's handled by rovecomm's layer
    private final static int PING = 1;
    private final static int PING_REPLY = 2;
    private final static int SUBSCRIBE = 3;
    private final static int UNSUBSCRIBE = 4;
    private final static int FORCE_UNSUBSCRIBE = 5;
    private final static int ACK = 6;

    // Communication constraints
    private final static int PORT = 11000;
    private final static int MAX_PACKET_SIZE = 1500;

    // ArrayList to handle who requested this node to send data to it
    private static ArrayList<String> subscribers;

    private Thread thread;
    private ListenRunnable runnable;
    private OnReceiveData onReceiveData;
    private DatagramSocket datagramSocket;

    /**
     * Constructor for instance of rovecomm. This is intended to stay alive for the continuation of
     * the java program, Android Activity or not.
     * @param onReceiveData Listener for received data. Instantiating method must implement it.
     */
    public RoveComm(OnReceiveData onReceiveData) {

        subscribers = new ArrayList<>();
        this.onReceiveData = onReceiveData;

        // Setup network connection. If failed to bind socket or anything else, print it out.
        // Application will continue even if it encounters an error.
        try {
            this.datagramSocket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Setup and start the network listener to run separately of the main thread.
        runnable = new ListenRunnable();
        thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Deconstruct method that needs to run to gracefully end rovecomm
     */
    public void onDestroy() {
        runnable.cancel();
        if (thread.isAlive()) {
            thread.interrupt();
        }

        datagramSocket.close();
    }

    /**
     * Method to request data from another node on the network
     * @param subscriber Other node's ip
     */
    public void subscribe(String subscriber) {
        subscribers.add(subscriber);
        sendData(SUBSCRIBE, null, subscriber);
    }

    /**
     * Method to request no data from another node on the network
     * @param subscriber Other node's ip
     */
    public void unSubscribe(String subscriber) {
        subscribers.remove(subscriber);
        sendData(UNSUBSCRIBE, null, subscriber);
    }

    /**
     * Method to send a rovecomm packet
     * @param id DataID for the data
     * @param contents Data byte[], formatted for specific dataID
     */
    public void sendData(int id, byte[] contents) {

        // Send packet specifically to every subscriber
        for (String subscriber : subscribers) {
            SendData sendData = new SendData(id, contents, subscriber);
            new Thread(sendData).start();
        }
    }

    /**
     * Method to send a rovecomm packet
     * @param id DataID for the data
     * @param contents Data byte[], formatted for specific dataID
     * @param ip String ip of node
     */
    public void sendData(int id, byte[] contents, String ip) {

        SendData sendData = new SendData(id, contents, ip);
        new Thread(sendData).start();
    }

    /**
     * Method that actually sends packet, private as must be run in separate thread
     * @param id DataID for the data
     * @param contents Data byte[], formatted for specific dataID
     * @param ip String ip of node
     */
    private void send(int id, byte[] contents, String ip) {

        try {

            byte[] array = RoveProtocol.encodePacket(id, contents, 0, false);
            datagramSocket.send(new DatagramPacket(
                    array,
                    array.length,
                    InetAddress.getByName(ip),
                    PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Interface that defines the return method for packets received. DataID's that do not match
     * those checked at rovecomm level are passed up to this implemented method.
     */
    public interface OnReceiveData {

        void receiveData(int id, byte[] content);
    }

    /**
     * Runnable that handles packet listening
     */
    private class ListenRunnable implements Runnable {

        // Boolean for loop control
        private volatile boolean cancelled = false;

        @Override
        public void run() {

            try {

                while (!cancelled) {

                    // Create packet and wait until one is recieved
                    DatagramPacket packet = new DatagramPacket(
                            new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
                    datagramSocket.receive(packet);

                    // Decode it
                    RoveProtocol.DataObject object = RoveProtocol.decodePacket(packet.getData());

                    // Switch on ID to handle these internally
                    switch (object.getDataId()) {

                        case PING:
                            break;
                        case PING_REPLY:
                            break;
                        case SUBSCRIBE:
                            break;
                        case UNSUBSCRIBE:
                            break;
                        case FORCE_UNSUBSCRIBE:
                            break;
                        case ACK:
                            break;
                        default:
                            // Give the data to the application
                            onReceiveData.receiveData(object.getDataId(), object.getData());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Method which cancels and ends the loop. Called within onDestroy()
         */
        private void cancel() {
            cancelled = true;
        }
    }

    /**
     * Runnable used to send a packet on a thread
     * Simply stores packet info to send using send()
     */
    public class SendData implements Runnable {

        int id;
        byte[] contents;
        String ip;

        public SendData(int id, byte[] contents, String ip) {
            this.id = id;
            this.contents = contents;
            this.ip = ip;
        }

        @Override
        public void run() {
            send(id, contents, ip);
        }
    }
}
