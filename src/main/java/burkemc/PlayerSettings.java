package burkemc;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

public class PlayerSettings {
    public boolean showMenuItem;

    public static PlayerSettings readFrom(ReadView view) {
        PlayerSettings settings = new PlayerSettings();
        ReadView data = view.getReadView("burkemc_settings");
        settings.showMenuItem = data.getBoolean("showMenuItem", true);
        return settings;
    }

    public void writeTo(WriteView view) {
        WriteView data = view.get("burkemc_settings");
        data.putBoolean("showMenuItem", showMenuItem);
    }
}