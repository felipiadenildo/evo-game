package game.evo.systems;

import game.evo.Main;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.*;
import game.evo.config.LevelConfig;
import game.evo.utils.AssetManager;
import game.evo.utils.GameConstants;
import game.evo.utils.SpriteGenerator;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * RenderSystem é responsável por desenhar todos os elementos visuais do jogo.
 * REFATORADO: Agora gerencia múltiplos estados de renderização (Carregamento,
 * Introdução, Jogo).
 */
public class RenderSystem extends GameSystem {

    private final SpriteGenerator spriteGenerator;

    // --- Variáveis de Estado para Renderização ---
    private Main.LoadingPhase currentPhase = Main.LoadingPhase.COMPLETE; // Inicia em um estado seguro
    private LevelConfig levelConfig;

    public RenderSystem(World world) {
        super(world);
        this.spriteGenerator = new SpriteGenerator();
    }

    /**
     * ADICIONE ESTE MÉTODO OBRIGATÓRIO Este método é exigido pela classe pai
     * GameSystem para todas as suas classes filhas. Como a nossa lógica de
     * renderização principal está no outro método update(), este pode ficar
     * vazio. Sua única função é satisfazer o contrato da herança.
     */
    @Override
    public void update() {
        // Intencionalmente vazio.
    }

    // --- Setters para o Main controlar o estado ---
    public void setLoadingPhase(Main.LoadingPhase phase) {
        this.currentPhase = phase;
    }

    public void setLevelConfig(LevelConfig config) {
        this.levelConfig = config;
    }

    public LevelConfig getLevelConfig() {
        return this.levelConfig;
    } // Getter para o Main usar

    /**
     * Ponto de entrada principal da renderização, chamado a cada frame pelo
     * GamePanel. Ele delega o trabalho para o método de desenho apropriado com
     * base no estado atual do jogo.
     */
    public void update(Graphics2D g2d, int cameraX, int cameraY, int screenWidth, int screenHeight) {
        // Habilita dicas de renderização para textos e formas mais suaves
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // O deltaTime é necessário para a animação de notificação
        float deltaTime = GameConstants.GAME_LOOP_DELAY_MS / 1000.0f;

        switch (currentPhase) {
            case SHOWING_LOADING_SCREEN:
                drawLoadingScreen(g2d, screenWidth, (levelConfig != null) ? levelConfig.levelName : "...");
                break;
            case SHOWING_LEVEL_INTRO:
                if (levelConfig != null) {
                    drawIntroScreen(g2d, screenWidth, levelConfig.levelName, levelConfig.description);
                }
                break;
            default: // Inclui MAP_LOADED, PLAYER_SPAWNED, WORLD_POPULATED, COMPLETE
                drawGameWorld(g2d, cameraX, cameraY, screenWidth, screenHeight, deltaTime);
                break;
        }
    }

    /**
     * Desenha a tela de carregamento com o nome do nível.
     */
    private void drawLoadingScreen(Graphics2D g, int screenWidth, String levelName) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screenWidth, GameConstants.SCREEN_HEIGHT_TILES * GameConstants.CELL_SIZE);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        String text = "Loading: " + levelName;
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (screenWidth - metrics.stringWidth(text)) / 2, 300);
    }

    /**
     * Desenha a tela de introdução do nível com título, descrição e um aviso
     * para começar.
     */
    private void drawIntroScreen(Graphics2D g, int screenWidth, String title, String description) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screenWidth, GameConstants.SCREEN_HEIGHT_TILES * GameConstants.CELL_SIZE);

        // Desenha o Título
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(Color.WHITE);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(title, (screenWidth - metrics.stringWidth(title)) / 2, 150);

        // Desenha a Descrição
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        metrics = g.getFontMetrics();
        // Lógica simples para quebrar a linha da descrição
        int y = 220;
        for (String line : description.split("\n")) {
            g.drawString(line, (screenWidth - metrics.stringWidth(line)) / 2, y);
            y += metrics.getHeight();
        }

        // Desenha o "Pressione Enter" piscando
        // Usa o tempo do sistema para alternar a visibilidade a cada meio segundo
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            g.setFont(new Font("Arial", Font.BOLD, 22));
            metrics = g.getFontMetrics();
            String startText = "Press Enter to Evolve";
            g.drawString(startText, (screenWidth - metrics.stringWidth(startText)) / 2, 450);
        }
    }

    /**
     * Desenha o mundo do jogo principal, incluindo entidades, HUD e
     * notificações.
     */
    private void drawGameWorld(Graphics2D g2d, int cameraX, int cameraY, int screenWidth, int screenHeight, float deltaTime) {
        if (world == null || g2d == null) {
            System.err.println("[ERROR RenderSystem] World or Graphics2D is null.");
            return;
        }

        List<Entity> entitiesToRender = getSortedRenderableEntities();

        // 1. Desenha todas as entidades normais do jogo
        for (Entity entity : entitiesToRender) {
            drawEntity(g2d, entity, cameraX, cameraY);
        }

        // 2. Desenha o HUD e as Notificações por cima de tudo
        drawHUD(g2d, screenWidth, screenHeight);
        drawNotifications(g2d, screenWidth, deltaTime); // Passa o deltaTime para a animação

        // 3. Se o modo debug estiver ativo, desenha as sobreposições visuais
        if (GameConstants.DEBUG_MODE_ON) {
            drawDebugOverlays(g2d, entitiesToRender, cameraX, cameraY);
        }
    }

