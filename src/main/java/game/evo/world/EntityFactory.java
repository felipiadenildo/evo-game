package game.evo.world;

import game.evo.config.EntityConfig;
import game.evo.config.PlayerConfig;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.*;
import game.evo.utils.GameConstants;

import java.awt.Color;
import java.util.Map;
import java.util.Random;

/**
 * Factory class to create game entities from configuration objects. It now uses
 * behavioral archetypes to construct NPCs.
 */
public class EntityFactory {

    private final World world;
    private final Random random = new Random();

    public EntityFactory(World world) {
        this.world = world;
    }

    /**
     * Cria a entidade do jogador com base em sua configuração. A seed da
     * entidade é usada para gerar status e uma forma de sprite determinística.
     */
    public Entity createPlayerCharacter(PlayerConfig config) {
        Entity playerEntity = world.createEntity();

        world.addComponent(playerEntity, new PositionComponent(config.row, config.column));
        world.addComponent(playerEntity, new PlayerControlledComponent());
        world.addComponent(playerEntity, new DirectionComponent());
        world.addComponent(playerEntity, new CollisionComponent());

        int playerSize = getIntFromProperties(config.properties, "size", 5);
        world.addComponent(playerEntity, new SizeComponent(playerSize));

        ProceduralSpriteComponent.BodyType playerBodyType = getBodyTypeFromProperties(config.properties, "bodyType", ProceduralSpriteComponent.BodyType.BIPED_TERRESTRIAL);

        EcologyComponent.DietaryType diet = getDietaryTypeFromProperties(config.properties, "diet", EcologyComponent.DietaryType.OMNIVORE);
        world.addComponent(playerEntity, new EcologyComponent(diet, EcologyComponent.Temperament.NEUTRAL));

        // LÓGICA DE SEED: Usa a seed do JSON, ou gera uma aleatória se não existir
        long entitySeed = getLongFromProperties(config.properties, "seed", random.nextLong());

        // Gera os status usando a seed determinística
        world.addComponent(playerEntity, generateStats(playerSize, playerBodyType, entitySeed, config.lives));

        // Cria o visual procedural usando a seed (para a forma), mas com cores fixas
        world.addComponent(playerEntity, new ProceduralSpriteComponent(
                entitySeed, playerSize, Color.CYAN, Color.WHITE, playerBodyType
        ));

        world.addComponent(playerEntity, new TraitComponent());

        System.out.println("[INFO EntityFactory] PlayerCharacter created from config with seed: " + entitySeed);
        return playerEntity;
    }

    /**
     * Creates a generic game entity based on its configuration 'type', which
     * now represents temperament/behavior.
     */
    public Entity createGameEntity(EntityConfig config) {
        if (config == null || config.type == null || config.type.isEmpty()) {
            return null;
        }

        // The switch now uses generic behavioral types
        switch (config.type) {
            case "SkittishNPC":
                return createBehavioralNPC(config, EcologyComponent.Temperament.SKITTISH);
            case "NeutralNPC":
                return createBehavioralNPC(config, EcologyComponent.Temperament.NEUTRAL);
            case "AggressiveNPC":
                return createBehavioralNPC(config, EcologyComponent.Temperament.AGGRESSIVE);
            case "StaticObject":
                return createStaticObject(config);
            case "FoodItem":
                return createFoodItem(config);
            case "Portal":
                return createPortal(config);
            case "ProceduralScenery": // <-- ADICIONADO
                return createProceduralScenery(config);
            case "CustomNPC":
                // Trata a criatura customizada como um NPC de temperamento neutro por padrão
                return createBehavioralNPC(config, EcologyComponent.Temperament.NEUTRAL);
            default:
                System.err.println("[ERROR EntityFactory] Unknown entity type in config: '" + config.type + "'");
                return null;
        }
    }

