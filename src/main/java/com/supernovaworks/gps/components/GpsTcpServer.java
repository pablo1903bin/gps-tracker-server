package com.supernovaworks.gps.components;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class GpsTcpServer implements CommandLineRunner, DisposableBean {

    private ServerSocket serverSocket;
    private Thread serverThread;

    @Value("${gps.tcp.port}")
    private int port;

    @Override
    public void run(String... args) {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"));
                System.out.println("🛰 Servidor GPS escuchando en el puerto " + port);

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleConnection(socket)).start();
                }
            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("❌ No se pudo iniciar el servidor TCP: " + e.getMessage());
                }
            }
        });

        serverThread.start();
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("🛑 Cerrando servidor TCP...");
        if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        if (serverThread != null && serverThread.isAlive()) serverThread.join(1000);
        System.out.println("🛑 Servidor TCP cerrado correctamente.");
    }

    private void handleConnection(Socket socket) {
        try (
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream()
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                if (bytesRead < 5) continue;

                // Log en HEX
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < bytesRead; i++) {
                    hex.append(String.format("%02X ", buffer[i]));
                }
                System.out.println("📡 Paquete recibido del GPS (HEX): " + hex);

                // 💬 LOGIN ASCII
                String texto = new String(buffer, 0, bytesRead).trim();
                if (texto.startsWith("##,imei:") && texto.endsWith("A;")) {
                    System.out.println("📨 Login ASCII recibido: " + texto);
                    String ackAscii = "ON"; // O "ON"
                    out.write(ackAscii.getBytes());
                    out.flush();
                    System.out.println("✅ ACK ASCII enviado vrg: " + ackAscii);
                    return; // Salir, no sigas con binario
                }

                // 🔐 LOGIN BINARIO
                if ((buffer[0] & 0xFF) == 0x78 && (buffer[1] & 0xFF) == 0x78) {
                    int length = buffer[2] & 0xFF;
                    int type = buffer[3] & 0xFF;

                    if (type == 0x01) { // login
                        System.out.println("🔐 Login binario detectado");

                        int serialHigh = buffer[bytesRead - 6] & 0xFF;
                        int serialLow = buffer[bytesRead - 5] & 0xFF;

                        System.out.printf("🔢 Serial extraído: %02X %02X\n", serialHigh, serialLow);

                        byte[] serial = new byte[] { (byte) serialHigh, (byte) serialLow };
                        sendLoginAck(out, serial);
                        continue;
                    }

                    // 🔄 Puedes agregar más tipos aquí (ej: 0x12 → posición)
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error al manejar la conexión: " + e.getMessage());
        }
    }

    private void sendLoginAck(OutputStream out, byte[] serial) throws Exception {
        byte[] body = new byte[3];
        body[0] = 0x01;         // Login response
        body[1] = serial[0];    // Serial high
        body[2] = serial[1];    // Serial low

        byte[] ack = buildPacketWithCrc(body);
        out.write(ack);
        out.flush();
        System.out.println("✅ ACK binario de login enviado con serial vrg ptm ");
    }

    private byte[] buildPacketWithCrc(byte[] body) {
        byte[] header = new byte[] { 0x78, 0x78 };
        byte length = (byte) (body.length + 2);

        byte[] preCrc = new byte[1 + body.length];
        preCrc[0] = length;
        System.arraycopy(body, 0, preCrc, 1, body.length);

        int crc = calculateCrc16(preCrc);
        byte[] crcBytes = new byte[] {
            (byte) ((crc >> 8) & 0xFF),
            (byte) (crc & 0xFF)
        };

        byte[] packet = new byte[header.length + preCrc.length + crcBytes.length + 2];
        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(preCrc, 0, packet, header.length, preCrc.length);
        System.arraycopy(crcBytes, 0, packet, header.length + preCrc.length, crcBytes.length);
        packet[packet.length - 2] = 0x0D;
        packet[packet.length - 1] = 0x0A;

        return packet;
    }

    private int calculateCrc16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0x8408;
                } else {
                    crc >>= 1;
                }
            }
        }
        crc = ~crc;
        return ((crc << 8) & 0xFF00) | ((crc >> 8) & 0x00FF); // Swap bytes
    }
}
