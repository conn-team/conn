package com.github.connteam.conn.client.app;

import com.github.connteam.conn.client.app.model.Session;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javafx.application.Application;

public class Main {
    @Option(name = "-help", usage = "print help")
    private boolean printHelp = false;

    @Option(name = "-host", usage = "server host")
    private String host = "localhost";

    @Option(name = "-port", usage = "server port")
    private int port = 7312;

    public void run(String[] args) {
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

        Session.setHost(host);
        Session.setPort(port);

        Application.launch(App.class, args);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
