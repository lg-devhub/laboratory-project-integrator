package br.com.projetointegrador.server;

import br.com.projetointegrador.controller.Controller_Login;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/login", new Controller_Login());
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor Java iniciado na porta 8080 (http://localhost:8080/login)");
    }
}