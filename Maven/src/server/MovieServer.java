package server;

import storage.InMemoryMovieStorage;
import storage.MovieStorage;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MovieServer {
    private final int port;
    private HttpServer server;
    private final MovieStorage storage = new InMemoryMovieStorage();

    public MovieServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/movies", new MoviesHandler(storage));
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}