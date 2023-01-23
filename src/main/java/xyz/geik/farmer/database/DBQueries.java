package xyz.geik.farmer.database;

import org.bukkit.Bukkit;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.model.Farmer;
import xyz.geik.farmer.model.FarmerLevel;
import xyz.geik.farmer.model.inventory.FarmerInv;
import xyz.geik.farmer.model.inventory.FarmerItem;
import xyz.geik.farmer.model.user.FarmerPerm;
import xyz.geik.farmer.model.user.User;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Database Queries for
 * database updating.
 */
public class DBQueries {

    /**
     * Create table if not exists.
     *
     * Farmers:
     * id (INTEGER PK AI), // Primary key, auto increment
     * RegionID (varchar(30), No Null, Unique), // Region id of farmer
     * State (smallint(1), default 1), // Gather state 1 farmer work, 0 they don't
     * items (text, default null), // items formatted info
     * level (int, default 0) // level of farmer
     *
     * FarmerUsers:
     * farmerId (int not null), // Farmers#id for primary key of it
     * name (varchar(30) DEFAULT User), // Display User as username if something goes wrong
     * uuid (char(36) default 0), // uuid of player
     * role (smallint(1) default 0) // role of player [COOP: 0, MEMBER: 1, OWNER: 2]
     */
    public static void createTable() {
        // Creating connection and statement
        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement()) {
            // Adding batch of table creation to the statement
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Farmers`\n" + "(\n"
                    + " `id`          INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                    + " `regionID`          varchar(30) NOT NULL UNIQUE,\n"
                    + " `state`          smallint(1) DEFAULT 1,\n"
                    + " `items`          text DEFAULT NULL,\n"
                    + " `level`          int DEFAULT 0);");
            // Adding batch of table creation to the statement
            statement.addBatch("CREATE TABLE IF NOT EXISTS `FarmerUsers`\n" + "(\n"
                    + " `farmerId`          int NOT NULL,\n"
                    + " `name`          varchar(30) DEFAULT User,\n"
                    + " `uuid`          char(36) DEFAULT 0,\n"
                    + " `role`          smallint(1) DEFAULT 0);");
            // executing batch
            statement.executeBatch();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Updates all farmers in server RAM.
     */
    public static void updateAllFarmers() {
        // Query of update
        final String FARMER_QUERY = "UPDATE Farmers SET state = ?, level = ?, items = ? WHERE id = ?";
        // Connection
        try (Connection con = DBConnection.connect()) {
            // foreach for all farmers
            for (Farmer farmer : Main.getFarmers().values()) {
                // and statement
                PreparedStatement farmerStmt = con.prepareStatement(FARMER_QUERY);
                farmerStmt.setInt(1, farmer.getState());
                farmerStmt.setInt(2, FarmerLevel.getAllLevels().indexOf(farmer.getLevel()));
                String serializedItems = FarmerItem.serializeItems(farmer.getInv().getItems());
                farmerStmt.setString(3, (serializedItems == "") ? null : serializedItems);
                farmerStmt.setInt(4, farmer.getId());
                farmerStmt.executeUpdate();
                // closing statement
                farmerStmt.close();
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Loads all the farmers and users to the RAM.
     */
    public static void loadAllFarmers() {
        // Query of farmer
        final String FARMER_QUERY = "SELECT * FROM Farmers;";
        // Query of farmer users
        final String USERS_QUERY = "SELECT * FROM FarmerUsers WHERE farmerId = ?";
        // Connection
        try (Connection con = DBConnection.connect()) {
            // statement for farmer query
            PreparedStatement pst = con.prepareStatement(FARMER_QUERY);
            ResultSet resultSet = pst.executeQuery();
            // Gets all data
            while (resultSet.next()) {
                int farmerID = resultSet.getInt("id");
                String regionID = resultSet.getString("regionID");
                int state = resultSet.getInt("state");
                int levelID = resultSet.getInt("level");
                // Fixes if there is issue
                if (levelID < 0)
                    levelID = 0;
                FarmerLevel level = (FarmerLevel.getAllLevels().size()-1 >= levelID )
                        ? FarmerLevel.getAllLevels().get(levelID)
                        : FarmerLevel.getAllLevels().get(FarmerLevel.getAllLevels().size()-1);
                // Items set
                Set<FarmerItem> items = FarmerItem.deserializeItems(resultSet.getString("items"));
                // Inventory model
                FarmerInv inv = new FarmerInv(items, level.getCapacity());

                // Creating users statement here
                PreparedStatement userState = con.prepareStatement(USERS_QUERY);
                userState.setInt(1, farmerID);
                ResultSet userSet = userState.executeQuery();
                Set<User> users = new LinkedHashSet<>();
                // Gets all users of the farmer
                while (userSet.next()) {
                    String name = userSet.getString("name");
                    String uuid = userSet.getString("uuid");
                    // FarmerPerm model for role calculating
                    FarmerPerm role = FarmerPerm.getRole(userSet.getInt("role"));
                    users.add(new User(farmerID, name, UUID.fromString(uuid), role));
                }
                // After all this task creating farmer model
                Farmer farmer = new Farmer(farmerID, regionID, users, inv, level, state);
                // Adding it to cache
                Main.getFarmers().put(regionID, farmer);
                // Closing user resultset and statement
                userSet.close();
                userState.close();
            }
            // Closing farmer resultset and statement
            resultSet.close();
            pst.close();
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Creates new farmer to database
     *
     * @param farmer
     * @param ownerUUID
     */
    public static void createFarmer(Farmer farmer, UUID ownerUUID) {
        // Query
        final String SQL_QUERY = "INSERT INTO Farmers (regionID, state, level) VALUES (?, ?, ?)";
        // Asynchronously task scheduler for running this task for async without delay
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            // Makes connect async and all the task because of the scheduler
            try (Connection con = DBConnection.connect()) {
                // Statement runs query
                PreparedStatement pst = con.prepareStatement(SQL_QUERY);
                pst.setString(1, farmer.getRegionID());
                pst.setInt(2, farmer.getState());
                pst.setInt(3, FarmerLevel.getAllLevels().indexOf(farmer.getLevel()));
                pst.executeUpdate();
                pst.close();

                // Gets id from SQL with same async task for updating farmer id
                // Because it created just now.
                PreparedStatement idGetter = con.prepareStatement("SELECT id FROM Farmers WHERE regionID = ?");
                idGetter.setString(1, farmer.getRegionID());
                int id = idGetter.executeQuery().getInt("id");
                // Updated id
                Main.getFarmers().get(farmer.getRegionID()).setId(id);
                idGetter.close();

                Main.getFarmers().get(farmer.getRegionID()).addUser(ownerUUID, Bukkit.getOfflinePlayer(ownerUUID).getName(), 2);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    /**
     * Removes farmer on database
     *
     * @param farmer
     */
    public static void removeFarmer(Farmer farmer) {
        // An async scheduler for running this task async. (no delay)
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            // two query for farmer and farmer users.
            String DELETE_FARMER = "DELETE FROM Farmers WHERE id = ?";
            String DELETE_USERS = "DELETE FROM FarmerUsers WHERE farmerId = ?";
            try (Connection con = DBConnection.connect()) {
                // Removes farmer
                PreparedStatement pst = con.prepareStatement(DELETE_FARMER);
                pst.setInt(1, farmer.getId());
                pst.executeUpdate();
                pst.close();

                // Removes users
                PreparedStatement removeUsers = con.prepareStatement(DELETE_USERS);
                removeUsers.setInt(1, farmer.getId());
                removeUsers.executeUpdate();
                removeUsers.close();

                // Removes from cached farmers
                if (Main.getFarmers().containsKey(farmer.getRegionID()))
                    Main.getFarmers().remove(farmer.getRegionID());
            }
            catch (Exception e) { e.printStackTrace(); }
        });
    }
}