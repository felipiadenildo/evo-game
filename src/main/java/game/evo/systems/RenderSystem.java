package game.evo.systems;

import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.*;
import game.evo.utils.AssetManager;
import game.evo.utils.GameConstants;
import game.evo.utils.SpriteGenerator;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * RenderSystem is responsible for drawing all visual entities.
 */
public class RenderSystem extends GameSystem {

    private final SpriteGenerator spriteGenerator;

    public RenderSystem(World world) {
        super(world);
        this.spriteGenerator = new SpriteGenerator();
    }

    @Override
    public void update() {
        /* Rendering logic is driven by the Swing paint cycle, not the game logic loop. */
    }

    /**
     * The main drawing method. NOTE: To draw notifications, this method now
     * requires the screen width. You must update the call in your GamePanel's
     * paintComponent method to pass this value. Example:
     * renderSystem.update(g2d, cameraX, cameraY, this.getWidth());
     */
    public void update(Graphics2D g2d, int cameraX, int cameraY, int screenWidth) {
        if (world == null || g2d == null) {
            System.err.println("[ERROR RenderSystem] World or Graphics2D is null.");
            return;
        }

        List<Entity> entitiesToRender = getSortedRenderableEntities();

        // 1. Draw all normal game entities
        for (Entity entity : entitiesToRender) {
            drawEntity(g2d, entity, cameraX, cameraY);
        }

        // 2. Draw notifications on top of everything
        drawHUD(g2d, screenWidth);
        drawNotifications(g2d, screenWidth);

        // 3. If debug mode is on, draw visual overlays on top of notifications
        if (GameConstants.DEBUG_MODE_ON) {
            drawDebugOverlays(g2d, entitiesToRender, cameraX, cameraY);
        }
    }

    // drawEntity, createTransformForEntity, drawDebugOverlays, getImageForEntity, etc.
    // ...
    // Adicione este novo método dentro da classe RenderSystem.java
    /**
     * Desenha a Interface do Usuário (HUD) com informações do jogador.
     */
    private void drawHUD(Graphics2D g, int screenWidth) {
        // 1. Encontrar a entidade do jogador
        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) {
            return; // Sai se não houver jogador na tela
        }
        Entity player = playerEntities.iterator().next();

        // 2. Pegar o componente de status do jogador
        StatusComponent status = world.getComponent(player, StatusComponent.class);
        if (status == null) {
            return; // Sai se o jogador não tiver status
        }

        // --- Início do Desenho da HUD ---
        // 3. Barra de Vida
        int healthBarX = 15;
        int healthBarY = 15;
        int healthBarWidth = 200;
        int healthBarHeight = 25;

        // Fundo da barra
        g.setColor(new Color(60, 0, 0, 200)); // Vermelho escuro, semi-transparente
        g.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // Vida atual (calcula a proporção)
        double healthPercentage = (double) status.health / status.maxHealth;
        int currentHealthWidth = (int) (healthBarWidth * healthPercentage);

