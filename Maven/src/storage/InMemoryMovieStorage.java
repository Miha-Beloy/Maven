package storage;

import model.Movie;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryMovieStorage implements MovieStorage {
    private final Map<Long, Movie> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Movie> getAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public Optional<Movie> getById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Movie add(Movie movie) {
        long newId = idGenerator.getAndIncrement();
        Movie newMovie = new Movie(newId, movie.getTitle(), movie.getYear());
        storage.put(newId, newMovie);
        return newMovie;
    }

    @Override
    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }

    @Override
    public List<Movie> getByYear(int year) {
        return storage.values().stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }
}