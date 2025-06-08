package game.evo.components;

import game.evo.ecs.Component;
import game.evo.utils.GameConstants;
import java.awt.Color; // For color-based rendering
import java.io.Serializable;

/**
 * Stores data necessary for rendering an entity.
 * Can hold an image path or a fallback color.
 */
public class RenderableComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public String imagePath;
    public Color color;
    public int width;
    public int height;
    public int layer;

    /**
     * Constructor for an image-based renderable.
     * @param imagePath Path to the image.
     * @param width Desired width.
     * @param height Desired height.
     * @param layer Drawing layer.
     */
    public RenderableComponent(String imagePath, int layer) {
        this.imagePath = imagePath;
        this.color = null;
        this.width = GameConstants.CELL_SIZE;
        this.height = GameConstants.CELL_SIZE;
        this.layer = layer;
    }

    /**
     * Constructor for a color-based renderable.
     * @param color The color to render the entity with.
     * @param width Desired width.
     * @param height Desired height.
     * @param layer Drawing layer.
     */
    public RenderableComponent(Color color, int width, int height, int layer) {
        this.imagePath = null;
        this.color = color;
        this.width = width;
        this.height = height;
        this.layer = layer;
    }

    @Override
    public String toString() {
        return "RenderableComponent[" +
               (imagePath != null ? "imagePath='" + imagePath + '\'' : "color=" + color) +
               ", width=" + width +
               ", height=" + height +
               ", layer=" + layer +
               ']';
    }
}