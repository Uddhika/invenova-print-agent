package org.example.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpApiServer {

    private final HttpServer server;

    public HttpApiServer(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/print", new PrintHandler());
            server.setExecutor(null); // creates a default executor
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    static class PrintHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers first
            addCorsHeaders(exchange);

            // Handle preflight OPTIONS request
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"status\":\"error\", \"message\":\"Method Not Allowed\"}");
                return;
            }

            try {
                // Read the request body
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // Parse JSON
                JSONObject json = new JSONObject(requestBody);
                String content = json.getString("content");
                // Optional: specify printer name in the API call
                String printerName = json.optString("printerName", null);

                // Call the printing logic
                boolean success = Printer.printReceipt(content, printerName);

                if (success) {
                    sendResponse(exchange, 200, "{\"status\":\"success\", \"message\":\"Printed successfully.\"}");
                } else {
                    sendResponse(exchange, 500, "{\"status\":\"error\", \"message\":\"Failed to print. Check printer status and name.\"}");
                }

            } catch (JSONException e) {
                sendResponse(exchange, 400, "{\"status\":\"error\", \"message\":\"Invalid JSON format.\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"status\":\"error\", \"message\":\"An internal error occurred: " + e.getMessage() + "\"}");
            }
        }

        private void addCorsHeaders(HttpExchange exchange) {
            // Get the origin from the request
            String origin = exchange.getRequestHeaders().getFirst("Origin");

            // List of allowed origins (add your frontend URLs here)
            String[] allowedOrigins = {
                    "http://localhost:5173",  // Vite dev server
                    "https://invenova.lk", // Production
            };

            // Check if the origin is allowed
            boolean isAllowed = false;
            if (origin != null) {
                for (String allowedOrigin : allowedOrigins) {
                    if (allowedOrigin.equals(origin)) {
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                        isAllowed = true;
                        break;
                    }
                }
            }

            // If no specific origin matched, allow localhost patterns for development
            if (!isAllowed && origin != null &&
                    (origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:"))) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                isAllowed = true;
            }

            // Allow specific HTTP methods
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

            // Allow specific headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

            // Allow credentials (set to true since your frontend is sending credentials)
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            // Set how long the browser can cache preflight response (in seconds)
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400"); // 24 hours
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}