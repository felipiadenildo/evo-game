package game.evo.view;

import javax.swing.JFrame;
import java.awt.HeadlessException;

/**
 * GameWindow is the main JFrame that holds the GamePanel.
 * It acts as a simple container for the game's primary view component.
 */
public class GameWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private GamePanel gamePanel;

    /**
     * Constructs the main game window.
     * It now receives a pre-constructed GamePanel to display.
     *
     * @param title The title of the game window.
     * @param gamePanel The fully initialized GamePanel to be added to this window.
     * @throws HeadlessException if the environment does not support a GUI.
     */
    public GameWindow(String title, GamePanel gamePanel) throws HeadlessException {
        super(title); // Set the window title

        if (gamePanel == null) {
            throw new IllegalArgumentException("GamePanel cannot be null.");
        }

        // Set the provided panel as the main content
        this.gamePanel = gamePanel;
        this.add(this.gamePanel);

        // Configure window properties
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true); // Allow resizing as requested
        this.pack(); // Size the window to fit the preferred size of its components (the GamePanel)
        this.setLocationRelativeTo(null); // Center the window on the screen
    }

    /**
     * Returns the GamePanel instance currently associated with this window.
     * @return The current GamePanel instance.
     */
    public GamePanel getGamePanel() {
        return gamePanel;
    }

    /**
     * Replaces the current GamePanel with a new one.
     * This is essential for loading a new level without creating a new window.
     * @param newPanel The new GamePanel to display.
     */
    public void switchPanel(GamePanel newPanel) {
        if (newPanel == null) return;

        if (this.gamePanel != null) {
            this.remove(this.gamePanel); // Remove the old panel from the frame
        }
        this.gamePanel = newPanel;
        this.add(this.gamePanel); // Add the new panel

        this.pack(); // Adjust window size if the new panel has a different preferred size
        this.revalidate(); // Re-layout the container
        this.repaint(); // Redraw the window with the new panel
        this.gamePanel.requestFocusInWindow(); // Ensure the new panel can receive keyboard input

        System.out.println("[INFO GameWindow] Switched to a new GamePanel.");
    }

    /**
     * Makes the game window visible and requests focus for the GamePanel.
     * Should be called after all initial setup is complete.
     */
    public void display() {
        this.setVisible(true);
        if (this.gamePanel != null) {
            this.gamePanel.requestFocusInWindow();
        }
        System.out.println("[INFO GameWindow] GameWindow is now visible.");
    }
}