package one.fayaz;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class LockoutScreen extends Screen {

    private final List<LockoutClient.PlayerData> players;
    private final String mode;
    private final int goal;

    // Tab system
    private enum Tab {
        STATUS("Status"),
        PLAYERS("Players"),
        SETTINGS("Settings");

        private final String name;
        Tab(String name) { this.name = name; }
    }

    private Tab currentTab = Tab.STATUS;

    // UI Components
    private Button startButton;
    private Button stopButton;
    private Button pauseButton;
    private Button unpauseButton;

    private EditBox goalInput;
    private Button setGoalButton;

    private Button joinButton;

    // Mode selection buttons
    private Button modeDeathButton;
    private Button modeKillsButton;
    private Button modeArmorButton;
    private Button modeAdvancementsButton;
    private Button modeFoodsButton;
    private Button modeBreedButton;
    private Button modeMixedButton;

    // Armor sub-mode
    private Button armorSetButton;
    private Button armorPieceButton;

    // Death sub-mode
    private Button deathSourceButton;
    private Button deathMessageButton;

    // Configuration toggles
    private Checkbox switchCheckbox;
    private Checkbox resetWorldCheckbox;
    private Checkbox snarkyMessagesCheckbox;

    // Mixed mode toggles
    private Checkbox mixedDeathCheckbox;
    private Checkbox mixedKillsCheckbox;
    private Checkbox mixedArmorCheckbox;
    private Checkbox mixedAdvancementsCheckbox;
    private Checkbox mixedFoodsCheckbox;
    private Checkbox mixedBreedCheckbox;

    public LockoutScreen(List<LockoutClient.PlayerData> players, String mode, int goal) {
        super(Component.literal("Lockout Configuration"));
        this.players = players;
        this.mode = mode;
        this.goal = goal;
    }

    @Override
    protected void init() {
        super.init();

        // Tab buttons
        int tabWidth = 80;
        int tabY = 30;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabX = (width / 2) - (Tab.values().length * tabWidth / 2) + (i * tabWidth);
            this.addRenderableWidget(Button.builder(
                    Component.literal(tab.name),
                    btn -> switchTab(tab)
            ).bounds(tabX, tabY, tabWidth - 5, 20).build());
        }

        // Initialize components based on current tab
        rebuildWidgets();
    }

    private void switchTab(Tab newTab) {
        this.currentTab = newTab;
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        // Clear all widgets except tab buttons
        this.children().removeIf(widget -> !(widget instanceof Button &&
                ((Button) widget).getMessage().getString().equals("Status") ||
                ((Button) widget).getMessage().getString().equals("Players") ||
                ((Button) widget).getMessage().getString().equals("Settings")));
        this.renderables.removeIf(widget -> !(widget instanceof Button &&
                ((Button) widget).getMessage().getString().equals("Status") ||
                ((Button) widget).getMessage().getString().equals("Players") ||
                ((Button) widget).getMessage().getString().equals("Settings")));

        int startY = 60;

        switch (currentTab) {
            case STATUS -> buildStatusTab(startY);
            case PLAYERS -> buildPlayersTab(startY);
            case SETTINGS -> buildSettingsTab(startY);
        }
    }

    private void buildStatusTab(int startY) {
        int centerX = width / 2;

        // Game control buttons
        boolean isActive = LockoutClient.clientPaused || players.stream().anyMatch(p -> !p.claims.isEmpty());
        boolean isPaused = LockoutClient.clientPaused;

        startButton = this.addRenderableWidget(Button.builder(
                Component.literal("Start Game"),
                btn -> sendCommand("/lockout start")
        ).bounds(centerX - 155, startY, 100, 20).build());
        startButton.active = !isActive;

        stopButton = this.addRenderableWidget(Button.builder(
                Component.literal("Stop Game"),
                btn -> sendCommand("/lockout stop")
        ).bounds(centerX - 50, startY, 100, 20).build());
        stopButton.active = isActive;

        pauseButton = this.addRenderableWidget(Button.builder(
                Component.literal("Pause"),
                btn -> sendCommand("/lockout pause")
        ).bounds(centerX + 55, startY, 100, 20).build());
        pauseButton.active = isActive && !isPaused;

        unpauseButton = this.addRenderableWidget(Button.builder(
                Component.literal("Unpause"),
                btn -> sendCommand("/lockout unpause")
        ).bounds(centerX + 55, startY, 100, 20).build());
        unpauseButton.active = isActive && isPaused;
        unpauseButton.visible = isPaused;
        pauseButton.visible = !isPaused;

        // Add some spacing for the HUD display
        // The HUD will be rendered in the render() method
    }

    private void buildPlayersTab(int startY) {
        int centerX = width / 2;

        // Join button
        joinButton = this.addRenderableWidget(Button.builder(
                Component.literal("Join Game"),
                btn -> sendCommand("/lockout join")
        ).bounds(centerX - 50, startY, 100, 20).build());

        // Player list will be rendered in render() method
    }

    private void buildSettingsTab(int startY) {
        int centerX = width / 2;
        int leftCol = centerX - 150;
        int rightCol = centerX + 10;
        int currentY = startY;

        // Goal setting
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Goal:", leftCol, currentY + 5, 0xFFFFFF));

        goalInput = this.addRenderableWidget(new EditBox(
                font, leftCol + 40, currentY, 50, 20, Component.literal("Goal")
        ));
        goalInput.setValue(String.valueOf(goal));
        goalInput.setMaxLength(3);

        setGoalButton = this.addRenderableWidget(Button.builder(
                Component.literal("Set"),
                btn -> {
                    try {
                        int newGoal = Integer.parseInt(goalInput.getValue());
                        if (newGoal > 0) {
                            sendCommand("/lockout configure goal " + newGoal);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid input
                    }
                }
        ).bounds(leftCol + 95, currentY, 40, 20).build());

        currentY += 30;

        // Game Mode Selection
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Game Mode:", leftCol, currentY, 0xFFFFFF));
        currentY += 15;

        String currentMode = mode;

        modeDeathButton = this.addRenderableWidget(Button.builder(
                Component.literal("Death" + (currentMode.equals("DEATH") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode death source")
        ).bounds(leftCol, currentY, 70, 20).build());

        modeKillsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Kills" + (currentMode.equals("KILLS") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode kills")
        ).bounds(leftCol + 75, currentY, 70, 20).build());

        currentY += 25;

        modeArmorButton = this.addRenderableWidget(Button.builder(
                Component.literal("Armor" + (currentMode.equals("ARMOR") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode armor set")
        ).bounds(leftCol, currentY, 70, 20).build());

        modeAdvancementsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Advancements" + (currentMode.equals("ADVANCEMENTS") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode advancements")
        ).bounds(leftCol + 75, currentY, 110, 20).build());

        currentY += 25;

        modeFoodsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Foods" + (currentMode.equals("FOODS") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode foods")
        ).bounds(leftCol, currentY, 70, 20).build());

        modeBreedButton = this.addRenderableWidget(Button.builder(
                Component.literal("Breed" + (currentMode.equals("BREED") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode breed")
        ).bounds(leftCol + 75, currentY, 70, 20).build());

        currentY += 25;

        modeMixedButton = this.addRenderableWidget(Button.builder(
                Component.literal("Mixed" + (currentMode.equals("MIXED") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode mixed")
        ).bounds(leftCol, currentY, 70, 20).build());

        currentY += 30;

        // Sub-mode settings (Death)
        if (currentMode.equals("DEATH")) {
            this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                    graphics.drawString(font, "Death Mode:", leftCol, currentY, 0xFFFFFF));
            currentY += 15;

            deathSourceButton = this.addRenderableWidget(Button.builder(
                    Component.literal("By Source"),
                    btn -> sendCommand("/lockout configure mode death source")
            ).bounds(leftCol, currentY, 90, 20).build());

            deathMessageButton = this.addRenderableWidget(Button.builder(
                    Component.literal("By Message"),
                    btn -> sendCommand("/lockout configure mode death message")
            ).bounds(leftCol + 95, currentY, 90, 20).build());

            currentY += 25;
        }

        // Sub-mode settings (Armor)
        if (currentMode.equals("ARMOR")) {
            this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                    graphics.drawString(font, "Armor Mode:", leftCol, currentY, 0xFFFFFF));
            currentY += 15;

            armorSetButton = this.addRenderableWidget(Button.builder(
                    Component.literal("Full Set"),
                    btn -> sendCommand("/lockout configure mode armor set")
            ).bounds(leftCol, currentY, 90, 20).build());

            armorPieceButton = this.addRenderableWidget(Button.builder(
                    Component.literal("Any Piece"),
                    btn -> sendCommand("/lockout configure mode armor piece")
            ).bounds(leftCol + 95, currentY, 90, 20).build());

            currentY += 25;
        }

        // Mixed mode configuration
        if (currentMode.equals("MIXED")) {
            currentY = startY;
            this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                    graphics.drawString(font, "Include in Mixed:", rightCol, currentY, 0xFFFFFF));
            currentY += 15;

            mixedDeathCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Death"), font
            ).pos(rightCol, currentY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include death");
                else sendCommand("/lockout configure mixed exclude death");
            }).build());
            currentY += 25;

            mixedKillsCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Kills"), font
            ).pos(rightCol, currentY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include kills");
                else sendCommand("/lockout configure mixed exclude kills");
            }).build());
            currentY += 25;

            mixedArmorCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Armor"), font
            ).pos(rightCol, currentY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include armor");
                else sendCommand("/lockout configure mixed exclude armor");
            }).build());
            currentY += 25;

            mixedAdvancementsCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Advancements"), font
            ).pos(rightCol, currentY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include advancements");
                else sendCommand("/lockout configure mixed exclude advancements");
            }).build());
            currentY += 25;

            mixedFoodsCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Foods"), font
            ).pos(rightCol, currentY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include foods");
                else sendCommand("/lockout configure mixed exclude foods");
            }).build());
            currentY += 25;

            mixedBreedCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Breed"), font
            ).pos(rightCol, currentY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include breed");
                else sendCommand("/lockout configure mixed exclude breed");
            }).build());
            currentY += 25;
        }

        // General toggles
        currentY = startY + 180;
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Options:", leftCol, currentY, 0xFFFFFF));
        currentY += 15;

        switchCheckbox = this.addRenderableWidget(Checkbox.builder(
                Component.literal("Player Switching"), font
        ).pos(leftCol, currentY).onValueChange((cb, val) ->
                sendCommand("/lockout configure switch " + val)
        ).build());
        currentY += 25;

        resetWorldCheckbox = this.addRenderableWidget(Checkbox.builder(
                Component.literal("Reset World on Start"), font
        ).pos(leftCol, currentY).onValueChange((cb, val) ->
                sendCommand("/lockout configure reset_world " + val)
        ).build());
        currentY += 25;

        snarkyMessagesCheckbox = this.addRenderableWidget(Checkbox.builder(
                Component.literal("Snarky Messages"), font
        ).pos(leftCol, currentY).onValueChange((cb, val) ->
                sendCommand("/lockout configure snarky_messages " + val)
        ).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderTransparentBackground(graphics);

        // Title
        graphics.drawCenteredString(
                font,
                "Lockout Configuration",
                width / 2,
                10,
                0xFFFFFF
        );

        // Render tab-specific content
        switch (currentTab) {
            case STATUS -> renderStatusTab(graphics, mouseX, mouseY);
            case PLAYERS -> renderPlayersTab(graphics, mouseX, mouseY);
            case SETTINGS -> renderSettingsTab(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderStatusTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Display current status
        int centerX = width / 2;
        int startY = 95;

        graphics.drawCenteredString(font, "Mode: " + mode, centerX, startY, 0xAAFFAA);
        graphics.drawCenteredString(font, "Goal: " + goal, centerX, startY + 15, 0xAAFFAA);

        if (LockoutClient.clientPaused) {
            String pauseMsg = LockoutClient.clientPausedPlayerName.isEmpty()
                    ? "Game Paused"
                    : "Waiting for " + LockoutClient.clientPausedPlayerName;
            graphics.drawCenteredString(font, pauseMsg, centerX, startY + 30, 0xFFAA00);
        }

        // Render the lockout HUD display
        if (!players.isEmpty() && goal > 0) {
            LockoutHud.HoverInfo hoverInfo = LockoutHud.renderLockout(
                    graphics,
                    players,
                    mode,
                    goal,
                    width,
                    startY + 60,
                    font,
                    mouseX,
                    mouseY
            );

            // Render tooltip if hovering over something
            if (hoverInfo != null) {
                renderTooltip(graphics, hoverInfo);
            }
        }
    }

    private void renderPlayersTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = width / 2;
        int startY = 95;

        if (players.isEmpty()) {
            graphics.drawCenteredString(font, "No players in game", centerX, startY + 20, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(font, "Players (" + players.size() + "):", centerX, startY, 0xFFFFFF);

            int y = startY + 20;
            for (LockoutClient.PlayerData player : players) {
                String playerText = player.name + " - " + player.claims.size() + "/" + goal;
                graphics.drawCenteredString(font, playerText, centerX, y, player.color);
                y += 15;
            }
        }
    }

    private void renderSettingsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Settings are rendered via widgets, no additional rendering needed
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderTooltip(GuiGraphics graphics, LockoutHud.HoverInfo hoverInfo) {
        LockoutClient.PlayerData player = hoverInfo.player;
        int index = hoverInfo.slotIndex;

        if (index >= player.claims.size()) return;

        ItemStack stack = player.icons.get(index);
        LockoutNetworking.ClaimData claim = player.claims.get(index);
        String claimText = claim.id();

        List<Component> tooltip = stack.getTooltipLines(
                Item.TooltipContext.EMPTY,
                this.minecraft.player,
                this.minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
        );

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(player.name).withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal(claimText).withStyle(ChatFormatting.GRAY));

        graphics.setComponentTooltipForNextFrame(font, tooltip, hoverInfo.mouseX, hoverInfo.mouseY);
    }

    private void sendCommand(String command) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand(command.substring(1)); // Remove leading slash
        }
    }
}