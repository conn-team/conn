package com.github.connteam.conn.client.app;

import java.io.FileInputStream;
import java.io.InputStream;

import com.github.connteam.conn.client.app.model.Session;
import com.github.connteam.conn.core.LoggingUtil;
import com.github.connteam.conn.core.crypto.SSLUtil;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javafx.application.Application;

public class Main {
    @Option(name = "-help", usage = "print help")
    private boolean printHelp = false;

    @Option(name = "-debug", usage = "enable debug logging")
    private boolean debugLogs = false;

    @Option(name = "-host", usage = "server host")
    private String host = "localhost";

    @Option(name = "-port", usage = "server port")
    private int port = 7312;

    @Option(name = "-jks", usage = "trusted certificates store (JKS format)")
    private String trustStorePath = "";

    @Option(name = "-jks-password", usage = "trusted certificates store (JKS format)")
    private String trustStorePassword = "";

    public void run(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        if (printHelp) {
            parser.printUsage(System.err);
            return;
        }

        LoggingUtil.setupLogging(debugLogs);

        Session.setHost(host);
        Session.setPort(port);

        if (trustStorePath.length() > 0) {
            try (InputStream in = new FileInputStream(trustStorePath)) {
                SSLUtil.setKeyStore(in, trustStorePassword);
            }
        } else {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("default_keystore")) {
                SSLUtil.setKeyStore(in, "password");
            }
        }

        Application.launch(App.class, args);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