    /**
     * Cria um objeto de cenário procedural (árvore, rocha, arbusto) a partir da
     * configuração. Este método lê propriedades do JSON para adicionar
     * componentes como Colisão e Comida, e usa a seed para gerar cores e formas
     * determinísticas.
     */
    private Entity createProceduralScenery(EntityConfig config) {
        Entity sceneryEntity = world.createEntity();

        world.addComponent(sceneryEntity, new PositionComponent(config.row, config.column));

        // LÓGICA DE SEED: Usa a seed do JSON, ou gera uma aleatória se não existir
        long entitySeed = getLongFromProperties(config.properties, "seed", random.nextLong());
        Random rand = new Random(entitySeed);

        boolean isWalkable = getBooleanFromProperties(config.properties, "isWalkable", true);
        boolean isEdible = getBooleanFromProperties(config.properties, "isEdible", false);
        int nutrition = getIntFromProperties(config.properties, "nutrition", 0);
        boolean isPoisonous = getBooleanFromProperties(config.properties, "isPoisonous", false);
        int size = getIntFromProperties(config.properties, "size", 3);
        ProceduralSpriteComponent.BodyType bodyType = getBodyTypeFromProperties(config.properties, "bodyType", ProceduralSpriteComponent.BodyType.BUSH);

        if (!isWalkable) {
            world.addComponent(sceneryEntity, new CollisionComponent());
        }

        if (isEdible) {
            world.addComponent(sceneryEntity, new FoodComponent(nutrition, isPoisonous));
        }

        // GERAÇÃO DE CORES ALEATÓRIAS E NATURAIS PARA O CENÁRIO
        Color primaryColor;
        Color secondaryColor;

        switch (bodyType) {
            case TREE, BUSH -> {
                primaryColor = new Color(30 + rand.nextInt(40), 90 + rand.nextInt(60), 30 + rand.nextInt(40)); // Tons de verde
                secondaryColor = new Color(100 + rand.nextInt(40), 60 + rand.nextInt(20), 15 + rand.nextInt(10)); // Tons de marrom para o tronco
            }
            case FLOWER_PATCH -> {
                primaryColor = new Color(30 + rand.nextInt(40), 90 + rand.nextInt(60), 30 + rand.nextInt(40)); // Verde para a base
                secondaryColor = Color.getHSBColor(rand.nextFloat(), 0.8f, 1.0f); // Flores bem coloridas
            }
            case MUSHROOM_CLUSTER -> {
                primaryColor = Color.getHSBColor(rand.nextFloat(), 0.6f, 0.9f); // Cor do chapéu
                secondaryColor = new Color(220, 210, 200); // Cor do caule
            }
            default -> { // Para ROCHA, CORAL, CACTO, etc.
                primaryColor = Color.getHSBColor(rand.nextFloat(), 0.3f, 0.7f);
                secondaryColor = Color.getHSBColor(rand.nextFloat(), 0.4f, 0.6f);
            }
        }

        world.addComponent(sceneryEntity, new ProceduralSpriteComponent(
                entitySeed, size, primaryColor, secondaryColor, bodyType
        ));

        System.out.println("[INFO EntityFactory] Procedural Scenery '" + bodyType + "' created with seed: " + entitySeed);
        return sceneryEntity;
    }

