package net.mrcappy.corelib.protocol.export;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * PCAP exporter for packet logs.
 * 
 * Ever wanted to open Minecraft packets in Wireshark like
 * you're some kind of network forensics expert? No? Well
 * too bad, now you can!
 * 
 * This writes packets in PCAP format so you can analyze them
 * with real tools instead of staring at console logs like
 * a caveman. Import into Wireshark and pretend you know
 * what you're doing.
 * 
 * Fair warning: Minecraft packets in Wireshark look like
 * someone encrypted garbage with more garbage. But hey,
 * at least it's *professional* garbage.
 */
public class PCAPExporter {
    
    private static final int PCAP_MAGIC = 0xa1b2c3d4;
    private static final short PCAP_VERSION_MAJOR = 2;
    private static final short PCAP_VERSION_MINOR = 4;
    private static final int PCAP_SNAPLEN = 65535;
    private static final int PCAP_NETWORK = 147; // User defined
    
    private final File outputFile;
    private final DataOutputStream output;
    private final BlockingQueue<PacketEntry> queue;
    private final Thread writerThread;
    private volatile boolean running = true;
    
    public PCAPExporter(File outputFile) throws IOException {
        this.outputFile = outputFile;
        this.output = new DataOutputStream(new FileOutputStream(outputFile));
        this.queue = new LinkedBlockingQueue<>();
        
        // Write PCAP header
        writePCAPHeader();
        
        // Start writer thread
        this.writerThread = new Thread(this::writerLoop, "PCAP-Writer");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }
    
    /**
     * Write PCAP global header.
     * Magic numbers everywhere because that's how PCAP rolls.
     */
    private void writePCAPHeader() throws IOException {
        output.writeInt(PCAP_MAGIC);
        output.writeShort(PCAP_VERSION_MAJOR);
        output.writeShort(PCAP_VERSION_MINOR);
        output.writeInt(0); // Timezone offset (GMT)
        output.writeInt(0); // Timestamp accuracy
        output.writeInt(PCAP_SNAPLEN); // Max packet length
        output.writeInt(PCAP_NETWORK); // Network type
    }
    
    /**
     * Add a packet to the export queue.
     * Non-blocking because we're not savages.
     */
    public void exportPacket(PacketContainer packet, boolean outgoing, String playerName) {
        try {
            byte[] data = serializePacket(packet, outgoing, playerName);
            queue.offer(new PacketEntry(System.currentTimeMillis(), data));
        } catch (Exception e) {
            // Serialization failed, oh well
        }
    }
    
    /**
     * Serialize packet to bytes.
     * This is where dreams go to die.
     */
    private byte[] serializePacket(PacketContainer packet, boolean outgoing, String player) 
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // Custom header for our packets
        dos.writeUTF(player); // Player name
        dos.writeBoolean(outgoing); // Direction
        dos.writeUTF(packet.getType().name()); // Packet type
        
        // Write actual packet data
        try {
            byte[] rawData = packet.getRawBytes();
            dos.writeInt(rawData.length);
            dos.write(rawData);
        } catch (Exception e) {
            // Fallback to field data
            dos.writeInt(-1); // Indicate field data format
            
            // Write integer fields
            var integers = packet.getIntegers().getValues();
            dos.writeInt(integers.size());
            for (int i : integers) {
                dos.writeInt(i);
            }
            
            // Write string fields
            var strings = packet.getStrings().getValues();
            dos.writeInt(strings.size());
            for (String s : strings) {
                dos.writeUTF(s != null ? s : "");
            }
            
            // Write double fields
            var doubles = packet.getDoubles().getValues();
            dos.writeInt(doubles.size());
            for (double d : doubles) {
                dos.writeDouble(d);
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Writer thread loop.
     * Consumes packets and writes to file.
     */
    private void writerLoop() {
        while (running) {
            try {
                PacketEntry entry = queue.take();
                writePacketRecord(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                // RIP writing, stop the exporter
                running = false;
            }
        }
    }
    
    /**
     * Write a single packet record.
     * PCAP packet format is weird but whatever.
     */
    private void writePacketRecord(PacketEntry entry) throws IOException {
        // Packet header
        int seconds = (int)(entry.timestamp / 1000);
        int microseconds = (int)((entry.timestamp % 1000) * 1000);
        
        output.writeInt(seconds);
        output.writeInt(microseconds);
        output.writeInt(entry.data.length); // Captured length
        output.writeInt(entry.data.length); // Original length
        
        // Packet data
        output.write(entry.data);
        output.flush();
    }
    
    /**
     * Stop exporting and close the file.
     */
    public void close() {
        running = false;
        writerThread.interrupt();
        
        try {
            writerThread.join(1000);
            output.close();
        } catch (Exception e) {
            // Whatever, we tried
        }
    }
    
    private static class PacketEntry {
        final long timestamp;
        final byte[] data;
        
        PacketEntry(long timestamp, byte[] data) {
            this.timestamp = timestamp;
            this.data = data;
        }
    }
}