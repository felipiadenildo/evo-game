package game.evo.components;

import game.evo.ecs.Component; // Importa a interface base do seu sistema ECS

/**
 * A component that holds a message to be displayed on the screen for a limited time.
 * It implements the base Component interface to be compatible with the World.
 */
public class NotificationComponent implements Component {
    private static final long serialVersionUID = 1L;

    public String message;
    public float remainingTimeSeconds; // How long the message should stay on screen

    public NotificationComponent(String message, float durationSeconds) {
        this.message = message;
        this.remainingTimeSeconds = durationSeconds;
    }
}
