package edu.mst.marsrover.reddroid.rovecomm;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class RoveProtocol {

    // Version number of this packet implementation
    public static final byte VERSION_NUMBER = 1;

    private RoveProtocol() {
    }

    /**
     * Static method to encode data into a packet byte[] readable by rovecomm
     * @param dataId ID given to the data[] for receiving node to interpret data correctly
     * @param data Data byte[], refer to data ID's definition for structure required
     * @param seqNum Order of packet in packet sequence
     * @param requireACK Should packet be delivered TCP
     * @return Packet data[] ready for sending
     */
    public static byte[] encodePacket(int dataId, byte[] data, int seqNum, boolean requireACK) {

        byte flags = (requireACK) ? Flags.ACK.data : Flags.NONE.data;

        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length);

        // Creating 8 Byte header
        buffer.put(VERSION_NUMBER);
        // Sequence Number in two bytes
        buffer.put((byte) (seqNum >> 8));
        buffer.put((byte) (seqNum & 0x00FF));
        buffer.put(flags);
        // Data ID Number in two bytes
        buffer.put((byte) (dataId >> 8));
        buffer.put((byte) (dataId & 0x00FF));
        // Data size/length in two bytes
        buffer.put((byte) (data.length >> 8));
        buffer.put((byte) (data.length & 0x00FF));

        // Insert data into array
        buffer.put(data);

        return buffer.array();
    }

    /**
     * Static method to decode recieved packet from rovecomm
     * @param rawData Packet data[] including header
     * @return DataObject containing dataID, sequence number, acknowledgement status, and data[]
     * @throws Exception
     */
    public static DataObject decodePacket(byte[] rawData) throws Exception {

        int protocol_version = rawData[0];
        byte[] data;
        int dataId;
        int seqNum;
        boolean requiresAck;

        int dataSize;
        byte flags;

        switch (protocol_version) {
            case 1:

                seqNum = rawData[1];
                seqNum = (seqNum << 8) | rawData[2];
                flags = rawData[3];
                dataId = rawData[4];
                dataId = (dataId << 8) | rawData[5];
                dataSize = rawData[6];
                dataSize = (dataSize << 8) | rawData[7];

                requiresAck = (flags & Flags.ACK.data) != Flags.NONE.data;
                data = Arrays.copyOfRange(rawData, 8, 8 + dataSize);

                return new DataObject(data, dataId, seqNum, requiresAck);

            default:
                throw new Exception("Non-existent packet version!");
        }
    }

    /**
     * Enum to handle possible acknowledgement states (true/false but fancy)
     */
    private enum Flags {

        NONE((byte) 0b0000000), ACK((byte) 0b000_0001);

        private byte data;

        Flags(byte data) {
            this.data = data;
        }
    }

    /**
     * Object class to contain data that is decoded from a packet. Is immutable, or should be.
     */
    public static class DataObject {

        private byte[] data;
        private int dataId;
        private int seqNum;
        private boolean requiresAck;

        public DataObject(byte[] data, int dataId, int seqNum, boolean requiresAck) {
            this.data = data;
            this.dataId = dataId;
            this.seqNum = seqNum;
            this.requiresAck = requiresAck;
        }

        public byte[] getData() {
            return data;
        }

        public int getDataId() {
            return dataId;
        }

        public int getSeqNum() {
            return seqNum;
        }

        public boolean isRequiresAck() {
            return requiresAck;
        }
    }
}
