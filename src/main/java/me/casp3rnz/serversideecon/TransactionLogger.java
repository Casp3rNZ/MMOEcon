package me.casp3rnz.serversideecon;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class TransactionLogger {
    private static final Logger logger = Logger.getLogger("TransactionLogger");

    static {
        try {
            FileHandler fileHandler = new FileHandler("MMOEconHistory.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        logger.info(message);
    }
}