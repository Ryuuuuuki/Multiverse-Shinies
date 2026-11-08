package com.multiverseshinies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The 3D ki aura, made to read from any angle (also from above):
 *  - a teardrop shell around the body, built every frame, with light streaks rising over it and a rim glow (opaque
 *    where the shell is seen edge-on, nearly clear where it faces the camera), so it's a bright outline with him visible
 *    inside, like an anime aura; its top breaks into flickering 3D flame spikes;
 *  - flame tongues cut from the hand-drawn aura key drawings, rooted on the shell and pointing up and outward, each one
 *    turned towards the camera around its own axis, rising along his sides.
 * Everything is in the entity's world-aligned frame (the pose stack translated to his feet); camX/Y/Z is the camera
 * relative to his feet. Drawn as: shell back faces, tongues, shell front faces (no depth write, so the order is the
 * blending order).
 */
final class AuraMesh {
    private static final ResourceLocation STREAKS = tex("aura3d_streaks");
    private static final ResourceLocation STREAKS_GOLD = tex("aura3d_streaks_gold");
    private static final ResourceLocation TONGUES = tex("aura3d_tongues");
    private static final ResourceLocation TONGUES_GOLD = tex("aura3d_tongues_gold");
    private static final int SEG = 28, ROWS = 16, TONGUE_SPRITES = 8;
    private static final float RADIUS = 0.62f;        // shell radius, in entity heights
    private static final float BOTTOM = -0.05f, TOP = 1.75f;   // shell span (before the spikes), in entity heights
    private static final float SPIKES = 0.3f;         // how high the flames at the top of the shell reach
    private static final float SCROLL = 0.55f;        // streaks rising, tiles per second

