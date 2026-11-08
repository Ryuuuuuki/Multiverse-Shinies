package com.multiverseshinies.client;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.multiverseshinies.MultiverseShinies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evolution cutscene of the Multiverse Pokemon (client side only, everyone watching sees it).
 *
 * The whole flight of the Rayquaza "Shenlong" is one baked animation (made in Blockbench, see
 * blockbench/rayquaza_shenlong.bbmodel, 'evolution_cutscene'): the dragon is a client-only PokemonEntity (rayquaza
 * with the aspect multiverse_shenlong) standing on the evolving Pokemon, facing away from the camera, and the
 * animation flies it out of a portal, around the Pokemon and out through a second portal.  This class draws the
 * portals (with flash and lightning), hides the bones that are still behind / already through a portal, leaves the
 * Dragon Ball trails and plays the sounds.  Timings, portals and per-bone show/hide times come from
 * assets/multiverseshinies/cutscene/shenlong_evolution.json, generated with the animation.
 * The server keeps the evolving Pokemon in place meanwhile (EvolutionLock).
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID, value = Dist.CLIENT)
public final class EvolutionCutscene {
    private static final String MULTIVERSE = "multiverseshinies";
    static final String SHENLONG_ASPECT = "multiverse_shenlong_cutscene";
    private static final float SCALE_MOD = 0.7f;

