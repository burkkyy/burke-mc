package burkemc.player;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

public class PlayerWallet {
    public static final long MAX_WALLET_AMOUNT = 10000;
    private long amount;

    public long getBalance() {
        return amount;
    }

    public boolean deposit(long amount) {
        long newAmount = this.amount + amount;

        if (newAmount > MAX_WALLET_AMOUNT) {
            return false;
        }

        this.amount = newAmount;
        return true;
    }

    public boolean withdraw(long amount) {
        if (this.amount < amount) {
            return false;
        }

        this.amount -= amount;
        return true;
    }

    public static PlayerWallet readFrom(ReadView view) {
        PlayerWallet wallet = new PlayerWallet();
        ReadView data = view.getReadView("burkemc_wallet");
        wallet.amount = data.getLong("amount", 0);
        return wallet;
    }

    public void writeTo(WriteView view) {
        WriteView data = view.get("burkemc_wallet");
        data.putLong("amount", amount);
    }
}
