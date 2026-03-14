

import server.MovieServer;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        new MovieServer(8080).start();
        System.out.println("MovieHub server running on http://localhost:8080");
    }
}