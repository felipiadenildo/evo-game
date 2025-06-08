package game.evo.state;

import game.evo.ecs.World;
import java.io.Serializable;

/**
 * A container class that holds all the data representing the state of a game session.
 * This object is what gets serialized to a file.
 */
public class GameState implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public final int levelNumber;
    public final World world;

    public GameState(int levelNumber, World world) {
        this.levelNumber = levelNumber;
        this.world = world;
    }
}