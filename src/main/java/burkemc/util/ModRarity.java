package burkemc.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum ModRarity {
    COMMON(Formatting.WHITE, Text.literal("COMMON")),
    UNCOMMON(Formatting.GREEN, Text.literal("UNCOMMON")),
    RARE(Formatting.BLUE, Text.literal("RARE")),
    EPIC(Formatting.DARK_PURPLE, Text.literal("EPIC")),
    LEGENDARY(Formatting.GOLD, Text.literal("LEGENDARY")),
    MYTHIC(Formatting.LIGHT_PURPLE, Text.literal("MYTHIC"));

    public final Formatting color;
    public final Text text;

    ModRarity(Formatting color, Text text){
        this.color = color;
        this.text = text.copy().formatted(color, Formatting.BOLD).styled(style -> style.withItalic(false));
    }
}
