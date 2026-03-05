package burkemc.screen;

import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import java.util.List;

public record SlotDefinition(
        ItemConvertible item,
        Text name,
        List<Text> lore
) {
    public static SlotDefinition of(ItemConvertible item, String name, String... loreLines) {
        Text title = Text.literal(name).styled(s -> s.withColor(0xFFFFFF).withItalic(false));
        List<Text> loreTexts = java.util.Arrays.stream(loreLines)
                .map(line -> (Text) Text.literal(line).styled(s -> s.withColor(0xAAAAAA).withItalic(false)))
                .toList();
        return new SlotDefinition(item, title, loreTexts);
    }
}