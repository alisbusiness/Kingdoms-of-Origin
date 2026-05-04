package com.example.kingdoms.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class OriginInfoScreen extends Screen {

    private static final List<String> KING_POWERS = List.of(
            "Iron Fist (+3 attack damage)",
            "Crown's Resilience (+5 hearts, +4 armor, +2 toughness)",
            "Sovereign's March (+15% movement speed)",
            "Royal Radiance (you glow)",
            "Mark of the Crown (-0.2 knockback resistance)",
            "Undeniable Presence (immune to invisibility)",
            "Divine Flight (creative-style flight)",
            "Soft Hands (-30% mining speed)",
            "Prime Target (+50% projectile damage taken)",
            "Creature of Light (cursed in darkness)",
            "Royal Decree — Primary Active (rally nearby subjects)",
            "Wrath of the King — Secondary Active (call lightning)"
    );

    private final String originId;
    private final Text title;
    private final Text description;
    private final List<String> powers;

    public OriginInfoScreen(String originId) {
        super(Text.literal("Your Origin"));
        this.originId = originId == null ? "" : originId;
        switch (this.originId) {
            case "kingdoms_of_origin:king" -> {
                this.title       = Text.literal("The King").formatted(Formatting.GOLD, Formatting.BOLD);
                this.description = Text.literal(
                        "You bear the crown of the realm. Power flows through you, but the weight of rule is heavy."
                ).formatted(Formatting.YELLOW);
                this.powers = KING_POWERS;
            }
            case "kingdoms_of_origin:human" -> {
                this.title       = Text.literal("Subject of the Realm").formatted(Formatting.WHITE, Formatting.BOLD);
                this.description = Text.literal(
                        "An ordinary citizen of the kingdom. You hold no special power, but the crown could yet find its way to you."
                ).formatted(Formatting.GRAY);
                this.powers = List.of();
            }
            default -> {
                this.title       = Text.literal("No Origin").formatted(Formatting.RED, Formatting.BOLD);
                this.description = Text.literal(
                        "Your origin could not be determined. (" + this.originId + ")"
                ).formatted(Formatting.GRAY);
                this.powers = List.of();
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(this.width / 2 - 50, this.height - 36, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        TextRenderer tr = this.textRenderer;

        int centerX = this.width / 2;
        int y = 30;

        context.drawCenteredTextWithShadow(tr, this.title, centerX, y, 0xFFFFFFFF);
        y += 18;

        int wrapWidth = Math.min(this.width - 60, 320);
        for (var line : tr.wrapLines(this.description, wrapWidth)) {
            context.drawCenteredTextWithShadow(tr, line, centerX, y, 0xFFFFFFFF);
            y += 12;
        }

        if (!this.powers.isEmpty()) {
            y += 10;
            context.drawCenteredTextWithShadow(tr,
                    Text.literal("Powers").formatted(Formatting.UNDERLINE), centerX, y, 0xFFFFFFFF);
            y += 14;
            for (String p : this.powers) {
                context.drawCenteredTextWithShadow(tr, Text.literal("• " + p), centerX, y, 0xFFDDDDDD);
                y += 11;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
