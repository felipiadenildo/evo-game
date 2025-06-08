package game.evo.components;

import game.evo.ecs.Component;
import java.awt.Color;
import java.io.Serializable;

/**
 * Component holding the "genes" or parameters for procedurally generating a sprite.
 */
public class ProceduralSpriteComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public enum BodyType {
        FINNED_AQUATIC,   // Fish-like creature
        BIPED_TERRESTRIAL,  // Two-legged creature (like a bird or caveman)
        MEAT_CHUNK,
        PORTAL_SPIRAL
    }

    public long creatureSeed; // Seed for this creature's unique shape
    public int size;          // A general size value (e.g., 1 to 10)
    public Color primaryColor;
    public Color secondaryColor; // For details like eyes or fins
    public BodyType bodyType;
    
    // Animation-related fields
    public long createdAtTime;  // Timestamp of creation for animation timing
    public boolean isMoving;     // Is the entity currently moving?
    public int animationFrame; // 0, 1, 2... to cycle through animation frames

    public ProceduralSpriteComponent(long creatureSeed, int size, Color primaryColor, Color secondaryColor, BodyType bodyType) {
        this.creatureSeed = creatureSeed;
        this.size = size;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.bodyType = bodyType;
        this.createdAtTime = System.currentTimeMillis();
        this.isMoving = false;
        this.animationFrame = 0; // Initialize animation frame
    }
}