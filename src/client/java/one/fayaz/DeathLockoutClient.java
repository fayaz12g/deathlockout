package one.fayaz;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import one.fayaz.LockoutNetworking.LockoutSyncPayload;

public class DeathLockoutClient implements ClientModInitializer {

    // Client-side state
    private static int clientGoal = 0;
    private static int clientScore1 = 0;
    private static int clientScore2 = 0;

    @Override
    public void onInitializeClient() {

        // 1. Handle Networking Packet
        ClientPlayNetworking.registerGlobalReceiver(LockoutNetworking.SYNC_TYPE, (payload, context) -> {
            context.client().execute(() -> {
                clientGoal = payload.goal();
                clientScore1 = payload.s1();
                clientScore2 = payload.s2();
            });
        });

        // 2. Render HUD
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (clientGoal <= 0) return; // Don't draw if game not running

            renderLockoutHud(drawContext);
        });
    }

    private void renderLockoutHud(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int centerX = width / 2;
        int topY = 15;

        // Draw Goal Text
        String goalText = String.valueOf(clientGoal);
        int textWidth = client.font.width(goalText);
        graphics.drawString(client.font, goalText, centerX - (textWidth / 2), topY, 0xFFFFFF, true);

        // Draw Circles Logic
        // Radius and spacing
        int radius = 5;
        int gap = 12;

        // Left Side (Player 1 - Red)
        for (int i = 0; i < clientGoal; i++) {
            int x = centerX - 20 - ((i + 1) * gap);
            int y = topY + 4;

            int color = (i < clientScore1) ? 0xFFFF0000 : 0x55000000; // Red if filled, Gray transparent if empty
            renderCircle(graphics, x, y, radius, color);
        }

        // Right Side (Player 2 - Orange)
        for (int i = 0; i < clientGoal; i++) {
            int x = centerX + 20 + ((i + 1) * gap); // Mirror direction
            int y = topY + 4;

            int color = (i < clientScore2) ? 0xFFFFAA00 : 0x55000000; // Orange if filled, Gray transparent if empty
            renderCircle(graphics, x, y, radius, color);
        }
    }

    // Simple circle renderer using standard helper
    private void renderCircle(GuiGraphics graphics, int x, int y, int radius, int color) {
        // Drawing a true circle in MC GUI is usually done by a texture or math.
        // For V1, let's draw a square for "filled" and hollow square for "empty"
        // OR just fillRect for simplicity.
        // If you want a circle, you usually need a texture "circle.png".

        // We will draw a small colored box for now to represent the "Circle"
        graphics.fill(x - radius, y - radius, x + radius, y + radius, color);
    }
}