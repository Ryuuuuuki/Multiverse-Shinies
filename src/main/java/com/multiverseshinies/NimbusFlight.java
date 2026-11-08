package com.multiverseshinies;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Server side: an owned Multiverse Infernape out of battle really flies after its trainer on the nimbus. When the
 * trainer gets away it takes off (no gravity, its own steering instead of the ground pathfinding), keeps about a block
 * of air under the cloud and hovers beside the trainer; once the trainer has stood still for a few seconds it lands.
 * Clients are told through NimbusNet and switch the poser to the flying cloud animations (CloudPoseDriver).
 */
@EventBusSubscriber(modid = MultiverseShinies.MODID)
public final class NimbusFlight {
    private static final double TAKEOFF_DIST = 5.0;     // blocks from the trainer before taking off
    private static final double HOVER = 1.0;            // blocks of air under the cloud
    private static final double CRUISE = 0.45;          // blocks per tick
    private static final double CATCH_UP = 0.95;        // when far behind
    private static final double TELEPORT_DIST = 28.0;
    private static final int LAND_AFTER = 60;           // ticks of the trainer standing still near it
    private static final double MOVING = 0.06;          // trainer speed (blocks/tick) that counts as travelling
    private static final double AHEAD = 1.8;            // while travelling: this far ahead of the trainer...
    private static final double SIDE = 2.2;             // ...and this far to the side, so he's in view
    private static final double NEAR = 1.8, FAR = 3.6;  // while the trainer hovers: stays where he is in this ring

    private static final class Flight {
        boolean flying;
        int still;
        int sync;
        double hx, hz;      // trainer's smoothed travel direction (not where the camera looks)
        int side = 1;       // which side of the trainer he flies on (+1 right, -1 left)
        boolean moving;
    }

    private static final Map<Integer, Flight> FLIGHTS = new HashMap<>();

    private NimbusFlight() {}

    private static boolean isOurInfernape(PokemonEntity pe) {
        try {
            return pe.getAspects().contains("multiverseshinies")
                && "cobblemon:infernape".equals(pe.getPokemon().getSpecies().getResourceIdentifier().toString());
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof PokemonEntity pe) || !(pe.level() instanceof ServerLevel level)) return;
        if (!isOurInfernape(pe)) return;
        Flight f = FLIGHTS.computeIfAbsent(pe.getId(), k -> new Flight());
        if (FLIGHTS.size() > 512) FLIGHTS.clear();

        LivingEntity owner = pe.getOwner();
        boolean allowed = owner instanceof Player p && !p.isSpectator() && owner.level() == level && owner.isAlive()
            && !pe.isBattling() && !pe.isBusy() && pe.getTethering() == null && !pe.isPassenger() && !pe.isVehicle()
            && !pe.isInWater() && !pe.isSleeping();
        if (!allowed) {
            land(pe, f);
            return;
        }

        Vec3 pos = pe.position();
        Vec3 o = owner.position();
        double horiz = Math.sqrt(sq(o.x - pos.x) + sq(o.z - pos.z));

        if (!f.flying) {
            if (horiz > TAKEOFF_DIST || o.y > pos.y + 2.5) {
                f.flying = true;
                f.still = 0;
                NimbusNet.send(pe, true);
            } else {
                return;
            }
        }

        if (pos.distanceTo(o) > TELEPORT_DIST) {
            pe.teleportTo(o.x, o.y + HOVER + 0.5, o.z);
            pe.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // where to fly: based on where the trainer is GOING, never on where the camera looks, so turning around to
        // look at him doesn't send him behind the trainer again
        double mx = owner.getX() - owner.xo, mz = owner.getZ() - owner.zo;   // players' delta movement isn't reliable server side
        double sp = Math.sqrt(mx * mx + mz * mz);
        double tx, tz;
        if (sp > MOVING) {
            double k = f.hx == 0 && f.hz == 0 ? 1.0 : 0.15;
            f.hx += (mx / sp - f.hx) * k;
            f.hz += (mz / sp - f.hz) * k;
            double hl = Math.sqrt(f.hx * f.hx + f.hz * f.hz);
            double fx = f.hx / hl, fz = f.hz / hl;          // travel direction
            double rx = -fz, rz = fx;                       // its right
            if (!f.moving) {                                // starting off: keep the side he's already on
                f.side = (pos.x - o.x) * rx + (pos.z - o.z) * rz >= 0 ? 1 : -1;
                f.moving = true;
            }
            tx = o.x + fx * AHEAD + rx * SIDE * f.side;
            tz = o.z + fz * AHEAD + rz * SIDE * f.side;
        } else {
            // trainer hovering or standing: he stays where he is around them, just not too close or too far
            f.moving = false;
            double dx = pos.x - o.x, dz = pos.z - o.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 1e-3) { dx = 1; dz = 0; d = 1; }
            double r = Math.max(NEAR, Math.min(FAR, d));
            tx = o.x + dx / d * r;
            tz = o.z + dz / d * r;
        }
        double floor = groundBelow(level, pos.x, pos.y + 0.5, pos.z);
        double ty = Math.max(floor + HOVER, o.y + 0.6);

        Vec3 to = new Vec3(tx - pos.x, ty - pos.y, tz - pos.z);
        double dist = to.length();
        double max = Math.sqrt(sq(o.x - pos.x) + sq(o.z - pos.z)) > 10 ? CATCH_UP : CRUISE;
        Vec3 want = dist < 0.05 ? Vec3.ZERO : to.scale(Math.min(max, dist * 0.18) / dist);
        if (pe.horizontalCollision) want = want.add(0, 0.3, 0);    // climb over walls in the way
        Vec3 v = pe.getDeltaMovement().lerp(want, 0.3);

        pe.setNoGravity(true);
        if (++f.sync % 20 == 0) NimbusNet.send(pe, true);   // also covers players that start tracking him mid-flight
        pe.fallDistance = 0;
        pe.getNavigation().stop();
        pe.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pe.setDeltaMovement(v);
        pe.hurtMarked = true;

        // face where it's going, or the trainer while hovering
        double hx = v.x, hz = v.z;
        if (hx * hx + hz * hz < 0.0025) { hx = o.x - pos.x; hz = o.z - pos.z; }
        float face = (float) (Mth.atan2(hz, hx) * Mth.RAD_TO_DEG) - 90f;
        pe.setYRot(face);
        pe.setYBodyRot(face);
        pe.setYHeadRot(face);

        // land once the trainer has been standing still with it close by
        boolean ownerStill = mx * mx + mz * mz < 0.0004 && owner.onGround();
        f.still = ownerStill && horiz < TAKEOFF_DIST ? f.still + 1 : 0;
        if (f.still > LAND_AFTER) land(pe, f);
    }

    private static void land(PokemonEntity pe, Flight f) {
        if (!f.flying) return;
        f.flying = false;
        f.still = 0;
        pe.setNoGravity(false);
        pe.fallDistance = 0;
        NimbusNet.send(pe, false);
    }

    /** y of the first solid surface under (x, y, z), searching 10 blocks down */
    private static double groundBelow(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, y, z).mutable();
        for (int i = 0; i < 10; i++) {
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return p.getY() + level.getBlockState(p).getCollisionShape(level, p).max(net.minecraft.core.Direction.Axis.Y);
            }
            p.move(0, -1, 0);
        }
        return y - 10;
    }

    private static double sq(double d) {
        return d * d;
    }
}