    private static final ResourceLocation DATA = ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "cutscene/shenlong_evolution.json");
    private static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "textures/evolution/portal.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(MultiverseShinies.MODID, "textures/aura/burst_glow.png");
    private static final ResourceLocation CRY = ResourceLocation.fromNamespaceAndPath("cobblemon", "pokemon.rayquaza.cry");
    private static final int PORTAL_FRAMES = 16;
    private static final float PORTAL_FPS = 1000f / 130f;
    private static final double CLOSE_TIME = 0.5;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("multiverseshinies/cutscene");
    private static final List<Scene> SCENES = new ArrayList<>();
    private static final Set<Integer> EVOLVING = new HashSet<>();
    private static final Set<Integer> DRAGONS = new HashSet<>();
    private static Data data;
    private static int nextId = -0x4D560000;
    private static ClientLevel lastLevel;

    private EvolutionCutscene() {}

    /** the running cutscenes (EvolutionLight draws the evolution light on them) */
    static List<Scene> scenes() { return SCENES; }

    /** seconds since the evolution started */
    static double time(Scene s, ClientLevel level, float partial) {
        return (level.getGameTime() - s.startTick + partial) / 20.0;
    }

    // ================================================================== data from the generator
    private static final class PortalData {
        double[] center, normal;          // model units / direction, Blockbench internal (-z = dragon front at spawn)
        double radius, open, close;
    }

    private static final class Data {
        double length, scale, emerge;
        final List<PortalData> portals = new ArrayList<>();
        final Map<String, double[]> bones = new HashMap<>();
    }

    private static Data data() {
        if (data != null) return data;
        Data d = new Data();
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(DATA);
            if (res.isPresent()) {
                try (Reader r = res.get().openAsReader()) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    d.length = o.get("length").getAsDouble();
                    d.scale = o.get("scale").getAsDouble();
                    d.emerge = o.get("emerge").getAsDouble();
                    for (var e : o.getAsJsonArray("portals")) {
                        JsonObject p = e.getAsJsonObject();
                        PortalData pd = new PortalData();
                        pd.center = vec(p.getAsJsonArray("center"));
                        pd.normal = vec(p.getAsJsonArray("normal"));
                        pd.radius = p.get("radius").getAsDouble();
                        pd.open = p.get("open").getAsDouble();
                        pd.close = p.get("close").getAsDouble();
                        d.portals.add(pd);
                    }
                    for (var e : o.getAsJsonObject("bones").entrySet()) {
                        JsonArray a = e.getValue().getAsJsonArray();
                        d.bones.put(e.getKey(), new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()});
                    }
                }
            }
        } catch (Throwable err) {
            LOG.warn("Could not read the cutscene data", err);
        }
        data = d;
        return d;
    }

    private static double[] vec(JsonArray a) {
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    // ================================================================== one cutscene
    static final class Scene {
        final int target;
        final long startTick;
        Vec3 c;                           // the evolving Pokemon (the server holds it still; followed just in case)
        final float yawDeg;
        final Vec3 f, right;              // world direction of the model's front (-z) and of its +x
        boolean mirror, calibrated;
        PokemonEntity dragon;
        boolean soundP, soundQ, cried;

        Scene(PokemonEntity pe, Vec3 cam, long now) {
            target = pe.getId();
            startTick = now;
            c = pe.position();
            double ax = c.x - cam.x, az = c.z - cam.z;
            if (ax * ax + az * az < 1e-4) { ax = -Math.sin(Math.toRadians(pe.getYRot())); az = Math.cos(Math.toRadians(pe.getYRot())); }
            // the dragon faces away from the camera: the scene was laid out with the camera on the model's +z side
            yawDeg = (float) Math.toDegrees(Math.atan2(-ax, az));
            double y = Math.toRadians(yawDeg);
            f = new Vec3(-Math.sin(y), 0, Math.cos(y));
            right = new Vec3(-Math.cos(y), 0, -Math.sin(y));
        }

        Vec3 dir(double[] m) {
            double x = mirror ? -m[0] : m[0];
            return right.scale(x).add(0, m[1], 0).add(f.scale(-m[2]));
        }

        Vec3 point(double[] m) {
            return c.add(dir(m).scale(data().scale / 16.0));
        }
    }

    // ================================================================== ticking
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            for (Scene s : SCENES) removeDragon(s, lastLevel);
            SCENES.clear(); EVOLVING.clear(); DRAGONS.clear();
            data = null;
            lastLevel = level;
        }
        if (level == null || mc.isPaused()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Set<Integer> now = new HashSet<>();
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof PokemonEntity pe) || DRAGONS.contains(pe.getId())) continue;
            boolean evolving;
            try { evolving = pe.isEvolving(); } catch (Throwable t) { continue; }
            if (!evolving) continue;
            now.add(pe.getId());
            if (EVOLVING.contains(pe.getId())) continue;
            if (pe.getAspects().contains(MULTIVERSE) && data().length > 0) SCENES.add(new Scene(pe, cam, level.getGameTime()));
        }
        EVOLVING.clear();
        EVOLVING.addAll(now);

        for (Iterator<Scene> it = SCENES.iterator(); it.hasNext(); ) {
            Scene s = it.next();
            double t = (level.getGameTime() - s.startTick) / 20.0;
            if (t > data().length + 0.6) {
                removeDragon(s, level);
                it.remove();
                continue;
            }
            try {
                tickScene(s, level, t);
            } catch (Throwable err) {
                LOG.warn("Evolution cutscene failed", err);
                removeDragon(s, level);
                it.remove();
            }
        }
    }

    private static void tickScene(Scene s, ClientLevel level, double t) {
        Data d = data();
        PortalData pIn = d.portals.get(0), pOut = d.portals.get(1);
        if (!s.soundP) { s.soundP = true; boom(level, s.point(pIn.center)); }
        if (!s.soundQ && t >= pOut.open) { s.soundQ = true; boom(level, s.point(pOut.center)); }
        if (!s.cried && t >= d.emerge + 0.2) {
            s.cried = true;
            Vec3 p = s.point(pIn.center);
            level.playLocalSound(p.x, p.y, p.z, SoundEvent.createVariableRangeEvent(CRY), SoundSource.NEUTRAL, 2.5f, 0.9f, false);
        }

        if (t > d.length) { removeDragon(s, level); return; }
        if (level.getEntity(s.target) instanceof PokemonEntity pe && pe.position().distanceToSqr(s.c) > 1.0e-4) {
            s.c = s.c.lerp(pe.position(), 0.35);
        }
        if (s.dragon == null) s.dragon = spawnDragon(level, s);
        if (s.dragon == null) return;
        PokemonEntity dr = s.dragon;
        dr.setOldPosAndRot();
        dr.setPos(s.c);
        dr.setYRot(s.yawDeg); dr.yRotO = s.yawDeg;
        dr.yBodyRot = s.yawDeg; dr.yBodyRotO = s.yawDeg;
        dr.yHeadRot = s.yawDeg; dr.yHeadRotO = s.yawDeg;
        dr.setXRot(0);
        dr.setDeltaMovement(Vec3.ZERO);
        if (dr.getDelegate() instanceof PokemonClientDelegate cd) cd.setActiveSendoutScale(1f);
        if (!(dr.getDelegate() instanceof PosableState st)) return;
        // the animation clock: Cobblemon only advances it when the entity ticks, which a client-only entity may not
        // do; the cutscene's own clock drives it so the flight stays in sync with the portals
        st.updateAge((int) (level.getGameTime() - s.startTick));

        // is the model mirrored in game? before it comes out, the snout sits right in the entry portal
        if (!s.calibrated && t > 0.15 && t < d.emerge - 0.1) {
            MatrixWrapper head = st.getLocatorStates().get("head");
            if (head != null) {
                Vec3 h = head.getOrigin();
                s.mirror = false; double plain = h.distanceTo(s.point(pIn.center));
                s.mirror = true; double mirrored = h.distanceTo(s.point(pIn.center));
                s.mirror = mirrored < plain && mirrored < 3.0;
                s.calibrated = true;
            }
        }

        // Dragon Ball trails
        for (int i = 1; i <= 7; i++) {
            MatrixWrapper mw = st.getLocatorStates().get("dragonball_" + i);
            if (mw == null || !shown(t, "dragonball_" + i)) continue;
            Vec3 o = mw.getOrigin();
            if (o.distanceToSqr(s.c) > 60 * 60) continue;
            level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.55f, 0.08f), 1.6f), o.x, o.y, o.z, 0, 0, 0);
            if (level.random.nextFloat() < 0.35f) level.addParticle(ParticleTypes.END_ROD, o.x, o.y, o.z, 0, 0, 0);
        }
    }

    private static boolean shown(double t, String bone) {
        double[] w = data().bones.get(bone);
        return w == null || (t >= w[0] && t < w[1]);
    }

    private static PokemonEntity spawnDragon(ClientLevel level, Scene s) {
        try {
            Species sp = PokemonSpecies.getByName("rayquaza");
            if (sp == null) return null;
            Pokemon pk = new Pokemon();
            pk.setSpecies(sp);
            pk.setScaleModifier(SCALE_MOD);
            pk.setForcedAspects(Set.of(SHENLONG_ASPECT));
            PokemonEntity d = new PokemonEntity(level, pk, CobblemonEntities.POKEMON);
            d.setId(nextId--);
            d.noPhysics = true;
            d.noCulling = true;                     // the model flies far away from the entity's own box
            d.setNoGravity(true);
            d.setSilent(true);
            d.setInvulnerable(true);
            var ed = d.getEntityData();
            ed.set(PokemonEntity.Companion.getSPECIES(), sp.getResourceIdentifier().toString());
            ed.set(PokemonEntity.Companion.getASPECTS(), Set.of(SHENLONG_ASPECT));
            ed.set(PokemonEntity.Companion.getPOSE_TYPE(), PoseType.FLY);
            ed.set(PokemonEntity.Companion.getHIDE_LABEL(), true);
            ed.set(PokemonEntity.Companion.getSHOULD_RENDER_NAME(), false);
            d.moveTo(s.c.x, s.c.y, s.c.z, s.yawDeg, 0);
            d.yBodyRot = s.yawDeg; d.yHeadRot = s.yawDeg;
            level.addEntity(d);
            DRAGONS.add(d.getId());
            return d;
        } catch (Throwable err) {
            LOG.warn("Could not create the Shenlong for the evolution cutscene", err);
            return null;
        }
    }

    private static void removeDragon(Scene s, ClientLevel level) {
        if (s.dragon == null) return;
        DRAGONS.remove(s.dragon.getId());
        if (level != null) level.removeEntity(s.dragon.getId(), Entity.RemovalReason.DISCARDED);
        s.dragon = null;
    }

    private static void boom(ClientLevel level, Vec3 at) {
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 1.6f, 1.15f, false);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.8f, 0.6f, false);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.PORTAL_TRIGGER, SoundSource.NEUTRAL, 0.5f, 1.6f, false);
    }

    private static Scene sceneOf(Entity e) {
        for (Scene s : SCENES) if (s.dragon == e) return s;
        return null;
    }

    private static double time(Scene s, float partial) {
        return (lastLevel == null ? 0 : lastLevel.getGameTime() - s.startTick + partial) / 20.0;
    }

    // ================================================================== hiding the bones behind the portals
    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!DRAGONS.contains(event.getEntity().getId())) return;
        Scene s = sceneOf(event.getEntity());
        if (s == null || !(s.dragon.getDelegate() instanceof PosableState st)) return;
        PosableModel model = st.getCurrentModel();
        if (model == null) return;
        double t = time(s, event.getPartialTick());
        for (Map.Entry<String, ModelPart> en : model.getRelevantPartsByName().entrySet()) {
            en.getValue().skipDraw = !shown(t, en.getKey());
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        if (!DRAGONS.contains(event.getEntity().getId())) return;
        if (!(event.getEntity() instanceof PokemonEntity pe) || !(pe.getDelegate() instanceof PosableState st)) return;
        PosableModel model = st.getCurrentModel();
        if (model == null) return;
        for (ModelPart part : model.getRelevantPartsByName().values()) part.skipDraw = false;
    }

    // ================================================================== portals, flashes, lightning
    private static double portalRadius(PortalData p, double t) {
        double k = t - p.open;
        if (k < 0) return 0;
        double small = 0.45 * smooth(k / 0.12);
        double x = clamp01((k - 0.4) / 1.0), y = x - 1;
        double r = small + (p.radius - 0.45) * (1 + y * y * (2.4 * y + 1.4));
        if (t > p.close) r *= 1 - smooth((t - p.close) / CLOSE_TIME);
        return Math.max(0, r);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || SCENES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        buffers.endBatch();   // the dragon must be in the depth buffer: the portal covers what is still behind it

        float seconds = (mc.level.getGameTime() + partial) / 20f;
        int frame = Math.floorMod((int) Math.floor(seconds * PORTAL_FPS), PORTAL_FRAMES);
        PoseStack.Pose pose = ps.last();
        Data d = data();

        VertexConsumer disc = buffers.getBuffer(RenderType.entityTranslucentEmissive(PORTAL));
        for (Scene s : SCENES) {
            double t = time(s, partial);
            for (int i = 0; i < d.portals.size(); i++) {
                PortalData p = d.portals.get(i);
                portalDisc(disc, pose, s.point(p.center).subtract(cam), s.dir(p.normal).normalize(), portalRadius(p, t), (frame + 5 * i) % PORTAL_FRAMES, t);
            }
        }
        buffers.endBatch();

        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
        for (Scene s : SCENES) {
            double t = time(s, partial);
            for (PortalData p : d.portals) {
                glows(glow, pose, s.point(p.center).subtract(cam), s.dir(p.normal).normalize(), portalRadius(p, t), t - p.open, p.close - p.open);
            }
        }
        buffers.endBatch();

        VertexConsumer bolts = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.pose();
        for (Scene s : SCENES) {
            double t = time(s, partial);
            for (int i = 0; i < d.portals.size(); i++) {
                PortalData p = d.portals.get(i);
                lightning(bolts, m, s.point(p.center).subtract(cam), s.dir(p.normal).normalize(), portalRadius(p, t), t - p.open, s.target * 31 + 7 * i, seconds);
            }
        }
        buffers.endBatch();
    }

    private static Vec3[] basis(Vec3 n, double spin) {
        Vec3 u = n.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1e-6) u = new Vec3(1, 0, 0);
        u = u.normalize();
        Vec3 w = u.cross(n).normalize();
        double cs = Math.cos(spin), sn = Math.sin(spin);
        return new Vec3[]{u.scale(cs).add(w.scale(sn)), w.scale(cs).subtract(u.scale(sn))};
    }

    private static void portalDisc(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, Vec3 n, double r, int frame, double t) {
        if (r <= 0.01) return;
        Vec3[] b = basis(n, t * 0.35);
        float u0 = frame / (float) PORTAL_FRAMES, u1 = (frame + 1) / (float) PORTAL_FRAMES;
        quad(vc, pose, c, b[0].scale(r), b[1].scale(r), u0, u1, 255, 255, 255, 255, n);
    }

    private static void glows(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, Vec3 n, double r, double t, double closeAt) {
        if (t < 0) return;
        Vec3[] b = basis(n, 0);
        if (r > 0.01) {                              // halo in the portal plane while it is open
            double hr = r * 1.65;
            int a = (int) (110 + 40 * Math.sin(t * 9));
            quad(vc, pose, c, b[0].scale(hr), b[1].scale(hr), 0, 1, 255, 190, 90, a, n);
        }
        flash(vc, pose, c, t / 0.7, 10.0, 255);                                  // when it opens
        flash(vc, pose, c, (t - closeAt - CLOSE_TIME * 0.6) / 0.45, 3.5, 200);   // when it snaps shut
    }

    private static void flash(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, double k, double size, int alpha) {
        if (k < 0 || k > 1) return;
        double r = size * (0.25 + 0.75 * Math.sqrt(k));
        int a = (int) (alpha * (1 - k) * (1 - k));
        Vec3 n = c.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : c.normalize().scale(-1);   // facing the camera
        Vec3[] b = basis(n, 0);
        Vec3 near = c.add(n.scale(Math.min(2.0, c.length() * 0.3)));
        quad(vc, pose, near, b[0].scale(r), b[1].scale(r), 0, 1, 255, 235, 190, a, n);
    }

    /** bolts crackling around the rim and out of it; more of them while the portal grows */
    private static void lightning(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 n, double r, double t, int seed, float seconds) {
        if (r <= 0.05 || t < 0) return;
        Vec3[] b = basis(n, 0);
        boolean growing = t < 1.8;
        int slots = growing ? 9 : 5;
        for (int k = 0; k < slots; k++) {
            int slot = (int) Math.floor(seconds * 13f + k * 0.37f);
            if (RageAuraRenderer.hash(seed, k, slot) > (growing ? 0.75f : 0.45f)) continue;
            java.util.Random rnd = new java.util.Random(seed * 977L + k * 131L + slot * 7919L);
            double ang = rnd.nextDouble() * Math.PI * 2;
            Vec3 dir = b[0].scale(Math.cos(ang)).add(b[1].scale(Math.sin(ang)));
            Vec3 from = c.add(dir.scale(r * (0.85 + 0.1 * rnd.nextDouble())));
            Vec3 to = c.add(dir.scale(r * (1.35 + 0.7 * rnd.nextDouble())))
                       .add(n.scale((rnd.nextDouble() - 0.5) * r * 0.8))
                       .add(dir.cross(n).scale((rnd.nextDouble() - 0.5) * r * 0.9));
            bolt(vc, m, from, to, rnd, (float) Math.max(0.05, r * 0.035));
        }
    }

    private static void bolt(VertexConsumer vc, Matrix4f m, Vec3 from, Vec3 to, java.util.Random rnd, float w) {
        int segs = 6 + rnd.nextInt(4);
        Vec3 axis = to.subtract(from);
        double len = axis.length();
        Vec3[] pts = new Vec3[segs + 1];
        Vec3 side1 = axis.cross(new Vec3(0, 1, 0));
        if (side1.lengthSqr() < 1e-6) side1 = new Vec3(1, 0, 0);
        side1 = side1.normalize();
        Vec3 side2 = side1.cross(axis).normalize();
        for (int i = 0; i <= segs; i++) {
            double fr = i / (double) segs;
            double j = (i == 0 || i == segs) ? 0 : len * 0.13;
            pts[i] = from.add(axis.scale(fr)).add(side1.scale((rnd.nextDouble() - 0.5) * 2 * j)).add(side2.scale((rnd.nextDouble() - 0.5) * 2 * j));
        }
        for (int i = 0; i < segs; i++) {
            ribbon(vc, m, pts[i], pts[i + 1], w, 255, 200, 90, 150);
            ribbon(vc, m, pts[i], pts[i + 1], w * 0.33f, 255, 250, 225, 255);
            if (i == segs / 2) {
                Vec3 fork = pts[i + 1].add(side1.scale((rnd.nextDouble() - 0.5) * len * 0.5)).add(axis.scale(0.18));
                ribbon(vc, m, pts[i + 1], fork, w * 0.7f, 255, 200, 90, 130);
                ribbon(vc, m, pts[i + 1], fork, w * 0.25f, 255, 250, 225, 240);
            }
        }
    }

    /** camera-facing strip from a to b (camera-relative coordinates) */
    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float w, int cr, int cg, int cb, int ca) {
        Vec3 side = b.subtract(a).cross(a);
        if (side.lengthSqr() < 1e-9) return;
        side = side.normalize().scale(w);
        vc.addVertex(m, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(cr, cg, cb, ca);
        vc.addVertex(m, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z)).setColor(cr, cg, cb, ca);
        vc.addVertex(m, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z)).setColor(cr, cg, cb, ca);
        vc.addVertex(m, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(cr, cg, cb, ca);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, Vec3 u, Vec3 w, float u0, float u1,
                             int cr, int cg, int cb, int ca, Vec3 n) {
        if (ca <= 1) return;
        Matrix4f m = pose.pose();
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;
        double[][] k = {{-1, -1, u0, 1}, {1, -1, u1, 1}, {1, 1, u1, 0}, {-1, 1, u0, 0}};
        for (double[] q : k) {
            Vec3 v = c.add(u.scale(q[0])).add(w.scale(q[1]));
            vc.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(cr, cg, cb, Math.min(255, ca))
              .setUv((float) q[2], (float) q[3]).setOverlay(OverlayTexture.NO_OVERLAY)
              .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        }
    }

    private static double clamp01(double x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }

    private static double smooth(double x) {
        x = clamp01(x);
        return x * x * (3 - 2 * x);
    }
}