// Em: src/main/java/game/evo/systems/RenderSystem.java
    /**
     * Desenha a nova HUD focada no jogador na parte inferior da tela. VERSÃO DE
     * DEBUG: Adicionadas mensagens no console para diagnosticar problemas.
     */
    private void drawHUD(Graphics2D g, int screenWidth, int screenHeight) {
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG HUD] Iniciando drawHUD...");
//        }

        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG HUD] ERRO: Nenhuma entidade de jogador encontrada. Saindo do drawHUD.");
//            }
            return;
        }
        Entity player = playerEntities.iterator().next();

        // Pega todos os componentes necessários do jogador e verifica um por um
        StatusComponent status = world.getComponent(player, StatusComponent.class);

        if (status == null) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG HUD] ERRO: StatusComponent NULO. Saindo do drawHUD.");
//            }
            return;
        }
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG HUD] StatusComponent: ENCONTRADO.");
//        }

        EcologyComponent ecology = world.getComponent(player, EcologyComponent.class);
        if (ecology == null) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG HUD] ERRO: EcologyComponent NULO. Saindo do drawHUD.");
//            }
            return;
        }
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG HUD] EcologyComponent: ENCONTRADO.");
//        }

        SizeComponent size = world.getComponent(player, SizeComponent.class);
        if (size == null) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG HUD] ERRO: SizeComponent NULO. Saindo do drawHUD.");
//            }
            return;
        }
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG HUD] SizeComponent: ENCONTRADO.");
//        }

        ProceduralSpriteComponent psc = world.getComponent(player, ProceduralSpriteComponent.class);
        if (psc == null) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG HUD] ERRO: ProceduralSpriteComponent NULO. Saindo do drawHUD.");
