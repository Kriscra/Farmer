package xyz.geik.farmer.modules.voucher;

import lombok.Getter;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.helpers.Settings;
import xyz.geik.farmer.modules.FarmerModule;

/**
 * Voucher module main class
 */
@Getter
public class Voucher extends FarmerModule {

    // Instance of the module
    private static Voucher instance;

    public static Voucher getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        this.setName("Voucher");
        this.setDescription("Voucher module");
        this.setModulePrefix("Voucher");
        instance = this;
        this.setConfig(Main.getInstance());
        if (!getConfig().getBoolean("settings.feature"))
            this.setEnabled(false);
    }

    @Override
    public void onReload() {}

    @Override
    public void onDisable() {}

    @Override
    public void onEnable() {
        this.setLang(Settings.lang, Main.getInstance());
        registerListener(new VoucherEvent());
    }
}
