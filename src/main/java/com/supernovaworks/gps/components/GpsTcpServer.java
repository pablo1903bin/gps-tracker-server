package com.supernovaworks.gps.components;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class GpsTcpServer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        int port = 5055;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("🛰 Servidor GPS escuchando en el puerto " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("📥 Nueva conexión desde " + socket.getInetAddress());

            // Manejamos la conexión en un nuevo hilo
            new Thread(() -> handleConnection(socket)).start();
        }
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