//            }
            return;
        }
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG HUD] ProceduralSpriteComponent: ENCONTRADO.");
//        }
//
//        // Se chegou até aqui, todos os componentes existem.
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG HUD] Todos os componentes encontrados. Desenhando a HUD...");
//        }

        int hudHeight = 95;
        // CORRIGIDO: Usa a screenHeight recebida como parâmetro
        int hudY = screenHeight - hudHeight; 

        // --- Desenha o Painel de Fundo ---
        g.setColor(new Color(15, 20, 30, 210));
        g.fillRect(0, hudY, screenWidth, hudHeight);
        g.setColor(new Color(80, 150, 255, 200));
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawLine(0, hudY, screenWidth, hudY);

        // --- LADO ESQUERDO: Retrato da Criatura ---
        int portraitBoxSize = 80;
        int portraitX = 10;
        int portraitY = hudY + (hudHeight - portraitBoxSize) / 2;

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(portraitX, portraitY, portraitBoxSize, portraitBoxSize, 15, 15));

        Image sprite = getImageForEntity(player);
        if (sprite != null) {
            g.drawImage(sprite, portraitX + 5, portraitY + 5, portraitBoxSize - 10, portraitBoxSize - 10, null);
        }

        g.setColor(new Color(80, 150, 255, 150));
        g.draw(new RoundRectangle2D.Double(portraitX, portraitY, portraitBoxSize, portraitBoxSize, 15, 15));

        // --- LADO DIREITO: Status e Características ---
        int statsX = portraitX + portraitBoxSize + 20;
        int statsY = hudY + 15;

        int barWidth = 200;
        drawStatBar(g, statsX, statsY, barWidth, "HP", status.health, status.maxHealth, Color.GREEN.darker());
        drawStatBar(g, statsX, statsY + 22, barWidth, "EVO", status.evolutionPoints, GameConstants.EVOLUTION_POINTS_FOR_PORTAL, Color.MAGENTA.darker());

        int infoX = statsX + barWidth + 30;
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Species: " + psc.bodyType.name(), infoX, statsY + 5);
        g.drawString("Diet: " + ecology.diet.name(), infoX, statsY + 25);
        g.drawString("Size: " + size.size, infoX, statsY + 45); // Supondo que o campo seja sizeValue
        g.drawString("ATK / DEF: " + status.attack + " / " + status.defense, infoX, statsY + 65);
    }

    /**
     * NOVO MÉTODO AUXILIAR: Desenha uma única barra de status (HP, EVO, etc.).
     */
    private void drawStatBar(Graphics2D g, int x, int y, int width, String label, int currentValue, int maxValue, Color color) {
        int height = 18;

        // Fundo da barra
        g.setColor(new Color(50, 50, 50));
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 8, 8));

        // Preenchimento da barra
        double percentage = (double) currentValue / maxValue;
        percentage = Math.max(0, Math.min(1, percentage)); // Garante que fique entre 0 e 1
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y, width * percentage, height, 8, 8));

        // Texto (label e valor)
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        g.drawString(label, x + 5, y + 13);
        String valueText = currentValue + "/" + maxValue;
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(valueText, x + width - metrics.stringWidth(valueText) - 5, y + 13);
    }

    /**
     * Desenha notificações ativas com fundo, ícone e animação de fade.
     */
    private void drawNotifications(Graphics2D g, int screenWidth, float deltaTime) {
        Set<Entity> entities = world.getEntitiesWithComponent(NotificationComponent.class);
        if (entities.isEmpty()) {
            return;
        }

        NotificationComponent notification = world.getComponent(entities.iterator().next(), NotificationComponent.class);
        if (notification == null) {
            return;
        }

        float timeAlive = notification.initialDuration - notification.remainingDuration;
        float fadeDuration = 0.4f;
        float alpha = 1.0f;

        if (timeAlive < fadeDuration) {
            alpha = timeAlive / fadeDuration;
        } else if (notification.remainingDuration < fadeDuration) {
            alpha = notification.remainingDuration / fadeDuration;
        }

        alpha = Math.max(0, Math.min(1, alpha));

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        Font font = new Font("Arial", Font.BOLD, 16);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        Image icon = getIconForNotification(notification.type);
        int iconWidth = (icon != null) ? icon.getWidth(null) + 10 : 0;

        int messageWidth = metrics.stringWidth(notification.message);
        int panelWidth = messageWidth + iconWidth + 30;
        int panelHeight = 40;
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = 30;

        g.setColor(new Color(0, 0, 0, 180));
        g.fill(new RoundRectangle2D.Double(panelX, panelY, panelWidth, panelHeight, 20, 20));

        int textY = panelY + (panelHeight - metrics.getHeight()) / 2 + metrics.getAscent();
        if (icon != null) {
            g.drawImage(icon, panelX + 15, panelY + (panelHeight - icon.getHeight(null)) / 2, null);
        }

        g.setColor(Color.WHITE);
        g.drawString(notification.message, panelX + 15 + iconWidth, textY);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private Image getIconForNotification(NotificationComponent.NotificationType type) {
        if (type == null) {
            return null;
        }
        String path = switch (type) {
            case INFO ->
                "assets/imgs/icons/info_icon.png";
            case SUCCESS ->
                "assets/imgs/icons/success_icon.png";
            case WARNING ->
                "assets/imgs/icons/warning_icon.png";
            case COMBAT ->
                "assets/imgs/icons/combat_icon.png";
        };
        return AssetManager.getInstance().getImage(path);
    }

    // --- MÉTODOS AUXILIARES DE RENDERIZAÇÃO (sem alterações) ---
    private List<Entity> getSortedRenderableEntities() {
        Set<Entity> fromRenderable = world.getEntitiesWithComponent(RenderableComponent.class);
        Set<Entity> fromProcedural = world.getEntitiesWithComponent(ProceduralSpriteComponent.class);
        Set<Entity> allVisuals = new HashSet<>(fromRenderable);
        allVisuals.addAll(fromProcedural);
        List<Entity> entityList = new ArrayList<>(allVisuals);
        entityList.sort(Comparator.comparingInt(this::getEntityRenderLayer));
        return entityList;
    }

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