    private AuraMesh() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "textures/aura/" + name + ".png");
    }

    /** the whole aura: shell (back), tongues, shell (front) */
    static void draw(MultiBufferSource buffers, PoseStack ps, float camX, float camY, float camZ, float h, float scale,
                     float alpha, float seconds, int seed, boolean golden, int tongues) {
        if (alpha <= 0.003f) return;
        VertexConsumer shell = buffers.getBuffer(RenderType.entityTranslucentEmissive(golden ? STREAKS_GOLD : STREAKS));
        shell(shell, ps, camX, camY, camZ, h, scale, alpha, seconds, seed, false);
        if (tongues > 0) {
            VertexConsumer t = buffers.getBuffer(RenderType.entityTranslucentEmissive(golden ? TONGUES_GOLD : TONGUES));
            tongues(t, ps, camX, camY, camZ, h, scale, alpha, seconds, seed, tongues);
        }
        shell = buffers.getBuffer(RenderType.entityTranslucentEmissive(golden ? STREAKS_GOLD : STREAKS));
        shell(shell, ps, camX, camY, camZ, h, scale, alpha, seconds, seed, true);
    }

    private static float profile(float t) {
        // teardrop: a bit open at the feet, widest around the waist, closing towards the top
        return (float) Math.sin(Math.PI * (0.16f + 0.84f * Math.pow(t, 0.8)));
    }

    /** 0..1 flame spike height around the top at angle a */
    private static float spike(float a, float s, int seed) {
        float v = 0;
        v = Math.max(v, (float) Math.pow(0.5 + 0.5 * Math.sin(a * 5 + s * 3.1f + seed), 4));
        v = Math.max(v, (float) Math.pow(0.5 + 0.5 * Math.sin(a * 8 - s * 4.3f + seed * 1.7f), 6));
        v = Math.max(v, 0.8f * (float) Math.pow(0.5 + 0.5 * Math.sin(a * 13 + s * 6.7f + seed * 2.3f), 8));
        return v;
    }

    private static void point(float[] out, float a, float t, float h, float scale, float s, int seed) {
        float r = RADIUS * h * scale * profile(t);
        float y = h * (BOTTOM + (TOP - BOTTOM) * t) * (0.6f + 0.4f * scale) ;
        if (t > 0.55f) {                                   // the top breaks into flames
            float k = (float) Math.pow((t - 0.55f) / 0.45f, 1.4);
            float sp = spike(a, s, seed);
            y += h * SPIKES * scale * k * sp;
            r *= 1f + 0.35f * k * sp;
        }
        out[0] = r * (float) Math.cos(a);
        out[1] = y;
        out[2] = r * (float) Math.sin(a);
    }

    private static void shell(VertexConsumer vc, PoseStack ps, float camX, float camY, float camZ, float h, float scale,
                              float alpha, float s, int seed, boolean front) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float[][][] P = new float[ROWS + 1][SEG + 1][3];
        for (int j = 0; j <= ROWS; j++) {
            for (int i = 0; i <= SEG; i++) {
                point(P[j][i], (float) (2 * Math.PI * i / SEG), j / (float) ROWS, h, scale, s, seed);
            }
        }
        // vertex normals (outward) from the neighbouring points, for the rim glow
        float[][][] N = new float[ROWS + 1][SEG + 1][3];
        for (int j = 0; j <= ROWS; j++) {
            for (int i = 0; i <= SEG; i++) {
                float[] pa = P[j][Math.floorMod(i + 1, SEG)], pb = P[j][Math.floorMod(i - 1, SEG)];
                float[] pu = P[Math.min(ROWS, j + 1)][i], pd = P[Math.max(0, j - 1)][i];
                float ax = pa[0] - pb[0], ay = pa[1] - pb[1], az = pa[2] - pb[2];
                float bx = pu[0] - pd[0], by = pu[1] - pd[1], bz = pu[2] - pd[2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float[] p = P[j][i];
                if (nx * p[0] + nz * p[2] + ny * 0.001f < 0) { nx = -nx; ny = -ny; nz = -nz; }
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (l < 1e-6f) { N[j][i][1] = 1; continue; }
                N[j][i][0] = nx / l; N[j][i][1] = ny / l; N[j][i][2] = nz / l;
            }
        }
        float o = 1f - (float) ((s * SCROLL) % 1.0);    // streaks scroll up (texture is 2 tiles tall)
        for (int j = 0; j < ROWS; j++) {
            for (int i = 0; i < SEG; i++) {
                float[] p00 = P[j][i], p10 = P[j][i + 1], p01 = P[j + 1][i], p11 = P[j + 1][i + 1];
                // face normal (outward) and which side of the shell it is on as seen from the camera
                float ax = p10[0] - p00[0], ay = p10[1] - p00[1], az = p10[2] - p00[2];
                float bx = p01[0] - p00[0], by = p01[1] - p00[1], bz = p01[2] - p00[2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float cx = (p00[0] + p11[0]) * 0.5f, cz = (p00[2] + p11[2]) * 0.5f;
                if (nx * cx + nz * cz < 0) { nx = -nx; ny = -ny; nz = -nz; }
                float vx = camX - cx, vy = camY - (p00[1] + p11[1]) * 0.5f, vz = camZ - cz;
                boolean facing = nx * vx + ny * vy + nz * vz > 0;
                if (facing != front) continue;
                float u0 = i / (float) SEG, u1 = (i + 1) / (float) SEG;
                float t0 = j / (float) ROWS, t1 = (j + 1) / (float) ROWS;
                vert(vc, pose, m, p00, N[j][i], u0, t0, o, camX, camY, camZ, alpha);
                vert(vc, pose, m, p10, N[j][i + 1], u1, t0, o, camX, camY, camZ, alpha);
                vert(vc, pose, m, p11, N[j + 1][i + 1], u1, t1, o, camX, camY, camZ, alpha);
                vert(vc, pose, m, p01, N[j + 1][i], u0, t1, o, camX, camY, camZ, alpha);
            }
        }
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose pose, Matrix4f m, float[] p, float[] n, float u, float t,
                             float o, float camX, float camY, float camZ, float alpha) {
        float tt = t;
        // rim glow: opaque where the shell is seen edge-on, nearly clear where it faces the camera
        float vx = camX - p[0], vy = camY - p[1], vz = camZ - p[2];
        float vl = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        float facing = vl > 1e-4f ? Math.abs((n[0] * vx + n[1] * vy + n[2] * vz) / vl) : 0;
        float edge = 1f - facing;
        float a = alpha * (0.07f + 0.93f * edge * edge);
        a *= Math.min(1f, tt / 0.12f);                               // soft at the feet
        if (tt > 0.8f) a *= 1f - (tt - 0.8f) / 0.2f * 0.85f;         // flame tips fade out
        int al = Math.round(Math.min(1f, a) * 255);
        float v = 0.5f * (t + o);
        vc.addVertex(m, p[0], p[1], p[2]).setColor(255, 255, 255, al).setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
    }

    private static float hash(int a, int b, int c) {
        return RageAuraRenderer.hash(a, b, c);
    }

    private static void tongues(VertexConsumer vc, PoseStack ps, float camX, float camY, float camZ, float h, float scale,
                                float alpha, float s, int seed, int count) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float[] base = new float[3];
        Vector3f axis = new Vector3f(), view = new Vector3f(), right = new Vector3f();
        for (int k = 0; k < count; k++) {
            float period = 0.45f + 0.4f * hash(seed, k, 1);
            float life = (float) ((s / period + hash(seed, k, 2)) % 1.0);
            int cycle = (int) Math.floor(s / period + hash(seed, k, 2));
            float a = (float) (2 * Math.PI * (k + 0.6f * hash(seed, k, 10 + cycle)) / count);
            float t = 0.1f + 0.5f * hash(seed, k, 20 + cycle);        // along his sides, none on top of his head
            point(base, a, t, h, scale, s, seed);
            float out = 0.35f;                                         // rising up along the shell, leaning out a bit
            axis.set((float) Math.cos(a) * out, 1f, (float) Math.sin(a) * out).normalize();
            float len = h * scale * (0.3f + 0.25f * hash(seed, k, 30 + cycle)) * (0.55f + 0.45f * (float) Math.sin(Math.PI * life));
            float wid = len * 0.5f;
            view.set(camX - base[0], camY - base[1], camZ - base[2]);
            axis.cross(view, right);
            if (right.lengthSquared() < 1e-6f) continue;
            right.normalize().mul(wid * 0.5f);
            int al = Math.round(255 * Math.min(1f, alpha * 0.9f * (float) Math.sin(Math.PI * life)));
            if (al <= 2) continue;
            int sprite = Math.floorMod(k * 3 + (int) Math.floor(s * 9f), TONGUE_SPRITES);
            float u0 = sprite / (float) TONGUE_SPRITES, u1 = (sprite + 1) / (float) TONGUE_SPRITES;
            float bx = base[0] - axis.x * len * 0.15f, by = base[1] - axis.y * len * 0.15f, bz = base[2] - axis.z * len * 0.15f;
            float tx = bx + axis.x * len, ty = by + axis.y * len, tz = bz + axis.z * len;
            tv(vc, pose, m, bx - right.x, by - right.y, bz - right.z, u0, 1, al);
            tv(vc, pose, m, bx + right.x, by + right.y, bz + right.z, u1, 1, al);
            tv(vc, pose, m, tx + right.x, ty + right.y, tz + right.z, u1, 0, al);
            tv(vc, pose, m, tx - right.x, ty - right.y, tz - right.z, u0, 0, al);
        }
    }

    private static void tv(VertexConsumer vc, PoseStack.Pose pose, Matrix4f m, float x, float y, float z, float u, float v, int a) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, a).setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
    }
}
