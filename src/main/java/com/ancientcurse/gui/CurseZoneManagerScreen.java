package com.ancientcurse.gui;

import com.ancientcurse.client.CurseZoneClientCache;
import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.client.CurseZoneClientCache.ZoneAreaData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CurseZoneManagerScreen extends Screen {
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 240;
    private ZoneListWidget zoneList;
    private ButtonWidget editButton;
    private ButtonWidget deleteButton;
    private ButtonWidget teleportButton;
    
    public CurseZoneManagerScreen() {
        super(Text.translatable("gui.ancientcurse.curse_zone_manager.title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;
        
        // Create zone list
        this.zoneList = new ZoneListWidget(this.client, GUI_WIDTH - 20, GUI_HEIGHT - 80, startY + 30, startY + GUI_HEIGHT - 50);
        this.zoneList.setLeftPos(startX + 10);
        this.addSelectableChild(this.zoneList);
        this.addDrawableChild(this.zoneList);
        
        // Populate the list
        for (var entry : CurseZoneClientCache.getAllAreas().entrySet()) {
            this.zoneList.addEntry(new ZoneListEntry(entry.getKey(), entry.getValue()));
        }
        
        // Edit button
        this.editButton = ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.curse_zone_manager.edit"),
                button -> this.editSelectedZone()
            )
            .dimensions(startX + 10, startY + GUI_HEIGHT - 40, 60, 20)
            .build();
        this.addDrawableChild(this.editButton);
        
        // Delete button
        this.deleteButton = ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.curse_zone_manager.delete"),
                button -> this.deleteSelectedZone()
            )
            .dimensions(startX + 80, startY + GUI_HEIGHT - 40, 60, 20)
            .build();
        this.addDrawableChild(this.deleteButton);
        
        // Teleport button
        this.teleportButton = ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.curse_zone_manager.teleport"),
                button -> this.teleportToSelectedZone()
            )
            .dimensions(startX + 150, startY + GUI_HEIGHT - 40, 60, 20)
            .build();
        this.addDrawableChild(this.teleportButton);
        
        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.common.close"),
                button -> this.close()
            )
            .dimensions(startX + GUI_WIDTH - 70, startY + GUI_HEIGHT - 40, 60, 20)
            .build()
        );
        
        updateButtonStates();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;
        
        // Draw GUI background
        context.fill(startX, startY, startX + GUI_WIDTH, startY + GUI_HEIGHT, 0xCC000000);
        context.drawBorder(startX, startY, GUI_WIDTH, GUI_HEIGHT, 0xFFFFFFFF);
        
        // Title
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_manager.title"),
            centerX,
            startY + 10,
            0xFFFFFF
        );
        
        super.render(context, mouseX, mouseY, delta);
        
        // Render the list after super to ensure it's on top
        this.zoneList.render(context, mouseX, mouseY, delta);
    }
    
    private void updateButtonStates() {
        boolean hasSelection = this.zoneList.getSelectedOrNull() != null;
        this.editButton.active = hasSelection;
        this.deleteButton.active = hasSelection;
        this.teleportButton.active = hasSelection && this.client.player != null && this.client.player.hasPermissionLevel(2);
    }
    
    private void editSelectedZone() {
        ZoneListEntry selected = this.zoneList.getSelectedOrNull();
        if (selected != null) {
            ZoneAreaData area = selected.getArea();
            this.client.setScreen(new CurseZoneEditorScreen(
                selected.getId(), 
                area.min, 
                area.max,
                area.zoneName,
                area.khamsinLevel,
                area.ankhDrainRate,
                area.effectsEnabled
            ));
        }
    }
    
    private void deleteSelectedZone() {
        ZoneListEntry selected = this.zoneList.getSelectedOrNull();
        if (selected != null) {
            // Send delete request to server
            CurseZonePackets.sendDeleteZoneRequest(selected.getId());
            
            // Remove from list by recreating it
            this.zoneList.clear();
            for (var entry : CurseZoneClientCache.getAllAreas().entrySet()) {
                if (!entry.getKey().equals(selected.getId())) {
                    this.zoneList.addEntry(new ZoneListEntry(entry.getKey(), entry.getValue()));
                }
            }
            updateButtonStates();
        }
    }
    
    private void teleportToSelectedZone() {
        ZoneListEntry selected = this.zoneList.getSelectedOrNull();
        if (selected != null && this.client.player != null) {
            ZoneAreaData area = selected.getArea();
            BlockPos center = new BlockPos(
                (area.min.getX() + area.max.getX()) / 2,
                area.min.getY(),
                (area.min.getZ() + area.max.getZ()) / 2
            );
            
            // Send teleport command
            this.client.player.networkHandler.sendChatCommand(
                String.format("tp %d %d %d", center.getX(), center.getY(), center.getZ())
            );
            this.close();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.zoneList != null && this.zoneList.mouseClicked(mouseX, mouseY, button)) {
            updateButtonStates();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.zoneList != null) {
            return this.zoneList.mouseScrolled(mouseX, mouseY, amount);
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    
    // Zone list widget
    private class ZoneListWidget extends AlwaysSelectedEntryListWidget<ZoneListEntry> {
        public ZoneListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, height, top, bottom, 24);
        }
        
        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }
        
        public int addEntry(ZoneListEntry entry) {
            return super.addEntry(entry);
        }
        
        public void clear() {
            this.clearEntries();
        }
        
        @Override
        public int getRowWidth() {
            return GUI_WIDTH - 40;
        }
    }
    
    // Zone list entry
    private class ZoneListEntry extends AlwaysSelectedEntryListWidget.Entry<ZoneListEntry> {
        private final String id;
        private final ZoneAreaData area;
        
        public ZoneListEntry(String id, ZoneAreaData area) {
            this.id = id;
            this.area = area;
        }
        
        public String getId() {
            return id;
        }
        
        public ZoneAreaData getArea() {
            return area;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {
            // Highlight selected entry
            if (CurseZoneManagerScreen.this.zoneList.getSelectedOrNull() == this) {
                context.fill(x - 2, y - 2, x + entryWidth + 2, y + entryHeight + 2, 0x80FFFFFF);
            }
            
            // Zone name
            String displayName = area.zoneName.isEmpty() ? Text.translatable("gui.ancientcurse.curse_zone_manager.unnamed").getString() : area.zoneName;
            context.drawTextWithShadow(textRenderer, displayName, x + 5, y + 2, 0xFFFFFF);
            
            // Zone info on second line
            String info = String.format("Khamsin: %d, Ankh: %d/min", 
                area.khamsinLevel, area.ankhDrainRate);
            context.drawTextWithShadow(textRenderer, info, x + 5, y + 12, 0xAAAAAA);
            
            // Don't draw bounds if they would overlap with the info text
            int infoWidth = textRenderer.getWidth(info);
            if (x + 10 + infoWidth < x + entryWidth - 140) {
                // Zone bounds on the right
                String bounds = String.format("%s to %s", 
                    area.min.toShortString(), area.max.toShortString());
                int boundsWidth = textRenderer.getWidth(bounds);
                context.drawTextWithShadow(textRenderer, bounds, x + entryWidth - boundsWidth - 5, y + 12, 0x888888);
            }
        }
        
        @Override
        public Text getNarration() {
            return Text.literal(area.zoneName);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            CurseZoneManagerScreen.this.zoneList.setSelected(this);
            return true;
        }
    }
}