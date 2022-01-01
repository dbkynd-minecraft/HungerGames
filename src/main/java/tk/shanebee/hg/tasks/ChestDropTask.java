package tk.shanebee.hg.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import tk.shanebee.hg.HG;
import tk.shanebee.hg.data.Config;
import tk.shanebee.hg.game.Bound;
import tk.shanebee.hg.game.Game;
import tk.shanebee.hg.listeners.ChestDrop;
import tk.shanebee.hg.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChestDropTask implements Runnable {

    private final Game game;
    private final int timerID;
    private final List<ChestDrop> chests = new ArrayList<>();
    private boolean halted = false;

    public ChestDropTask(Game game) {
        this.game = game;
        timerID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(HG.getPlugin(), this, Config.randomChestInterval, Config.randomChestInterval);
    }

    public void run() {
        if (halted) return;

        World world = game.getGameArenaData().getSpawns().get(0).getWorld();
        WorldBorder border = world.getWorldBorder();

        Location center = border.getCenter();
        double size = border.getSize();
        Bound bound = game.getGameArenaData().getBound();

        // Make sure the padding does not exceed the border radius
        double padding = Math.min((size / 2) - 1, Config.randomChestBorderPadding);

        // Get the edges of the border with additional padding
        double minX = center.getX() - (size / 2) + padding;
        double minZ = center.getZ() - (size / 2) + padding;
        double maxX = center.getX() + (size / 2) - padding;
        double maxZ = center.getZ() + (size / 2) - padding;

        int maxHeight = Math.min((int) bound.getGreaterCorner().getY(), Config.randomChestMaxHeight);
        int minHeight = (int) (bound.getLesserCorner().getY());

        Bound constrainedBound = new Bound(bound.getWorld().getName(), (int) (minX), minHeight, (int) (minZ), (int) (maxX), maxHeight, (int) (maxZ));

        Integer[] i = constrainedBound.getRandomLocs();

        int x = i[0];
        int y = minHeight;
        int z = i[2];
        World w = constrainedBound.getWorld();

        boolean locAccepted = false;
        int count = 0;

        outer:
        while (!locAccepted) {
            count++;
            if (count >= 100) break;
            y++;

            if (y > maxHeight) {
                i = constrainedBound.getRandomLocs();

                x = i[0];
                y = minHeight;
                z = i[2];
                continue;
            }

            // Make sure the block to spawn the chest on is solid and not a chest
            Block targetLoc = w.getBlockAt(x, y - 1, z);
            Material targetLocType = targetLoc.getType();
            if (!targetLoc.isSolid()
                    || targetLocType == Material.CHEST
                    || targetLocType == Material.ENDER_CHEST
                    || targetLocType == Material.SHULKER_BOX
            ) {
                continue;
            }

            // Make sure that the target location, and 10 blocks above, are all air for the falling block to fall through
            for (int j = 0; j <= 10; j++) {
                if (w.getBlockAt(x, y + j, z).getType() != Material.AIR) {
                    continue outer;
                }
            }

            locAccepted = true;
        }

        // Shift the falling block position to the center of the block and 10 blocks above the target location
        Location l = new Location(w, x + 0.5, y + 10, z + 0.5);

        FallingBlock fb = w.spawnFallingBlock(l, Bukkit.getServer().createBlockData(Material.STRIPPED_SPRUCE_WOOD));

        chests.add(new ChestDrop(fb));

        for (UUID u : game.getGamePlayerData().getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                Util.scm(p, HG.getPlugin().getLang().chest_drop_1);
                Util.scm(p, HG.getPlugin().getLang().chest_drop_2
                        .replace("<x>", String.valueOf(x))
                        .replace("<y>", String.valueOf(y))
                        .replace("<z>", String.valueOf(z)));
                Util.scm(p, HG.getPlugin().getLang().chest_drop_1);
            }
        }
    }

    public void halt() {
        halted = true;
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTask(timerID);
        for (ChestDrop cd : chests) {
            if (cd != null) cd.remove();
        }
    }
}
