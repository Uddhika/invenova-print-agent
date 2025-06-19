package org.example.service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Printer {
    // ESC/POS Commands
    private static final byte[] INIT = {27, 64}; // ESC @: Initialize printer
    private static final byte[] BOLD_ON = {27, 69, 1}; // ESC E 1: Turn on bold
    private static final byte[] BOLD_OFF = {27, 69, 0}; // ESC E 0: Turn off bold
    private static final byte[] CENTER_ON = {27, 97, 1}; // ESC a 1: Center alignment
    private static final byte[] CENTER_OFF = {27, 97, 0}; // ESC a 0: Left alignment (default)
    private static final byte[] DOUBLE_HEIGHT_ON = {27, 33, 16}; // ESC ! 16: Double height text
    private static final byte[] NORMAL_TEXT_SIZE = {27, 33, 0}; // ESC ! 0: Normal text
    private static final byte[] CUT_PAPER = {29, 86, 66, 0}; // GS V B 0: Partial cut
    private static final byte[] LINE_FEED = {10}; // LF
    private static final byte[] DOUBLE_SIZE_ON = {27, 33, 48};

    public static PrintService findPrinter(String printerName) {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : printServices) {
            if (service.getName().equalsIgnoreCase(printerName)) {
                System.out.println("Found printer: " + service.getName());
                return service;
            }
        }
        System.err.println("Printer '" + printerName + "' not found.");
        return (printServices.length > 0) ? printServices[0] : null;
    }

    public static boolean printReceipt(String receiptText, String printerName) {
        PrintService ps = findPrinter(printerName);
        if (ps == null) {
            System.err.println("No printer found.");
            return false;
        }

        System.out.println("Printing receipt to: " + ps.getName());
        DocPrintJob job = ps.createPrintJob();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(INIT);
            processReceiptText(receiptText, out);
            out.write(new byte[]{'\n', '\n', '\n', '\n'});
            out.write(CUT_PAPER);

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(out.toByteArray(), flavor, null);
            job.print(doc, null);
            System.out.println("Receipt printed successfully.");
            return true;
        } catch (PrintException | IOException e) {
            System.err.println("Error printing receipt: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void processReceiptText(String receiptText, ByteArrayOutputStream out) throws IOException {
        String[] lines = receiptText.split("\n");
        for (String line : lines) {
            String textToPrint = line;

            boolean isCenter = textToPrint.contains("**CENTER**");
            boolean isLarge = textToPrint.contains("**LARGE**");
            boolean isXLarge = textToPrint.contains("**XLARGE**");

            textToPrint = textToPrint.replace("**CENTER**", "")
                    .replace("**LARGE**", "")
                    .replace("**XLARGE**", "");

            if (isCenter) out.write(CENTER_ON);
            if (isXLarge) {
                out.write(DOUBLE_SIZE_ON); // Use the "much bigger" command
            } else if (isLarge) {
                out.write(DOUBLE_HEIGHT_ON); // Use the original "taller" command
            }
            // Add bold for both large sizes
            if (isLarge || isXLarge) {
                out.write(BOLD_ON);
            }

            out.write(textToPrint.getBytes(StandardCharsets.UTF_8));
            out.write(LINE_FEED);

            if (isLarge || isXLarge) {
                out.write(BOLD_OFF);
                out.write(NORMAL_TEXT_SIZE); // Reset text size to normal
            }
            if (isCenter) out.write(CENTER_OFF);
        }
    }
}