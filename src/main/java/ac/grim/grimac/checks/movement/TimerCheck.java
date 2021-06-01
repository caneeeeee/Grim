package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class TimerCheck extends Check {
    public int exempt = 400; // Exempt for 20 seconds on login
    GrimPlayer player;
    double packetX = Double.MAX_VALUE;
    double packetY = Double.MAX_VALUE;
    double packetZ = Double.MAX_VALUE;
    float packetXRot = Float.MAX_VALUE;
    float packetYRot = Float.MAX_VALUE;
    long timerMillis = Integer.MIN_VALUE;

    public TimerCheck(GrimPlayer player) {
        this.player = player;
    }

    public void processMovementPacket(double playerX, double playerY, double playerZ, float xRot, float yRot) {

        // Teleporting sends it's own packet
        if (exempt-- > 0) {
            timerMillis = Math.max(timerMillis, player.getPlayerClockAtLeast());
            return;
        }

        // Teleports send their own packet and mess up reminder packet status
        // Exempting reminder packets for 5 movement packets for teleports is fine.
        // 1.8 clients spam movement packets every tick, even if they didn't move
        boolean isReminder = playerX == packetX && playerY == packetY && playerZ == packetZ && packetXRot == xRot && packetYRot == yRot && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);

        // Note that players are exempt by the current structure of the check until they respond to a transaction
        if (isReminder) {
            timerMillis += 950;
        }

        if (timerMillis > System.currentTimeMillis()) {
            Bukkit.broadcastMessage(ChatColor.RED + player.bukkitPlayer.getName() + " is using timer!");

            // This seems like the best way to reset violations
            timerMillis -= 50;
        }

        timerMillis += 50;

        // Don't let the player's movement millis value fall behind the known base from transaction ping
        timerMillis = Math.max(timerMillis, player.getPlayerClockAtLeast());

        player.movementPackets++;

        this.packetX = playerX;
        this.packetY = playerY;
        this.packetZ = playerZ;
        this.packetXRot = xRot;
        this.packetYRot = yRot;
    }
}
