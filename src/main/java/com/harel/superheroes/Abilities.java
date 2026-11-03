package com.harel.superheroes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.IntConsumer;

/** All hero powers: primary (right-click), secondary (sneak + right-click), ultimate (F key). */
final class Abilities {
    private final SuperHeroesPlugin pl;
    private final Random rng = new Random();

    Abilities(SuperHeroesPlugin pl) { this.pl = pl; }

    // ---------------------------------------------------------------- dispatch
    boolean primary(Player p, Hero h) {
        return switch (h) {
            case BATMAN -> grapnel(p);
            case BLACKPANTHER -> vibraniumRelease(p);
            case CAPTAINAMERICA -> shieldThrow(p);
            case DEADPOOL, WOLVERINE -> katanaFrenzy(p, 4, 4, 3.0);
            case DOCTORSTRANGE, SCARLETWITCH -> eyeOfAgamotto(p);
            case FLASH -> speedDash(p);
            case GREENLANTERN -> { beam(p, p.getEyeLocation().getDirection(), 50, Color.LIME, 8, false); sound(p, Sound.ENTITY_BLAZE_SHOOT, 1f, 1.6f); yield true; }
            case HULK -> hulkSmash(p, 1.35, 8, 10, 2.0);
            case IRONMAN -> { beam(p, p.getEyeLocation().getDirection(), 50, Color.AQUA, 8, false); sound(p, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.3f); yield true; }
            case SPIDERMAN -> webShot(p);
            case STORM -> lightningStorm(p);
            case SUPERMAN, CAPTAINMARVEL -> { beam(p, p.getEyeLocation().getDirection(), 60, Color.RED, 8, true); sound(p, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1.5f); yield true; }
            case THOR, AQUAMAN -> mjolnirThrow(p);
            case WONDERWOMAN -> lassoOfTruth(p);
        };
    }

    boolean secondary(Player p, Hero h) {
        return switch (h) {
            case BATMAN -> smokeBomb(p);
            case BLACKPANTHER -> kineticPounce(p);
            case CAPTAINAMERICA -> shieldBlock(p);
            case DEADPOOL, WOLVERINE -> dualPistols(p);
            case DOCTORSTRANGE, SCARLETWITCH -> portalTeleport(p);
            case FLASH -> speedForceLightning(p);
            case GREENLANTERN -> constructWall(p);
            case HULK -> gammaRoar(p);
            case IRONMAN -> unibeam(p);
            case SPIDERMAN -> webSwing(p);
            case STORM -> tornado(p);
            case SUPERMAN, CAPTAINMARVEL -> freezeBreath(p);
            case THOR, AQUAMAN -> thunderStrike(p);
            case WONDERWOMAN -> bracelets(p);
        };
    }

