package game.evo.utils;

import game.evo.components.ProceduralSpriteComponent;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A utility class to procedurally generate sprites based on a configuration. It
 * is now also the authority for calculating sprite dimensions.
 */
public class SpriteGenerator {

    /**
     * Calculates the render width for a sprite based on its configuration.
     *
     * @param config The component with the sprite's "genes".
     * @return The calculated width in pixels.
     */
    public static int getWidthFor(ProceduralSpriteComponent config) {
        if (config == null) {
            return 0;
        }
        switch (config.bodyType) {
            case FINNED_AQUATIC:
                return 8 + config.size * 2; // Ex: Size 1=10px, Size 5=18px
            case BIPED_TERRESTRIAL:
                return 6 + config.size;
            case MEAT_CHUNK:
                return 8 + config.size; // Tamanho da carne
            case PORTAL_SPIRAL:
                return GameConstants.CELL_SIZE;
            default:
                return 8 + config.size; // Default size for blobs, etc.
        }
    }

    /**
     * Calculates the render height for a sprite based on its configuration.
     *
     * @param config The component with the sprite's "genes".
     * @return The calculated height in pixels.
     */
    public static int getHeightFor(ProceduralSpriteComponent config) {
        if (config == null) {
            return 0;
        }
        switch (config.bodyType) {
            case FINNED_AQUATIC:
                return 5 + config.size;
            case BIPED_TERRESTRIAL:
                return 8 + config.size;
            case MEAT_CHUNK:
                return 6 + config.size; // Tamanho da carne
            case PORTAL_SPIRAL:
                return GameConstants.CELL_SIZE;
            default:
                return 8 + config.size;
        }
    }

    /**
     * Generates a sprite image.
     *
     * @param config The component containing the generation parameters.
     * @return A BufferedImage representing the generated sprite.
     */
    public BufferedImage generate(ProceduralSpriteComponent config) {
        switch (config.bodyType) {
            case FINNED_AQUATIC:
                return generateFinned(config);
            case BIPED_TERRESTRIAL:
                return generateBiped(config);
            case MEAT_CHUNK: // << ADICIONAR ESTE CASO
                return generateMeatChunk(config);
            case PORTAL_SPIRAL: // << CASO ADICIONADO
                return generatePortalSpiral(config);
            default:
                // This can be a fallback if needed
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
    }
    
    /**
     * Generates an animated, multi-colored spiral sprite.
     * @param config The configuration for the sprite.
     * @return A BufferedImage representing the generated portal.
     */
    private BufferedImage generatePortalSpiral(ProceduralSpriteComponent config) {
        int width = getWidthFor(config);
        int height = getHeightFor(config);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Color[] portalColors = {Color.GREEN, Color.BLUE, Color.YELLOW};
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Desenha a espiral usando coordenadas polares (r = a * θ)
        // Loop através do ângulo (theta)
        for (double theta = 0; theta < 8 * Math.PI; theta += 0.05) {
            // O raio 'r' aumenta com o ângulo, criando a espiral
            double r = (width / (16 * Math.PI)) * theta;
            
            // Converte de polar para cartesiano
            int x = (int) (centerX + r * Math.cos(theta));
            int y = (int) (centerY + r * Math.sin(theta));
            
            // Escolhe a cor baseada no quadro de animação e no ângulo, criando o efeito de rotação de cor
            int colorIndex = (config.animationFrame + (int)(theta / 2)) % portalColors.length;
            Color pixelColor = portalColors[colorIndex];
            
            // Desenha o pixel na imagem, se estiver dentro dos limites
            if (x >= 0 && x < width && y >= 0 && y < height) {
                image.setRGB(x, y, pixelColor.getRGB());
            }
        }
        return image;
    }

    private BufferedImage generateFinned(ProceduralSpriteComponent config) {
        int width = 5 + config.size;
        int height = 8 + config.size * 2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // ... (resto da lógica de generateFinned como estava antes) ...
        int bodyWidth = width;
        int bodyHeight = height - (height / 4);
        for (int y = 0; y < bodyHeight; y++) {
            for (int x = 0; x < bodyWidth; x++) {
                if (Math.pow((x - bodyWidth / 2.0) / (bodyWidth / 2.0), 2)
                        + Math.pow((y - bodyHeight / 2.0) / (bodyHeight / 2.0), 2) <= 1) {
                    image.setRGB(x, y, config.primaryColor.getRGB());
                }
            }
        }
        int tailHeight = height / 2;
        for (int y = 0; y < height; y++) {
            for (int x = bodyWidth; x < width; x++) {
                if (y >= (height - tailHeight) / 2 && y < (height + tailHeight) / 2) {
                    image.setRGB(x, y, config.primaryColor.getRGB());
                }
            }
        }
        int eyeX = (int) (bodyWidth * 0.7);
        int eyeY = (int) (bodyHeight * 0.3);
        image.setRGB(eyeX, eyeY, config.secondaryColor.getRGB());
        return image;
    }

    private BufferedImage generateBiped(ProceduralSpriteComponent config) {
        int width = getWidthFor(config); // Usa o próprio método para consistência
        int height = getHeightFor(config);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // ... (resto da lógica de generateBiped como estava antes) ...
        int torsoWidth = width / 2;
        int torsoHeight = height / 2;
        int torsoX = (width - torsoWidth) / 2;
        int torsoY = height / 4;
        fillRect(image, torsoX, torsoY, torsoWidth, torsoHeight, config.primaryColor);
        int headSize = torsoWidth;
        fillRect(image, torsoX, 0, headSize, headSize, config.primaryColor);
        int legWidth = Math.max(1, torsoWidth / 3);
        int legHeight = height - (torsoY + torsoHeight);
        fillRect(image, torsoX, torsoY + torsoHeight, legWidth, legHeight, config.secondaryColor);
        fillRect(image, torsoX + torsoWidth - legWidth, torsoY + torsoHeight, legWidth, legHeight, config.secondaryColor);
        return image;
    }

    private void fillRect(BufferedImage img, int x, int y, int w, int h, Color c) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i >= 0 && i < img.getWidth() && j >= 0 && j < img.getHeight()) {
                    img.setRGB(i, j, c.getRGB());
                }
            }
        }
    }

// --- NOVO MÉTODO PRIVADO ---
    /**
     * Generates a T-bone steak-like sprite.
     */
    private BufferedImage generateMeatChunk(ProceduralSpriteComponent config) {
        int width = getWidthFor(config);
        int height = getHeightFor(config);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Color meatColor = new Color(227, 60, 42); // Vermelho carne
        Color boneColor = new Color(255, 249, 222); // Branco osso

        // Desenha a parte da carne (um oval)
        int meatHeight = height - 2;
        int meatY = (height - meatHeight) / 2;
        for (int y = meatY; y < meatY + meatHeight; y++) {
            for (int x = 2; x < width; x++) {
                if (Math.pow((x - (width + 2) / 2.0) / ((width - 2) / 2.0), 2)
                        + Math.pow((y - height / 2.0) / (meatHeight / 2.0), 2) <= 1) {
                    image.setRGB(x, y, meatColor.getRGB());
                }
            }
        }

        // Desenha o osso em formato de "T"
        fillRect(image, 0, 0, 3, height, boneColor); // Parte vertical do "T"
        fillRect(image, 1, 0, width / 3, 2, boneColor); // Parte horizontal do "T"

        return image;
    }
    
    
}
