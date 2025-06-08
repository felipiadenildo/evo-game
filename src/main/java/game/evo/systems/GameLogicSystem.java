package game.evo.systems;

import game.evo.config.EntityConfig;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.world.EntityFactory;
import game.evo.components.PlayerControlledComponent;
import game.evo.components.PortalComponent;
import game.evo.components.PositionComponent;
import game.evo.components.StatusComponent;
import game.evo.utils.GameConstants;

import java.util.Set;

/**
 * Manages general game state rules and events, such as checking for
 * level completion and spawning the exit portal.
 */
public class GameLogicSystem extends GameSystem {

    private final EntityFactory entityFactory;
    private boolean isPortalSpawned = false;

    public GameLogicSystem(World world, EntityFactory entityFactory) {
        super(world);
        this.entityFactory = entityFactory;
    }
    
    /**
     * Resets the state of the system for a new level.
     */
    public void reset() {
        this.isPortalSpawned = false;
    }

    @Override
    public void update() {
        // Se o portal já apareceu, não há mais nada a fazer aqui por enquanto
        if (isPortalSpawned) {
            return;
        }

        // Encontra o jogador
        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) return;
        Entity player = playerEntities.iterator().next();
        
        StatusComponent playerStatus = world.getComponent(player, StatusComponent.class);
        if (playerStatus == null) return;
        
        // Verifica se o jogador atingiu os pontos necessários
        if (playerStatus.evolutionPoints >= GameConstants.EVOLUTION_POINTS_FOR_PORTAL) {
            spawnPortal(player);
            isPortalSpawned = true; // Marca que o portal foi criado para não criar de novo
        }
    }

    /**
     * Creates a portal entity at the player's current location.
     * @param player The player entity.
     */
    private void spawnPortal(Entity player) {
        PositionComponent playerPos = world.getComponent(player, PositionComponent.class);
        if (playerPos == null) return;

        System.out.println("[INFO GAME] Player reached evolution goal! Spawning portal...");

        EntityConfig portalConfig = new EntityConfig();
        portalConfig.type = "Portal";
        portalConfig.image = "objects/portal.png"; // Certifique-se que este sprite existe
        portalConfig.row = playerPos.row;
        portalConfig.column = playerPos.column;
        
        entityFactory.createGameEntity(portalConfig);
    }
}