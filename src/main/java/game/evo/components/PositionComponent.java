package game.evo.components;

import game.evo.ecs.Component; // Import the marker interface
import java.io.Serializable;

/**
 * Stores the 2D grid position (row and column) of an entity.
 */
public class PositionComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public int row;    // Corresponds to Y in a typical Cartesian coordinate if row 0 is top
    public int column; // Corresponds to X in a typical Cartesian coordinate if column 0 is left

    /**
     * Constructs a PositionComponent.
     * @param row The initial row (y-coordinate on the grid).
     * @param column The initial column (x-coordinate on the grid).
     */
    public PositionComponent(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override
    public String toString() {
        return "PositionComponent[Row=" + row + ", Col=" + column + "]";
    }
}