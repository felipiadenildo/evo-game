package game.evo.systems;

import game.evo.components.ActivatingPortalComponent;
import game.evo.components.GoToNextLevelComponent;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.utils.GameConstants;

import java.util.Set;

/**
 * Sistema que gerencia a ativação de portais.
 * Aplica um delay antes de acionar a transição de nível.
 */
public class PortalSystem extends GameSystem {

    public PortalSystem(World world) {
        super(world);
    }

    @Override
    public void update() {
        Set<Entity> entities = world.getEntitiesWithComponent(ActivatingPortalComponent.class);
        if (entities.isEmpty()) {
            return;
        }
        
        float deltaTime = GameConstants.GAME_LOOP_DELAY_MS / 1000.0f;
        Entity entity = entities.iterator().next(); // Supõe que apenas o jogador pode ativar portais

        ActivatingPortalComponent portalActivation = world.getComponent(entity, ActivatingPortalComponent.class);
        portalActivation.activationTimer -= deltaTime;

        // EFEITOS VISUAIS (OPCIONAL): Aqui você poderia adicionar lógica para fazer a tela tremer
        // ou um som de portal se intensificando enquanto o timer corre.

        if (portalActivation.activationTimer <= 0) {
            // O tempo acabou, remove o componente de ativação
            world.removeComponent(entity, ActivatingPortalComponent.class);
            // E finalmente adiciona o componente que realmente muda o nível
            world.addComponent(entity, new GoToNextLevelComponent());
            System.out.println("[PortalSystem] Portal activation complete. Triggering next level.");
        }
    }
}