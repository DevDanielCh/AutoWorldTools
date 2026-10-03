package com.github.rypengu23.autoworldtools.util;

import com.github.rypengu23.autoworldtools.AutoWorldTools;
import com.github.rypengu23.autoworldtools.config.ConfigLoader;
import com.github.rypengu23.autoworldtools.config.ConsoleMessage;
import com.github.rypengu23.autoworldtools.config.MainConfig;
import com.github.rypengu23.autoworldtools.config.MessageConfig;
import com.github.rypengu23.autoworldtools.model.ResetWorldModel;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

public class ResetUtil {

    private final ConfigLoader configLoader;
    private final MainConfig mainConfig;
    private final MessageConfig messageConfig;

    public ResetUtil() {
        this.configLoader = new ConfigLoader();
        this.mainConfig = configLoader.getMainConfig();
        this.messageConfig = configLoader.getMessageConfig();
    }

    /**
     * 現在時刻がリセット実行時刻か判定
     *
     * @param nowCalendar
     * @return
     */
    public boolean checkResetTime(Calendar nowCalendar) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        //リセット時刻リストを取得
        ArrayList<Calendar> resetTimeList = convertUtil.convertCalendar(mainConfig.getResetDayOfTheWeekList(), mainConfig.getResetTimeList());

        //比較
        if (resetTimeList != null) {
            for (Calendar resetTime : resetTimeList) {
                if (checkUtil.checkComparisonTime(nowCalendar, resetTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 現在時刻がリセット前アナウンス時刻か判定。
     * 戻り地が-1の場合、アナウンス時刻ではない。
     *
     * @param nowCalendar
     * @return
     */
    public int checkAnnounceBeforeResetTime(Calendar nowCalendar) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        //リセット時刻リストを取得
        ArrayList<Calendar> resetTimeList = convertUtil.convertCalendar(mainConfig.getResetDayOfTheWeekList(), mainConfig.getResetTimeList());

        //比較
        if (resetTimeList != null) {
            for (Calendar resetTime : resetTimeList) {
                int result = checkUtil.checkComparisonTimeOfList(nowCalendar, resetTime, mainConfig.getResetNotifyTimeList());
                if (result != -1) {
                    return result;
                }
            }
        }
        return -1;
    }

    /**
     * Configに登録された全ワールドをリセット・ゲート生成する。
     * メッセージ等も送信
     */
    public void autoReset() {

        CheckUtil checkUtil = new CheckUtil();
        MultiversePortalsUtil multiversePortalsUtil = new MultiversePortalsUtil();

        //メッセージが空白で無ければ送信
        //リセット開始メッセージ
        if (!checkUtil.checkNullOrBlank(messageConfig.getResetStart())) {
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + messageConfig.getResetStart());
        }

        //メッセージが空白で無ければ送信
        //リセット開始メッセージ(Discord)
        if (mainConfig.isUseDiscordSRV() && !checkUtil.checkNullOrBlank(messageConfig.getResetStartOfDiscord())) {
            DiscordUtil discordUtil = new DiscordUtil();
            discordUtil.sendMessageMainChannel(messageConfig.getResetStartOfDiscord());
        }

        //全てのワールドのリセット
        for (int i = 0; i <= 2; i++) {
            regenerateWorld(i);
        }

        //全てのワールドへゲートを再生成
        if (mainConfig.isUseMultiversePortals()) {
            for (int i = 0; i <= 2; i++) {
                if ((i == 0 && mainConfig.isGateAutoBuildOfNormal()) || (i == 1 && mainConfig.isGateAutoBuildOfNether()) || (i == 2 && mainConfig.isGateAutoBuildOfEnd())) {
                    multiversePortalsUtil.createWarpGateUtil(i);
                }
            }
        }

        //ワールド名指定でのDynmap削除
        if (mainConfig.isUseDynmap()) {
            DynmapUtil dynmapUtil = new DynmapUtil();

            for (String worldName : mainConfig.getMapPurgeAtResetWorldName()) {
                dynmapUtil.deleteMapDataOfWorldName(worldName);
            }
        }

        //Multiverse-Portals再起動
        if(mainConfig.isUseMultiversePortals()){
            multiversePortalsUtil.reloadPlugin();
        }

        //メッセージが空白で無ければ送信
        //リセット完了メッセージ
        if (!checkUtil.checkNullOrBlank(messageConfig.getResetComplete())) {
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + messageConfig.getResetComplete());
        }

        //メッセージが空白で無ければ送信
        //リセット完了メッセージ(Discord)
        if (mainConfig.isUseDiscordSRV() && !checkUtil.checkNullOrBlank(messageConfig.getResetCompleteOfDiscord())) {
            DiscordUtil discordUtil = new DiscordUtil();
            discordUtil.sendMessageMainChannel(messageConfig.getResetCompleteOfDiscord());
        }
    }

    /**
     * 引数のカウントダウン秒数をもとに、メッセージを送信。
     *
     * @param second
     */
    public void sendNotify(int second) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        if (second <= 0) {
            return;
        }

        String countdownStr = convertUtil.createCountdown(second, messageConfig);

        //メッセージが空白で無ければ送信
        //カウントダウンメッセージ
        if (!checkUtil.checkNullOrBlank(messageConfig.getResetCountdown())) {
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + convertUtil.placeholderUtil("{countdown}", countdownStr, messageConfig.getResetCountdown()));
        }
    }

    /**
     * 指定されたタイプの素材世界を再生成
     * 0:ノーマル 1:ネザー 2:ジエンド
     *
     * @param worldType
     */
    public void regenerateWorld(int worldType) {

        ConvertUtil convertUtil = new ConvertUtil();

        //ワールド名リストの取得
        ArrayList<ResetWorldModel> worldList = new ArrayList<>();
        if (worldType == 0) {
            worldList = mainConfig.getResetWorldNameOfNormal();
        } else if (worldType == 1) {
            worldList = mainConfig.getResetWorldNameOfNether();
        } else {
            worldList = mainConfig.getResetWorldNameOfEnd();
        }

        //ワールド再生成
        for (ResetWorldModel worldInfo : worldList) {
            String worldName = worldInfo.getWorldName();
            Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetStart + worldName);
            World resetWorld = Bukkit.getWorld(worldName);
            if (resetWorld == null) {
                Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetFailure + worldName);
                continue;
            }

            //削除対象フォルダのパスを先に控えておく(アンロード後は World から取得できない)
            //Paper 26.1以降、World#getWorldFolder は world/dimensions/<namespace>/<name> を返す
            File worldFolder = resetWorld.getWorldFolder();

            //プレイヤー退避
            movePlayer(resetWorld);

            //ワールド削除(アンロード)
            //アンロードに失敗した場合はそのまま再作成すると旧データと混ざるため、スキップする
            if (!Bukkit.unloadWorld(resetWorld, false) || Bukkit.getWorld(worldName) != null) {
                Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_unloadFailure + worldName);
                continue;
            }

            //ワールドフォルダ削除
            Bukkit.getLogger().info("[AutoWorldTools] " + convertUtil.placeholderUtil("{worldname}", worldName, "{folder}", worldFolder.getPath(), ConsoleMessage.ResetUtil_deleteStart));
            logRegionCount(worldName, worldFolder, ConsoleMessage.ResetUtil_regionCountBeforeDelete);
            deleteDirectory(worldFolder);
            int remainingFileCount = countRemainingFiles(worldFolder);
            if (remainingFileCount > 0) {
                Bukkit.getLogger().warning("[AutoWorldTools] " + convertUtil.placeholderUtil("{folder}", worldFolder.getPath(), "{remaining}", String.valueOf(remainingFileCount), ConsoleMessage.ResetUtil_deleteFailure));
            }

            //ワールド生成
            //Paper 26.1以降、WorldCreator#folder は存在しない。ワールドは名前(维度キー)から
            //world/dimensions/<namespace>/<name> に作られるため、削除した場所と一致する
            WorldCreator worldCreator = new WorldCreator(worldName);
            if (worldInfo.useSeed()) {
                worldCreator.seed(worldInfo.getSeed());
            } else {
                Random r = new Random();
                worldCreator.seed(r.nextLong());
            }
            if (worldType == 0) {
                worldCreator.environment(World.Environment.NORMAL);
            } else if (worldType == 1) {
                worldCreator.environment(World.Environment.NETHER);
            } else {
                worldCreator.environment(World.Environment.THE_END);
            }
            World createdWorld = worldCreator.createWorld();
            if (createdWorld == null) {
                Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_createFailure + worldName);
                continue;
            }
            //再生成されたワールドの検証用ログ
            //Paper 26.1以降、level.dat は world 直下に1つだけ存在し、ワールドごとにできる region_*.mca
            //が terrain の実体。region が 0 なら pasted 前と同じ地形はあり得ない
            Bukkit.getLogger().info("[AutoWorldTools] " + convertUtil.placeholderUtil(
                    "{worldname}", worldName,
                    "{folder}", worldFolder.getPath(),
                    "{regioncount}", String.valueOf(countRegionFiles(worldFolder)),
                    "{seed}", String.valueOf(createdWorld.getSeed()),
                    ConsoleMessage.ResetUtil_newWorldInfo));

            //region ファイルは spawn 領域の生成・保存が終わってから初めて Folder 内に現れるため、
            // 再生成直後に数えると必ず 0 になる。一定時間後に数え直す(検証用ログ)。
            scheduleRegionCountCheck(worldName, createdWorld.getWorldFolder());

            //ワールドボーダーをセット
            int worldSize = 0;
            if (worldType == 0) {
                worldSize = mainConfig.getWorldOfNormalSize();
            } else if (worldType == 1) {
                worldSize = mainConfig.getWorldOfNetherSize();
            } else {
                worldSize = mainConfig.getWorldOfEndSize();
            }
            WorldBorder worldBorder = createdWorld.getWorldBorder();
            worldBorder.setCenter(0.0, 0.0);
            worldBorder.setSize(worldSize);

            //Dynmap削除
            if (mainConfig.isUseDynmap()) {
                DynmapUtil dynmapUtil = new DynmapUtil();
                dynmapUtil.deleteMapDataOfWorldName(worldName);
            }

            Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetComp + worldName);

        }

    }

    /**
     * フォルダを再帰的に削除する。
     * 削除できなかったファイルは数え、ログに出力する。
     *
     * @param file
     * @return 削除に成功した場合 true
     */
    public boolean deleteDirectory(File file) {
        if (!file.exists()) {
            return false;
        }

        final int[] failedCount = {0};

        try {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        failedCount[0]++;
                        Bukkit.getLogger().warning("[AutoWorldTools] " + path + " : " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) {
                    failedCount[0]++;
                    Bukkit.getLogger().warning("[AutoWorldTools] " + path + " : " + e.getMessage());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) {
                    if (e != null) {
                        failedCount[0]++;
                        Bukkit.getLogger().warning("[AutoWorldTools] " + path + " : " + e.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        failedCount[0]++;
                        Bukkit.getLogger().warning("[AutoWorldTools] " + path + " : " + ex.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AutoWorldTools] " + file.getPath() + " : " + e.getMessage());
            return false;
        }

        return failedCount[0] == 0 && !file.exists();
    }

    /**
     * フォルダ内に残っているファイル数を返す(削除失敗の確認用)。
     *
     * @param file
     * @return ファイル数。存在しない場合は0
     */
    public int countRemainingFiles(File file) {
        if (!file.exists()) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(file.toPath())) {
            return (int) stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AutoWorldTools] " + file.getPath() + " : " + e.getMessage());
            return -1;
        }
    }

    /**
     * ワールドフォルダ内の Chunk ファイル数( *.mca )を数える。
     * Paper 26.1 以降、terrain/construções の実体はこのファイル群なので、
     * 数が 0 でなければ古いワールドデータが再利用された疑いがある。
     *
     * @param file
     * @return ファイル数。存在しない場合は 0
     */
    public int countRegionFiles(File file) {
        if (!file.exists()) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(file.toPath())) {
            return (int) stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".mca"))
                    .count();
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AutoWorldTools] " + file.getPath() + " : " + e.getMessage());
            return -1;
        }
    }