        g.setColor(new Color(0, 200, 50, 220)); // Verde, semi-transparente
        g.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);

        // Borda da barra
        g.setColor(Color.WHITE);
        g.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // Texto da vida (ex: "80 / 100")
        g.setFont(new Font("Arial", Font.BOLD, 14));
        String healthText = status.health + " / " + status.maxHealth;
        int textWidth = g.getFontMetrics().stringWidth(healthText);
        g.drawString(healthText, healthBarX + (healthBarWidth - textWidth) / 2, healthBarY + 18);

        // 4. Vidas restantes
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String livesText = "Vidas: " + status.lives;
        g.drawString(livesText, 15, 60);

        // Você pode adicionar mais informações aqui (pontos, era atual, etc.)
    }

    /**
     * NEW METHOD: Draws any active notifications on the screen.
     */
    private void drawNotifications(Graphics2D g, int screenWidth) {
        Set<Entity> entities = world.getEntitiesWithComponent(NotificationComponent.class);
        if (entities.isEmpty()) {
            return;
        }

        NotificationComponent notification = world.getComponent(entities.iterator().next(), NotificationComponent.class);
        if (notification == null) {
            return;
        }

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);

        int messageWidth = g.getFontMetrics().stringWidth(notification.message);
        int x = (screenWidth - messageWidth) / 2;
        int y = 50; // Fixed Y position from the top

        g.drawString(notification.message, x, y);
    }

    // --- Resto dos seus métodos Helper ---
    private void drawEntity(Graphics2D g2d, Entity entity, int cameraX, int cameraY) {
        PositionComponent position = world.getComponent(entity, PositionComponent.class);
        if (position == null) {
            return;
        }

        Image imageToDraw = getImageForEntity(entity);
        if (imageToDraw == null) {
            return;
        }

        AffineTransform oldTransform = g2d.getTransform();
        try {
            AffineTransform tx = createTransformForEntity(entity, position, cameraX, cameraY);
            g2d.transform(tx);
            g2d.drawImage(imageToDraw, 0, 0, null);
        } finally {
            g2d.setTransform(oldTransform);
        }
    }

    private AffineTransform createTransformForEntity(Entity entity, PositionComponent position, int cameraX, int cameraY) {
        int width = getEntityRenderWidth(entity);
        int height = getEntityRenderHeight(entity);

        int offsetX = (GameConstants.CELL_SIZE - width) / 2;
        int offsetY = (GameConstants.CELL_SIZE - height) / 2;

        int screenX = (position.column * GameConstants.CELL_SIZE) + offsetX - cameraX;
        int screenY = (position.row * GameConstants.CELL_SIZE) + offsetY - cameraY;

        AffineTransform tx = new AffineTransform();
        tx.translate(screenX, screenY);

        DirectionComponent direction = world.getComponent(entity, DirectionComponent.class);
        if (direction != null) {
            double rotationAngle = getRotationForDirection(direction.facing);
            tx.rotate(rotationAngle, width / 2.0, height / 2.0);
        }

        ProceduralSpriteComponent psc = world.getComponent(entity, ProceduralSpriteComponent.class);
        if (psc != null && psc.isMoving) {
            double time = (System.currentTimeMillis() - psc.createdAtTime) / 150.0;
            double scaleFactor = 0.08 * Math.sin(time);
            double scaleX = 1.0 + scaleFactor;
            double scaleY = 1.0 - scaleFactor;

            tx.translate(width / 2.0, height / 2.0);
            tx.scale(scaleX, scaleY);
            tx.translate(-width / 2.0, -height / 2.0);
        }

        return tx;
    }

    private void drawDebugOverlays(Graphics2D g2d, List<Entity> entities, int cameraX, int cameraY) {
        g2d.setColor(new Color(255, 255, 255, 50));
        int startCol = cameraX / GameConstants.CELL_SIZE;
        int startRow = cameraY / GameConstants.CELL_SIZE;
        int endCol = startCol + GameConstants.SCREEN_WIDTH_TILES + 1;
        int endRow = startRow + GameConstants.SCREEN_HEIGHT_TILES + 1;

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                g2d.drawRect(c * GameConstants.CELL_SIZE - cameraX, r * GameConstants.CELL_SIZE - cameraY, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
            }
        }

        for (Entity entity : entities) {
            PositionComponent position = world.getComponent(entity, PositionComponent.class);
            if (position == null) {
                continue;
            }

            int entityScreenX = position.column * GameConstants.CELL_SIZE - cameraX;
            int entityScreenY = position.row * GameConstants.CELL_SIZE - cameraY;

            if (world.hasComponent(entity, PlayerControlledComponent.class)) {
                g2d.setColor(Color.CYAN);
                g2d.drawRect(entityScreenX, entityScreenY, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);

                DirectionComponent dir = world.getComponent(entity, DirectionComponent.class);
                if (dir != null) {
                    int targetCol = position.column;
                    int targetRow = position.row;
                    switch (dir.facing) {
                        case UP:
                            targetRow--;
                            break;
                        case DOWN:
                            targetRow++;
                            break;
                        case LEFT:
                            targetCol--;
                            break;
                        case RIGHT:
                            targetCol++;
                            break;
                    }
                    g2d.setColor(Color.RED);
                    g2d.drawRect(targetCol * GameConstants.CELL_SIZE - cameraX, targetRow * GameConstants.CELL_SIZE - cameraY, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
                }
            } else if (world.hasComponent(entity, NpcComponent.class)) {
                g2d.setColor(Color.ORANGE);
                g2d.drawRect(entityScreenX, entityScreenY, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
            }
        }
    }

    private Image getImageForEntity(Entity entity) {
        ProceduralSpriteComponent psc = world.getComponent(entity, ProceduralSpriteComponent.class);
        if (psc != null) {
            int frame = (int) ((System.currentTimeMillis() - psc.createdAtTime) / 200) % 2;
            psc.animationFrame = frame;
            String cacheKey = "proc_S" + psc.creatureSeed + "_F" + frame;
            Image cachedImage = AssetManager.getInstance().getImage(cacheKey);
            if (cachedImage == null) {
                BufferedImage newSprite = spriteGenerator.generate(psc);
                AssetManager.getInstance().cacheImage(cacheKey, newSprite);
                return newSprite;
            }
            return cachedImage;
        }
        RenderableComponent rc = world.getComponent(entity, RenderableComponent.class);
        if (rc != null && rc.imagePath != null) {
            return AssetManager.getInstance().getImage(rc.imagePath);
        }
        return null;
    }

    private List<Entity> getSortedRenderableEntities() {
        Set<Entity> fromRenderable = world.getEntitiesWithComponent(RenderableComponent.class);
        Set<Entity> fromProcedural = world.getEntitiesWithComponent(ProceduralSpriteComponent.class);
        Set<Entity> allVisuals = new HashSet<>(fromRenderable);
        allVisuals.addAll(fromProcedural);
        List<Entity> entityList = new ArrayList<>();
        for (Entity entity : allVisuals) {
            if (world.hasComponent(entity, PositionComponent.class)) {
                entityList.add(entity);
            }
        }
        entityList.sort(Comparator.comparingInt(this::getEntityRenderLayer));
        return entityList;
    }

    private int getEntityRenderLayer(Entity entity) {
        RenderableComponent rc = world.getComponent(entity, RenderableComponent.class);
        if (rc != null) {
            return rc.layer;
        }
        if (world.hasComponent(entity, PlayerControlledComponent.class)) {
            return GameConstants.LAYER_PLAYER;
        }
        if (world.hasComponent(entity, NpcComponent.class)) {
            return GameConstants.LAYER_NPC;
        }
        return GameConstants.LAYER_ENVIRONMENT;
    }

    private int getEntityRenderWidth(Entity entity) {
        ProceduralSpriteComponent psc = world.getComponent(entity, ProceduralSpriteComponent.class);
        if (psc != null) {
            return SpriteGenerator.getWidthFor(psc);
        }
        RenderableComponent rc = world.getComponent(entity, RenderableComponent.class);
        if (rc != null) {
            return rc.width;
        }
        return GameConstants.CELL_SIZE;
    }

    private int getEntityRenderHeight(Entity entity) {
        ProceduralSpriteComponent psc = world.getComponent(entity, ProceduralSpriteComponent.class);
        if (psc != null) {
            return SpriteGenerator.getHeightFor(psc);
        }
        RenderableComponent rc = world.getComponent(entity, RenderableComponent.class);
        if (rc != null) {
            return rc.height;
        }
        return GameConstants.CELL_SIZE;
    }

    private double getRotationForDirection(DirectionComponent.Direction direction) {
        switch (direction) {
            case RIGHT:
                return Math.toRadians(90);
            case DOWN:
                return Math.toRadians(180);
            case LEFT:
                return Math.toRadians(-90);
            case UP:
            default:
                return 0;
        }
    }
}
