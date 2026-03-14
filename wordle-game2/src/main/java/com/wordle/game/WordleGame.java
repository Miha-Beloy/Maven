package main.java.com.wordle.game;

// WordleGame.java

import main.java.com.wordle.dictionary.WordleDictionary;
import java.util.*;

public class WordleGame {
    private final WordleDictionary dictionary;
    private final String secretWord;
    private int attemptsLeft;
    private boolean gameOver;
    private boolean win;
    private final List<String> guesses;

    public WordleGame(WordleDictionary dictionary) {
        this.dictionary = dictionary;
        this.guesses = new ArrayList<>();
        this.attemptsLeft = 6;
        this.gameOver = false;
        this.win = false;
        this.secretWord = selectRandomWord();
    }

    private String selectRandomWord() {
        Set<String> words = dictionary.getWords();
        if (words.isEmpty()) {
            throw new IllegalStateException("Словарь пуст");
        }
        List<String> wordList = new ArrayList<>(words);
        Random random = new Random();
        return wordList.get(random.nextInt(wordList.size()));
    }

    public static class GameResult {
        public final String hint;
        public final int attemptsLeft;
        public final boolean isWin;
        public final boolean isGameOver;

        public GameResult(String hint, int attemptsLeft, boolean isWin, boolean isGameOver) {
            this.hint = hint;
            this.attemptsLeft = attemptsLeft;
            this.isWin = isWin;
            this.isGameOver = isGameOver;
        }
    }

    public GameResult makeGuess(String guess) {
        if (gameOver) {
            throw new IllegalStateException("Игра уже завершена");
        }

        if (guess.length() != 5) {
            throw new IllegalArgumentException("Слово должно состоять из 5 букв");
        }

        if (!dictionary.contains(guess)) {
            throw new IllegalArgumentException("Слово отсутствует в словаре");
        }

        guesses.add(guess);
        attemptsLeft--;

        if (guess.equalsIgnoreCase(secretWord)) {
            win = true;
            gameOver = true;
            return new GameResult(generateHint(guess), attemptsLeft, true, true);
        }

        if (attemptsLeft <= 0) {
            gameOver = true;
            return new GameResult(generateHint(guess), 0, false, true);
        }

        return new GameResult(generateHint(guess), attemptsLeft, false, false);
    }

    private String generateHint(String guess) {
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char guessChar = guess.charAt(i);
            char secretChar = secretWord.charAt(i);

            if (guessChar == secretChar) {
                hint.append("[").append(guessChar).append("]");
            } else if (secretWord.contains(String.valueOf(guessChar))) {
                hint.append("(").append(guessChar).append(")");
            } else {
                hint.append(" ").append(guessChar).append(" ");
            }
        }
        return hint.toString();
    }

    public String getHint() {
        if (guesses.isEmpty()) {
            return "Сделайте первую попытку";
        }

        String lastGuess = guesses.get(guesses.size() - 1);
        return generateHint(lastGuess);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }
}