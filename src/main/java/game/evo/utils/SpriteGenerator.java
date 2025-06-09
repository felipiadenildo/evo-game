package game.evo.utils;

import game.evo.components.ProceduralSpriteComponent;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
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
            case TREE:
            case ROCK:
            case BUSH:
            case CACTUS:
            case FLOWER_PATCH:
            case CORAL:
            case MUSHROOM_CLUSTER:
                return 12 + config.size * 2;
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
            case TREE:
            case ROCK:
            case BUSH:
            case CACTUS:
            case FLOWER_PATCH:
            case CORAL:
            case MUSHROOM_CLUSTER:
                return 12 + config.size * 2;
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
    /**
     * Método principal que gera a imagem do sprite. Agora ele prepara o
     * ambiente de desenho (Graphics2D) e chama o método de desenho específico.
     */
    public BufferedImage generate(ProceduralSpriteComponent config) {
        int width = getWidthFor(config) + 4;  // Adiciona um pequeno preenchimento para evitar cortes
        int height = getHeightFor(config) + 4;

        // Cria uma imagem com fundo transparente
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // IMPORTANTE: Ativa o antialiasing para formas com bordas suaves
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // O "Dispatcher" que escolhe o que desenhar com base no BodyType
        switch (config.bodyType) {
            case FINNED_AQUATIC:
                drawFinned(g2d, config);
                break;
            case BIPED_TERRESTRIAL:
                drawBiped(g2d, config);
                break;
            case MEAT_CHUNK: // << ADICIONAR ESTE CASO
                return generateMeatChunk(config);
            case PORTAL_SPIRAL: // << CASO ADICIONADO
                return generatePortalSpiral(config);
            case TREE:
                drawTree(g2d, config);
                break;
            case ROCK:
                drawRock(g2d, config);
                break;
            case BUSH:
                drawBush(g2d, config);
                break;
            case CACTUS:
                drawCactus(g2d, config);
                break;
            case FLOWER_PATCH:
                drawFlowerPatch(g2d, config);
                break;
            case CORAL:
                drawCoral(g2d, config);
                break;
            case MUSHROOM_CLUSTER:
                drawMushroomCluster(g2d, config);
                break;
            default:
                // Fallback para um sprite vazio se o tipo for desconhecido
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        g2d.dispose(); // Libera os recursos gráficos
        return image;
    }

    /**
     * Desenha um cacto com um corpo principal e braços.
     */
    private void drawCactus(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2 + 2;
        int centerY = height / 2 + 2;

        Color cactusColor = new Color(45, 130, 85);
        g.setColor(cactusColor);

        // Corpo principal
        int bodyWidth = width / 3;
        g.fillRect(centerX - bodyWidth / 2, centerY - height / 3, bodyWidth, (int) (height * 0.8));

        // Braços (um ou dois, em posições aleatórias)
        int armWidth = (int) (bodyWidth * 1.5);
        int armHeight = bodyWidth / 2;
        if (rand.nextBoolean()) {
            g.fillRect(centerX - bodyWidth / 2 - armWidth / 2, centerY, armWidth, armHeight);
        }
        if (rand.nextBoolean()) {
            g.fillRect(centerX, centerY - height / 4, armWidth, armHeight);
        }
    }

    /**
     * Desenha um canteiro de flores coloridas.
     */
    private void drawFlowerPatch(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);

        // Folhagem base
        g.setColor(new Color(90, 160, 90, 150)); // Verde semi-transparente
        g.fill(new Ellipse2D.Double(0, height / 3.0, width, height * 0.66));

        // Flores (usa a cor secundária do componente)
        Color flowerColor = psc.secondaryColor != Color.BLACK ? psc.secondaryColor : Color.RED;
        for (int i = 0; i < 3 + psc.size; i++) {
            int flowerSize = 3 + rand.nextInt(3);
            int x = rand.nextInt(width - flowerSize);
            int y = rand.nextInt(height - flowerSize);

            g.setColor(flowerColor.brighter());
            g.fill(new Ellipse2D.Double(x, y, flowerSize, flowerSize));
            g.setColor(Color.YELLOW); // miolo
            g.fill(new Ellipse2D.Double(x + flowerSize / 4.0, y + flowerSize / 4.0, flowerSize / 2.0, flowerSize / 2.0));
        }
    }

    /**
     * Desenha uma estrutura de coral ramificada.
     */
    private void drawCoral(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2;
        int centerY = height; // Começa a desenhar da base

        Color coralColor = psc.primaryColor != Color.GREEN ? psc.primaryColor : new Color(255, 100, 80);
        g.setColor(coralColor);
        g.setStroke(new BasicStroke(2 + rand.nextInt(psc.size / 2 + 1)));

        // Desenha galhos recursivamente
        drawBranch(g, centerX, centerY, 90, height / 2, rand);
    }

