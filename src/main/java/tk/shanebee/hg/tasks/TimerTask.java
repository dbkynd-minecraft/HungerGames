package tk.shanebee.hg.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import tk.shanebee.hg.data.Config;
import tk.shanebee.hg.data.Language;
import tk.shanebee.hg.game.Game;
import tk.shanebee.hg.HG;
import tk.shanebee.hg.Status;
import tk.shanebee.hg.game.GameArenaData;

import java.util.Objects;

public class TimerTask implements Runnable {

	private int timer = 0;
	private int remainingtime;
	private final int teleportTimer;
	private final int borderCountdownStart;
	private final int borderCountdownEnd;
	private final int id;
	private final Game game;
	private final Language lang;
    private final String end_min;
    private final String end_minsec;
    private final String end_sec;

	public TimerTask(Game g, int time) {
		this.remainingtime = time;
		this.game = g;
		HG plugin = game.getGameArenaData().getPlugin();
		this.lang = plugin.getLang();
		this.teleportTimer = Config.teleportEndTime;
		this.borderCountdownStart = g.getGameBorderData().getBorderTimer().get(0);
		this.borderCountdownEnd = g.getGameBorderData().getBorderTimer().get(1);
		g.getGamePlayerData().getPlayers().forEach(uuid -> Objects.requireNonNull(Bukkit.getPlayer(uuid)).setInvulnerable(false));

		this.end_min = lang.game_ending_min;
		this.end_minsec = lang.game_ending_minsec;
		this.end_sec = lang.game_ending_sec;

		this.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 30 * 20L);
	}
	
	@Override
	public void run() {
		GameArenaData gameArenaData = game.getGameArenaData();
		if (game == null || gameArenaData.getStatus() != Status.RUNNING) stop(); //A quick null check!


		if (Config.bossbar) game.getGameBarData().bossbarUpdate(remainingtime);

		if (Config.borderEnabled && remainingtime == borderCountdownStart) {
			int closingIn = remainingtime - borderCountdownEnd;
			Location center = game.getGameBorderData().setBorder(closingIn);

			if (Config.overtime) {
				// Message that the border will be collapsing
				Bukkit.getScheduler().scheduleSyncDelayedTask(HG.getPlugin(), new Runnable() {
					@Override
					public void run() {
						if (Config.randomChest || Config.randomChestStopOnWbCountdownEnd) {
							game.getChestDropTask().halt();
						}
						if (game.getGameArenaData().getStatus() == Status.RUNNING) {
							String game_border_closed = HG.getPlugin().getLang().game_border_closed;
							String game_border_closed_replacement;
							if (game_border_closed.contains("<minutes")) {
								double minutes = (double) borderCountdownEnd / 60.0;
								game_border_closed_replacement = game_border_closed
										.replace("<minutes>", String.valueOf(minutes));
							} else {
								game_border_closed_replacement = game_border_closed
										.replace("<seconds>", String.valueOf(borderCountdownEnd));
							}
							game.getGamePlayerData().msgAll(game_border_closed_replacement);
						}
					}
				}, closingIn * 20L);
			}

			String game_border_closing = HG.getPlugin().getLang().game_border_closing;
			String game_border_closing_replacement;
			if (game_border_closing.contains("<minutes")) {
				double minutes = (double) closingIn / 60.0;
				game_border_closing_replacement = game_border_closing
						.replace("<minutes>", String.valueOf(minutes));
			} else {
				game_border_closing_replacement = game_border_closing
						.replace("<seconds>", String.valueOf(closingIn));
			}
			game.getGamePlayerData().msgAll(game_border_closing_replacement
					.replace("<position>", "x:" + center.getBlockX() + " z:" + center.getBlockZ()));
		}

		if (gameArenaData.getChestRefillTime() > 0 && remainingtime == gameArenaData.getChestRefillTime()) {
			game.getGameBlockData().refillChests();
			game.getGamePlayerData().msgAll(lang.game_chest_refill);
		}

		int refillRepeat = gameArenaData.getChestRefillRepeat();
		if (refillRepeat > 0 && timer % refillRepeat == 0) {
			game.getGameBlockData().refillChests();
			game.getGamePlayerData().msgAll(lang.game_chest_refill);
		}

		if (remainingtime == teleportTimer && Config.teleportEnd) {
			game.getGamePlayerData().msgAll(lang.game_almost_over);
			game.getGamePlayerData().respawnAll();
		} else if (this.remainingtime < 10) { // End of normal play
			if (Config.borderEnabled && Config.overtime) {
				// Stop the current timer task
				stop();
				game.getGamePlayerData().msgAll(HG.getPlugin().getLang().game_border_overtime.replace("<seconds>", String.valueOf(Config.overtimeSeconds)));
				// Show overtime bossbar even if not using bossbar for normal gameplay
				game.getGameBarData().bossBarOvertime();
				// Close the border to 0 width
				game.getGameBorderData().closeBorder();
				// Stop the game with no deaths if the overtime runs 60 seconds over the overtime-seconds
				// This is a catch in case something unexpected happens and there is no winner
				Bukkit.getScheduler().scheduleSyncDelayedTask(HG.getPlugin(), new Runnable() {
					@Override
					public void run() {
						if (game.getGameArenaData().getStatus() == Status.RUNNING) game.stop(false);
					}
				}, (60 + Config.overtimeSeconds) * 20L);
			} else {
				stop();
				game.stop(false);
			}
		} else {
			if (!Config.bossbar) {
				int minutes = this.remainingtime / 60;
				int asd = this.remainingtime % 60;
				if (minutes != 0) {
					if (asd == 0) {
					    if (end_min.length() < 1) return;
                        game.getGamePlayerData().msgAll(end_min.replace("<minutes>", "" + minutes));
                    } else {
					    if (end_minsec.length() < 1) return;
                        game.getGamePlayerData().msgAll(end_minsec.replace("<minutes>", "" + minutes).replace("<seconds>", "" + asd));
                    }
				} else {
				    if (end_sec.length() < 1) return;
				    game.getGamePlayerData().msgAll(end_sec.replace("<seconds>", "" + this.remainingtime));
                }
			}
		}
		remainingtime = (remainingtime - 30);
		timer += 30;
	}
	
	public void stop() {
		Bukkit.getScheduler().cancelTask(id);
	}

}
