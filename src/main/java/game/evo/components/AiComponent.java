package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Stores data related to an entity's AI behavior.
 */
public class AiComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 

    public enum BehaviorType {
        STATIC,
        PATROL_HORIZONTAL,
        PATROL_VERTICAL,
        WANDER_RANDOM,
        CHASE_PLAYER
    }

    public BehaviorType behavior;

    // --- Properties for Patrol behavior ---
    public int patrolRange;
    public int initialColumn;   // The initial column where the patrol started
    public int initialRow;      // << CAMPO ADICIONADO AQUI
    public boolean movingRight = true; // Current direction for horizontal patrol
    public boolean movingDown = true;  // Current direction for vertical patrol

    // --- Properties for timing ---
    public long lastMoveTime;
    public long moveDelay;

    /**
     * Constructor for an AI Component.
     * @param behavior The type of behavior for this entity.
     * @param moveDelay The delay in milliseconds between moves.
     */
    public AiComponent(BehaviorType behavior, long moveDelay) {
        this.behavior = behavior;
        this.moveDelay = moveDelay;
        this.lastMoveTime = System.currentTimeMillis();
    }
}