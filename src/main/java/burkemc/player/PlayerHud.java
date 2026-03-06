package burkemc.player;

import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Optional;

public class PlayerHud {
    private static final String OBJECTIVE_NAME = "BURKEMC";
    private static final Text DISPLAY_NAME = Text.literal("§bburke.host");

    private static ScoreboardObjective makeObjective() {
        return new ScoreboardObjective(
                new Scoreboard(),
                OBJECTIVE_NAME,
                ScoreboardCriterion.DUMMY,
                DISPLAY_NAME,
                ScoreboardCriterion.RenderType.INTEGER,
                false,
                null
        );
    }

    public static void initialize(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }

        var scoreboard = server.getScoreboard();

        var scoreboardObjective = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (scoreboardObjective != null) {
            player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(scoreboardObjective, 1));
        }

        ScoreboardObjective obj = makeObjective();
        player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(obj, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
        player.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, obj));

        refresh(player);
    }

    public static void refresh(ServerPlayerEntity player) {
        int balance = (int) IPlayerData.of(player).getWallet().getBalance();

        sendScore(player, " ", 1, null);
        sendScore(player, "purse_row", 0, Text.literal("§6Purse: §f" + balance));
    }

    private static void sendScore(ServerPlayerEntity player, String name, int score, Text display) {
        player.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket(
                name,
                OBJECTIVE_NAME,
                score,
                Optional.ofNullable(display),
                Optional.of(BlankNumberFormat.INSTANCE)
        ));
    }
}
