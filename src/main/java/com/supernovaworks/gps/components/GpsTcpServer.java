package com.supernovaworks.gps.components;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
                serverSocket = new ServerSocket(port);
                System.out.println("🛰 Servidor GPS escuchando en el puerto " + port);

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept(); // esta línea se desbloquea si se cierra el socket
                    new Thread(() -> handleConnection(socket)).start();
                }
            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("❌ No se pudo iniciar el servidor TCP en el puerto " + port + ": " + e.getMessage());
                }
            }
        });

        serverThread.start();
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("🛑 Cerrando servidor TCP...");
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close(); // esto desbloquea el accept()
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.join(1000); // espera 1 segundo que termine
        }
        System.out.println("🛑 Servidor TCP cerrado correctamente.");
    }

    private void handleConnection(Socket socket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("📡 Paquete recibido del GPS: " + line);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al leer del socket: " + e.getMessage());
        }
    }
}
