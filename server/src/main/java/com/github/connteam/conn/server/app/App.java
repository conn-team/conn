package com.github.connteam.conn.server.app;

import com.github.connteam.conn.core.LoggingUtil;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.ConnServer;
import com.github.connteam.conn.server.database.provider.DataProvider;
import com.github.connteam.conn.server.database.provider.PostgresDataProvider;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    @Option(name = "-help", usage = "print help")
    private boolean printHelp = false;

    @Option(name = "-debug", usage = "enable debug logging")
    private boolean debugLogs = false;

    @Option(name = "-db-name", usage = "database name")
    private String dbName = "conn";

    @Option(name = "-db-username", usage = "database user name")
    private String dbUsername = "conn";

    @Option(name = "-db-password", usage = "database password")
    private String dbPassword = "";

    @Option(name = "-port", usage = "server port")
    private int port = 7312;

    @Option(name = "-reset-database", usage = "reset database")
    private boolean resetDatabase = false;

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

        try (DataProvider provider = new PostgresDataProvider.Builder().setName(dbName).setUser(dbUsername)
                .setPassword(dbPassword).build()) {

            if (resetDatabase) {
                LOG.info("Dropping tables");
                provider.dropTables();
            }

            LOG.info("Creating tables");
            provider.createTables();

            try (ConnServer server = ConnServer.builder().setPort(port).setTransport(Transport.SSL)
                    .setDataProvider(provider).build()) {
                server.listen();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }
}
