package tk.shanebee.hg.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import tk.shanebee.hg.data.Config;

import java.util.Arrays;
import java.util.List;

/**
 * Data class for holding a {@link Game Game's} world border
 */
public class GameBorderData extends Data {

    private Location borderCenter = null;
    private int borderSize;
    private int borderCountdownStart;
    private int borderCountdownEnd;

    protected GameBorderData(Game game) {
        super(game);
    }

    private double getBorderSize(Location center) {
        Bound bound = game.gameArenaData.getBound();
        double x1 = Math.abs(bound.getGreaterCorner().getX() - center.getX());
        double x2 = Math.abs(bound.getLesserCorner().getX() - center.getX());
        double z1 = Math.abs(bound.getGreaterCorner().getZ() - center.getZ());
        double z2 = Math.abs(bound.getLesserCorner().getZ() - center.getZ());

        double x = Math.max(x1, x2);
        double z = Math.max(z1, z2);
        double r = Math.max(x, z);

        return (r * 2) + 10;
    }

    /**
     * Set the center of the border of this game
     *
     * @param borderCenter Location of the center
     */
    public void setBorderCenter(Location borderCenter) {
        this.borderCenter = borderCenter;
    }

    /**
     * Set the final size for the border of this game
     *
     * @param borderSize The final size of the border
     */
    public void setBorderSize(int borderSize) {
        this.borderSize = borderSize;
    }

    public void setBorderTimer(int start, int end) {
        this.borderCountdownStart = start;
        this.borderCountdownEnd = end;
    }

    public List<Integer> getBorderTimer() {
        return Arrays.asList(borderCountdownStart, borderCountdownEnd);
    }

    public Location setBorder(int time) {
        Location center;
        if (Config.centerSpawn && borderCenter == null) {
            center = game.gameArenaData.spawns.get(0);
        } else if (borderCenter != null) {
            center = borderCenter;
        } else {
            center = game.gameArenaData.bound.getCenter();
        }

        if (Config.borderRandomOffset) {
            // Get the center of the arena
            Location arenaCenter = game.gameArenaData.bound.getCenter();
            // Use the center to determine the shortest side if a rectangle
            // So we make sure the random point is within it
            double shortestDistance = getBorderShortestSide(arenaCenter);
            // Offset by half the final border size so the edge is inside the bounds
            double padding = Math.abs(Config.borderFinalSize / 2);
            double areaRadius = shortestDistance - padding;
            // Use the config radius but only if smaller than the arena
            double maxRadius = Math.min(areaRadius, Config.borderRandomMaxDistance - padding);
            double minRadius = Config.borderRandomMinDistance;
            if (minRadius >= maxRadius) minRadius = 0;
            double t = Math.random() * Math.PI;
            double radius = Math.random() * (maxRadius - minRadius) + minRadius;
            double x = Math.cos(t) * radius;
            double z = Math.sin(t) * radius;
            center = new Location(game.gameArenaData.bound.getWorld(), x, 0, z);
        }

        World world = center.getWorld();
        assert world != null;
        WorldBorder border = world.getWorldBorder();
        double size = Math.min(border.getSize(), getBorderSize(center));

        border.setCenter(center);
        border.setSize(((int) size));
        border.setWarningTime(5);
        border.setDamageBuffer(2);
        border.setSize(Math.min(size, borderSize), time);

        return center;
    }

    void resetBorder() {
        World world = game.gameArenaData.getBound().getWorld();
        assert world != null;
        world.getWorldBorder().reset();
    }

    private double getBorderShortestSide(Location center) {
        Bound bound = game.gameArenaData.bound;
        double x1 = Math.abs(bound.getGreaterCorner().getX() - center.getX());
        double x2 = Math.abs(bound.getLesserCorner().getX() - center.getX());
        double z1 = Math.abs(bound.getGreaterCorner().getZ() - center.getZ());
        double z2 = Math.abs(bound.getLesserCorner().getZ() - center.getZ());

        double x = Math.min(x1, x2);
        double z = Math.min(z1, z2);

        return Math.min(x, z);
    }

    public void closeBorder() {
        Location center = game.gameArenaData.spawns.get(0);
        World world = center.getWorld();
        assert world != null;
        WorldBorder border = world.getWorldBorder();
        border.setSize(0, Config.overtimeSeconds);
    }

}
