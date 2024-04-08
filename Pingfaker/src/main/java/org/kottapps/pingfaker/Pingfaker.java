package org.kottapps.pingfaker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public final class Pingfaker extends JavaPlugin implements Listener {

    private HashMap<UUID, Integer> _playerFakeLatency;
    private HashMap<UUID, BukkitScheduler> _playerFakeLatencyScheduler;

    private Boolean _isRealistic = false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        if (!this.getConfig().getBoolean("enabled"))
            return;

        _isRealistic = this.getConfig().getBoolean("realistic");

        _playerFakeLatencyScheduler = new HashMap<UUID, BukkitScheduler>();
        _playerFakeLatency = new HashMap<UUID, Integer>();

        for (Player player : getServer().getOnlinePlayers()) {
            AddPlayer(player);
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (Player player : getServer().getOnlinePlayers()) {
            RemovePlayer(player);
        }

        _playerFakeLatencyScheduler.clear();
        _playerFakeLatency.clear();
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e)
    {
        Player player = e.getPlayer();

        RemovePlayer(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e)
    {
        Player player = e.getPlayer();

        AddPlayer(player);
    }

    public void AddPlayer(Player player)
    {
        BukkitScheduler pingScheduler = Bukkit.getScheduler();
        _playerFakeLatencyScheduler.put(player.getUniqueId(), pingScheduler);
        _playerFakeLatency.put(player.getUniqueId(), 0);
        //new Random().nextLong(400L, 440L)
        pingScheduler.runTaskTimer(this, () -> {FakePlayerPing(player);}, 100L, 578L);
    }

    public void RemovePlayer(Player player)
    {
        for (BukkitScheduler scheduler : _playerFakeLatencyScheduler.values()) {
            scheduler.cancelTask(0);
        }

        _playerFakeLatencyScheduler.remove(player.getUniqueId());
        _playerFakeLatency.remove(player.getUniqueId());
    }

    private void FakePlayerPing(Player p)
    {
        int latency = latency = new Random().nextInt(30, 77);

        if (_isRealistic)
        {
            if (p.getPing() < 20)
                latency = new Random().nextInt(9, 15);
            else if (p.getPing() < 5)
                latency = new Random().nextInt(0, 3);
            else if (p.getPing() > 10 && p.getPing() < 250)
                latency = new Random().nextInt(21, 46);
            else if (p.getPing() > 250 && p.getPing() < 400)
                latency = new Random().nextInt(47, 102);
            else
                latency = new Random().nextInt(103, 2000);
        }
        else
            latency = new Random().nextInt(10, 40);

        

        System.out.println(p.getName() + " ping: " + latency);

        _playerFakeLatency.put(p.getUniqueId(), latency);

        ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        PacketContainer pingPacket = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);

        Set<EnumWrappers.PlayerInfoAction> pia = new HashSet<>();
        EnumWrappers.PlayerInfoAction infoact = EnumWrappers.PlayerInfoAction.UPDATE_LATENCY;
        pia.add(infoact);

        pingPacket.getPlayerInfoActions().write(0, pia);

        List<PlayerInfoData> data = new ArrayList<>();
        PlayerInfoData xuy = new PlayerInfoData(WrappedGameProfile.fromPlayer(p), latency, EnumWrappers.NativeGameMode.ADVENTURE, null);
        data.add(xuy);

        pingPacket.getPlayerInfoDataLists().write(1, data);

        for (Player serverPlayer : this.getServer().getOnlinePlayers()) {
            pm.sendServerPacket(serverPlayer, pingPacket);
        }
    }
}
