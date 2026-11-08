package com.multiverseshinies.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import java.util.Map;

/**
 * The evolution light of the Multiverse cutscene: light pours out of the evolving Pokemon and keeps growing while the
 * Shenlong circles it (a glowing core, god rays turning around it, a pillar of light to the sky, light gathering in
 * with rising chimes), until Cobblemon swaps the species at 11.2 s: white-out flash, shockwaves along the ground and
 * in the air, the rays shoot out, a burst of sparkles and the camera shakes; then a golden afterglow fades out.
 * Additive (eyes render type), textures premultiplied.
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class EvolutionLight {
    static final double SWAP = 11.2;                     // Cobblemon swaps the species here
    private static final double END = SWAP + 3.0;

    private static final ResourceLocation RAY = tex("ray");
    private static final ResourceLocation RING = tex("shock_ring");
    private static final ResourceLocation GLOW = tex("glow_add");

    private static final float FLASH_RANGE = 64f;
    private static final float SHAKE_DEGREES = 2.4f;

    /** scene start tick -> last cutscene time handled (for the one-shot sounds / bursts) */
    private static final Map<Long, Double> LAST = new HashMap<>();

    private EvolutionLight() {}

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "textures/evolution/" + n + ".png");
    }

    // ------------------------------------------------------------------ curves
    /** 0..1: how far the light has built up before the swap */
    private static double charge(double t) { return Math.pow(smooth((t - 0.6) / (SWAP - 0.6)), 1.4); }

    /** 0..1: the last stretch right before the swap, everything surges */
    private static double surge(double t) { return smooth((t - (SWAP - 1.4)) / 1.4); }

    private static double clamp01(double x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }

    private static double smooth(double x) { x = clamp01(x); return x * x * (3 - 2 * x); }

    private static double easeOut(double x) { x = clamp01(x); return 1 - (1 - x) * (1 - x) * (1 - x); }

    private static float hash(int a, int b, int c) { return RageAuraRenderer.hash(a, b, c); }

    private static final class Where {
        final Vec3 feet, mid;
        final double h;
        Where(Vec3 feet, double h) { this.feet = feet; this.h = h; this.mid = feet.add(0, h * 0.55, 0); }
    }

    private static Where where(EvolutionCutscene.Scene s, ClientLevel level, float partial) {
        if (level.getEntity(s.target) instanceof PokemonEntity pe) {
            return new Where(pe.getPosition(partial), Math.max(0.6, Math.min(3.0, pe.getBbHeight())));
        }
        return new Where(s.c, 1.0);
    }

    // ------------------------------------------------------------------ particles and sounds
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        if (EvolutionCutscene.scenes().isEmpty()) { LAST.clear(); return; }
        RandomSource rnd = level.random;
        for (EvolutionCutscene.Scene s : EvolutionCutscene.scenes()) {
            double t = EvolutionCutscene.time(s, level, 0);
            double last = LAST.getOrDefault(s.startTick, -1.0);
            LAST.put(s.startTick, t);
            if (t > END) continue;
            Where w = where(s, level, 0);
            double ch = charge(t);

            if (t < SWAP) {
                // light gathering into him from all around
                int n = (int) (1 + 7 * ch + 6 * surge(t));
                for (int i = 0; i < n; i++) {
                    Vec3 d = randomDir(rnd);
                    double r = w.h * (2.5 + 3.5 * rnd.nextDouble());
                    Vec3 p = w.mid.add(d.scale(r));
                    Vec3 v = w.mid.subtract(p).scale(1 / 22.0);
                    add(level, rnd.nextFloat() < 0.8f ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK, p, v);
                }
            } else if (t < SWAP + 2.2) {
                // afterglow: golden sparkles drifting up
                for (int i = 0; i < 3; i++) {
                    Vec3 p = w.feet.add((rnd.nextDouble() - 0.5) * w.h * 3, rnd.nextDouble() * w.h * 1.5, (rnd.nextDouble() - 0.5) * w.h * 3);
                    add(level, ParticleTypes.END_ROD, p, new Vec3(0, 0.04 + 0.04 * rnd.nextDouble(), 0));
                }
            }

            // sounds on the way up
            if (crossed(last, t, 3.0)) sound(level, w.mid, SoundEvents.BEACON_POWER_SELECT, 1.6f, 0.7f);
            if (crossed(last, t, SWAP - 1.0)) sound(level, w.mid, SoundEvents.BEACON_ACTIVATE, 2.0f, 1.5f);
            if (t > 7.5 && t < SWAP) {
                int a = (int) Math.floor((last - 7.5) / 0.3), b = (int) Math.floor((t - 7.5) / 0.3);
                if (b != a && b >= 0) {
                    float pitch = (float) (0.6 + 1.4 * clamp01((t - 7.5) / (SWAP - 7.5)));
                    sound(level, w.mid, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, pitch);
                }
            }

            // the swap: everything goes off
            if (crossed(last, t, SWAP)) {
                sound(level, w.mid, SoundEvents.TOTEM_USE, 1.2f, 1.0f);
                sound(level, w.mid, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.7f);
                sound(level, w.mid, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.4f);
                for (int i = 0; i < 140; i++) add(level, ParticleTypes.END_ROD, w.mid, randomDir(rnd).scale(0.25 + 0.4 * rnd.nextDouble()));
                for (int i = 0; i < 60; i++) add(level, ParticleTypes.FIREWORK, w.mid, randomDir(rnd).scale(0.3 + 0.3 * rnd.nextDouble()));
                for (int i = 0; i < 90; i++) add(level, ParticleTypes.TOTEM_OF_UNDYING, w.mid, randomDir(rnd).scale(0.5 + 0.5 * rnd.nextDouble()));
            }
        }
        if (LAST.size() > 32) LAST.clear();
    }

    private static boolean crossed(double last, double t, double at) { return last < at && t >= at; }

    private static Vec3 randomDir(RandomSource rnd) {
        double z = rnd.nextDouble() * 2 - 1, a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(1 - z * z);
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }

    private static void add(ClientLevel level, ParticleOptions type, Vec3 p, Vec3 v) {
        level.addParticle(type, true, p.x, p.y, p.z, v.x, v.y, v.z);
    }

    private static void sound(ClientLevel level, Vec3 at, SoundEvent ev, float vol, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, ev, SoundSource.NEUTRAL, vol, pitch, false);
    }

    // ------------------------------------------------------------------ the light itself
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || EvolutionCutscene.scenes().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        ResourceLocation[] passes = {GLOW, RAY, RING};
        for (int pass = 0; pass < passes.length; pass++) {        // one batch per texture
            RenderType type = RenderType.eyes(passes[pass]);
            VertexConsumer vc = buffers.getBuffer(type);
            for (EvolutionCutscene.Scene s : EvolutionCutscene.scenes()) {
                double t = EvolutionCutscene.time(s, level, partial);
                if (t < 0 || t > END) continue;
                Where w = where(s, level, partial);
                Vec3 feet = w.feet.subtract(cam), mid = w.mid.subtract(cam);
                double h = w.h, ch = charge(t), sg = surge(t), a = t - SWAP;
                double post = a >= 0 ? 1 - smooth(a / 1.6) : 0;
                double I = a < 0 ? 0.15 + 0.85 * ch : post;          // overall intensity

                if (pass == 0) {
                    // the ground lights up under him
                    double gr = h * (1.2 + 4 * ch + 3 * sg) + (a >= 0 ? h * 6 * easeOut(a / 0.6) : 0);
                    flat(vc, pose, feet.add(0, 0.06, 0), gr, 255, 205, 120, 0.7 * I);
                    // the glowing core (and the blinding ball at the swap)
                    double core = h * (0.7 + 1.8 * ch + 3 * sg) * (1 + 0.15 * Math.sin(t * (5 + 15 * ch)));
                    double coreI = a < 0 ? 0.3 + 0.7 * ch : 0;
                    if (a >= 0) { core = h * (4 + 10 * easeOut(a / 0.25)); coreI = 1 - smooth(a / 0.8); }
                    billboard(vc, pose, mid, core * 2.6, 255, 190, 90, 0.55 * coreI);
                    billboard(vc, pose, mid, core, 255, 250, 235, coreI);
                } else if (pass == 1) {
                    // pillar of light to the sky
                    double pil = a < 0 ? smooth((t - 3) / 2) * (0.25 + 0.75 * ch) : post * 1.3;
                    if (pil > 0.004) {
                        double pw = h * (0.18 + 0.9 * ch + 0.8 * sg) + (a >= 0 ? h * 3 * smooth(a / 0.3) : 0);
                        pillar(vc, pose, feet, pw * 2.2, 90, 255, 200, 110, 0.45 * pil);
                        pillar(vc, pose, feet, pw, 90, 255, 250, 230, pil);
                    }
                    // god rays turning around him
                    int n = a < 0 ? (int) (5 + 25 * ch + 10 * sg) : 40;
                    for (int k = 0; k < n; k++) {
                        double az = Math.PI * 2 * hash(k, 1, s.target) + t * (0.25 + 0.5 * hash(k, 2, 0)) * (k % 2 == 0 ? 1 : -1);
                        double el = Math.toRadians(-20 + 95 * hash(k, 3, 0));
                        Vec3 dir = new Vec3(Math.cos(el) * Math.cos(az), Math.sin(el), Math.cos(el) * Math.sin(az));
                        double flick = 0.7 + 0.3 * Math.sin(t * (3 + 4 * hash(k, 5, 0)) + 6.28 * hash(k, 6, 0));
                        double len = h * (1.2 + 6 * ch + 9 * sg) * (0.55 + 0.45 * hash(k, 4, 0)) * flick;
                        if (a >= 0) len = h * 16 * (0.55 + 0.45 * hash(k, 4, 0)) * (0.3 + 0.7 * easeOut(a / 0.5));
                        double in = I * (0.35 + 0.65 * hash(k, 7, 0));
                        godRay(vc, pose, mid, dir, len, h * 0.06, len * (0.10 + 0.08 * hash(k, 8, 0)), 255, 235, 180, in);
                    }
                } else if (a >= 0 && a < 1.3) {
                    // shockwaves: two along the ground, one in the air
                    for (int i = 0; i < 2; i++) {
                        double k = (a - 0.18 * i) / 0.9;
                        if (k < 0 || k > 1) continue;
                        flat(vc, pose, feet.add(0, 0.1 + 0.02 * i, 0), 18 * easeOut(k), 255, 225, 160, Math.pow(1 - k, 1.5));
                    }
                    double k = a / 0.6;
                    if (k <= 1) billboard(vc, pose, mid, 11 * easeOut(k), 255, 245, 220, Math.pow(1 - k, 1.5));
                }
            }
            buffers.endBatch(type);
        }
    }

    // additive: the color carries the intensity (alpha is not used by the blend)
    private static void vtx(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, int r, int g, int b, double in, Vec3 n) {
        double k = Math.max(0, Math.min(1, in));
        vc.addVertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z)
          .setColor((int) (r * k), (int) (g * k), (int) (b * k), 255).setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    /** both windings: the eyes render type culls back faces */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3[] p, float[][] uv, int r, int g, int b, double in, Vec3 n) {
        for (int i = 0; i < 4; i++) vtx(vc, pose, p[i], uv[i][0], uv[i][1], r, g, b, in, n);
        for (int i = 3; i >= 0; i--) vtx(vc, pose, p[i], uv[i][0], uv[i][1], r, g, b, in, n);
    }

    private static void flat(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, double half, int r, int g, int b, double in) {
        if (in <= 0.004 || half <= 0.01) return;
        Vec3 up = new Vec3(0, 1, 0);
        quad(vc, pose, new Vec3[]{c.add(-half, 0, -half), c.add(-half, 0, half), c.add(half, 0, half), c.add(half, 0, -half)}, new float[][]{{0f, 0f}, {0f, 1f}, {1f, 1f}, {1f, 0f}}, r, g, b, in, up);
    }

    private static void billboard(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, double half, int r, int g, int b, double in) {
        if (in <= 0.004 || half <= 0.01 || c.lengthSqr() < 1e-6) return;
        Vec3 n = c.normalize().scale(-1);
        Vec3 u = n.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1e-6) u = new Vec3(1, 0, 0);
        u = u.normalize().scale(half);
        Vec3 v = u.cross(n).normalize().scale(half);
        quad(vc, pose, new Vec3[]{c.subtract(u).subtract(v), c.add(u).subtract(v), c.add(u).add(v), c.subtract(u).add(v)}, new float[][]{{0f, 1f}, {1f, 1f}, {1f, 0f}, {0f, 0f}}, r, g, b, in, n);
    }

    /** a ray from c along dir, widening, facing the camera (positions are camera-relative) */
    private static void godRay(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, Vec3 dir, double len, double wBase, double wTip,
                               int r, int g, int b, double in) {
        if (in <= 0.004 || len <= 0.05) return;
        Vec3 side = dir.cross(c);
        if (side.lengthSqr() < 1e-9) side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-9) return;
        side = side.normalize();
        Vec3 tip = c.add(dir.scale(len));
        Vec3 n = c.normalize().scale(-1);
        quad(vc, pose, new Vec3[]{c.subtract(side.scale(wBase)), c.add(side.scale(wBase)), tip.add(side.scale(wTip)), tip.subtract(side.scale(wTip))}, new float[][]{{0f, 0f}, {1f, 0f}, {1f, 1f}, {0f, 1f}}, r, g, b, in, n);
    }

    /** vertical beam from the feet up, turned toward the camera around the vertical axis */
    private static void pillar(VertexConsumer vc, PoseStack.Pose pose, Vec3 feet, double half, double height, int r, int g, int b, double in) {
        if (in <= 0.004) return;
        Vec3 side = new Vec3(-feet.z, 0, feet.x);
        if (side.lengthSqr() < 1e-9) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(half);
        Vec3 top = feet.add(0, height, 0);
        Vec3 n = new Vec3(-feet.x, 0, -feet.z);
        n = n.lengthSqr() < 1e-9 ? new Vec3(0, 0, 1) : n.normalize();
        quad(vc, pose, new Vec3[]{feet.subtract(side), feet.add(side), top.add(side), top.subtract(side)}, new float[][]{{0f, 0f}, {1f, 0f}, {1f, 1f}, {0f, 1f}}, r, g, b, in, n);
    }

    // ------------------------------------------------------------------ white-out and shake
    private static float exposure(Vec3 at, Camera camera) {
        Vec3 d = at.subtract(camera.getPosition());
        float near = (float) Math.max(0, 1 - d.length() / FLASH_RANGE);
        Vec3 look = new Vec3(camera.getLookVector());
        float facing = (float) (0.4 + 0.6 * Math.max(0, d.normalize().dot(look)));
        return near * facing;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (EvolutionCutscene.scenes().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = mc.gameRenderer.getMainCamera();
        float flash = 0;
        for (EvolutionCutscene.Scene s : EvolutionCutscene.scenes()) {
            double t = EvolutionCutscene.time(s, level, partial), a = t - SWAP;
            double f = 0.18 * surge(t);                                         // the light floods in before
            if (a >= 0 && a < 1.06) f = a < 0.06 ? Math.max(f, a / 0.06) : Math.pow(1 - (a - 0.06) / 1.0, 2);
            flash = Math.max(flash, (float) f * exposure(where(s, level, partial).mid, camera));
        }
        if (flash <= 0.004f) return;
        GuiGraphics g = event.getGuiGraphics();
        int al = Math.round(255 * 0.92f * Math.min(1, flash));
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (al << 24) | (255 << 16) | (250 << 8) | 235);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (EvolutionCutscene.scenes().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = (float) event.getPartialTick();
        float s = 0;
        double seconds = (level.getGameTime() + partial) / 20.0;
        for (EvolutionCutscene.Scene sc : EvolutionCutscene.scenes()) {
            double t = EvolutionCutscene.time(sc, level, partial), a = t - SWAP;
            double k = 0.12 * surge(t);                                         // rumble building up
            if (a >= 0 && a < 0.9) k = Math.max(k, Math.pow(1 - a / 0.9, 2));
            Vec3 d = where(sc, level, partial).mid.subtract(event.getCamera().getPosition());
            s = Math.max(s, (float) (k * Math.max(0, 1 - d.length() / FLASH_RANGE)));
        }
        if (s <= 0.002f) return;
        double x = seconds * 2.6;
        event.setYaw(event.getYaw() + SHAKE_DEGREES * s * (float) Math.sin(x * 5.3));
        event.setPitch(event.getPitch() + SHAKE_DEGREES * s * (float) Math.sin(x * 6.1 + 1));
        event.setRoll(event.getRoll() + SHAKE_DEGREES * 0.8f * s * (float) Math.sin(x * 4.7 + 2));
    }
}
