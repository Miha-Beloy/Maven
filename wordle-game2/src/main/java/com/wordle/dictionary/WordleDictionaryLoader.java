package main.java.com.wordle.dictionary;

// WordleDictionaryLoader.java

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class WordleDictionaryLoader {
    public WordleDictionary loadDictionary(String filename) {
        WordleDictionary dictionary = new WordleDictionary();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (word.length() == 5) {
                    dictionary.addWord(word);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки словаря: " + e.getMessage());
        }

        return dictionary;
    }
}