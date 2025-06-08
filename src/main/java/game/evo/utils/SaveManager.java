package game.evo.utils;


import game.evo.state.GameState;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles all file operations related to saving and loading the game state.
 */
public class SaveManager {

    private static final String SAVE_DIRECTORY = "saves";
    private static final String SAVE_FILE_EXTENSION = ".sav";

    public SaveManager() {
        // Ensure the save directory exists when the manager is created
        try {
            Files.createDirectories(Paths.get(SAVE_DIRECTORY));
        } catch (IOException e) {
            System.err.println("[ERROR SaveManager] Could not create save directory: " + e.getMessage());
        }
    }

    /**
     * Generates a filename based on the specified format: level-[number]-<date>-<time>.sav.
     * @param levelNumber The current level number.
     * @return A formatted string for the save file name.
     */
    public String generateSaveFilename(int levelNumber) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return String.format("level-%d-%s%s", levelNumber, now.format(formatter), SAVE_FILE_EXTENSION);
    }

    /**
     * Saves the provided GameState object to a file.
     * @param state The GameState to save.
     * @param filename The name of the file to save to.
     * @return true if saving was successful, false otherwise.
     */
    public boolean saveStateToFile(GameState state, String filename) {
        Path filePath = Paths.get(SAVE_DIRECTORY, filename);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(state);
            System.out.println("[INFO SaveManager] Game state saved successfully to: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("[ERROR SaveManager] Failed to save game state to " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads a GameState object from a specified file.
     * @param filename The name of the file to load.
     * @return The loaded GameState object, or null if loading fails.
     */
    public GameState loadStateFromFile(String filename) {
        Path filePath = Paths.get(SAVE_DIRECTORY, filename);
        if (!Files.exists(filePath)) {
            System.err.println("[ERROR SaveManager] Save file not found: " + filename);
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
            GameState loadedState = (GameState) ois.readObject();
            System.out.println("[INFO SaveManager] Game state loaded successfully from: " + filename);
            return loadedState;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ERROR SaveManager] Failed to load game state from " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Scans the save directory and returns a list of all available save file names.
     * @return A list of strings containing the names of the save files.
     */
    public List<String> getAvailableSaveFiles() {
        try (Stream<Path> stream = Files.list(Paths.get(SAVE_DIRECTORY))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(SAVE_FILE_EXTENSION))
                    .sorted(Collections.reverseOrder()) // Show newest files first
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[ERROR SaveManager] Could not read save directory: " + e.getMessage());
            return Collections.emptyList(); // Return an empty list on error
        }
    }
}
