package game.evo.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Handles loading level configuration data from JSON files.
 */
public class LevelLoader {

    private final Gson gson;

    public LevelLoader() {
        // Creates a new Gson instance for deserializing JSON.
        this.gson = new Gson();
    }

    /**
     * Loads a level configuration from a JSON file located in the project's resources.
     * @param resourcePath The path to the JSON file relative to the resources root
     * (e.g., "assets/levels/level-1.json").
     * @return A LevelConfig object populated with data from the file, or null if loading fails.
     */
    public LevelConfig loadLevelFromResource(String resourcePath) {
        System.out.println("[INFO LevelLoader] Attempting to load level from: " + resourcePath);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("[ERROR LevelLoader] Resource not found: " + resourcePath);
                return null;
            }
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            JsonReader jsonReader = new JsonReader(reader);
            LevelConfig levelConfig = gson.fromJson(jsonReader, LevelConfig.class);
            System.out.println("[INFO LevelLoader] Successfully loaded and parsed level: " + levelConfig.levelName);
            return levelConfig;
        } catch (JsonSyntaxException e) {
            System.err.println("[ERROR LevelLoader] JSON syntax error in file " + resourcePath + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[ERROR LevelLoader] An unexpected error occurred while loading " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
}