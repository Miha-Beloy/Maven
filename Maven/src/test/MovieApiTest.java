package test;


import model.Movie;
import server.ErrorResponse;
import server.MovieServer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MovieApiTest {
    private static final int PORT = 8081;
    private MovieServer server;
    private HttpClient client;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws IOException {
        server = new MovieServer(PORT);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    // --- GET /movies (empty) ---
    @Test
    void getMovies_EmptyList_Returns200WithEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", response.body());
    }

    // --- POST /movies ---
    @Test
    void postMovie_ValidData_Returns201AndMovie() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(""));
        Movie movie = gson.fromJson(response.body(), Movie.class);
        assertNotNull(movie.getId());
        assertEquals("Inception", movie.getTitle());
        assertEquals(2010, movie.getYear());
    }

    @Test
    void postMovie_EmptyTitle_Returns422WithDetails() throws Exception {
        String json = "{\"title\":\"\",\"year\":2010}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_TitleTooLong_Returns422() throws Exception {
        String longTitle = "a".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2010}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains("название должно быть не длиннее 100 символов"));
    }

    @Test
    void postMovie_InvalidYear_BelowMin_Returns422() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":1800}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().get(0).contains("год должен быть между"));
    }

    @Test
    void postMovie_InvalidYear_AboveMax_Returns422() throws Exception {
        int futureYear = Year.now().getValue() + 2;
        String json = "{\"title\":\"Test\",\"year\":" + futureYear + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
    }

    @Test
    void postMovie_WrongContentType_Returns415() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":2000}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, response.statusCode());
    }

    @Test
    void postMovie_InvalidJson_Returns422() throws Exception {
        String invalidJson = "{title:Inception,year:2010}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("Некорректный JSON"));
    }

    // --- GET /movies/{id} ---
    @Test
    void getMovieById_ExistingId_Returns200AndMovie() throws Exception {
        long id = addMovieAndGetId("Test", 2000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/" + id))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Movie fetched = gson.fromJson(response.body(), Movie.class);
        assertEquals(id, fetched.getId());
    }

    @Test
    void getMovieById_NotFound_Returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/999"))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals("Фильм не найден", response.body());
    }

    @Test
    void getMovieById_InvalidId_Returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/abc"))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertEquals("Некорректный ID", response.body());
    }

    // --- DELETE /movies/{id} ---
    @Test
    void deleteMovie_ExistingId_Returns204() throws Exception {
        long id = addMovieAndGetId("ToDelete", 2020);

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/" + id))
                .DELETE().build();
        HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, deleteResponse.statusCode());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/" + id))
                .GET().build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getResponse.statusCode());
    }

    @Test
    void deleteMovie_NotFound_Returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/999"))
                .DELETE().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteMovie_InvalidId_Returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies/abc"))
                .DELETE().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    // --- GET /movies?year=YYYY ---
    @Test
    void getMoviesByYear_ValidYear_ReturnsMovies() throws Exception {
        addMovie("M1", 2000);
        addMovie("M2", 2001);
        addMovie("M3", 2000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies?year=2000"))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type movieListType = new TypeToken<List<Movie>>(){}.getType();
        List<Movie> movies = gson.fromJson(response.body(), movieListType);
        assertEquals(2, movies.size());
    }

    @Test
    void getMoviesByYear_NoMoviesForYear_ReturnsEmptyList() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies?year=3000"))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void getMoviesByYear_InvalidYear_Returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies?year=abc"))
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка запроса", error.getError());
        assertTrue(error.getDetails().get(0).contains("Некорректный параметр запроса"));
    }

    // --- Unsupported method ---
    @Test
    void unsupportedMethod_Returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    // --- Helper methods ---
    private long addMovieAndGetId(String title, int year) throws Exception {
        String json = "{\"title\":\"" + title + "\",\"year\":" + year + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Movie movie = gson.fromJson(response.body(), Movie.class);
        return movie.getId();
    }

    private void addMovie(String title, int year) throws Exception {
        String json = "{\"title\":\"" + title + "\",\"year\":" + year + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}