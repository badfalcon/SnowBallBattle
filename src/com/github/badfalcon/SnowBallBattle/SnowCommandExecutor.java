package com.github.badfalcon.SnowBallBattle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import me.confuser.barapi.BarAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class SnowCommandExecutor implements CommandExecutor {

	SnowBallBattle plugin;
	BukkitTask gameStart;
	BukkitTask gameTimeCount;
	BukkitTask spawnItem;
	BukkitTask gameEnd;
	boolean ingame = false;
	int count = 10;

	public SnowCommandExecutor(SnowBallBattle plugin) {
		this.plugin = plugin;
	}

	World world = Bukkit.getServer().getWorlds().get(0);

	public boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException nfex) {
			return false;
		}
	}

	class GameCountdown extends BukkitRunnable {
		private int maxtime;
		private int gametime;

		public GameCountdown() {
			maxtime = plugin.getConfig().getInt("Game.GameTime") * 60;
			gametime = plugin.getConfig().getInt("Game.GameTime") * 60;
		}

		public void run() {
			int gamemin = gametime / 60;
			int gamesec = gametime % 60;

			String gamesecString;
			if (gamesec < 10) {
				gamesecString = "0" + String.valueOf(gamesec);
			} else {
				gamesecString = String.valueOf(gamesec);
			}

			for (Player player : Bukkit.getOnlinePlayers()) {

				BarAPI.setMessage(player, "残り時間  " + gamemin + ":"
						+ gamesecString);
				BarAPI.setHealth(player, (float) gametime / (float) maxtime
						* 100F);
				int maxFillTime = plugin.getConfig().getInt(
						"Game.GiveSnowBallTime");
				float current = player.getExp();
				if (current - (float) (1.0f / maxFillTime) <= 0.0f) {

					if (gametime != maxtime && gametime != 0) {
						int SnowNum = plugin.getConfig().getInt(
								"Game.GiveSnowBallNum");
						if (!Spectator.isSpectating(player)) {
							PlayerInventory inventory = player.getInventory();
							inventory.addItem(new ItemStack(Material.SNOW_BALL,
									SnowNum));

						}
					}
					player.setExp(1.0f);
				} else {
					player.setExp(current - (float) (1.0f / maxFillTime));
				}
			}
			if (gametime > 0) {
				gametime--;
			} else {
				for (Player player : Bukkit.getOnlinePlayers()) {
					BarAPI.removeBar(player);
				}
				cancel();
			}
		}
	}

	class SnowCountdown extends BukkitRunnable {

		private int countdown = count;

		public void run() {

			if (countdown < 10) {
				Player[] players = Bukkit.getOnlinePlayers();
				if (countdown <= 3) {
					for (Player player : players) {
						player.playSound(player.getLocation(), Sound.CLICK, 1,
								1);
					}
				}
				for (Player player : players) {
					player.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム開始まで " + countdown);
				}
			} else {
				Bukkit.getServer().broadcastMessage(
						SnowBallBattle.messagePrefix + "ゲーム開始まで" + countdown);
			}

			if (countdown > 1) {
				countdown--;
			} else {
				cancel();
			}
		}
	}

	boolean sendToLobby(CommandSender sender) {
		if (sender instanceof Player) {
			new SnowLobby(plugin).warpLobby((Player) sender);
			return true;
		} else {
			sender.sendMessage(SnowBallBattle.messagePrefix + ChatColor.RED
					+ "コマンドを実行したプレイヤー(" + sender.getName() + ")を特定できませんでした。");
			return false;
		}

	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		Spectator spec = new Spectator(plugin);
		/*
		 * sender.sendMessage("command = " + cmd.getName() + "\nlength = " +
		 * args.length); sender.sendMessage("label = " + label); for (int i = 0;
		 * i < args.length; i++) { sender.sendMessage("args[" + i + "] = " +
		 * args[i]); }
		 */
		// lobby

		if (cmd.getName().equalsIgnoreCase("lobby")) {
			if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
				sender.sendMessage(SnowBallBattle.messagePrefix
						+ "ゲーム中はこのコマンドは実行できません");
				return true;
			}
			sendToLobby(sender);
			return true;
		}

		// sbb

		if (cmd.getName().equalsIgnoreCase("sbb")) {

			FileConfiguration config = plugin.getConfig();

			if (args.length == 0) {
				sender.sendMessage(SnowBallBattle.messagePrefix
						+ "パラメータが足りません。");
				return false;
			}
			Player[] players = plugin.getServer().getOnlinePlayers();

			// getmeta

			if (args[0].equals("getmeta")) {
				if (args[1] == null || args.length != 3) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "パラメータエラー");
					return false;
				}
				if (args[1].equals("world")) {
					World world = Bukkit.getServer().getWorlds().get(0);
					if (args[2] == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "チーム名が与えられていません。");
						return false;
					}
					if (!config.getStringList("Team.Names").contains(args[2])
							&& !args[2].equals("lobby")) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "チームが存在しません。もう一度確認して下さい。");
						return true;
					}
					if (!world.hasMetadata(args[2] + "set")) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "データがありません。");
						return true;
					}
					sender.sendMessage(SnowBallBattle.messagePrefix + args[2]
							+ "のリスポーンポイントは");
					sender.sendMessage("x = "
							+ world.getMetadata(args[2] + "Resx").get(0)
									.asString());
					sender.sendMessage("y = "
							+ world.getMetadata(args[2] + "Resy").get(0)
									.asString());
					sender.sendMessage("z = "
							+ world.getMetadata(args[2] + "Resz").get(0)
									.asString());
					return true;
				} else if (args[1].equals("player")) {
					if (world.hasMetadata("ingame")) {
						if (args[2] == null) {
							sender.sendMessage(SnowBallBattle.messagePrefix
									+ "プレイヤー名が与えられていません。");
							return false;
						}
						if (!Arrays.asList(players).contains(
								Bukkit.getPlayer(args[2]))) {
							sender.sendMessage(SnowBallBattle.messagePrefix
									+ "プレイヤーは存在しません。");
							return true;
						}
						Player obj = Bukkit.getPlayer(args[2]);
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + " team:"
								+ obj.getMetadata("team").get(0).asString());
						return true;
					} else {
						return true;
					}
				}
			}

			// help

			else if (args[0].equals("help")) {
				sender.sendMessage(SnowBallBattle.messagePrefix
						+ "SnowBallBattle ヘルプ\n" + "/lobby - ロビーへのワープ\n"
						+ "/sbb getmeta - メタデータを取得\n" + "/sbb help - ヘルプを表示\n"
						+ "/sbb item list - アイテム一覧を表示\n"
						+ "/sbb item toggle [item] - [item]を有効/無効\n"
						+ "/sbb item dur [item] [time] - [item]の効果時間をを変更\n"
						+ "/sbb item add [item] [time] - [item]のスポーン地点を追加\n"
						+ "/sbb item rem [item] - 最寄りの[item]のスポーン地点を削除\n"
						+ "/sbb ready - ゲーム開始コマンド\n"
						+ "/sbb set lobby - ロビーの登録\n"
						+ "/sbb set spawn [team] - [team]のスポーン地点の登録\n"
						+ "/sbb spect height [height] - 観戦者の最低高度を[height]に設定\n"
						+ "/sbb spect add [player] - 観戦者に[player]を追加\n"
						+ "/sbb spect remove [player] - 観戦者から[player]を削除\n"
						+ "/sbb stop ゲームが進行中の時、ゲームを強制終了\n"
						+ "/sbb teams list - チーム一覧を表示\n"
						+ "/sbb teams add [team] [color] [armor] - チームを作成\n"
						+ "/sbb teams remove [team] - チームを削除\n");
			}

			// item

			else if (args[0].equals("item")) {

				if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム中はこのコマンドは実行できません");
					return true;
				}

				// toggle <ItemName>

				else if (args[1].equals("toggle")) {
					if (args[2] == null || args.length != 3) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					try {
						SnowItem.valueOf(args[2]);
					} catch (Exception e) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "アイテムが存在しません。もう一度確認して下さい。");
						return true;
					}

					boolean bool = !config.getBoolean("Item." + args[2]
							+ ".Active");
					config.set("Item." + args[2] + ".Active", bool);
					sender.sendMessage(SnowBallBattle.messagePrefix + args[2]
							+ " toggled to " + bool);
					plugin.saveConfig();
				}

				// item add <ItemName> <SpawnTime>

				else if (args[1].equals("add")) {
					if (args.length != 4) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					if (args[2] == null || args[3] == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					if (sender instanceof Player) {
						final Player player = (Player) sender;
						if (!player.hasMetadata("Location")) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "範囲がスロットに記録されていません。");
							return true;
						}

						try {
							SnowItem.valueOf(args[2]);
						} catch (Exception e) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "アイテムが存在しません。もう一度確認して下さい。");
							return true;
						}

						String itemName = args[2];
						int spawnTime;
						try {
							int gameTime = config.getInt("Game.GameTime");
							spawnTime = Integer.parseInt(args[3]);
							if (gameTime < spawnTime) {
								sender.sendMessage(spawnTime
										+ "must be less than" + gameTime);
								return true;
							}
						} catch (Exception e) {
							sender.sendMessage(args[3] + "must be an integer");
							return true;
						}
						World world = Bukkit.getServer().getWorlds().get(0);
						double locx = player.getMetadata("Locx").get(0)
								.asDouble();
						double locy = player.getMetadata("Locy").get(0)
								.asDouble();
						double locz = player.getMetadata("Locz").get(0)
								.asDouble();
						double itemlocy = locy + 2;
						world.setMetadata(itemName + "Set",
								new FixedMetadataValue(plugin, true));
						int itemNum = config.getInt("Item." + itemName
								+ ".Numbers") + 1;
						config.set("Item." + itemName + ".Numbers", itemNum);
						config.set("Item." + itemName + ".num" + itemNum
								+ ".SpawnTime", spawnTime);
						config.set("Item." + itemName + ".num" + itemNum
								+ ".Spawn", new Vector(locx, itemlocy, locz));
						plugin.saveConfig();
						player.sendMessage(SnowBallBattle.messagePrefix
								+ itemName + "のスポーン地点を追加しました。");
						player.sendMessage(SnowBallBattle.messagePrefix
								+ "time :" + spawnTime);
						player.sendMessage(SnowBallBattle.messagePrefix
								+ "location : " + "X:" + locx + " Y:"
								+ itemlocy + " Z:" + locz);
						return true;
					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ ChatColor.RED + "コマンドを実行したプレイヤー("
								+ sender.getName() + ")を特定できませんでした。");
						return false;
					}
				}

				// item dur <ItemName> <Duration>

				else if (args[1].equals("dur")) {
					if (args.length != 4) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					if (args[2] == null || args[3] == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					if (sender instanceof Player) {
						final Player player = (Player) sender;
						try {
							SnowItem.valueOf(args[2]);
						} catch (Exception e) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "アイテムが存在しません。もう一度確認して下さい。");
							return true;
						}

						String itemName = args[2];
						int duration;
						try {
							int gameTime = config.getInt("Game.GameTime");
							duration = Integer.parseInt(args[3]);
							if (gameTime < duration) {
								sender.sendMessage(duration
										+ "must be less than" + gameTime);
								return true;
							}
						} catch (Exception e) {
							sender.sendMessage(args[3] + "must be an integer");
							return true;
						}
						config.set("Item." + itemName + ".Duration", duration);
						plugin.saveConfig();
						player.sendMessage(SnowBallBattle.messagePrefix
								+ itemName + "の効果時間を" + duration + "にしました。");

						return true;
					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ ChatColor.RED + "コマンドを実行したプレイヤー("
								+ sender.getName() + ")を特定できませんでした。");
						return false;
					}
				}

				// item type <ItemName>

				else if (args[1].equals("type")) {
					if (args[2] == null || args.length != 3) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					if (sender instanceof Player) {

						Player player = (Player) sender;
						try {
							SnowItem.valueOf(args[2]);
						} catch (Exception e) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "アイテムが存在しません。もう一度確認して下さい。");
							return true;
						}

						Material itemInHand = player.getItemInHand().getType();

						if (itemInHand.equals(Material.AIR)) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "手にアイテムがありません。");
							return true;
						}

						config.set("Item." + args[2] + ".Item", new ItemStack(
								itemInHand));
						plugin.saveConfig();
						player.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + "のアイテムタイプを" + itemInHand.toString()
								+ "にしました");

					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ ChatColor.RED + "コマンドを実行したプレイヤー("
								+ sender.getName() + ")を特定できませんでした。");
						return false;
					}

				}

				// item rem <ItemName>

				else if (args[1].equals("rem")) {
					if (args[2] == null || args.length != 3) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					if (sender instanceof Player) {
						Player player = (Player) sender;
						Location playerLocation = player.getLocation();

						try {
							SnowItem.valueOf(args[2]);
						} catch (Exception e) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "アイテムが存在しません。もう一度確認して下さい。");
							return true;
						}

						List<Location> itemLocations = new LinkedList<Location>();
						List<Integer> itemSpawnTimes = new LinkedList<Integer>();
						String itemName = args[2];

						int itemNum = config.getInt("Item." + itemName
								+ ".Numbers");

						if (itemNum <= 0) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "no locations registered");
							return true;
						}

						double nearestDistance = Double.MAX_VALUE;
						Location nearest = null;

						for (int i = 1; i <= itemNum; i++) {
							int spawnTime = config.getInt("Item." + itemName
									+ ".num" + i + ".SpawnTime");
							itemSpawnTimes.add(spawnTime);
							Vector vector = config.getVector("Item." + itemName
									+ ".num" + i + ".Spawn");
							Location location = new Location(world,
									vector.getX(), vector.getY(), vector.getZ());
							itemLocations.add(location);
							double distance = playerLocation.distance(location);
							if (distance < nearestDistance) {
								nearest = location;
								nearestDistance = distance;
							}
						}

						for (int i = 0; i < itemNum; i++) {
							try {
								int spawnTime = itemSpawnTimes.get(i);
								Location location = itemLocations.get(i);
								Vector vector = new Vector(location.getX(),
										location.getY(), location.getZ());
								if (location.equals(nearest)) {
									itemLocations.remove(i);
									itemSpawnTimes.remove(i);
									i--;
								} else {
									config.set("Item." + itemName + ".num" + i
											+ ".SpawnTime", spawnTime);
									config.set("Item." + itemName + ".num" + i
											+ ".Spawn", vector);
								}

							} catch (Exception e) {
								e.printStackTrace();
								config.set("Item." + itemName + ".num" + i,
										null);
							}
						}

						config.set("Item." + itemName + ".Numbers", itemNum - 1);

						player.sendMessage(SnowBallBattle.messagePrefix + "X:"
								+ nearest.getX() + " Y:" + nearest.getY()
								+ " Z:" + nearest.getZ() + " の" + itemName
								+ "を削除しました");

						plugin.saveConfig();

						return true;

					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ ChatColor.RED + "コマンドを実行したプレイヤー("
								+ sender.getName() + ")を特定できませんでした。");
						return false;
					}
				}

				else if (args[1].equals("list")) {
					if (args.length != 2) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}

					SnowItem[] si = SnowItem.values();
					for (SnowItem snowItem : si) {
						String itemName = snowItem.name();
						Boolean itemActive = config.getBoolean("Item."
								+ itemName + ".Active");
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ itemName + "  active:" + itemActive);
						int itemNum = config.getInt("Item." + itemName
								+ ".Numbers");
						for (int j = 1; j <= itemNum; j++) {
							Vector vector = config.getVector("Item." + itemName
									+ ".num" + j + ".Spawn");
							Bukkit.getLogger().info(
									"Item." + itemName + ".num" + j
											+ ".SpawnTime");
							int spawnTime = config.getInt("Item." + itemName
									+ ".num" + j + ".SpawnTime");
							sender.sendMessage(SnowBallBattle.messagePrefix + j
									+ "  X:" + vector.getX() + " Y:"
									+ vector.getY() + " Z:" + vector.getZ()
									+ " , spawns at " + spawnTime + "min");
						}

					}
				}

			}

			// rearrange

			else if (args[0].equals("rearrange")) {
				sender.sendMessage("under construction");
				/*
				 * if (world.hasMetadata("ingame")) {
				 * sender.sendMessage("ゲーム中はこのコマンドは実行できません"); return true; } new
				 * SnowScoreboard(plugin).removePlayers(); for (Player player :
				 * players) { new PlayerJoinTeam(plugin).joinTeam(player); }
				 */
				return true;
			}

			// set

			else if (args[0].equals("set")) {
				if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム中はこのコマンドは実行できません");
					return true;
				}

				if (sender instanceof Player) {
					if (args[1] == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}
					final Player player = (Player) sender;
					if (!player.hasMetadata("Location")) {
						player.sendMessage(SnowBallBattle.messagePrefix
								+ "範囲がスロットに記録されていません。");
						return true;
					}
					World world = Bukkit.getServer().getWorlds().get(0);
					double locx = player.getMetadata("Locx").get(0).asDouble();
					double locy = player.getMetadata("Locy").get(0).asDouble();
					double locz = player.getMetadata("Locz").get(0).asDouble();
					float locyaw = player.getMetadata("Locyaw").get(0)
							.asFloat();

					List<Float> locyaw1 = new ArrayList<Float>();
					locyaw1.add(locyaw);

					// lobby

					if (args[1].equals("lobby")) {
						if (args.length != 2) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "パラメータエラー");
							return false;
						}
						world.setMetadata("lobbyresx", new FixedMetadataValue(
								plugin, locx));
						world.setMetadata("lobbyresy", new FixedMetadataValue(
								plugin, locy));
						world.setMetadata("lobbyresz", new FixedMetadataValue(
								plugin, locz));
						world.setMetadata("lobbyyaw", new FixedMetadataValue(
								plugin, locyaw));
						world.setMetadata("lobbyset", new FixedMetadataValue(
								plugin, true));
						config.set("lobby", new Vector(locx, locy, locz));
						config.set("lobbyyaw", locyaw1);
						plugin.saveConfig();
						player.sendMessage(SnowBallBattle.messagePrefix
								+ "ロビーのリスポーン地点を\nX:" + locx + "\nY:" + locy
								+ "\nZ:" + locz + "に設定しました。");
						return true;
					}

					// spawn

					else if (args[1].equals("spawn")) {
						if (args.length != 3) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "パラメータエラー");
							return false;
						}
						if (!config.getStringList("Team.Names").contains(
								args[2])) {
							player.sendMessage(SnowBallBattle.messagePrefix
									+ "チームが存在しません。もう一度確認して下さい。");
							return true;
						}
						world.setMetadata(args[2] + "Resx",
								new FixedMetadataValue(plugin, locx));
						world.setMetadata(args[2] + "Resy",
								new FixedMetadataValue(plugin, locy));
						world.setMetadata(args[2] + "Resz",
								new FixedMetadataValue(plugin, locz));
						world.setMetadata(args[2] + "Resyaw",
								new FixedMetadataValue(plugin, locyaw));
						world.setMetadata(args[2] + "Set",
								new FixedMetadataValue(plugin, true));
						config.set(args[2] + ".Respawn", new Vector(locx, locy,
								locz));
						config.set(args[2] + ".RespawnYaw", locyaw1);
						plugin.saveConfig();
						player.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + "のリスポーン地点を\nX:" + locx + "\nY:"
								+ locy + "\nZ:" + locz + "に設定しました。");
						return true;
					}

				} else {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ ChatColor.RED + "コマンドを実行したプレイヤー("
							+ sender.getName() + ")を特定できませんでした。");
					return false;
				}
			}

			// ready

			else if (args[0].equals("ready")) {
				if (config.getString("Mode").equals("premade")) {
					if (TeamsWithoutPlayers()) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "teams need members!");
						return true;
					}
					if (MissingMembers()) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "missing member or wrong name!");
						return true;
					}
				}

				if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム中はこのコマンドは実行できません");
					return true;
				}
				if (config.getStringList("Team.Names").size() < 2) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "チーム数が少なすぎます。");
					return true;
				}
				World world = Bukkit.getServer().getWorlds().get(0);
				if (!world.hasMetadata("lobbyset")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ロビーが設定されていません。");
					return true;
				}
				for (String teamName : config.getStringList("Team.Names")) {
					if (world.hasMetadata(teamName + "Set")) {
						continue;
					} else {
						Team team = SnowBallBattle.board.getTeam(teamName);
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ team.getPrefix() + team.getName()
								+ team.getSuffix() + "のリスポーンポイントが設定されていません。");
						return true;
					}
				}
				for (String itemName : config.getStringList("Item.Names")) {
					if (world.hasMetadata(itemName + "Set")) {
						continue;
					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ itemName + "のスポーンポイントが設定されていません。");
						return true;
					}
				}
				new SnowScoreboard(plugin).removePlayers();
				PlayerJoinTeam pjt = new PlayerJoinTeam(plugin);
				if (config.getString("Mode").equals("premade")) {

					// premade

					// check

					for (String teamName : config.getStringList("Team.Names")) {
						for (String teamMember : config.getStringList(teamName
								+ "." + "Members")) {
							pjt.joinTeam(Bukkit.getPlayer(teamMember), teamName);
						}
					}

					for (Player player : players) {
						if (!player.hasMetadata("TeamName")) {
							spec.setSpectate(player);
						}
					}

				} else {

					// random

					for (Player player : players) {
						pjt.joinRandomTeam(player);

						// ↓途中参加者への例外処理が不完全
						if (!spec.isSpectator(player.getName())) {

							String team = player.getMetadata("TeamName").get(0)
									.asString();
							double spawnx = world.getMetadata(team + "Resx")
									.get(0).asDouble();
							double spawny = world.getMetadata(team + "Resy")
									.get(0).asDouble();
							double spawnz = world.getMetadata(team + "Resz")
									.get(0).asDouble();
							float spawnyaw = world.getMetadata(team + "Resyaw")
									.get(0).asFloat();
							Location respawn = new Location(world, spawnx,
									spawny + 1, spawnz, spawnyaw, 0);
							player.setBedSpawnLocation(respawn, true);
							player.teleport(respawn);
							player.setWalkSpeed(0.001F);
							PotionEffect noJump = new PotionEffect(
									PotionEffectType.JUMP, 200, -100, false);
							player.addPotionEffect(noJump);
							for (Player player1 : players) {
								player.hidePlayer(player1);
							}
						} else {
							spec.setSpectate(player);
						}
					}
				}
				world.setMetadata("ready", new FixedMetadataValue(plugin, true));
				int MaxTime = config.getInt("Game.GameTime") * 60;
				int gamemin = MaxTime / 60;
				int gamesec = MaxTime % 60;

				String gamesecString;
				if (gamesec < 10) {
					gamesecString = "0" + String.valueOf(gamesec);
				} else {
					gamesecString = String.valueOf(gamesec);
				}

				// SnowBallBattle.board.getObjective("Tscore").setDisplayName(
				// "Time  " + gamemin + ":" + gamesecString);
				SnowBallBattle.board.getObjective("Tscore").setDisplayName(
						"チームスコア");
				new SnowScoreboard(plugin).showScore();
				plugin.getLogger().info("ゲーム開始コマンドが実行されました。");
				plugin.getServer().broadcastMessage(
						SnowBallBattle.messagePrefix + "もうすぐゲームが始まります。");
				for (Player player : players) {
					BarAPI.setMessage(player, "残り時間  " + gamemin + ":"
							+ gamesecString);
				}
				new SnowCountdown().runTaskTimer(plugin, 0, 20);
				gameStart = new SnowRunnableStart(this.plugin).runTaskLater(
						this.plugin, 20 * count);
				spawnItem = new SpawnItems(plugin).runTaskLater(plugin,
						20 * count);
				gameTimeCount = new GameCountdown().runTaskTimer(plugin,
						20 * count, 20);
				gameEnd = new SnowRunnableFinish(this.plugin).runTaskLater(
						this.plugin,
						20 * (count + 60 * config.getInt("Game.GameTime")));
				return true;
			}

			// rule
			else if (args[0].equals("spect")) {
				if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム中はこのコマンドは実行できません");
					return true;
				}
				if (args[1].equals(null)) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "パラメータエラー");
					return false;
				} else if (args[1].equals("add")) {
					if (args[2].equals(null)) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "プレイヤー名が入力されていません。");
						return false;
					}
					if (!Arrays.asList(players).contains(
							Bukkit.getPlayer(args[2]))) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "プレイヤーは存在しません。");
						return true;
					}
					if (spec.addSpectator(args[2])) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + "をspectatorに追加しました。");
					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + "はすでにspectatorです。");
					}
					return true;
				} else if (args[1].equals("remove")) {
					if (args[2].equals(null)) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "プレイヤー名が入力されていません。");
						return false;
					}
					if (!Arrays.asList(players).contains(
							Bukkit.getPlayer(args[2]))) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "プレイヤーは存在しません。");
						return true;
					}
					if (spec.removeSpectator(args[2])) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + "をspectatorから削除しました。");
					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ args[2] + "をspectatorから削除できませんでした。");
					}
					return true;
				} else if (args[1].equals("height")) {
					if (args[2].equals(null)) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					} else {
						if (!isInteger(args[1])) {
							sender.sendMessage(SnowBallBattle.messagePrefix
									+ args[1] + "は整数にしてください。");
							return true;
						} else {
							config.set("Spectator.Height",
									Integer.parseInt(args[1]));
							plugin.saveConfig();
							return true;
						}
					}
				}
			}

			// stop

			else if (args[0].equals("stop")) {
				if (world.hasMetadata("ingame") && gameEnd != null) {
					Bukkit.getServer().getScheduler()
							.cancelTask(gameEnd.getTaskId());
					Bukkit.getServer().getScheduler()
							.cancelTask(gameTimeCount.getTaskId());
					spawnItem.cancel();
					Bukkit.getServer().getScheduler()
							.cancelTask(spawnItem.getTaskId());

					for (Player player : Bukkit.getOnlinePlayers()) {
						player.setExp(0);
					}
					gameEnd = new SnowRunnableFinish(this.plugin)
							.runTask(plugin);
				}
			}

			// maxplayers

			else if (args[0].equals("maxplayers")) {
				if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム中はこのコマンドは実行できません");
					return true;
				}
				if (args[1].equals(null)) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "パラメータエラー");
					return false;
				} else {
					if (!isInteger(args[1])) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ args[1] + "は整数にしてください。");
						return true;
					} else {
						config.set("Team.MaxPlayers", args[1]);
						plugin.saveConfig();
						return true;
					}
				}
			} else if (args[0].equals("teams")) {
				if (world.hasMetadata("ingame") || world.hasMetadata("ready")) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "ゲーム中はこのコマンドは実行できません");
					return true;
				}
				if (args[1] == null) {
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "パラメーターエラー");
					return false;
				}
				if (args[1].equals("add")) {

					if (args[2] == null || args[3] == null || args[4] == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメータエラー");
						return false;
					}

					if (config.getStringList("Team.Names").contains(args[2])) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "既に存在しているチーム名です。");
						return true;
					}

					List<String> teamNames = config.getStringList("Team.Names");

					for (String teamName : teamNames) {
						if (config.getString(teamName + ".Color")
								.equalsIgnoreCase(args[3])) {
							sender.sendMessage(SnowBallBattle.messagePrefix
									+ "既に使われている色です。");
							return true;
						} else {
							continue;
						}
					}

					for (String teamName : teamNames) {
						if (config.getString(teamName + ".Armor")
								.equalsIgnoreCase(args[4])) {
							sender.sendMessage(SnowBallBattle.messagePrefix
									+ "既に使われている装備です。");
							return true;
						} else {
							continue;
						}
					}

					ChatColor teamColor = getColor(args[3]);
					if (teamColor == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "利用可能な色ではありません。");
						return true;
					}

					String armor = getArmor(args[4]);
					if (armor == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "利用可能な装備ではありません。");
						return true;
					}

					Team team = SnowBallBattle.board.registerNewTeam(args[2]);
					team.setPrefix(teamColor.toString());
					team.setSuffix(ChatColor.RESET.toString());
					team.setAllowFriendlyFire(false);
					OfflinePlayer teamPlayer = Bukkit.getOfflinePlayer(team
							.getPrefix() + team.getName() + team.getSuffix());

					team.addPlayer(teamPlayer);
					SnowBallBattle.board.getObjective("Tscore")
							.getScore(teamPlayer).setScore(0);

					// configへ保存
					teamNames.add(args[2]);
					config.set("Team.Names", teamNames);
					config.set(args[2] + ".Color", teamColor.toString());
					config.set(args[2] + ".Armor", armor);
					plugin.saveConfig();
					sender.sendMessage(SnowBallBattle.messagePrefix + "チーム:"
							+ team.getPrefix() + args[2] + team.getSuffix()
							+ "を作成しました。");
					sender.sendMessage(SnowBallBattle.messagePrefix
							+ "続けてリスポーン地点を設定してください。");
					return true;
				} else if (args[1].equals("remove")) {
					if (args[2] == null) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "パラメーターエラー。");
						return false;
					}
					if (SnowBallBattle.board.getTeam(args[2]) != null) {
						Team team = SnowBallBattle.board.getTeam(args[2]);
						String teamName = team.getPrefix() + args[2]
								+ team.getSuffix();
						team.unregister();
						List<String> teamNames = config
								.getStringList("Team.Names");
						teamNames.remove(args[2]);
						config.set("Team.Names", teamNames);
						config.set("args[2]", null);
						plugin.saveConfig();
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "チーム:" + teamName + "を削除しました。");
						return true;
					} else {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ "チームが存在しません。");
						return true;
					}
				} else if (args[1].equals("list")) {
					for (Team team : SnowBallBattle.board.getTeams()) {
						sender.sendMessage(SnowBallBattle.messagePrefix
								+ team.getPrefix() + team.getName()
								+ team.getSuffix());
					}
				}
			} else {
				sender.sendMessage(SnowBallBattle.messagePrefix
						+ "定義されていないコマンドです。");
				return false;
			}
			/*
			 * switch (args[0]) { case "setspawn": case "ready": case "start":
			 * case "stop": default: return false; }
			 */
		}
		return true;
	}

	private boolean MissingMembers() {
		// TODO 自動生成されたメソッド・スタブ
		// for チーム
		List<String> teams = plugin.getConfig().getStringList("Team.Names");
		for (String team : teams) {
			List<String> teamMembers = plugin.getConfig().getStringList(
					team + ".Members");
			for (String teamMemberName : teamMembers) {
				Bukkit.getLogger().info("teammembername:" + teamMemberName);
				Player teamMember = Bukkit.getPlayer(teamMemberName);
				if (teamMember == null) {
					return true;
				} else {
					if (!teamMember.isOnline()) {
						return true;
					}
				}
			}
		}
		// for チームメンバー
		// if メンバーオフライン
		// return true
		return false;
	}

	private boolean TeamsWithoutPlayers() {
		// TODO 自動生成されたメソッド・スタブ
		// for チーム
		List<String> teams = plugin.getConfig().getStringList("Team.Names");
		for (String team : teams) {
			List<String> teamMembers = plugin.getConfig().getStringList(
					team + ".Members");
			Bukkit.getLogger().info(team + " membersize:" + teamMembers.size());
			if (teamMembers.size() == 0) {
				return true;
			}
		}
		// if チームメンバー.length = 0
		// return true
		return false;
	}

	public String getArmor(String str) {
		if (str.equalsIgnoreCase("leather")) {
			return "LEATHER";
		} else if (str.equalsIgnoreCase("chainmail")) {
			return "CHAINMAIL";
		} else if (str.equalsIgnoreCase("iron")) {
			return "IRON";
		} else if (str.equalsIgnoreCase("gold")) {
			return "GOLD";
		} else if (str.equalsIgnoreCase("diamond")) {
			return "DIAMOND";
		} else {
			return null;
		}
	}

	public ChatColor getColor(String str) {
		if (str.equalsIgnoreCase("black")) {
			return ChatColor.BLACK;
		} else if (str.equalsIgnoreCase("dark_blue")) {
			return ChatColor.DARK_BLUE;
		} else if (str.equalsIgnoreCase("dark_green")) {
			return ChatColor.DARK_GREEN;
		} else if (str.equalsIgnoreCase("dark_aqua")) {
			return ChatColor.DARK_AQUA;
		} else if (str.equalsIgnoreCase("dark_red")) {
			return ChatColor.DARK_RED;
		} else if (str.equalsIgnoreCase("dark_purple")) {
			return ChatColor.DARK_PURPLE;
		} else if (str.equalsIgnoreCase("gold")) {
			return ChatColor.GOLD;
		} else if (str.equalsIgnoreCase("gray")) {
			return ChatColor.GRAY;
		} else if (str.equalsIgnoreCase("dark_gray")) {
			return ChatColor.DARK_GRAY;
		} else if (str.equalsIgnoreCase("blue")) {
			return ChatColor.BLUE;
		} else if (str.equalsIgnoreCase("green")) {
			return ChatColor.GREEN;
		} else if (str.equalsIgnoreCase("aqua")) {
			return ChatColor.AQUA;
		} else if (str.equalsIgnoreCase("red")) {
			return ChatColor.RED;
		} else if (str.equalsIgnoreCase("light_purple")) {
			return ChatColor.LIGHT_PURPLE;
		} else if (str.equalsIgnoreCase("yellow")) {
			return ChatColor.YELLOW;
		} else if (str.equalsIgnoreCase("white")) {
			return ChatColor.WHITE;
		} else {
			return null;
		}
	}
}