    void ultimate(Player p, Hero h) {
        World w = p.getWorld();
        Location c = p.getLocation();
        sound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, c.clone().add(0, 1, 0), 60, 0.6, 1, 0.6, 0.3);
        switch (h) {
            case BATMAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2));
                repeat(p, 6, 6, i -> pulse(p, p.getLocation(), 6, 3, 0.5, Particle.CRIT, true));
            }
            case BLACKPANTHER -> {
                buff(p, PotionEffectType.STRENGTH, 200, 2); buff(p, PotionEffectType.SPEED, 200, 2); buff(p, PotionEffectType.RESISTANCE, 200, 2);
                ring(w, c, 4, Color.fromRGB(150, 90, 230), 40);
            }
            case CAPTAINAMERICA -> {
                for (Player o : playersAround(p, 12)) { buff(o, PotionEffectType.STRENGTH, 200, 1); buff(o, PotionEffectType.REGENERATION, 200, 1); buff(o, PotionEffectType.SPEED, 200, 1); }
                ring(w, c, 6, Color.fromRGB(25, 60, 170), 60);
            }
            case DEADPOOL, WOLVERINE -> {
                buff(p, PotionEffectType.SPEED, 80, 2);
                repeat(p, 12, 3, i -> { for (LivingEntity e : nearest(p, 5, 2)) { hurt(p, e, 2.5); w.spawnParticle(Particle.CRIT, e.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.2); } sound(p, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f); });
            }
            case DOCTORSTRANGE, SCARLETWITCH -> {
                buff(p, PotionEffectType.RESISTANCE, 100, 2);
                for (LivingEntity e : targets(p, c, 10)) {
                    e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 6));
                    e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
                    e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                    hurt(p, e, 4);
                }
                repeat(p, 5, 8, i -> ring(w, p.getLocation(), 2 + i * 2, Color.fromRGB(190, 40, 40), 40));
            }
            case FLASH -> {
                buff(p, PotionEffectType.SPEED, 160, 6);
                for (LivingEntity e : targets(p, c, 12)) { e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 4)); e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0)); }
                repeat(p, 8, 4, i -> w.spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.3));
            }
            case GREENLANTERN -> {
                for (LivingEntity e : targets(p, c, 7)) { hurt(p, e, 7); e.setVelocity(new Vector(0, 1.4, 0)); }
                ring(w, c, 7, Color.LIME, 70);
                sound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
            }
            case HULK -> {
                p.setVelocity(new Vector(0, 1.8, 0).add(c.getDirection().multiply(0.6)));
                sound(p, Sound.ENTITY_BREEZE_JUMP, 1f, 0.6f);
                waitForLanding(p, () -> pulse(p, p.getLocation(), 10, 14, 2.2, Particle.EXPLOSION, true));
            }
            case IRONMAN -> {
                Vector base = p.getEyeLocation().getDirection();
                int[] offsets = {-20, -10, 0, 10, 20};
                repeat(p, 5, 4, i -> {
                    Vector d = base.clone().rotateAroundY(Math.toRadians(offsets[i]));
                    beam(p, d, 40, Color.AQUA, 7, false);
                    sound(p, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.1f);
                });
            }
            case SPIDERMAN -> {
                for (LivingEntity e : targets(p, c, 8)) {
                    e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 5));
                    e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                    hurt(p, e, 3);
                    w.spawnParticle(Particle.CLOUD, e.getLocation().add(0, 1, 0), 25, 0.4, 0.8, 0.4, 0.02);
                }
                sound(p, Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.6f);
            }
            case STORM -> repeat(p, 16, 5, i -> {
                Location l = p.getLocation().add(rng.nextDouble(-12, 12), 0, rng.nextDouble(-12, 12));
                l.setY(w.getHighestBlockYAt(l) + 1);
                w.strikeLightningEffect(l);
                for (LivingEntity e : targets(p, l, 3)) hurt(p, e, 5);
            });
            case SUPERMAN, CAPTAINMARVEL -> {
                pulse(p, c, 10, 10, 2.0, Particle.FIREWORK, true);
                for (LivingEntity e : targets(p, c, 10)) e.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                ring(w, c, 10, Color.fromRGB(255, 215, 0), 90);
                sound(p, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            }
            case THOR, AQUAMAN -> {
                pulse(p, c, 9, 9, 2.2, Particle.ELECTRIC_SPARK, true);
                for (int i = 0; i < 4; i++) w.strikeLightningEffect(c.clone().add(rng.nextDouble(-6, 6), 0, rng.nextDouble(-6, 6)));
                sound(p, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.8f);
            }
            case WONDERWOMAN -> {
                buff(p, PotionEffectType.STRENGTH, 160, 2); buff(p, PotionEffectType.SPEED, 160, 2); buff(p, PotionEffectType.RESISTANCE, 160, 2);
                repeat(p, 4, 10, i -> pulse(p, p.getLocation(), 5, 5, 0.9, Particle.CRIT, true));
            }
        }
        p.sendMessage(h.chat + "" + ChatColor.BOLD + "ULTIMATE: " + h.ultimate + "!");
    }

    // ---------------------------------------------------------------- BATMAN
    private boolean grapnel(Player p) {
        Block b = p.getTargetBlockExact(40);
        if (b == null) { p.sendMessage(ChatColor.RED + "Nothing to attach to!"); return false; }
        Location anchor = b.getLocation().add(0.5, 0.5, 0.5);
        line(p.getWorld(), p.getEyeLocation(), anchor, Particle.CRIT);
        sound(p, Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
        repeat(p, 25, 1, i -> {
            Vector to = anchor.toVector().subtract(p.getLocation().toVector());
            if (to.length() < 2.5) { p.setVelocity(new Vector(0, 0.35, 0)); throw new CancelTask(); }
            p.setVelocity(to.normalize().multiply(1.25));
            p.setFallDistance(0);
        });
        return true;
    }

    private boolean smokeBomb(Player p) {
        World w = p.getWorld();
        Location c = p.getLocation();
        w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1, 0), 80, 1.5, 1, 1.5, 0.02);
        w.spawnParticle(Particle.CLOUD, c.clone().add(0, 1, 0), 60, 1.5, 1, 1.5, 0.05);
        sound(p, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.7f);
        for (LivingEntity e : targets(p, c, 5)) e.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
        buff(p, PotionEffectType.INVISIBILITY, 60, 0);
        buff(p, PotionEffectType.SPEED, 60, 2);
        return true;
    }

    // ---------------------------------------------------------------- BLACK PANTHER
    private boolean vibraniumRelease(Player p) {
        double charge = pl.kinetic.getOrDefault(p.getUniqueId(), 0.0);
        pl.kinetic.put(p.getUniqueId(), 0.0);
        double radius = 5 + charge / 10.0;
        pulse(p, p.getLocation(), radius, 4 + charge * 0.4, 1.2 + charge / 25.0, Particle.WITCH, true);
        ring(p.getWorld(), p.getLocation(), radius, Color.fromRGB(150, 90, 230), 60);
        sound(p, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.4f);
        p.sendMessage(ChatColor.DARK_PURPLE + "Vibranium kinetic charge: " + String.format("%.1f", charge));
        return true;
    }

    private boolean kineticPounce(Player p) {
        p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(0.5));
        buff(p, PotionEffectType.STRENGTH, 60, 1);
        sound(p, Sound.ENTITY_BREEZE_JUMP, 1f, 1.3f);
        later(10, () -> { if (p.isOnline()) pulse(p, p.getLocation(), 3.5, 5, 0.9, Particle.CRIT, true); });
        return true;
    }

    // ---------------------------------------------------------------- CAPTAIN AMERICA
    private boolean shieldThrow(Player p) {
        Set<UUID> hit = new HashSet<>();
        RayTraceResult r = traceEntity(p, 20);
        sound(p, Sound.ITEM_TRIDENT_THROW, 1f, 1.4f);
        if (r == null || !(r.getHitEntity() instanceof LivingEntity first)) { line(p.getWorld(), p.getEyeLocation(), p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(12)), Particle.CRIT); return true; }
        LivingEntity cur = first;
        Location from = p.getEyeLocation();
        for (int i = 0; i < 3 && cur != null; i++) {
            hit.add(cur.getUniqueId());
            hurt(p, cur, i == 0 ? 7 : 5);
            line(p.getWorld(), from, cur.getLocation().add(0, 1, 0), Particle.CRIT);
            sound(p, Sound.ITEM_SHIELD_BLOCK, 1f, 1.3f);
            from = cur.getLocation().add(0, 1, 0);
            LivingEntity next = null; double best = 36;
            for (LivingEntity e : targets(p, cur.getLocation(), 6)) {
                if (hit.contains(e.getUniqueId())) continue;
                double d = e.getLocation().distanceSquared(cur.getLocation());
                if (d < best) { best = d; next = e; }
            }
            cur = next;
        }
        return true;
    }

    private boolean shieldBlock(Player p) {
        pl.shieldUntil.put(p.getUniqueId(), System.currentTimeMillis() + 4000);
        sound(p, Sound.ITEM_SHIELD_BLOCK, 1.2f, 0.8f);
        p.sendMessage(ChatColor.BLUE + "Vibranium shield raised! (4s)");
        repeat(p, 8, 10, i -> p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 15, 0.7, 0.9, 0.7, 0.1));
        return true;
    }

    // ---------------------------------------------------------------- DEADPOOL
    private boolean katanaFrenzy(Player p, int hits, int period, double dmg) {
        sound(p, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        repeat(p, hits, period, i -> {
            for (LivingEntity e : nearest(p, 4, 3)) { hurt(p, e, dmg); p.getWorld().spawnParticle(Particle.CRIT, e.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.3); }
            sound(p, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        });
        return true;
    }

    private boolean dualPistols(Player p) {
        repeat(p, 2, 4, i -> {
            Vector d = p.getEyeLocation().getDirection();
            RayTraceResult r = traceEntity(p, 28);
            line(p.getWorld(), p.getEyeLocation().add(d.clone().multiply(0.8)), p.getEyeLocation().add(d.clone().multiply(r != null && r.getHitPosition() != null ? r.getHitPosition().distance(p.getEyeLocation().toVector()) : 20)), Particle.CRIT);
            if (r != null && r.getHitEntity() instanceof LivingEntity e) hurt(p, e, 4);
            sound(p, Sound.ITEM_CROSSBOW_SHOOT, 1f, 1.6f);
        });
        return true;
    }

    // ---------------------------------------------------------------- DOCTOR STRANGE
    private boolean eyeOfAgamotto(Player p) {
        Deque<SuperHeroesPlugin.Snapshot> h = pl.history.get(p.getUniqueId());
        if (h == null || h.isEmpty()) { p.sendMessage(ChatColor.RED + "Not enough time has passed to rewind."); return false; }
        SuperHeroesPlugin.Snapshot s = h.peekFirst();
        World w = p.getWorld();
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.3);
        p.teleport(s.loc());
        double max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        p.setHealth(Math.min(max, Math.max(p.getHealth(), s.health())));
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, s.loc().clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.3);
        sound(p, Sound.BLOCK_BELL_RESONATE, 1.2f, 0.6f);
        p.sendMessage(ChatColor.GOLD + "The Eye of Agamotto reverses time...");
        return true;
    }

    private boolean portalTeleport(Player p) {
        Location dest;
        Block b = p.getTargetBlockExact(35);
        if (b != null) dest = b.getLocation().add(0.5, 1, 0.5);
        else dest = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(20));
        dest.setYaw(p.getLocation().getYaw()); dest.setPitch(p.getLocation().getPitch());
        if (dest.getBlock().getType().isSolid() || dest.clone().add(0, 1, 0).getBlock().getType().isSolid()) { p.sendMessage(ChatColor.RED + "The portal collapses — no room there."); return false; }
        portal(p.getWorld(), p.getLocation());
        p.teleport(dest);
        portal(p.getWorld(), dest);
        p.setFallDistance(0);
        sound(p, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- FLASH
    private boolean speedDash(Player p) {
        p.setVelocity(p.getLocation().getDirection().multiply(3.0).setY(0.15));
        buff(p, PotionEffectType.SPEED, 60, 4);
        sound(p, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.8f);
        Set<UUID> hit = new HashSet<>();
        repeat(p, 8, 1, i -> {
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.2);
            for (LivingEntity e : targets(p, p.getLocation(), 1.6)) if (hit.add(e.getUniqueId())) hurt(p, e, 4);
        });
        return true;
    }

    private boolean speedForceLightning(Player p) {
        List<LivingEntity> t = nearest(p, 12, 4);
        if (t.isEmpty()) { p.sendMessage(ChatColor.RED + "No targets nearby."); return false; }
        for (LivingEntity e : t) { p.getWorld().strikeLightningEffect(e.getLocation()); hurt(p, e, 5); p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, e.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.3); }
        return true;
    }

    // ---------------------------------------------------------------- GREEN LANTERN
    private boolean constructWall(Player p) {
        Vector d = p.getEyeLocation().getDirection().setY(0);
        if (d.lengthSquared() < 1e-4) d = new Vector(0, 0, 1);
        d.normalize();
        Vector right = d.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location center = p.getLocation().add(d.clone().multiply(4));
        List<Block> placed = new ArrayList<>();
        for (int x = -2; x <= 2; x++) for (int y = 0; y < 3; y++) {
            Block b = center.clone().add(right.clone().multiply(x)).add(0, y, 0).getBlock();
            if (b.getType().isAir()) { b.setType(Material.LIME_STAINED_GLASS); placed.add(b); }
        }
        if (placed.isEmpty()) { p.sendMessage(ChatColor.RED + "No room for a wall."); return false; }
        sound(p, Sound.BLOCK_ANVIL_PLACE, 1f, 1.5f);
        later(120, () -> { for (Block b : placed) if (b.getType() == Material.LIME_STAINED_GLASS) { b.setType(Material.AIR); b.getWorld().spawnParticle(Particle.CRIT, b.getLocation().add(0.5, 0.5, 0.5), 3); } });
        return true;
    }

    // ---------------------------------------------------------------- HULK
    private boolean hulkSmash(Player p, double power, double radius, double dmg, double kb) {
        p.setVelocity(p.getLocation().getDirection().multiply(0.8 * power).setY(1.0 * power));
        sound(p, Sound.ENTITY_BREEZE_JUMP, 1f, 0.7f);
        waitForLanding(p, () -> {
            pulse(p, p.getLocation(), radius, dmg, kb, Particle.EXPLOSION, true);
            Block under = p.getLocation().add(0, -1, 0).getBlock();
            if (!under.getType().isAir()) p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation(), 60, radius / 3, 0.2, radius / 3, under.getBlockData());
            sound(p, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.8f);
        });
        return true;
    }

    private boolean gammaRoar(Player p) {
        pulse(p, p.getLocation(), 7, 2, 1.8, Particle.CLOUD, true);
        for (LivingEntity e : targets(p, p.getLocation(), 7)) e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
        buff(p, PotionEffectType.RESISTANCE, 60, 2);
        sound(p, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.5f);
        p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "THE MADDER HULK GETS, THE STRONGER HULK GETS!");
        return true;
    }

    // ---------------------------------------------------------------- IRON MAN
    private boolean unibeam(Player p) {
        sound(p, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.2f, 1.2f);
        repeat(p, 30, 1, i -> p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().multiply(0.5)), 4, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.AQUA, 0.8f + i / 15f)));
        later(30, () -> {
            if (!p.isOnline() || p.isDead()) return;
            beam(p, p.getEyeLocation().getDirection(), 50, Color.WHITE, 14, true);
            p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(2)), 1);
            sound(p, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1f);
        });
        return true;
    }

    // ---------------------------------------------------------------- SPIDER-MAN
    /** Shoots a visible web: snare and reel in an entity, or zip toward a block anchor. */
    private boolean webShot(Player p) {
        RayTraceResult hit = traceEntity(p, 30);
        Location origin = p.getEyeLocation();
        sound(p, Sound.ENTITY_SPIDER_STEP, 1f, 1.8f);

        if (hit != null && hit.getHitEntity() instanceof LivingEntity target
                && target != p && !(target instanceof ArmorStand)) {
            Location end = target.getEyeLocation();
            line(p.getWorld(), origin, end, Particle.CLOUD);
            p.getWorld().spawnParticle(Particle.CRIT, end, 18, 0.35, 0.45, 0.35, 0.08);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128, false, false, true));
            hurt(p, target, 2.0);
            Vector pull = p.getLocation().add(0, 0.8, 0).toVector()
                    .subtract(target.getLocation().toVector());
            if (pull.lengthSquared() > 0.01) target.setVelocity(pull.normalize().multiply(0.75).setY(0.22));
            p.sendMessage(ChatColor.WHITE + "Thwip! " + ChatColor.GRAY + "Web snared your target.");
            return true;
        }

        Block anchorBlock = p.getTargetBlockExact(30);
        if (anchorBlock == null || !anchorBlock.getType().isSolid()) {
            p.sendMessage(ChatColor.RED + "No target in range — aim at an entity or a solid block.");
            return false;
        }
        Location anchor = anchorBlock.getLocation().add(0.5, 0.5, 0.5);
        line(p.getWorld(), origin, anchor, Particle.CLOUD);
        Vector zip = anchor.toVector().subtract(p.getLocation().toVector());
        if (zip.lengthSquared() > 0.01) {
            Vector velocity = zip.normalize().multiply(1.05).setY(Math.max(0.35, zip.normalize().getY() * 1.05 + 0.18));
            p.setVelocity(velocity);
            p.setFallDistance(0);
        }
        p.sendMessage(ChatColor.WHITE + "Thwip! " + ChatColor.GRAY + "Web zip!");
        return true;
    }

    /** Sustained web-swing grapple with rope visuals, forward momentum and safe fall reset. */
    private boolean webSwing(Player p) {
        Block b = p.getTargetBlockExact(40);
        if (b == null || !b.getType().isSolid()) {
            p.sendMessage(ChatColor.RED + "Aim at a solid block within 40 blocks to attach your web.");
            return false;
        }
        final Location anchor = b.getLocation().add(0.5, 0.5, 0.5);
        sound(p, Sound.ENTITY_SPIDER_STEP, 1f, 1.2f);
        p.sendMessage(ChatColor.WHITE + "THWIP! " + ChatColor.GRAY + "Web swing engaged — sneak to release.");
        p.setFallDistance(0);

        repeat(p, 55, 1, tick -> {
            if (tick > 4 && p.isSneaking()) throw new CancelTask();
            if (!anchor.getWorld().equals(p.getWorld())) throw new CancelTask();
            Vector toAnchor = anchor.toVector().subtract(p.getLocation().toVector());
            double distance = toAnchor.length();
            if (distance < 2.4 || distance > 55) throw new CancelTask();

            Vector pull = toAnchor.normalize().multiply(0.19);
            Vector forward = p.getLocation().getDirection().setY(0).normalize().multiply(0.10);
            Vector velocity = p.getVelocity().add(pull).add(forward);
            if (velocity.length() > 2.05) velocity.normalize().multiply(2.05);
            p.setVelocity(velocity);
            p.setFallDistance(0);

            Location from = p.getEyeLocation();
            Vector rope = anchor.toVector().subtract(from.toVector());
            int points = Math.max(2, Math.min(18, (int) (rope.length() * 1.4)));
            for (int j = 0; j <= points; j++) {
                Location dot = from.clone().add(rope.clone().multiply(j / (double) points));
                p.getWorld().spawnParticle(Particle.CLOUD, dot, 1, 0.015, 0.015, 0.015, 0);
            }
            if (tick % 8 == 0) sound(p, Sound.BLOCK_WOOL_HIT, 0.25f, 1.8f);
        });
        return true;
    }

    // ---------------------------------------------------------------- STORM
    private boolean lightningStorm(Player p) {
        List<LivingEntity> t = nearest(p, 15, 4);
        if (t.isEmpty()) { p.sendMessage(ChatColor.RED + "No targets for the storm."); return false; }
        for (LivingEntity e : t) { p.getWorld().strikeLightningEffect(e.getLocation()); hurt(p, e, 6); }
        sound(p, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1f);
        return true;
    }

    private boolean tornado(Player p) {
        Block b = p.getTargetBlockExact(25);
        Location center = b != null ? b.getLocation().add(0.5, 1, 0.5) : p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(8));
        World w = p.getWorld();
        sound(p, Sound.ENTITY_BREEZE_WHIRL, 1.5f, 0.8f);
        repeat(p, 40, 2, i -> {
            for (int y = 0; y < 6; y++) {
                double a = (i * 0.6) + y * 0.9, r = 0.6 + y * 0.35;
                w.spawnParticle(Particle.CLOUD, center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r), 2, 0.05, 0.05, 0.05, 0);
            }
            for (LivingEntity e : targets(p, center.clone().add(0, 2, 0), 3.2)) {
                Vector off = e.getLocation().toVector().subtract(center.toVector());
                Vector tangent = new Vector(-off.getZ(), 0, off.getX()).normalize().multiply(0.45);
                e.setVelocity(tangent.setY(0.55));
                if (i % 5 == 0) hurt(p, e, 1.5);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- SUPERMAN
    private boolean freezeBreath(Player p) {
        Location eye = p.getEyeLocation();
        Vector d = eye.getDirection().normalize();
        World w = p.getWorld();
        sound(p, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 0.6f);
        for (double t = 1; t <= 10; t += 0.7) w.spawnParticle(Particle.SNOWFLAKE, eye.clone().add(d.clone().multiply(t)), 8, t * 0.12, t * 0.12, t * 0.12, 0.02);
        for (LivingEntity e : targets(p, eye.clone().add(d.clone().multiply(5)), 6)) {
            Vector to = e.getEyeLocation().toVector().subtract(eye.toVector());
            if (to.length() < 10 && to.normalize().dot(d) > 0.8) {
                e.setFreezeTicks(200);
                e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                hurt(p, e, 2);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- THOR
    private boolean mjolnirThrow(Player p) {
        Location hit = aimPoint(p, 35);
        line(p.getWorld(), p.getEyeLocation(), hit, Particle.ELECTRIC_SPARK);
        sound(p, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.8f);
        p.getWorld().strikeLightningEffect(hit);
        pulse(p, hit, 3, 8, 1.0, Particle.ELECTRIC_SPARK, false);
        later(8, () -> sound(p, Sound.ITEM_TRIDENT_RETURN, 1f, 1f));
        return true;
    }

    private boolean thunderStrike(Player p) {
        Location hit = aimPoint(p, 40);
        p.getWorld().strikeLightningEffect(hit);
        pulse(p, hit, 4, 10, 1.3, Particle.ELECTRIC_SPARK, false);
        for (LivingEntity e : targets(p, hit, 4)) e.setFireTicks(60);
        sound(p, Sound.ITEM_TRIDENT_THUNDER, 1.5f, 1f);
        return true;
    }

    // ---------------------------------------------------------------- WONDER WOMAN
    private boolean lassoOfTruth(Player p) {
        RayTraceResult r = traceEntity(p, 20);
        sound(p, Sound.BLOCK_CHAIN_PLACE, 1f, 0.8f);
        if (r == null || !(r.getHitEntity() instanceof LivingEntity e)) { p.sendMessage(ChatColor.RED + "Your lasso finds no one."); return false; }
        line(p.getWorld(), p.getEyeLocation(), e.getLocation().add(0, 1, 0), Particle.WAX_ON);
        e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        hurt(p, e, 2);
        e.setVelocity(p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(1.3).setY(0.25));
        return true;
    }

    private boolean bracelets(Player p) {
        pl.braceletsUntil.put(p.getUniqueId(), System.currentTimeMillis() + 5000);
        p.sendMessage(ChatColor.GOLD + "Bracelets of Submission! Projectiles deflected (5s)");
        sound(p, Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
        repeat(p, 50, 2, i -> {
            for (Entity e : p.getNearbyEntities(4, 4, 4)) {
                if (e instanceof Projectile pr && !p.equals(pr.getShooter())) {
                    pr.setVelocity(pr.getVelocity().multiply(-1));
                    p.getWorld().spawnParticle(Particle.CRIT, pr.getLocation(), 10, 0.2, 0.2, 0.2, 0.2);
                    sound(p, Sound.ITEM_SHIELD_BLOCK, 1f, 1.8f);
                }
            }
        });
        return true;
    }

    // ================================================================ helpers
    /** Thrown inside a repeat() body to stop the task early. */
    static final class CancelTask extends RuntimeException { CancelTask() { super(null, null, false, false); } }

    void repeat(Player p, int times, int period, IntConsumer body) {
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead() || i >= times) { cancel(); return; }
                try { body.accept(i++); } catch (CancelTask e) { cancel(); }
            }
        }.runTaskTimer(pl, 0L, period);
    }

    void later(long ticks, Runnable r) { Bukkit.getScheduler().runTaskLater(pl, r, ticks); }

    private void waitForLanding(Player p, Runnable onLand) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                t++;
                if (!p.isOnline() || p.isDead() || t > 80) { cancel(); return; }
                p.setFallDistance(0);
                if (t > 6 && p.isOnGround()) { cancel(); onLand.run(); }
            }
        }.runTaskTimer(pl, 0L, 1L);
    }

    private void sound(Player p, Sound s, float vol, float pitch) { p.getWorld().playSound(p.getLocation(), s, vol, pitch); }

    private void buff(Player p, PotionEffectType t, int ticks, int amp) { p.addPotionEffect(new PotionEffect(t, ticks, amp, false, true, true)); }

    private double dmg(Player p, double base) { return base * pl.damageMult(p); }

    void hurt(Player p, LivingEntity e, double base) { e.damage(dmg(p, base), p); }

    List<LivingEntity> targets(Player p, Location c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : c.getWorld().getNearbyEntities(c, r, r, r))
            if (e instanceof LivingEntity le && !(e instanceof ArmorStand) && !e.equals(p) && pl.canHurt(p, le)) out.add(le);
        return out;
    }

    private List<LivingEntity> nearest(Player p, double r, int max) {
        List<LivingEntity> l = targets(p, p.getLocation(), r);
        l.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())));
        return l.size() > max ? new ArrayList<>(l.subList(0, max)) : l;
    }

    private List<Player> playersAround(Player p, double r) {
        List<Player> l = new ArrayList<>(List.of(p));
        for (Entity e : p.getNearbyEntities(r, r, r)) if (e instanceof Player o) l.add(o);
        return l;
    }

    private void pulse(Player p, Location c, double r, double dmg, double kb, Particle part, boolean sphereFx) {
        World w = c.getWorld();
        for (LivingEntity e : targets(p, c, r)) {
            hurt(p, e, dmg);
            Vector away = e.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (away.lengthSquared() < 1e-4) away = new Vector(0, 0, 1);
            e.setVelocity(away.normalize().multiply(kb).setY(0.35 + kb * 0.1));
        }
        if (sphereFx) w.spawnParticle(part, c.clone().add(0, 1, 0), (int) Math.min(120, 20 + r * 8), r / 3, 0.6, r / 3, 0.05);
    }

    private void beam(Player p, Vector dir, double range, Color color, int particles, boolean sonicBoom) {
        World w = p.getWorld();
        Location start = p.getEyeLocation();
        for (double d = 0; d <= range; d += 0.5) {
            Location loc = start.clone().add(dir.clone().multiply(d));
            w.spawnParticle(Particle.DUST, loc, particles, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(color, 1.0f));
            for (LivingEntity e : targets(p, loc, 1.0)) {
                hurt(p, e, 6.0);
            }
        }
    }

    private RayTraceResult traceEntity(Player p, double range) {
        Location eye = p.getEyeLocation();
        RayTraceResult er = p.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.6,
                e -> e instanceof LivingEntity && !e.equals(p) && !(e instanceof ArmorStand) && pl.canHurt(p, (LivingEntity) e));
        if (er == null) return null;
        RayTraceResult br = p.getWorld().rayTraceBlocks(eye, eye.getDirection(), range, FluidCollisionMode.NEVER);
        if (br != null && br.getHitPosition().distance(eye.toVector()) < er.getHitPosition().distance(eye.toVector())) {
            return null;
        }
        return er;
    }

    private void line(World w, Location from, Location to, Particle part) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = dir.length();
        dir.normalize();
        for (double d = 0; d <= dist; d += 0.4) {
            Location loc = from.clone().add(dir.clone().multiply(d));
            w.spawnParticle(part, loc, 1, 0, 0, 0, 0);
        }
    }

    private void portal(World w, Location loc) {
        w.spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.5);
    }

    private void ring(World w, Location center, double radius, Color color, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location l = center.clone().add(x, 0.1, z);
            w.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, new Particle.DustOptions(color, 1.2f));
        }
    }

    private Location aimPoint(Player p, double maxRange) {
        Block b = p.getTargetBlockExact((int) maxRange);
        if (b != null) return b.getLocation().add(0.5, 1, 0.5);
        return p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(maxRange));
    }
}
