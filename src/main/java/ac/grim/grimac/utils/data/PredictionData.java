package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.Collection;

public class PredictionData {
    private static final Method onePointEightAttribute;
    private static Object movementSpeedAttribute;

    static {
        onePointEightAttribute = Reflection.getMethod(NMSUtils.entityHumanClass, "getAttributeInstance", 0);
        try {
            if (XMaterial.getVersion() == 8) {
                // 1.8 mappings
                movementSpeedAttribute = NMSUtils.getNMSClass("GenericAttributes").getDeclaredField("MOVEMENT_SPEED").get(null);
            } else if (XMaterial.getVersion() < 8) {
                // 1.7 mappings
                movementSpeedAttribute = NMSUtils.getNMSClass("GenericAttributes").getDeclaredField("d").get(null);
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public GrimPlayer player;
    public double playerX;
    public double playerY;
    public double playerZ;
    public float xRot;
    public float yRot;
    public boolean onGround;
    public boolean isSprinting;
    public boolean isSneaking;
    public boolean isFlying;
    public boolean isClimbing;
    public boolean isFallFlying;
    public World playerWorld;
    public WorldBorder playerWorldBorder;
    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float slowFallingAmplifier;
    public float dolphinsGraceAmplifier;
    public float flySpeed;
    public double fallDistance;
    // Debug, does nothing.
    public int number;
    public boolean inVehicle;
    public Entity playerVehicle;
    public float vehicleHorizontal;
    public float vehicleForward;
    public boolean isSprintingChange;
    public boolean isSneakingChange;
    public boolean isJustTeleported = false;
    public VelocityData firstBreadKB = null;
    public VelocityData requiredKB = null;
    public VelocityData firstBreadExplosion = null;
    public VelocityData possibleExplosion = null;
    public int minimumTickRequiredToContinue;
    public int lastTransaction;

    // For regular movement
    public PredictionData(GrimPlayer player, double playerX, double playerY, double playerZ, float xRot, float yRot, boolean onGround) {
        this.player = player;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;
        this.inVehicle = player.playerVehicle != null;

        this.number = player.taskNumber.getAndIncrement();

        this.isSprinting = player.isPacketSprinting;
        this.isSneaking = player.isPacketSneaking;

        this.isSprintingChange = player.isPacketSprintingChange;
        this.isSneakingChange = player.isPacketSneakingChange;
        player.isPacketSprintingChange = false;
        player.isPacketSneakingChange = false;

        this.isFlying = player.compensatedFlying.canFlyLagCompensated();

        this.isClimbing = Collisions.onClimbable(player);
        this.isFallFlying = XMaterial.getVersion() > 8 && player.bukkitPlayer.isGliding();
        this.playerWorld = player.bukkitPlayer.getWorld();
        this.fallDistance = player.bukkitPlayer.getFallDistance();
        this.movementSpeed = getMovementSpeedAttribute(player.bukkitPlayer);

        // When a player punches a mob, bukkit thinks the player isn't sprinting
        if (isSprinting && !player.bukkitPlayer.isSprinting()) this.movementSpeed *= 1.3D;

        Collection<PotionEffect> playerPotionEffects = player.bukkitPlayer.getActivePotionEffects();

        this.jumpAmplifier = getHighestPotionEffect(playerPotionEffects, "JUMP", 0);
        this.levitationAmplifier = getHighestPotionEffect(playerPotionEffects, "LEVITATION", 9);
        this.slowFallingAmplifier = getHighestPotionEffect(playerPotionEffects, "SLOW_FALLING", 13);
        this.dolphinsGraceAmplifier = getHighestPotionEffect(playerPotionEffects, "DOLPHINS_GRACE", 13);

        this.flySpeed = player.bukkitPlayer.getFlySpeed() / 2;
        this.playerVehicle = player.bukkitPlayer.getVehicle();

        firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback();
        requiredKB = player.knockbackHandler.getRequiredKB();

        firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion();
        possibleExplosion = player.explosionHandler.getPossibleExplosions();

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 1;
        lastTransaction = player.packetLastTransactionReceived;
    }

    // For boat movement
    public PredictionData(GrimPlayer player, double boatX, double boatY, double boatZ, float xRot, float yRot) {
        this.player = player;
        this.playerX = boatX;
        this.playerY = boatY;
        this.playerZ = boatZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.playerVehicle = player.bukkitPlayer.getVehicle();
        this.vehicleForward = player.packetVehicleForward;
        this.vehicleHorizontal = player.packetVehicleHorizontal;

        this.inVehicle = true;

        this.isFlying = false;
        this.isClimbing = false;
        this.isFallFlying = false;
        this.playerWorld = player.bukkitPlayer.getWorld();
        this.fallDistance = player.bukkitPlayer.getFallDistance();
        this.movementSpeed = getMovementSpeedAttribute(player.bukkitPlayer);

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 1;
        lastTransaction = player.packetLastTransactionReceived;
    }

    private double getMovementSpeedAttribute(Player player) {
        if (XMaterial.getVersion() > 8) {
            return player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
        }

        try {
            Method handle = Reflection.getMethod(player.getClass(), "getHandle", 0);
            Object attribute = onePointEightAttribute.invoke(handle.invoke(player), movementSpeedAttribute);
            Method valueField = attribute.getClass().getMethod("getValue");
            return (double) valueField.invoke(attribute);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.1f;
    }

    private float getHighestPotionEffect(Collection<PotionEffect> effects, String typeName, int minimumVersion) {
        if (XMaterial.getVersion() < minimumVersion) return 0;

        PotionEffectType type = PotionEffectType.getByName(typeName);

        float highestEffect = 0;
        for (PotionEffect effect : effects) {
            if (effect.getType() == type && effect.getAmplifier() > highestEffect)
                highestEffect = effect.getAmplifier();
        }

        return highestEffect;
    }
}
