package server;

import model.Movie;
import storage.MovieStorage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

public class MoviesHandler implements HttpHandler {
    private final MovieStorage storage;
    private final Gson gson = new Gson();

    public MoviesHandler(MovieStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && "/movies".equals(path)) {
            handleGetAll(exchange);
        } else if ("GET".equals(method) && path.startsWith("/movies/")) {
            handleGetById(exchange, path.substring("/movies/".length()));
        } else if ("POST".equals(method) && "/movies".equals(path)) {
            handlePost(exchange);
        } else if ("DELETE".equals(method) && path.startsWith("/movies/")) {
            handleDelete(exchange, path.substring("/movies/".length()));
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = queryToMap(query);

        if (params.containsKey("year")) {
            String yearStr = params.get("year");
            try {
                int year = Integer.parseInt(yearStr);
                List<Movie> movies = storage.getByYear(year);
                sendResponse(exchange, 200, movies);
            } catch (NumberFormatException e) {
                ErrorResponse error = new ErrorResponse("Ошибка запроса",
                        List.of("Некорректный параметр запроса — 'year'"));
                sendResponse(exchange, 400, error);
            }
        } else {
            sendResponse(exchange, 200, storage.getAll());
        }
    }

    private void handleGetById(HttpExchange exchange, String idStr) throws IOException {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(400, "Некорректный ID".getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("Некорректный ID".getBytes());
            }
            return;
        }

        Optional<Movie> movie = storage.getById(id);
        if (movie.isPresent()) {
            sendResponse(exchange, 200, movie.get());
        } else {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, "Фильм не найден".getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("Фильм не найден".getBytes());
            }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            exchange.sendResponseHeaders(415, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException e) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации", List.of("Некорректный JSON"));
            sendResponse(exchange, 422, error);
            return;
        }

        List<String> errors = validateMovie(movie);
        if (!errors.isEmpty()) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации", errors);
            sendResponse(exchange, 422, error);
            return;
        }

        Movie created = storage.add(movie);
        sendResponse(exchange, 201, created);
    }

    private void handleDelete(HttpExchange exchange, String idStr) throws IOException {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        boolean deleted = storage.deleteById(id);
        if (deleted) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название должно быть не длиннее 100 символов");
        }
        int currentYear = Year.now().getValue();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и " + (currentYear + 1));
        }
        return errors;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        String response = gson.toJson(data);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }
}