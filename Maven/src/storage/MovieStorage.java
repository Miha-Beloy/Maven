import model.Movie;
import java.util.List;
import java.util.Optional;

public interface MovieStorage {
    List<Movie> getAll();
    Optional<Movie> getById(long id);
    Movie add(Movie movie);
    boolean deleteById(long id);
    List<Movie> getByYear(int year);
}