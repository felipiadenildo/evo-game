package game.evo.utils;

import java.io.Serializable;

/**
 * A simple, immutable data record to represent a grid coordinate (row, column).
 * This class is a utility for passing coordinate pairs.
 *
 * Records automatically provide a constructor, getters (e.g., pos.row()),
 * equals(), hashCode(), and toString() methods.
 */
public record GridPosition(int row, int column) implements Serializable {
    private static final long serialVersionUID = 1L;
    // By implementing Serializable, we ensure this can be part of any object state we save to a file.
}