    /**
     * Cria um NPC genérico com um temperamento definido. Todas as outras
     * propriedades, incluindo cores e forma, são geradas a partir da seed da
     * entidade.
     */
    private Entity createBehavioralNPC(EntityConfig config, EcologyComponent.Temperament temperament) {
        Entity npcEntity = world.createEntity();

        world.addComponent(npcEntity, new PositionComponent(config.row, config.column));
        world.addComponent(npcEntity, new NpcComponent());
        world.addComponent(npcEntity, new DirectionComponent());
        world.addComponent(npcEntity, new CollisionComponent());

        EcologyComponent.DietaryType diet = getDietaryTypeFromProperties(config.properties, "diet", EcologyComponent.DietaryType.HERBIVORE);
        world.addComponent(npcEntity, new EcologyComponent(diet, temperament));

        int npcSize = getIntFromProperties(config.properties, "size", 1);
        world.addComponent(npcEntity, new SizeComponent(npcSize));

        ProceduralSpriteComponent.BodyType npcBodyType = getBodyTypeFromProperties(config.properties, "bodyType", ProceduralSpriteComponent.BodyType.BIPED_TERRESTRIAL);

        // LÓGICA DE SEED: Usa a seed do JSON, ou gera uma aleatória se não existir
        long entitySeed = getLongFromProperties(config.properties, "seed", random.nextLong());
        Random rand = new Random(entitySeed); // Cria um gerador aleatório com a seed da entidade

        // GERAÇÃO DE CORES ALEATÓRIAS
        float hue = rand.nextFloat(); // Sorteia uma matiz (0.0 a 1.0)
        Color primaryColor = Color.getHSBColor(hue, 0.7f, 0.85f);
        Color secondaryColor = Color.getHSBColor(hue, 0.9f, 0.5f);

        // Gera os status usando a seed determinística
        StatusComponent status = generateStats(npcSize, npcBodyType, entitySeed, 1);
        world.addComponent(npcEntity, status);
        world.addComponent(npcEntity, new TraitComponent());

        long moveDelay = 25000 / (long) Math.max(1, status.speed);

        // Passa as cores aleatórias e a seed para o componente do sprite
        world.addComponent(npcEntity, new ProceduralSpriteComponent(
                entitySeed, npcSize, primaryColor, secondaryColor, npcBodyType
        ));

        AiComponent ai = new AiComponent(AiComponent.BehaviorType.WANDER_RANDOM, moveDelay);
        world.addComponent(npcEntity, ai);

        System.out.println("[INFO EntityFactory] Created " + temperament + " " + diet + " NPC with seed: " + entitySeed);
        return npcEntity;
    }

    /**
     * Creates a Portal entity using a procedurally generated sprite.
     */
    private Entity createPortal(EntityConfig config) {
        Entity portalEntity = world.createEntity();

        world.addComponent(portalEntity, new PositionComponent(config.row, config.column));
        world.addComponent(portalEntity, new PortalComponent());

        world.addComponent(portalEntity, new ProceduralSpriteComponent(
                random.nextLong(), GameConstants.CELL_SIZE, Color.BLACK, Color.WHITE,
                ProceduralSpriteComponent.BodyType.PORTAL_SPIRAL
        ));

        System.out.println("[INFO EntityFactory] Procedural Portal created: " + portalEntity);
        return portalEntity;
    }

    /**
     * Creates a Food Item entity.
     */
    private Entity createFoodItem(EntityConfig config) {
        Entity foodEntity = world.createEntity();

        world.addComponent(foodEntity, new PositionComponent(config.row, config.column));

        int nutrition = getIntFromProperties(config.properties, "nutrition", 10);
        int size = getIntFromProperties(config.properties, "size", 1);
        world.addComponent(foodEntity, new FoodComponent(nutrition));

        world.addComponent(foodEntity, new ProceduralSpriteComponent(
                random.nextLong(), size, Color.RED, Color.WHITE,
                ProceduralSpriteComponent.BodyType.MEAT_CHUNK
        ));

        System.out.println("[INFO EntityFactory] Procedural Food Item created: " + foodEntity);
        return foodEntity;
    }

    /**
     * Creates a static object, like a tree or a rock.
     */
    private Entity createStaticObject(EntityConfig config) {
        Entity staticEntity = world.createEntity();
        world.addComponent(staticEntity, new PositionComponent(config.row, config.column));
        world.addComponent(staticEntity, new RenderableComponent(config.image, GameConstants.LAYER_ENVIRONMENT));

        boolean isWalkable = getBooleanFromProperties(config.properties, "isWalkable", false);
        world.addComponent(staticEntity, new TileComponent(null, isWalkable, true));

        if (!isWalkable) {
            world.addComponent(staticEntity, new CollisionComponent());
        }

        System.out.println("[INFO EntityFactory] Static Object with image '" + config.image + "' created: " + staticEntity);
        return staticEntity;
    }

