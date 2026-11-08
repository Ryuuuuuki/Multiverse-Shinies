package com.multiverseshinies.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The moment a transforming Multiverse Pokemon bursts into the rage (the Infernape's roar, the Monferno's Power Pole
 * hitting the ground), made of the aura itself: the aura blasts out in two expanding shells of the same aura drawings,
 * the aura flares up big and settles, aura rings rush out along the ground, the screen flashes with a vignette that
 * lingers, and the camera shakes; the screen part scaled by how close and how in view it is.
 * Infernape: red, with a burst of rocks and dust. Monferno (golden): gold, with a storm of lightning instead.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class RageBurst {
    private static final ResourceLocation AURA = tex("rage_aura");
    private static final ResourceLocation RING = tex("rage_ring");
    private static final ResourceLocation VIGNETTE = tex("rage_vignette");
    private static final ResourceLocation AURA_GOLD = tex("rage_aura_gold");
    private static final ResourceLocation RING_GOLD = tex("rage_ring_gold");
    private static final float BOLT_TIME = 0.6f;       // the golden burst's lightning storm
    private static final int FRAMES = 16;
    private static final float FPS = 9f;
    private static final float DURATION = 1.8f;        // seconds
    private static final float SHELL_TIME = 0.45f;     // each aura shell blasting outward
    private static final float RING_TIME = 0.9f;       // each ground ring
    private static final float FLASH_ALPHA = 0.25f;    // red-orange flash over the screen, next to him and looking at him
    private static final float VIGNETTE_ALPHA = 0.45f; // red vignette
    private static final float FLASH_RANGE = 40f;      // blocks
    private static final float SHAKE_DEGREES = 1.8f;

    /** entity id -> game time (ticks) of the burst */
    private static final Map<Integer, Float> START = new HashMap<>();
    private static final Set<Integer> WAS_RAGING = new HashSet<>();

    private RageBurst() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "textures/aura/" + name + ".png");
    }

    private static float now(ClientLevel level, float partial) {
        return level.getGameTime() + partial;
    }

    /** seconds since this entity's burst, or -1 */
    private static float age(int id, float time) {
        Float s = START.get(id);
        if (s == null) return -1f;
        float a = (time - s) / 20f;
        return a < 0 || a > DURATION ? -1f : a;
    }

    /** the aura flares up bigger and stronger right after the burst, then settles */
    static float auraBoost(int id, float time) {
        float a = age(id, time);
        if (a < 0 || a > 1.3f) return 1f;
        float k = 1f - a / 1.3f;
        return 1f + 0.55f * k * k;
    }

    private static float easeOut(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return 1f - (1f - x) * (1f - x) * (1f - x);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { START.clear(); WAS_RAGING.clear(); return; }
        if (mc.isPaused()) return;
        float time = now(level, 0f);
        Set<Integer> raging = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PokemonEntity pe) || !RageAuraRenderer.isRaging(pe)) continue;
            raging.add(pe.getId());
            if (!WAS_RAGING.contains(pe.getId())) {
                START.put(pe.getId(), time);
                if (!RageAuraRenderer.isGolden(pe)) RageAuraRenderer.burstParticles(pe);   // rocks and dust
            }
        }
        WAS_RAGING.clear();
        WAS_RAGING.addAll(raging);
        START.values().removeIf(s -> (time - s) / 20f > DURATION);
    }

    // ------------------------------------------------------------------ in the world
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || START.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = now(level, partial);
        float seconds = time / 20f;
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        boolean any = false;
        for (Map.Entry<Integer, Float> e : START.entrySet()) {
            if (!(level.getEntity(e.getKey()) instanceof PokemonEntity pe)) continue;
            float a = age(e.getKey(), time);
            if (a < 0) continue;
            any = true;
            int id = pe.getId();
            float h = Math.max(0.5f, pe.getBbHeight());
            Vec3 pos = pe.getPosition(partial);
            boolean golden = RageAuraRenderer.isGolden(pe);

            ps.pushPose();
            ps.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

            // aura rings rushing out along the ground
            VertexConsumer ring = buffers.getBuffer(RenderType.entityTranslucentEmissive(golden ? RING_GOLD : RING));
            int base = Math.floorMod((int) Math.floor(seconds * FPS), FRAMES);
            for (int k = 0; k < 3; k++) {
                float ra = a - k * 0.12f;
                if (ra < 0 || ra > RING_TIME) continue;
                float r = h * (0.5f + 5.0f * easeOut(ra / RING_TIME));
                ground(ring, ps, r, 0.05f + 0.01f * k, (base + 5 * k) % FRAMES, 0.95f * (1f - ra / RING_TIME));
            }

            // the aura blasting out of him: the 3D aura blown up and fading, two waves
            for (int k = 0; k < 2; k++) {
                float sa = a - k * 0.14f;
                if (sa < 0 || sa > SHELL_TIME) continue;
                float g = easeOut(sa / SHELL_TIME);
                float x = 1f - sa / SHELL_TIME;
                AuraMesh.draw(buffers, ps, (float) (cam.x - pos.x), (float) (cam.y - pos.y), (float) (cam.z - pos.z),
                    h, 1f + 2.4f * g, 0.85f * x * x, seconds, id * 7 + k, golden, 26);
            }
            double dx = cam.x - pos.x, dz = cam.z - pos.z;
            ps.mulPose(Axis.YP.rotation((float) Math.atan2(dx, dz)));
            // golden: a storm of big bolts lashing out of him, flickering fast and dying away
            if (golden && a < BOLT_TIME) {
                VertexConsumer bolts = buffers.getBuffer(RenderType.lightning());
                float fade = 1f - a / BOLT_TIME;
                int slot = (int) Math.floor(a * 30f);
                for (int k = 0; k < 14; k++) {
                    if (RageAuraRenderer.hash(id, k, slot) > 0.7f) continue;
                    RageAuraRenderer.lightning(bolts, ps.last().pose(), h, id * 53 + k * 19 + slot * 211, fade,
                        1.6f + 0.8f * RageAuraRenderer.hash(id, k, 7), k % 2 == 0 ? 0.45f * h : 0.02f);
                }
            }
            ps.popPose();
        }
        if (any) buffers.endBatch();
    }

    private static void billboard(VertexConsumer vc, PoseStack ps, float half, float cy, int frame, float alpha, float z) {
        if (alpha <= 0.004f) return;
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float u0 = frame / (float) FRAMES, u1 = (frame + 1) / (float) FRAMES;
        int al = Math.round(alpha * 255);
        v(vc, pose, m, -half, cy - half, z, u0, 1, al);
        v(vc, pose, m, half, cy - half, z, u1, 1, al);
        v(vc, pose, m, half, cy + half, z, u1, 0, al);
        v(vc, pose, m, -half, cy + half, z, u0, 0, al);
    }

    private static void ground(VertexConsumer vc, PoseStack ps, float half, float y, int frame, float alpha) {
        if (alpha <= 0.004f) return;
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float u0 = frame / (float) FRAMES, u1 = (frame + 1) / (float) FRAMES;
        int al = Math.round(alpha * 255);
        v(vc, pose, m, -half, y, -half, u0, 0, al);
        v(vc, pose, m, -half, y, half, u0, 1, al);
        v(vc, pose, m, half, y, half, u1, 1, al);
        v(vc, pose, m, half, y, -half, u1, 0, al);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, Matrix4f m, float x, float y, float z, float u, float vv, int a) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, a).setUv(u, vv)
          .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
    }

    // ------------------------------------------------------------------ on the screen
    /** 0..1: how hard a burst hits this viewer (distance and whether he's in front of the camera) */
    private static float exposure(PokemonEntity pe, Vec3 cam, Vec3 look) {
        Vec3 c = pe.position().add(0, pe.getBbHeight() * 0.6, 0);
        Vec3 d = c.subtract(cam);
        double dist = d.length();
        if (dist > FLASH_RANGE) return 0f;
        float near = (float) (1.0 - dist / FLASH_RANGE);
        double dot = dist < 1e-3 ? 1 : look.dot(d.scale(1 / dist));
        float facing = dot <= 0 ? 0.3f : 0.3f + 0.7f * (float) dot;
        return near * near * facing;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (START.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 look = new Vec3(camera.getLookVector());
        float time = now(level, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        float flash = 0f, vignette = 0f;
        boolean golden = false;
        for (Map.Entry<Integer, Float> e : START.entrySet()) {
            if (!(level.getEntity(e.getKey()) instanceof PokemonEntity pe)) continue;
            float a = age(e.getKey(), time);
            if (a < 0) continue;
            float ex = exposure(pe, camera.getPosition(), look);
            if (ex > 0.01f && RageAuraRenderer.isGolden(pe)) golden = true;
            if (a < 0.55f) {
                float k = a < 0.04f ? a / 0.04f : (float) Math.pow(1f - (a - 0.04f) / 0.51f, 2);
                flash = Math.max(flash, k * ex);
            }
            if (a < 1.5f) vignette = Math.max(vignette, (float) Math.pow(1f - a / 1.5f, 1.5) * ex);
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        if (vignette > 0.004f) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (golden) RenderSystem.setShaderColor(215 / 255f, 160 / 255f, 20 / 255f, VIGNETTE_ALPHA * vignette);
            else RenderSystem.setShaderColor(190 / 255f, 25 / 255f, 20 / 255f, VIGNETTE_ALPHA * vignette);
            g.blit(VIGNETTE, 0, 0, w, h, 0f, 0f, 256, 256, 256, 256);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
        if (flash > 0.004f) {
            int al = Math.round(255 * FLASH_ALPHA * flash);
            int rgb = golden ? (255 << 16) | (225 << 8) | 120 : (255 << 16) | (110 << 8) | 70;
            g.fill(0, 0, w, h, (al << 24) | rgb);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (START.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float time = now(level, (float) event.getPartialTick());
        Vec3 cam = event.getCamera().getPosition();
        float s = 0f;
        for (Map.Entry<Integer, Float> e : START.entrySet()) {
            if (!(level.getEntity(e.getKey()) instanceof PokemonEntity pe)) continue;
            float a = age(e.getKey(), time);
            if (a < 0 || a > 0.8f) continue;
            float k = 1f - a / 0.8f;
            Vec3 d = pe.position().subtract(cam);
            float near = (float) Math.max(0, 1 - d.length() / FLASH_RANGE);
            s = Math.max(s, k * k * near);
        }
        if (s <= 0.002f) return;
        float t = time * 2.6f;
        event.setYaw(event.getYaw() + SHAKE_DEGREES * s * (float) Math.sin(t * 5.3));
        event.setPitch(event.getPitch() + SHAKE_DEGREES * s * (float) Math.sin(t * 6.1 + 1));
        event.setRoll(event.getRoll() + SHAKE_DEGREES * 0.8f * s * (float) Math.sin(t * 4.7 + 2));
    }
}