// Método auxiliar para o coral
    private void drawBranch(Graphics2D g, int x1, int y1, double angle, double length, Random rand) {
        if (length < 2) {
            return;
        }

        int x2 = x1 + (int) (Math.cos(Math.toRadians(angle)) * length);
        int y2 = y1 - (int) (Math.sin(Math.toRadians(angle)) * length);
        g.drawLine(x1, y1, x2, y2);

        // Cria novos galhos
        drawBranch(g, x2, y2, angle - 20 - rand.nextInt(15), length * 0.75, rand);
        if (rand.nextBoolean()) {
            drawBranch(g, x2, y2, angle + 20 + rand.nextInt(15), length * 0.75, rand);
        }
    }

    /**
     * Desenha um grupo de cogumelos.
     */
    private void drawMushroomCluster(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);

        Color stemColor = new Color(220, 210, 200);
        Color capColor = psc.primaryColor != Color.GREEN ? psc.primaryColor : new Color(180, 50, 50);

        for (int i = 0; i < 2 + psc.size / 2; i++) {
            int mushroomHeight = height / 2 + rand.nextInt(height / 2);
            int mushroomWidth = width / 3 + rand.nextInt(width / 4);
            int x = rand.nextInt(width - mushroomWidth);
            int y = height - mushroomHeight;

            // Caule
            g.setColor(stemColor);
            g.fillRect(x + mushroomWidth / 3, y + mushroomHeight / 3, mushroomWidth / 3, mushroomHeight * 2 / 3);

            // Chapéu
            g.setColor(capColor);
            g.fill(new Arc2D.Double(x, y, mushroomWidth, mushroomHeight, 0, 180, Arc2D.CHORD));
        }
    }

    /**
     * Desenha uma árvore simples com tronco e copa.
     */
    private void drawTree(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2 + 2;
        int centerY = height / 2 + 2;

        // Tronco
        Color trunkColor = new Color(139, 69, 19); // Marrom
        int trunkWidth = Math.max(2, width / 4 + rand.nextInt(psc.size));
        g.setColor(trunkColor);
        g.fillRect(centerX - trunkWidth / 2, centerY, trunkWidth, height / 2);

        // Copa (desenhada como múltiplos círculos para dar volume)
        Color canopyColor = new Color(34, 139, 34); // Verde escuro
        int canopyRadius = width / 2;
        for (int i = 0; i < 3 + rand.nextInt(3); i++) {
            g.setColor(canopyColor.brighter().brighter());
            int offsetX = rand.nextInt(canopyRadius) - canopyRadius / 2;
            int offsetY = rand.nextInt(canopyRadius / 2) - canopyRadius / 4;
            g.fill(new Ellipse2D.Double(centerX - canopyRadius + offsetX, centerY - canopyRadius + offsetY, canopyRadius, canopyRadius));
        }
    }

    /**
     * Desenha uma rocha com formato irregular.
     */
    private void drawRock(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2 + 2;
        int centerY = height / 2 + 2;

        // Gera um polígono irregular para a rocha
        int points = 5 + rand.nextInt(4);
        int[] xPoints = new int[points];
        int[] yPoints = new int[points];
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double radius = (width / 2.0) * (0.7 + rand.nextDouble() * 0.3); // Varia o raio
            xPoints[i] = (int) (centerX + radius * Math.cos(angle));
            yPoints[i] = (int) (centerY + radius * Math.sin(angle));
        }

        g.setColor(new Color(128, 128, 128)); // Cinza
        g.fillPolygon(xPoints, yPoints, points);
        g.setColor(Color.DARK_GRAY);
        g.drawPolygon(xPoints, yPoints, points);
    }

    /**
     * Desenha um arbusto com múltiplas "bolas" de folhagem.
     */
    private void drawBush(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2 + 2;
        int centerY = height / 2 + 2;

        Color bushColor = new Color(85, 107, 47); // Verde oliva
        int clusterRadius = width / 3;
        for (int i = 0; i < 4 + rand.nextInt(3); i++) {
            g.setColor(bushColor.brighter());
            int offsetX = rand.nextInt(width / 2) - width / 4;
            int offsetY = rand.nextInt(height / 2) - height / 4;
            g.fill(new Ellipse2D.Double(centerX - clusterRadius + offsetX, centerY - clusterRadius + offsetY, clusterRadius, clusterRadius));
        }
    }

    // --- MÉTODOS DE DESENHO REATORADOS ---
    /**
     * Desenha uma criatura bípede (top-down) com corpo, cabeça, pernas e olhos.
     */
    private void drawBiped(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2 + 2; // Centraliza na imagem com preenchimento
        int centerY = height / 2 + 2;

        // Parâmetros corporais baseados no 'size' e em variações aleatórias
        int bodyWidth = width - rand.nextInt(width / 3);
        int bodyHeight = (int) (height * 0.6) + rand.nextInt(psc.size);
        int headSize = Math.max(4, (int) (bodyWidth * 0.8));

        // 1. Desenhar as Pernas (desenhadas primeiro para ficarem "atrás" do corpo)
        drawLegs(g, psc.secondaryColor, centerX, centerY, bodyWidth, psc.size, rand);

        // 2. Desenhar o Corpo (um oval)
        g.setColor(psc.primaryColor);
        g.fill(new Ellipse2D.Double(centerX - bodyWidth / 2.0, centerY - bodyHeight / 2.0, bodyWidth, bodyHeight));
        // Adiciona uma borda sutil ao corpo
        g.setColor(psc.primaryColor.darker());
        g.draw(new Ellipse2D.Double(centerX - bodyWidth / 2.0, centerY - bodyHeight / 2.0, bodyWidth, bodyHeight));

        // 3. Desenhar a Cabeça (um círculo sobre o corpo)
        g.setColor(psc.primaryColor);
        g.fill(new Ellipse2D.Double(centerX - headSize / 2.0, centerY - bodyHeight / 2.5 - headSize / 2.0, headSize, headSize));
        g.setColor(psc.primaryColor.darker());
        g.draw(new Ellipse2D.Double(centerX - headSize / 2.0, centerY - bodyHeight / 2.5 - headSize / 2.0, headSize, headSize));

        // 4. Desenhar os Olhos
        drawEyes(g, psc.secondaryColor, centerX, (int) (centerY - bodyHeight / 2.2), headSize, psc.size, rand);
    }

    /**
     * Desenha uma criatura aquática (top-down) com corpo, cauda, barbatanas e
     * olhos.
     */
    private void drawFinned(Graphics2D g, ProceduralSpriteComponent psc) {
        Random rand = new Random(psc.creatureSeed);
        int width = getWidthFor(psc);
        int height = getHeightFor(psc);
        int centerX = width / 2 + 2;
        int centerY = height / 2 + 2;

        // Parâmetros do corpo de peixe
        int bodyWidth = (int) (width * 0.8) + rand.nextInt(psc.size);
        int bodyHeight = height;

        // 1. Desenhar a Cauda
        drawTail(g, psc.primaryColor.darker(), centerX + bodyWidth / 2, centerY, bodyHeight, psc.size, rand);

        // 2. Desenhar as Barbatanas Laterais
        drawFins(g, psc.primaryColor.darker(), centerX, centerY, bodyWidth, bodyHeight, psc.size, rand);

        // 3. Desenhar o Corpo principal
        g.setColor(psc.primaryColor);
        g.fill(new Ellipse2D.Double(centerX - bodyWidth / 2.0, centerY - bodyHeight / 2.0, bodyWidth, bodyHeight));
        g.setColor(psc.primaryColor.darker());
        g.draw(new Ellipse2D.Double(centerX - bodyWidth / 2.0, centerY - bodyHeight / 2.0, bodyWidth, bodyHeight));

        // 4. Desenhar os Olhos
        drawEyes(g, psc.secondaryColor, (int) (centerX - bodyWidth * 0.3), centerY, bodyWidth, psc.size, rand);
    }

    // --- NOVOS MÉTODOS AUXILIARES PARA DESENHAR PARTES ---
    private void drawLegs(Graphics2D g, Color color, int cx, int cy, int bodyWidth, int size, Random rand) {
        g.setColor(color);
        int legWidth = 2 + rand.nextInt(size + 1);
        int legHeight = 4 + size;
        int legSeparation = bodyWidth / 3 + rand.nextInt(bodyWidth / 4);

        // Perna Esquerda
        g.fillRect(cx - legSeparation, cy, legWidth, legHeight);
        // Perna Direita
        g.fillRect(cx + legSeparation - legWidth, cy, legWidth, legHeight);
    }

    private void drawEyes(Graphics2D g, Color color, int cx, int cy, int headSize, int size, Random rand) {
        g.setColor(color);
        int eyeSize = Math.max(1, 1 + rand.nextInt(size / 2 + 1));
        int eyeSeparation = headSize / 4 + rand.nextInt(headSize / 5 + 1);

        g.fill(new Ellipse2D.Double(cx - eyeSeparation, cy - eyeSize / 2.0, eyeSize, eyeSize));
        g.fill(new Ellipse2D.Double(cx + eyeSeparation - eyeSize, cy - eyeSize / 2.0, eyeSize, eyeSize));
    }

    private void drawTail(Graphics2D g, Color color, int cx, int cy, int bodyHeight, int size, Random rand) {
        g.setColor(color);
        int tailWidth = 3 + size + rand.nextInt(size + 1);
        int tailHeight = bodyHeight / 2 + rand.nextInt(bodyHeight / 3);

        int[] xPoints = {cx, cx + tailWidth, cx + tailWidth};
        int[] yPoints = {cy, cy - tailHeight / 2, cy + tailHeight / 2};
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawFins(Graphics2D g, Color color, int cx, int cy, int bodyWidth, int bodyHeight, int size, Random rand) {
        g.setColor(color);
        int finWidth = 2 + size / 2 + rand.nextInt(size / 2 + 1);
        int finHeight = bodyHeight / 3 + rand.nextInt(size + 1);

        // Barbatanas são desenhadas como retângulos rotacionados
        AffineTransform oldTransform = g.getTransform();

        // Barbatanas de cima
        g.rotate(Math.toRadians(-35), cx, cy);
        g.fillRect(cx, cy - bodyHeight / 2, finWidth, finHeight);
        g.setTransform(oldTransform); // Reseta a rotação

        // Barbatanas de baixo
        g.rotate(Math.toRadians(35), cx, cy);
        g.fillRect(cx, cy + bodyHeight / 2 - finHeight, finWidth, finHeight);
        g.setTransform(oldTransform);
    }

    /**
     * Generates an animated, multi-colored spiral sprite.
     *
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
            int colorIndex = (config.animationFrame + (int) (theta / 2)) % portalColors.length;
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
