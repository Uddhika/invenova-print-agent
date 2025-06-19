package org.example.service;

import java.awt.SystemTray;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.TrayIcon;
import java.awt.AWTException;

public class PrintServiceApp {

    private static final int PORT = 8082; // The port our API will run on

    public static void main(String[] args) {
        // 1. Check if System Tray is supported
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported. Exiting.");
            return;
        }

        // 2. Start the HTTP server in a new thread
        HttpApiServer apiServer = new HttpApiServer(PORT);
        apiServer.start();
        System.out.println("Print API server started on port " + PORT);

        // 3. Create the System Tray icon
        setupSystemTray(apiServer);

        System.out.println("Lightweight Print Service is running in the background.");
    }

    private static void setupSystemTray(HttpApiServer server) {
        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Load an icon image (place icon.png in src/main/resources)
            Image image = Toolkit.getDefaultToolkit().createImage(
                    PrintServiceApp.class.getResource("/icon.png")
            );

            // Create a popup menu
            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                server.stop();
                tray.remove(tray.getTrayIcons()[0]);
                System.exit(0);
            });
            popup.add(exitItem);

            // Create the tray icon
            TrayIcon trayIcon = new TrayIcon(image, "Lightweight Print Service", popup);
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
            trayIcon.displayMessage("Print Service", "Print service is running.", TrayIcon.MessageType.INFO);

        } catch (AWTException e) {
            System.err.println("Could not create system tray icon: " + e.getMessage());
        }
    }
}