    /**
     * Generates a balanced StatusComponent for a creature using Pokémon Gen 1
     * formulas.
     */
    private StatusComponent generateStats(int size, ProceduralSpriteComponent.BodyType bodyType, long ivSeed, int lives) {
        Random ivRandom = new Random(ivSeed);

        int baseHealth, baseAttack, baseDefense, baseSpeed, baseSpecial;
        switch (bodyType) {
            case FINNED_AQUATIC:
                baseHealth = 40;
                baseAttack = 45;
                baseDefense = 30;
                baseSpeed = 70;
                baseSpecial = 50;
                break;
            case BIPED_TERRESTRIAL:
            default:
                baseHealth = 55;
                baseAttack = 60;
                baseDefense = 50;
                baseSpeed = 50;
                baseSpecial = 45;
                break;
        }

        int attackIV = ivRandom.nextInt(16);
        int defenseIV = ivRandom.nextInt(16);
        int speedIV = ivRandom.nextInt(16);
        int specialIV = ivRandom.nextInt(16);
        int hpIV = ((attackIV % 2) * 8) + ((defenseIV % 2) * 4) + ((speedIV % 2) * 2) + ((specialIV % 2) * 1);

        int evTerm = 0; // EVs are 0 for newly generated creatures

        int maxHealth = ((((baseHealth + hpIV) * 2 + evTerm) * size) / 100) + size + 10;
        int attack = ((((baseAttack + attackIV) * 2 + evTerm) * size) / 100) + 5;
        int defense = ((((baseDefense + defenseIV) * 2 + evTerm) * size) / 100) + 5;
        int speed = ((((baseSpeed + speedIV) * 2 + evTerm) * size) / 100) + 5;
        int special = ((((baseSpecial + specialIV) * 2 + evTerm) * size) / 100) + 5;

        return new StatusComponent(maxHealth, attack, defense, speed, special, lives,
                hpIV, attackIV, defenseIV, speedIV, specialIV);
    }

    // --- Utility functions to safely read from the 'properties' map ---
    private int getIntFromProperties(Map<String, Object> props, String key, int defaultValue) {
        if (props != null && props.get(key) instanceof Number) {
            return ((Number) props.get(key)).intValue();
        }
        return defaultValue;
    }

    private double getDoubleFromProperties(Map<String, Object> props, String key, double defaultValue) {
        if (props != null && props.get(key) instanceof Number) {
            return ((Number) props.get(key)).doubleValue();
        }
        return defaultValue;
    }

    private long getLongFromProperties(Map<String, Object> props, String key, long defaultValue) {
        if (props != null && props.get(key) instanceof Number) {
            return ((Number) props.get(key)).longValue();
        }
        return defaultValue;
    }

    private boolean getBooleanFromProperties(Map<String, Object> props, String key, boolean defaultValue) {
        if (props != null && props.get(key) instanceof Boolean) {
            return (Boolean) props.get(key);
        }
        return defaultValue;
    }

    private ProceduralSpriteComponent.BodyType getBodyTypeFromProperties(Map<String, Object> props, String key, ProceduralSpriteComponent.BodyType defaultValue) {
        if (props != null && props.get(key) instanceof String) {
            try {
                return ProceduralSpriteComponent.BodyType.valueOf(((String) props.get(key)).toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("[WARN EntityFactory] Invalid BodyType '" + props.get(key) + "'. Using default.");
            }
        }
        return defaultValue;
    }

    private EcologyComponent.DietaryType getDietaryTypeFromProperties(Map<String, Object> props, String key, EcologyComponent.DietaryType defaultValue) {
        if (props != null && props.get(key) instanceof String) {
            try {
                return EcologyComponent.DietaryType.valueOf(((String) props.get(key)).toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("[WARN EntityFactory] Invalid DietaryType '" + props.get(key) + "'. Using default.");
            }
        }
        return defaultValue;
    }
}
