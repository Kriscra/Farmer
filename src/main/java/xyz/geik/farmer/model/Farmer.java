package xyz.geik.farmer.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.model.inventory.FarmerInv;
import xyz.geik.farmer.model.user.FarmerPerm;
import xyz.geik.farmer.model.user.User;
import xyz.geik.farmer.modules.autoharvest.AutoHarvest;
import xyz.geik.farmer.modules.autoseller.AutoSeller;
import xyz.geik.farmer.modules.spawnerkiller.SpawnerKiller;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main farmer object
 */
@Setter
@Getter
public class Farmer implements Cloneable {

    // Region id of farmer
    private String regionID;

    // User list of farmer
    private Set<User> users;

    // Inventory of farmer
    private FarmerInv inv;

    // Level of farmer
    private FarmerLevel level;

    // State shows farmer collect state
    // id is farmer id generated by database
    private int state, id;

    private HashMap<String, Boolean> moduleAttributes = new HashMap<>();

    /**
     * Gets attribute from Farmer
     *
     * @param attribute
     * @return
     */
    public boolean getAttributeStatus(String attribute) {
        if (getModuleAttributes().containsKey(attribute))
            return getModuleAttributes().get(attribute);
        else return getDefaultStatus(attribute);
    }

    /**
     * Change attribute
     *
     * @param attribute
     * @return
     */
    public boolean changeAttribute(String attribute) {
        if (getModuleAttributes().containsKey(attribute)) {
            getModuleAttributes().remove(attribute);
            return getDefaultStatus(attribute);
        }
        else {
            boolean status = !getDefaultStatus(attribute);
            getModuleAttributes().put(attribute, status);
            return status;
        }
    }

    /**
     * Get default status of attribute
     *
     * @param attribute
     * @return
     */
    private boolean getDefaultStatus(@NotNull String attribute) {
        switch (attribute) {
            case "spawnerkiller":
                return SpawnerKiller.getInstance().isDefaultStatus();
            case "autoharvest":
                return AutoHarvest.getInstance().isDefaultStatus();
            case "autoseller":
                return AutoSeller.getInstance().isDefaultStatus();
            default:
                return false;
        }
    }

    /**
     * First constructor of farmer which already created before
     * and loads it again.
     *
     * @param id
     * @param regionID
     * @param users
     * @param inv
     * @param level
     * @param state
     */
    public Farmer(int id, String regionID, Set<User> users,
                  FarmerInv inv, FarmerLevel level, int state) {
        this.id = id;
        this.regionID = regionID;
        this.users = users;
        this.inv = inv;
        this.level = level;
        this.state = state;
    }

    /**
     * Second constructor of farmer which creates fresh farmer.
     *
     * @param regionID id of region
     * @param ownerUUID uuid of owner
     * @param level level of farmer
     */
    public Farmer(String regionID, UUID ownerUUID, int level) {
        this.regionID = regionID;
        Set<User> users = new LinkedHashSet<>();
        this.users = users;
        this.inv = new FarmerInv();
        this.level = FarmerLevel.getAllLevels().get(level);
        this.state = 1;
        Main.getInstance().getSql().createFarmer(this);
    }

    /**
     * Gets owner uuid of farmer
     *
     * @return
     */
    public UUID getOwnerUUID() {
        return Main.getIntegration().getOwnerUUID(getRegionID());
    }

    /**
     * Is the user owner returns true or false
     *
     * @param user
     * @return
     */
    private boolean isUserOwner(@NotNull User user) {
        return user.getPerm().equals(FarmerPerm.OWNER);
    }

    /**
     * Gets users without owner
     *
     * @return
     */
    public Set<User> getUsersWithoutOwner() {
        return users.stream().filter(this::isUserNotOwner).collect(Collectors.toSet());
    }

    /**
     * Is user not owner
     *
     * @param user
     * @return
     */
    private boolean isUserNotOwner(@NotNull User user) {
        return !user.getPerm().equals(FarmerPerm.OWNER);
    }


    /**
     * Adds user to farmer with COOP role
     *
     * @param uuid
     * @param name
     */
    public void addUser(UUID uuid, String name) {
        addUser(uuid, name, FarmerPerm.COOP);
    }

    /**
     * Adds user to farmer with desired role
     *
     * @param uuid uuid of player
     * @param name name of player
     * @param perm perm of player
     */
    public void addUser(UUID uuid, String name, FarmerPerm perm) {
        Main.getInstance().getSql().addUser(uuid, name, perm, this);
    }

    /**
     * Delete user from farmer
     *
     * @param user
     * @return
     */
    public boolean removeUser(@NotNull User user) {
        return Main.getInstance().getSql().removeUser(user, this);
    }

    /**
     * Saves farmer async
     */
    public void saveFarmerAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), this::saveFarmer);
    }

    /**
     * Saves farmer sync
     */
    public void saveFarmer() {
        Main.getInstance().getSql().saveFarmer(this);
    }

    /**
     * Clones farmer object
     * @return Farmer object
     */
    @Override
    public Farmer clone() {
        try {
            Farmer clone = (Farmer) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}