/**
 * region ファイル数を一定時間後に数え直してログする(検証用)。
 * region ファイルは spawn 領域の生成と保存が完了するまで Folder 内に現れないため、
 * 再生成直後の数(always 0)では判断できない。
 *
 * @param worldName
 * @param worldFolder
 */
    private void scheduleRegionCountCheck(String worldName, File worldFolder) {
        Bukkit.getScheduler().runTaskLater(AutoWorldTools.getInstance(), new Runnable() {
            @Override
            public void run() {
                logRegionCount(worldName, worldFolder, ConsoleMessage.ResetUtil_regionCountAfterCreate);
            }
        }, 200L);
    }

    /**
 * 検証用ログ(region ファイル数の記録)。
     *
     * @param worldName
     * @param worldFolder
     * @param message
     */
    private void logRegionCount(String worldName, File worldFolder, String message) {
        Bukkit.getLogger().info("[AutoWorldTools] " + new ConvertUtil().placeholderUtil(
                "{worldname}", worldName,
                "{folder}", worldFolder.getPath(),
                "{regioncount}", String.valueOf(countRegionFiles(worldFolder)),
                message));
    }

    public void movePlayer(World world) {
        List<Player> playerList = world.getPlayers();

        for (Player player : playerList) {
            Location respawnLocation = Bukkit.getWorld("world").getSpawnLocation();
            player.teleport(respawnLocation);
        }
    }
}
