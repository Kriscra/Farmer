package xyz.geik.farmer.model;

import de.leonhard.storage.Config;
import lombok.Getter;
import lombok.Setter;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.helpers.Settings;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FarmerLevel {

    private static List<FarmerLevel> farmerLevels = new ArrayList<>();

    private String dataName;
    private long capacity, reqMoney;
    private double tax;
    private String perm;

    public FarmerLevel(String dataName, long capacity, long reqMoney, double tax, String perm) {
        this.dataName = dataName;
        this.capacity = capacity;
        this.reqMoney = reqMoney;
        this.tax = tax;
        this.perm = perm;
    }

    /**
     * installs level of farmer dependencies
     * and stats then convert it to FarmerLevel object
     */
    public static void installLevels() {
        if (!farmerLevels.isEmpty())
            farmerLevels.clear();

        Config config = Main.getConfigFile();
        config.singleLayerKeySet("levels").stream().forEach(key -> {
            long capacity = config.getLong("levels." + key + ".capacity");
            long reqMoney = (config.contains("levels." + key + ".reqMoney")) ?
                    config.getLong("levels." + key + ".reqMoney") : 0;
            double tax = (config.contains("levels." + key + ".tax")) ?
                    config.getDouble("levels." + key + ".tax") : Settings.defaultTax;
            String perm = (config.contains("levels." + key + ".reqPerm")) ?
                    config.getString("levels." + key + ".reqPerm") : null;
            FarmerLevel level = new FarmerLevel(key, capacity, reqMoney, tax, perm);
            farmerLevels.add(level);
        });
    }

    public static List<FarmerLevel> getAllLevels() {
        return farmerLevels;
    }

    public static FarmerLevel getLevel(String name) {
        return getAllLevels().stream().filter(level -> (level.getDataName() == name)).findFirst().get();
    }
}
