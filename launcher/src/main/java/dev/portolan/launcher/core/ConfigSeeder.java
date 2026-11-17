package dev.portolan.launcher.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.function.Consumer;

public final class ConfigSeeder {

    private static final String ENTITYCULLING =
            "{\n" +
            "  \"entityCulling\": true,\n" +
            "  \"blockEntityCulling\": true,\n" +
            "  \"tickCulling\": true,\n" +
            "  \"hitboxLimit\": 50,\n" +
            "  \"forceDisplayCulling\": true,\n" +
            "  \"useWhitelist\": false,\n" +
            "  \"useBlacklist\": true,\n" +
            "  \"blacklist\": []\n" +
            "}\n";

    private static final String DYNAMICFPS =
            "[general]\n" +
            "enabled = true\n" +
            "idle_fps = 1\n" +
            "\n" +
            "[reduced]\n" +
            "enabled = true\n" +
            "fps = 15\n";

    private static final String IMMEDIATELYFAST =
            "{\n" +
            "  \"font_atlas_resizing\": true,\n" +
            "  \"map_atlas_generation\": true,\n" +
            "  \"hud_batching\": true,\n" +
            "  \"fast_text_lookup\": true,\n" +
            "  \"fast_buffer_upload\": true,\n" +
            "  \"fast_buffer_upload_size_mb\": 256,\n" +
            "  \"experimental_disable_error_checking\": false\n" +
            "}\n";

    // options.txt : render 6, mipmaps 0, AO off, biome blend 0, maxFps tres haut.
    // Vanilla clampe l'UI a 260, mais Sodium lit la valeur brute du fichier
    // et permet "unlimited" via sodium-options.json ci-dessous.
    private static final String OPTIONS_TXT =
            "ao:false\n" +
            "autoJump:false\n" +
            "biomeBlendRadius:0\n" +
            "chatOpacity:1.0\n" +
            "chatScale:1.0\n" +
            "chatHeightFocused:1.0\n" +
            "chatHeightUnfocused:0.4375\n" +
            "chatDelay:0.0\n" +
            "chatTextOpacity:1.0\n" +
            "darkMojangStudiosBackground:true\n" +
            "entityShadows:false\n" +
            "fov:0.0\n" +
            "fovEffectScale:0.0\n" +
            "fullscreen:false\n" +
            "gamma:0.5\n" +
            "graphicsMode:0\n" +
            "guiScale:0\n" +
            "lang:en_us\n" +
            "mipmapLevels:0\n" +
            "mouseSensitivity:0.5\n" +
            "maxFps:1000\n" +
            "narrator:0\n" +
            "particles:2\n" +
            "renderDistance:6\n" +
            "renderClouds:false\n" +
            "screenEffectScale:0.0\n" +
            "simulationDistance:6\n" +
            "tutorialStep:none\n" +
            "viewBobbing:false\n" +
            "vsync:false\n";

    // Sodium 0.6.x — structure detectee sur les versions 1.21+/26.x.
    // "max_fps": "unlimited" est la cle qui debloque > 260 fps.
    // Si Sodium 0.6 change de format, il rejette ce fichier et repart sur
    // ses defauts — pas de crash, juste pas de seed.
    private static final String SODIUM_JSON =
            "{\n" +
            "  \"quality\": {\n" +
            "    \"graphics_quality\": \"fast\",\n" +
            "    \"weather_quality\": \"fast\",\n" +
            "    \"leaves_quality\": \"fast\",\n" +
            "    \"enable_vignette\": false,\n" +
            "    \"use_persistent_mapping\": false,\n" +
            "    \"entity_distance_scale\": 1.0,\n" +
            "    \"cloud_quality\": \"off\"\n" +
            "  },\n" +
            "  \"performance\": {\n" +
            "    \"max_fps\": \"unlimited\",\n" +
            "    \"chunk_build_defer_mode\": \"always\",\n" +
            "    \"chunk_build_budget\": \"low\",\n" +
            "    \"always_defer_chunk_updates\": true,\n" +
            "    \"use_entity_culling\": true,\n" +
            "    \"use_fog_occlusion\": true,\n" +
            "    \"use_block_face_culling\": true,\n" +
            "    \"use_compact_vertex_format\": true,\n" +
            "    \"use_translucent_face_sorting\": true,\n" +
            "    \"animate_only_visible_textures\": true,\n" +
            "    \"use_no_error_g_l_context\": true,\n" +
            "    \"use_early_z\": true\n" +
            "  },\n" +
            "  \"advanced\": {\n" +
            "    \"enable_memory_tracing\": false,\n" +
            "    \"use_advanced_staging_buffers\": true,\n" +
            "    \"cpu_render_ahead_limit\": 2,\n" +
            "    \"allow_direct_memory_access\": true\n" +
            "  },\n" +
            "  \"notification\": {\n" +
            "    \"has_seen_sodium_notice\": true\n" +
            "  }\n" +
            "}\n";

    private static final Map<String, String> MOD_CONFIGS = Map.of(
            "entityculling.json",   ENTITYCULLING,
            "dynamicfps.toml",      DYNAMICFPS,
            "immediatelyfast.json", IMMEDIATELYFAST
    );

    public void seed(Path instanceDir, String mcVersion, Consumer<String> log) throws IOException {
        Path config = instanceDir.resolve("config");
        Files.createDirectories(config);
        for (Map.Entry<String, String> e : MOD_CONFIGS.entrySet()) {
            Path target = config.resolve(e.getKey());
            if (Files.exists(target)) {
                log.accept("  skip (deja la): " + e.getKey());
                continue;
            }
            Files.writeString(target, e.getValue());
            log.accept("  seed mod: " + e.getKey());
        }

        Path opts = instanceDir.resolve("options.txt");
        if (!Files.exists(opts)) {
            Files.writeString(opts, OPTIONS_TXT);
            log.accept("  seed vanilla: options.txt");
        } else {
            log.accept("  skip (deja la): options.txt");
        }

        // Sodium seed sur toutes versions : 0.5.x et 0.6.x supportent max_fps
        // dans un format proche. Si le format ne matche pas, Sodium repart
        // sur ses defauts, aucune casse.
        Path sodium = config.resolve("sodium-options.json");
        if (!Files.exists(sodium)) {
            Files.writeString(sodium, SODIUM_JSON);
            log.accept("  seed mod: sodium-options.json");
        } else {
            log.accept("  skip (deja la): sodium-options.json");
        }
    }

    public ConfigSeeder() {}
}