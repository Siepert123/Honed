package quantumstudios.honed.data.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.data.trait.TraitDefinition;
import quantumstudios.honed.data.part.PartSchema;
import quantumstudios.honed.data.forging.ForgingRecipe;
import quantumstudios.honed.data.tool.ToolSchema;
import quantumstudios.honed.data.component.ComponentType;
import quantumstudios.honed.Honed;
import java.net.URL;
import java.nio.file.*;
import java.io.Reader;
import java.util.function.Consumer;

import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static Path configPath;

    public static void setConfigPath(Path path) {
        configPath = path;
    }

    public static void load() {
        if (configPath == null) {
            Honed.LOGGER.error("Config path not set before load()!");
            return;
        }
        loadFolder("materials", json -> {
            MaterialDefinition mat = GSON.fromJson(json, MaterialDefinition.class);
            HonedRegistries.MATERIALS.put(mat.id, mat);
        });
        loadFolder("traits", json -> {
            TraitDefinition trait = GSON.fromJson(json, TraitDefinition.class);
            HonedRegistries.TRAITS.put(trait.id, trait);
        });
        loadFolder("part_schemas", json -> {
            PartSchema schema = GSON.fromJson(json, PartSchema.class);
            HonedRegistries.PART_SCHEMAS.put(schema.toolType, schema);
        });
        loadFolder("tool_schemas", json -> {
            ToolSchema tool = GSON.fromJson(json, ToolSchema.class);
            HonedRegistries.TOOL_SCHEMAS.put(tool.id, tool);
        });
        loadFolder("forging", json -> {
            ForgingRecipe recipe = GSON.fromJson(json, ForgingRecipe.class);
            HonedRegistries.FORGING.put(recipe.id, recipe);
        });

        Honed.LOGGER.info("Loaded {} materials, {} traits, {} forging recipes",
            HonedRegistries.MATERIALS.size(),
            HonedRegistries.TRAITS.size(),
            HonedRegistries.FORGING.size());
    }

    private static void loadFolder(String folder, Consumer<JsonObject> handler) {
        Path dir = configPath.resolve(folder);
        if (!Files.exists(dir)) {
            Honed.LOGGER.warn("Config folder missing: {}", dir);
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(f -> f.toString().endsWith(".json"))
                    .forEach(f -> {
                        try (Reader r = Files.newBufferedReader(f)) {
                            handler.accept(GSON.fromJson(r, JsonObject.class));
                        } catch (Exception e) {
                            Honed.LOGGER.error("Failed to parse {}: {}", f.getFileName(), e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Honed.LOGGER.error("Failed to scan {}: {}", folder, e.getMessage());
        }
    }
}