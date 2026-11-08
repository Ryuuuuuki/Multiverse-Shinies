package com.multiverseshinies.client;

import com.bedrockk.molang.runtime.value.MoValue;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Infernape SSJ4 rage aura, drawn every frame like a Dragon-Ball ki aura:
 *  - a 3D aura around him (AuraMesh): a teardrop shell with rising light streaks and a rim glow, its top breaking into
 *    flickering flames, plus flame tongues cut from the hand-drawn aura key drawings;
 *  - ground pulses (rings that grow out and fade);
 *  - wind streaks running outward along the ground;
 *  - short red/white lightning sparks crackling around the aura;
 *  - (client tick) chunks of the block under him and dust thrown outward, embers rising.
 * Shown while: a transforming Multiverse Pokemon (RageSpecies) in battle with the posable variable
 * v.multiverse_rage == 1 (set by the mod's boost.json; gone when the Pokemon is recalled), starting only when the
 * rage animation bursts (RageSpecies.burstTicks after the variable is set). RageBurst adds the burst itself.
 * Golden species (Monferno) get the golden aura and Super Saiyan 2 lightning instead of the ground effects.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class RageAuraRenderer {
    private static final ResourceLocation AURA = tex("rage_aura");
    private static final ResourceLocation RING = tex("rage_ring");
    private static final ResourceLocation WIND = tex("wind_streak");
    private static final ResourceLocation GLOW = tex("burst_glow");
    private static final ResourceLocation AURA_GOLD = tex("rage_aura_gold");
    private static final ResourceLocation RING_GOLD = tex("rage_ring_gold");
    private static final int GOLD_BOLT_SLOTS = 10;
    private static final int FRAMES = 16;
    private static final float FPS = 9f;
    private static final float AURA_HALF = 1.28f;     // in entity heights
    private static final float AURA_CENTER = 1.0f;
    private static final float AURA_ALPHA = 0.45f;
    private static final float RING_HALF = 1.3f;
    private static final float PULSE_ALPHA = 0.6f;
    private static final float PULSES_PER_SECOND = 0.9f;
    private static final int WIND_STREAKS = 14;
    private static final int SPARK_SLOTS = 6;
    private static final float FADE_PER_SECOND = 4f;

    private static final Map<Integer, Float> FADE = new HashMap<>();
    /** entity id -> game time the rage variable was first seen on (the boost; the burst comes burstTicks later) */
    private static final Map<Integer, Long> RAGE_SINCE = new HashMap<>();
    private static long lastNanos = -1;

    private RageAuraRenderer() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "textures/aura/" + name + ".png");
    }

    static boolean isRaging(PokemonEntity pe) {
        try {
            com.multiverseshinies.RageSpecies sp = com.multiverseshinies.RageSpecies.of(pe);
            if (sp == null) return false;
            if (!(pe.getDelegate() instanceof PosableState state)) return false;
            var env = state.getRuntime().getEnvironment();
            MoValue v = env.getSimpleVariable("multiverse_rage");
            if (v == null || v.asDouble() < 0.5) { RAGE_SINCE.remove(pe.getId()); return false; }
            long now = pe.level().getGameTime();
            if (RAGE_SINCE.size() > 256) RAGE_SINCE.clear();
            long since = RAGE_SINCE.computeIfAbsent(pe.getId(), k -> now);
            // the variable sticks to the entity: drop it when the boosts are over (battle ended, or, once the
            // server has had time to say so, no stat raised any more: Haze, lowered back to 0...)
            boolean grace = now - since <= sp.burstTicks + 40;
            int id = pe.getId();
            boolean serverEnded = com.multiverseshinies.RageNet.CONFIRMED.contains(id) && !com.multiverseshinies.RageNet.BUFFED.contains(id);
            if (!pe.isBattling() || (!grace && serverEnded)) {
                env.setSimpleVariable("multiverse_rage", new com.bedrockk.molang.runtime.value.DoubleValue(0.0));
                RAGE_SINCE.remove(id);
                com.multiverseshinies.RageNet.CONFIRMED.remove(id);
                return false;
            }
            return now - since >= sp.burstTicks;
        } catch (Throwable t) {
            return false;
        }
    }

    /** deterministic 0..1 random from a few ints */
    static float hash(int a, int b, int c) {
        int h = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    // ------------------------------------------------------------------ own effect particles
    private static final ResourceLocation KI = tex("ki_spark");
    private static final ResourceLocation DUST = tex("dust");
    private static final int ROCK = 0, DUSTP = 1, KIP = 2;
    private static final java.util.List<Fx> FX = new java.util.ArrayList<>();

    private static final class Fx {
        int type, variant, age, life;
        double x, y, z, px, py, pz, vx, vy, vz;
        float size, size1, rot, prot, spin, ax, az;
        BlockState block;
    }

    /** the burst: a ring of dust thrown out fast, sparks shooting up and chunks of ground blown into the air */
    static void burstParticles(PokemonEntity pe) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource rnd = level.getRandom();
        float h = Math.max(0.5f, pe.getBbHeight());
        Vec3 p = pe.position();
        BlockState ground = level.getBlockState(BlockPos.containing(p.x, p.y - 0.2, p.z));
        for (int i = 0; i < 48; i++) {
            Fx f = new Fx(); f.type = DUSTP; f.variant = rnd.nextInt(4);
            double ang = 2 * Math.PI * i / 48 + rnd.nextDouble() * 0.1, sp = 0.28 + 0.14 * rnd.nextDouble();
            f.x = p.x + Math.cos(ang) * h * 0.3; f.y = p.y + 0.15; f.z = p.z + Math.sin(ang) * h * 0.3;
            f.vx = Math.cos(ang) * sp; f.vz = Math.sin(ang) * sp; f.vy = 0.01;
            f.size = h * 0.25f; f.size1 = h * (0.7f + 0.3f * rnd.nextFloat());
            f.rot = rnd.nextFloat() * 360; f.spin = (float) ((rnd.nextDouble() - 0.5) * 10);
            f.life = 18 + rnd.nextInt(10);
            spawn(f);
        }
        for (int i = 0; i < 36; i++) {
            Fx f = new Fx(); f.type = KIP;
            double ang = rnd.nextDouble() * Math.PI * 2, r = h * (0.2 + 0.5 * rnd.nextDouble());
            f.x = p.x + Math.cos(ang) * r; f.y = p.y + h * rnd.nextDouble(); f.z = p.z + Math.sin(ang) * r;
            f.vx = Math.cos(ang) * 0.06; f.vz = Math.sin(ang) * 0.06; f.vy = h * (0.16 + 0.12 * rnd.nextDouble());
            f.size = h * (0.35f + 0.3f * rnd.nextFloat());
            f.life = 8 + rnd.nextInt(8);
            spawn(f);
        }
        if (!ground.isAir()) {
            for (int i = 0; i < 14; i++) {
                Fx f = new Fx(); f.type = ROCK; f.block = ground;
                double ang = rnd.nextDouble() * Math.PI * 2, r = h * (0.3 + 0.5 * rnd.nextDouble());
                f.x = p.x + Math.cos(ang) * r; f.y = p.y + 0.1; f.z = p.z + Math.sin(ang) * r;
                f.vx = Math.cos(ang) * 0.09; f.vz = Math.sin(ang) * 0.09; f.vy = 0.06 + 0.05 * rnd.nextDouble();
                f.size = (float) (0.12 + 0.14 * rnd.nextDouble()) * Math.min(1.6f, 0.6f + 0.4f * h);
                f.spin = (float) ((rnd.nextDouble() - 0.5) * 24); f.ax = rnd.nextFloat(); f.az = rnd.nextFloat();
                f.life = 30 + rnd.nextInt(20);
                spawn(f);
            }
        }
    }

    private static void spawn(Fx f) {
        f.px = f.x; f.py = f.y; f.pz = f.z; f.prot = f.rot;
        if (FX.size() < 700) FX.add(f);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { FX.clear(); return; }
        if (mc.isPaused()) return;
        for (int i = FX.size() - 1; i >= 0; i--) {
            Fx f = FX.get(i);
            f.px = f.x; f.py = f.y; f.pz = f.z; f.prot = f.rot;
            f.x += f.vx; f.y += f.vy; f.z += f.vz; f.rot += f.spin;
            if (f.type == DUSTP) { f.vx *= 0.93; f.vz *= 0.93; }
            if (f.type == ROCK) { f.vx *= 0.97; f.vz *= 0.97; f.vy *= 0.995; }
            if (++f.age >= f.life) FX.remove(i);
        }
        RandomSource rnd = level.getRandom();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pe) || FADE.getOrDefault(pe.getId(), 0f) < 0.5f) continue;
            if (isGolden(pe)) continue;                       // the golden ones crackle with lightning instead
            float h = Math.max(0.5f, pe.getBbHeight());
            Vec3 p = pe.position();
            BlockState ground = level.getBlockState(BlockPos.containing(p.x, p.y - 0.2, p.z));
            // levitating chunks of the ground (real mini blocks with the ground's texture)
            if (!ground.isAir() && rnd.nextFloat() < 0.3f) {
                Fx f = new Fx(); f.type = ROCK; f.block = ground;
                double ang = rnd.nextDouble() * Math.PI * 2, r = h * (0.35 + 0.6 * rnd.nextDouble());
                f.x = p.x + Math.cos(ang) * r; f.y = p.y + 0.05; f.z = p.z + Math.sin(ang) * r;
                f.vx = Math.cos(ang) * 0.004; f.vz = Math.sin(ang) * 0.004; f.vy = 0.025 + 0.04 * rnd.nextDouble();
                f.size = (float) (0.09 + 0.12 * rnd.nextDouble()) * Math.min(1.6f, 0.6f + 0.4f * h);
                f.spin = (float) ((rnd.nextDouble() - 0.5) * 14); f.ax = rnd.nextFloat(); f.az = rnd.nextFloat();
                f.life = 40 + rnd.nextInt(35);
                spawn(f);
            }
            // dust rolling outward along the ground
            if (rnd.nextFloat() < 0.8f) {
                Fx f = new Fx(); f.type = DUSTP; f.variant = rnd.nextInt(4);
                double ang = rnd.nextDouble() * Math.PI * 2, r = h * 0.3;
                f.x = p.x + Math.cos(ang) * r; f.y = p.y + 0.12; f.z = p.z + Math.sin(ang) * r;
                double sp = 0.07 + 0.06 * rnd.nextDouble();
                f.vx = Math.cos(ang) * sp; f.vz = Math.sin(ang) * sp; f.vy = 0.004;
                f.size = h * 0.18f; f.size1 = h * (0.45f + 0.2f * rnd.nextFloat());
                f.rot = rnd.nextFloat() * 360; f.spin = (float) ((rnd.nextDouble() - 0.5) * 6);
                f.life = 24 + rnd.nextInt(14);
                spawn(f);
            }
            // ki sparks shooting up the edge of the aura
            for (int k = 0; k < 2; k++) {
                Fx f = new Fx(); f.type = KIP;
                double ang = rnd.nextDouble() * Math.PI * 2, r = h * (0.32 + 0.22 * rnd.nextDouble());
                f.x = p.x + Math.cos(ang) * r; f.y = p.y + h * (0.05 + 1.0 * rnd.nextDouble()); f.z = p.z + Math.sin(ang) * r;
                f.vy = h * (0.07 + 0.06 * rnd.nextDouble());
                f.size = h * (0.22f + 0.18f * rnd.nextFloat());
                f.life = 7 + rnd.nextInt(6);
                spawn(f);
            }
        }
    }

    private static void renderFx(PoseStack ps, MultiBufferSource.BufferSource buffers, Camera camera, Vec3 cam, float partial, ClientLevel level) {
        if (FX.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        org.joml.Quaternionf camRot = camera.rotation();
        for (Fx f : FX) {
            double x = f.px + (f.x - f.px) * partial, y = f.py + (f.y - f.py) * partial, z = f.pz + (f.z - f.pz) * partial;
            float life = Math.min(1f, (f.age + partial) / f.life);
            ps.pushPose();
            ps.translate(x - cam.x, y - cam.y, z - cam.z);
            if (f.type == ROCK) {
                float s = f.size * Math.min(1f, (1f - life) / 0.2f) * Math.min(1f, life / 0.1f + 0.3f);
                float rot = f.prot + (f.rot - f.prot) * partial;
                ps.mulPose(Axis.of(new org.joml.Vector3f(f.ax, 1f, f.az).normalize()).rotationDegrees(rot));
                ps.scale(s, s, s);
                ps.translate(-0.5, -0.5, -0.5);
                int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
                // a plain cube drawn by hand with the ground block's particle texture: Minecraft's block model renderer,
                // fed from here mid-frame, smeared some chunks into long twisted spikes (with or without shaders)
                net.minecraft.client.renderer.texture.TextureAtlasSprite sprite =
                    mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(f.block);
                VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
                cube(vc, ps.last(), sprite, light);
            } else if (f.type == DUSTP) {
                ps.mulPose(camRot);
                ps.mulPose(Axis.ZP.rotationDegrees(f.prot + (f.rot - f.prot) * partial));
                float s = f.size + (f.size1 - f.size) * life;
                float al = 0.6f * (float) Math.sin(Math.PI * life);
                // no depth write (emissive type) so the dust can never cut holes in the aura; shaded by hand instead
                VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(DUST));
                PoseStack.Pose pose = ps.last(); Matrix4f m = pose.pose();
                float u0 = f.variant / 4f, u1 = (f.variant + 1) / 4f; int a = Math.round(al * 255);
                int raw = level.getMaxLocalRawBrightness(BlockPos.containing(x, y, z));
                int shade = Math.round(255 * (0.3f + 0.7f * raw / 15f));
                litVertex(vc, pose, m, -s, -s, u0, 1, a, shade); litVertex(vc, pose, m, s, -s, u1, 1, a, shade);
                litVertex(vc, pose, m, s, s, u1, 0, a, shade); litVertex(vc, pose, m, -s, s, u0, 0, a, shade);
            } else {
                double dx = cam.x - x, dz = cam.z - z;
                ps.mulPose(Axis.YP.rotation((float) Math.atan2(dx, dz)));
                float len = f.size, wid = f.size * 0.16f;
                float al = (float) Math.sin(Math.PI * life);
                VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(KI));
                PoseStack.Pose pose = ps.last(); Matrix4f m = pose.pose(); int a = Math.round(al * 235);
                vertex(vc, pose, m, -wid, -len * 0.5f, 0, 0, 1, a); vertex(vc, pose, m, wid, -len * 0.5f, 0, 1, 1, a);
                vertex(vc, pose, m, wid, len * 0.5f, 0, 1, 0, a); vertex(vc, pose, m, -wid, len * 0.5f, 0, 0, 0, a);
            }
            ps.popPose();
        }
    }

    private static final float[][][] CUBE = {   // face -> 4 corners (unit cube), normal last
        {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}, {0, -1, 0}},
        {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}, {0, 1, 0}},
        {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}, {0, 0, -1}},
        {{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}, {0, 0, 1}},
        {{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}, {-1, 0, 0}},
        {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 0}},
    };

    private static void cube(VertexConsumer vc, PoseStack.Pose pose, net.minecraft.client.renderer.texture.TextureAtlasSprite sp, int light) {
        Matrix4f m = pose.pose();
        float[][] uv = {{sp.getU0(), sp.getV1()}, {sp.getU0(), sp.getV0()}, {sp.getU1(), sp.getV0()}, {sp.getU1(), sp.getV1()}};
        for (float[][] face : CUBE) {
            float[] n = face[4];
            for (int i = 0; i < 4; i++) {
                vc.addVertex(m, face[i][0], face[i][1], face[i][2]).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                  .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n[0], n[1], n[2]);
            }
        }
    }

    private static void litVertex(VertexConsumer vc, PoseStack.Pose pose, Matrix4f m, float x, float y, float u, float v, int alpha, int shade) {
        vc.addVertex(m, x, y, 0).setColor(shade, shade, shade, alpha).setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 0f, 1f);
    }

    // ------------------------------------------------------------------ aura, pulses, wind, sparks
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0f : Math.min(0.25f, (now - lastNanos) / 1e9f);
        lastNanos = now;

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float seconds = (level.getGameTime() + partial) / 20f;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // rocks/dust/ki first and flushed, so the solid rocks are already in the depth buffer when the aura is drawn:
        // a rock in front hides its bit of aura, a rock behind it gets tinted by it, and nothing pokes holes in it
        if (!FX.isEmpty()) {
            renderFx(poseStack, buffers, camera, cam, partial, level);
            buffers.endBatch();
        }
        boolean any = false;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pe)) continue;
            boolean on = isRaging(pe);
            float fade = FADE.getOrDefault(pe.getId(), 0f);
            fade = on ? Math.min(1f, fade + dt * FADE_PER_SECOND) : Math.max(0f, fade - dt * FADE_PER_SECOND);
            if (fade <= 0f) {
                FADE.remove(pe.getId());
                continue;
            }
            FADE.put(pe.getId(), fade);
            any = true;

            int id = pe.getId();
            Vec3 pos = pe.getPosition(partial);
            float h = Math.max(0.5f, pe.getBbHeight());
            float t = seconds * FPS + (id % 7) * 2.3f;
            int a = Math.floorMod((int) Math.floor(t), FRAMES);
            int b = (a + 1) % FRAMES;
            float w = (float) (t - Math.floor(t));

            poseStack.pushPose();
            poseStack.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

            float boost = RageBurst.auraBoost(id, level.getGameTime() + partial);
            boolean golden = isGolden(pe);

            // the ground glows red (gold) under him (stronger while the aura flares up after the burst)
            VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
            groundGlow(glow, poseStack, 1.4f * h * boost, 0.02f, fade * 0.43f * Math.min(1f, boost - 0.3f), golden);

            // ground pulses
            VertexConsumer ring = buffers.getBuffer(RenderType.entityTranslucentEmissive(golden ? RING_GOLD : RING));
            for (int k = 0; k < 2; k++) {
                float p = (float) ((seconds * PULSES_PER_SECOND + k * 0.5f + (id % 5) * 0.13f) % 1.0);
                float r = RING_HALF * h * (0.35f + 0.85f * p);
                groundQuad(ring, poseStack, r, 0.03f + 0.002f * k, a, fade * PULSE_ALPHA * (float) Math.sin(Math.PI * p));
            }

            // wind streaks running outward along the ground (not for the golden ones: no ground effects)
            VertexConsumer wind = buffers.getBuffer(RenderType.entityTranslucentEmissive(WIND));
            for (int k = 0; k < (golden ? 0 : WIND_STREAKS); k++) {
                float speed = 0.9f + 0.6f * hash(id, k, 1);
                float p = (float) ((seconds * speed + hash(id, k, 2)) % 1.0);
                int cycle = (int) Math.floor(seconds * speed + hash(id, k, 2));
                float ang = (float) (2 * Math.PI * hash(id, k, 100 + cycle));
                float r0 = h * (0.3f + 1.3f * p);
                float len = h * (0.35f + 0.25f * hash(id, k, 3));
                float al = fade * 0.55f * (float) Math.sin(Math.PI * p);
                windStreak(wind, poseStack, ang, r0, len, h * 0.045f, 0.06f + 0.05f * hash(id, k, 4) * h, al);
            }

            // the 3D aura around him (world-aligned, from his feet): shell with rim glow + flame tongues
            AuraMesh.draw(buffers, poseStack, (float) (cam.x - pos.x), (float) (cam.y - pos.y), (float) (cam.z - pos.z),
                h, boost, fade * Math.min(1f, 0.8f * (float) Math.pow(boost, 1.2)), seconds, id, golden, 16);

            // the sparks live in the camera-facing frame
            double dx = cam.x - pos.x, dz = cam.z - pos.z;
            poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(dx, dz)));

            VertexConsumer bolts = buffers.getBuffer(RenderType.lightning());
            if (golden) {
                // Super Saiyan 2 lightning: bright cyan-white bolts crackling all around him, some in front of his body
                for (int k = 0; k < GOLD_BOLT_SLOTS; k++) {
                    int slot = (int) Math.floor(seconds * 12f + k * 0.29f);
                    if (hash(id, k, slot) > 0.5f) continue;
                    lightning(bolts, poseStack.last().pose(), h, id * 37 + k * 11 + slot * 173, fade, 1f, k % 2 == 0 ? 0.4f * h : 0.02f);
                }
            } else {
                for (int k = 0; k < SPARK_SLOTS; k++) {
                    int slot = (int) Math.floor(seconds * 9f + k * 0.37f);
                    if (hash(id, k, slot) > 0.55f) continue;                     // crackles on and off
                    spark(bolts, poseStack, h, id * 31 + k * 7 + slot * 131, fade);
                }
            }
            poseStack.popPose();
        }
        if (any) {
            buffers.endBatch();
        }
        if (FADE.size() > 256) FADE.clear();
    }

    private static void billboard(VertexConsumer vc, PoseStack ps, float half, float cy, int frame, float alpha) {
        if (alpha <= 0.003f) return;
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float u0 = frame / (float) FRAMES, u1 = (frame + 1) / (float) FRAMES;
        int al = Math.round(alpha * 255);
        vertex(vc, pose, m, -half, cy - half, 0, u0, 1, al);
        vertex(vc, pose, m, half, cy - half, 0, u1, 1, al);
        vertex(vc, pose, m, half, cy + half, 0, u1, 0, al);
        vertex(vc, pose, m, -half, cy + half, 0, u0, 0, al);
    }

    private static void groundQuad(VertexConsumer vc, PoseStack ps, float half, float y, int frame, float alpha) {
        if (alpha <= 0.003f) return;
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float u0 = frame / (float) FRAMES, u1 = (frame + 1) / (float) FRAMES;
        int al = Math.round(alpha * 255);
        vertex(vc, pose, m, -half, y, -half, u0, 0, al);
        vertex(vc, pose, m, -half, y, half, u0, 1, al);
        vertex(vc, pose, m, half, y, half, u1, 1, al);
        vertex(vc, pose, m, half, y, -half, u1, 0, al);
    }

    static boolean isGolden(PokemonEntity pe) {
        com.multiverseshinies.RageSpecies sp = com.multiverseshinies.RageSpecies.of(pe);
        return sp != null && sp.golden;
    }

    /**
     * a jagged Super Saiyan 2 lightning bolt (camera-facing frame): a cyan glow with a white-hot core, plus a short
     * fork. Starts somewhere around his body; `scale` makes it bigger (the burst uses bigger ones).
     */
    static void lightning(VertexConsumer vc, Matrix4f m, float h, int seed, float fade, float scale, float z) {
        java.util.Random r = new java.util.Random(seed);
        float x = (r.nextFloat() - 0.5f) * h * scale;
        float y = h * (0.05f + 1.15f * r.nextFloat());
        float ang = (float) (r.nextFloat() * Math.PI * 2);
        float dirx = (float) Math.cos(ang), diry = (float) Math.sin(ang);
        int segs = 5 + r.nextInt(4);
        float seg = h * (0.05f + 0.05f * r.nextFloat()) * scale;
        float wide = h * 0.022f * scale, core = h * 0.007f * scale;
        int ga = Math.round(170 * fade), ca = Math.round(255 * fade);
        for (int i = 0; i < segs; i++) {
            float nx = x + (dirx + (r.nextFloat() - 0.5f) * 1.6f) * seg;
            float ny = y + (diry + (r.nextFloat() - 0.5f) * 1.6f) * seg;
            float ex = ny - y, ey = -(nx - x);
            float el = (float) Math.sqrt(ex * ex + ey * ey); if (el < 1e-5f) el = 1;
            quad(vc, m, x, y, nx, ny, ex / el * wide, ey / el * wide, z, 110, 225, 255, ga);
            quad(vc, m, x, y, nx, ny, ex / el * core, ey / el * core, z + 0.001f, 240, 252, 255, ca);
            if (i == segs / 2) {                                    // a little fork
                float bx = nx + (-diry + (r.nextFloat() - 0.5f)) * seg, by = ny + (dirx + (r.nextFloat() - 0.5f)) * seg;
                quad(vc, m, nx, ny, bx, by, ex / el * wide * 0.7f, ey / el * wide * 0.7f, z, 110, 225, 255, ga);
                quad(vc, m, nx, ny, bx, by, ex / el * core, ey / el * core, z + 0.001f, 240, 252, 255, ca);
            }
            x = nx; y = ny;
        }
    }

    private static void groundGlow(VertexConsumer vc, PoseStack ps, float half, float y, float alpha, boolean golden) {
        if (alpha <= 0.003f) return;
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        int al = Math.round(alpha * 255);
        float[][] c = {{-half, -half, 0, 0}, {-half, half, 0, 1}, {half, half, 1, 1}, {half, -half, 1, 0}};
        for (float[] q : c) {
            vc.addVertex(m, q[0], y, q[1]).setColor(255, golden ? 190 : 70, golden ? 50 : 40, al).setUv(q[2], q[3])
              .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
        }
    }

    /** flat streak on the ground, pointing outward along angle ang, from radius r0 */
    private static void windStreak(VertexConsumer vc, PoseStack ps, float ang, float r0, float len, float halfW, float y, float alpha) {
        if (alpha <= 0.003f) return;
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float cx = (float) Math.cos(ang), cz = (float) Math.sin(ang);
        float px = -cz * halfW, pz = cx * halfW;
        float x0 = cx * r0, z0 = cz * r0, x1 = cx * (r0 + len), z1 = cz * (r0 + len);
        int al = Math.round(alpha * 255);
        // u runs along the streak (tail at u=0, head at u=1)
        vertex(vc, pose, m, x0 - px, y, z0 - pz, 0, 1, al);
        vertex(vc, pose, m, x0 + px, y, z0 + pz, 0, 0, al);
        vertex(vc, pose, m, x1 + px, y, z1 + pz, 1, 0, al);
        vertex(vc, pose, m, x1 - px, y, z1 - pz, 1, 1, al);
    }

    /** a short jagged lightning spark near the aura edge (in the camera-facing frame) */
    private static void spark(VertexConsumer vc, PoseStack ps, float h, int seed, float fade) {
        Matrix4f m = ps.last().pose();
        java.util.Random r = new java.util.Random(seed);
        float side = r.nextBoolean() ? 1f : -1f;
        float x = side * h * (0.25f + 0.4f * r.nextFloat());
        float y = h * (0.1f + 1.2f * r.nextFloat());
        float z = 0.02f;
        int segs = 4 + r.nextInt(4);
        float seg = h * (0.06f + 0.05f * r.nextFloat());
        float dirx = side * (0.2f + 0.6f * r.nextFloat()), diry = 0.6f + 0.6f * r.nextFloat();
        float n = (float) Math.sqrt(dirx * dirx + diry * diry); dirx /= n; diry /= n;
        for (int i = 0; i < segs; i++) {
            float nx = x + (dirx + (r.nextFloat() - 0.5f) * 1.4f) * seg;
            float ny = y + (diry + (r.nextFloat() - 0.5f) * 1.0f) * seg;
            float ex = ny - y, ey = -(nx - x);
            float el = (float) Math.sqrt(ex * ex + ey * ey); if (el < 1e-5f) el = 1;
            float wide = h * 0.022f, core = h * 0.008f;
            quad(vc, m, x, y, nx, ny, ex / el * wide, ey / el * wide, z, 255, 70, 60, (int) (150 * fade));
            quad(vc, m, x, y, nx, ny, ex / el * core, ey / el * core, z + 0.001f, 255, 235, 225, (int) (255 * fade));
            x = nx; y = ny;
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float ox, float oy, float z,
                             int cr, int cg, int cb, int ca) {
        vc.addVertex(m, x0 - ox, y0 - oy, z).setColor(cr, cg, cb, ca);
        vc.addVertex(m, x1 - ox, y1 - oy, z).setColor(cr, cg, cb, ca);
        vc.addVertex(m, x1 + ox, y1 + oy, z).setColor(cr, cg, cb, ca);
        vc.addVertex(m, x0 + ox, y0 + oy, z).setColor(cr, cg, cb, ca);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Matrix4f m, float x, float y, float z, float u, float v, int alpha) {
        vc.addVertex(m, x, y, z)
          .setColor(255, 255, 255, alpha)
          .setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal(pose, 0f, 1f, 0f);
    }
}
