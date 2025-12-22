package one.fayaz;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LockoutGame {
    public static final LockoutGame INSTANCE = new LockoutGame();

    private boolean active = false;
    private int goal = 10;

    private UUID player1;
    private UUID player2;
    private String p1Name = "";
    private String p2Name = "";

    private int score1 = 0;
    private int score2 = 0;

    // We store the "Cleaned" death strings here
    private final Set<String> claimedDeaths = new HashSet<>();

    public void start(int goal, ServerPlayer p1, ServerPlayer p2) {
        this.active = true;
        this.goal = goal;
        this.player1 = p1.getUUID();
        this.player2 = p2.getUUID();
        this.p1Name = p1.getName().getString();
        this.p2Name = p2.getName().getString();
        this.score1 = 0;
        this.score2 = 0;
        this.claimedDeaths.clear();

        LockoutNetworking.broadcastState(p1.level().getServer(), goal, score1, score2, player1, player2);
    }

    public void stop(MinecraftServer server) {
        this.active = false;
        LockoutNetworking.broadcastState(server, 0, 0, 0, player1, player2);
    }

    public boolean isActive() {
        return active;
    }

    public void handleDeath(ServerPlayer player, DamageSource source) {
        if (!active) return;

        boolean isP1 = player.getUUID().equals(player1);
        boolean isP2 = player.getUUID().equals(player2);
        if (!isP1 && !isP2) return;

        Component deathMessage = source.getLocalizedDeathMessage(player);

        // 1. Get the raw string: "Fayaz fell from a high place"
        String rawText = deathMessage.getString();

        // 2. Remove the player's name: " fell from a high place"
        // We use the player's current name to strip it out.
//        String playerName = player.getName().getString();
//        String uniqueKey = rawText.replace(playerName, "").trim();

        String uniqueKey = rawText.replace(p1Name, "").replace(p2Name, "").trim();

        // --- LOGIC FIX END ---

        if (claimedDeaths.contains(uniqueKey)) {
            player.sendSystemMessage(Component.literal("❌ That one's already been claimed!").withStyle(style -> style.withColor(0xFF5555)));
            return;
        }

        claimedDeaths.add(uniqueKey);

        if (isP1) {
            score1++;
            broadcast(player, Component.literal("🟦 "+ p1Name + "got a point!"));
//            broadcast(player, Component.literal("🟦 "+ p1Name + "got a point: " + rawText));
        } else {
            score2++;
            broadcast(player, Component.literal("🟧 "+ p1Name + "got a point!"));
        }

        LockoutNetworking.broadcastState(player.level().getServer(), goal, score1, score2, player1, player2);

        if (score1 >= goal) win(player, p1Name);
        else if (score2 >= goal) win(player, p2Name);
    }

    private void win(ServerPlayer player, String winner) {
        broadcast(player, Component.literal("🏆 " + winner + " WINS THE LOCKOUT! 🏆").withStyle(style -> style.withBold(true).withColor(0x55FF55)));
        stop(player.level().getServer());
    }

    private void broadcast(ServerPlayer player, Component msg) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }
}