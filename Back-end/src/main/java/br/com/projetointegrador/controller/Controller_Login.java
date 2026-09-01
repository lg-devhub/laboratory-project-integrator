package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.DAO_User;
import br.com.projetointegrador.model.Model_User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Controller_Login implements HttpHandler {

    private final DAO_User daoUser = new DAO_User();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Suporte para CORS (Requisições HTTP vindas do navegador/Vue.js)
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStream is = exchange.getRequestBody()) {
                Model_User loginRequest = objectMapper.readValue(is, Model_User.class);

                if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                    sendResponse(exchange, 400, "{\"error\": \"E-mail e senha são obrigatórios.\"}");
                    return;
                }

                Model_User authenticatedUser = daoUser.authenticate(
                        loginRequest.getEmail(), 
                        loginRequest.getPassword()
                );

                if (authenticatedUser != null) {
                    String responseJson = String.format(
                        "{\"status\": \"success\", \"message\": \"Login efetuado com sucesso!\", \"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\"}}",
                        authenticatedUser.getId(), authenticatedUser.getName(), authenticatedUser.getEmail()
                    );
                    sendResponse(exchange, 200, responseJson);
                } else {
                    sendResponse(exchange, 401, "{\"error\": \"E-mail ou senha incorretos.\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\": \"Erro interno no servidor.\"}");
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseText.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}