package game.evo.tools;

import game.evo.config.EntityConfig;
import game.evo.components.ProceduralSpriteComponent; // Precisamos do enum
import game.evo.components.EcologyComponent; // Precisamos do enum

import java.io.*;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ferramenta offline para criar um arquivo .zip de uma criatura customizada.
 * Este programa gera um arquivo .zip contendo um objeto EntityConfig serializado.
 */
public class CreaturePacker {

    public static void main(String[] args) {
        // --- PASSO 1: Defina as características da sua criatura customizada ---
        EntityConfig customCreature = new EntityConfig();
        
        // Este tipo será usado na EntityFactory para identificar que é uma criatura customizada
        customCreature.type = "CustomNPC"; 
        
        customCreature.properties = new HashMap<>();
        customCreature.properties.put("size", 6);
        customCreature.properties.put("diet", EcologyComponent.DietaryType.CARNIVORE.name());
        customCreature.properties.put("bodyType", ProceduralSpriteComponent.BodyType.BIPED_TERRESTRIAL.name());
        customCreature.properties.put("seed", 999888777L); // Seed para uma aparência única e consistente
        
        String zipFileName = "DragaoPequeno.zip";
        String dataFileName = "creature.dat"; // Nome do arquivo de dados dentro do zip

        // --- PASSO 2: Serializar o objeto EntityConfig ---
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(byteStream)) {
            oos.writeObject(customCreature);
        } catch (IOException e) {
            System.err.println("Erro ao serializar a criatura!");
            e.printStackTrace();
            return;
        }

        // --- PASSO 3: Criar o arquivo .zip e adicionar o arquivo serializado ---
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            ZipEntry entry = new ZipEntry(dataFileName);
            zos.putNextEntry(entry);
            zos.write(byteStream.toByteArray());
            zos.closeEntry();
            System.out.println("Criatura customizada criada com sucesso em: " + zipFileName);
        } catch (IOException e) {
            System.err.println("Erro ao criar o arquivo .zip!");
            e.printStackTrace();
        }
    }
}