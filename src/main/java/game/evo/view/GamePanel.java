package game.evo.view;

import game.evo.ecs.World;
import game.evo.systems.RenderSystem;
import game.evo.utils.GameConstants;
import game.evo.world.GameMap;
import game.evo.input.InputManager;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * GamePanel is the custom JPanel where the game is rendered.
 * It draws the pre-rendered map background and then uses the RenderSystem
 * to draw dynamic entities on top.
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final World world;
    private final RenderSystem renderSystem;
    private final GameMap gameMap;

    private int cameraX = 0;
    private int cameraY = 0;

    public GamePanel(World world, GameMap gameMap, RenderSystem renderSystem, InputManager inputManager) {
        if (world == null || gameMap == null || renderSystem == null || inputManager == null) {
            throw new IllegalArgumentException("Nenhum argumento do construtor pode ser nulo para GamePanel.");
        }

        this.world = world;
        this.gameMap = gameMap;
        this.renderSystem = renderSystem;

        int panelWidth = GameConstants.SCREEN_WIDTH_TILES * GameConstants.CELL_SIZE;
        int panelHeight = GameConstants.SCREEN_HEIGHT_TILES * GameConstants.CELL_SIZE;
        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(inputManager);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            // 1. Draw the pre-rendered map background
            BufferedImage mapImage = gameMap.getMapImage();
            if (mapImage != null) {
                // Draw the portion of the map image that the camera is currently viewing
                g2d.drawImage(mapImage,
                        0, 0, getWidth(), getHeight(),
                        cameraX, cameraY, cameraX + getWidth(), cameraY + getHeight(),
                        null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.RED);
                g2d.drawString("Map Image not available!", 50, 50);
            }

            // 2. Delegate the rendering of dynamic entities to the RenderSystem
            // FIX: Pass the panel's width as the fourth argument to match the updated method signature.
            this.renderSystem.update(g2d, cameraX, cameraY, this.getWidth());

        } catch (Exception e) {
            System.err.println("[CRITICAL ERROR in GamePanel.paintComponent] Exception during rendering:");
            e.printStackTrace();
            g2d.setColor(Color.RED);
            g2d.drawString("RENDERING ERROR: " + e.getMessage(), 10, 20);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Updates the camera's position, ensuring it doesn't go outside the map boundaries.
     * @param newCamX The new desired X-coordinate for the camera.
     * @param newCamY The new desired Y-coordinate for the camera.
     */
    public void setCameraPosition(int newCamX, int newCamY) {
        int worldPixelWidth = gameMap.getWidthInTiles() * GameConstants.CELL_SIZE;
        int worldPixelHeight = gameMap.getHeightInTiles() * GameConstants.CELL_SIZE;
        
        int screenPixelWidth = getWidth();
        int screenPixelHeight = getHeight();

        // Calculate the maximum allowed coordinates for the camera's top-left corner
        int maxCamX = Math.max(0, worldPixelWidth - screenPixelWidth);
        int maxCamY = Math.max(0, worldPixelHeight - screenPixelHeight);

        // Clamp the new camera position within the valid bounds [0, max]
        // FIX: The horizontal position (cameraX) should be clamped by maxCamX, not maxCamY.
        this.cameraX = Math.max(0, Math.min(newCamX, maxCamX));
        this.cameraY = Math.max(0, Math.min(newCamY, maxCamY));
    }
}
