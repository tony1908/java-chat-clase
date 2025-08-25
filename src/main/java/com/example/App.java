package com.example;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        String bootstrapServers = "kafka:9092";

        KafkaConsumerService consumerService = new KafkaConsumerService(bootstrapServers);
        KafkaProducerService producerService = new KafkaProducerService(bootstrapServers);

        consumerService.setMessageHandler(message -> {
            WebsocketManager.broadcastMessage(message.getRoom(), message.getMessage());
        });

        consumerService.start();

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        JettyWebSocketServletContainerInitializer.configure(context, (serverletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.addMapping("/websocket/*", ChatWebSocket.class);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumerService.stop();
        }));

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumerService.stop();
        }
    }
}
