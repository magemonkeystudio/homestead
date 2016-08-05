package com.gotofinal.darkrise.plots.deeds;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.gotofinal.darkrise.plots.config.ConfigHandler;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * A {@link org.bukkit.scheduler.BukkitRunnable} that updates plot signs.
 */
public class SignUpdater implements Listener
{

    private static long interval = 10L;
    private static int  perTask  = 5;
    private final DarkRisePlots  plugin;
    private final PlotManager    manager;
    private       BukkitRunnable task;
    private static final TLongObjectMap<Set<Plot>> unupdated = new TLongObjectHashMap<>(20, .50f, 997474747697423574L);

    public SignUpdater(final DarkRisePlots instance, final PlotManager manager)
    {

        this.plugin = instance;
        this.manager = manager;
        interval = (long) this.plugin.getConfigHandler().getInt(ConfigHandler.PLOT_SIGN_UPDATE_INTERVAL);
        perTask = this.plugin.getConfigHandler().getInt(ConfigHandler.PLOT_SIGN_UPDATE_PER_TASK);
    }

    public static TLongObjectMap<Set<Plot>> getUnupdated()
    {
        return unupdated;
    }

    public void update(final Iterator<Plot> plots, final ArrayDeque<Plot> waiting)
    {
        for (int i = 0; (i < perTask) && plots.hasNext(); i++)
        {
            final Plot plot;
            final boolean wasWaiting;
            if (waiting.isEmpty())
            {
                plot = plots.next();
                wasWaiting = false;
            }
            else
            {
                plot = waiting.poll();
                wasWaiting = true;
            }

            final boolean reset = (plot.getExpiry() > - 1) && (System.currentTimeMillis() >= plot.getExpiry());
            if (reset)
            {
                plot.reset(plot.getProtectedRegion());
            }
            final Location location = plot.getSignLocation();
            if (location == null)
            {
                continue;
            }
            final long key = (((long) (location.getBlockX() >> 4)) << 32) | ((location.getBlockZ() >> 4) & 0xffffffffL);
            Set<Plot> unupdatedPlots = unupdated.get(key);
            if (! location.getChunk().isLoaded() && ! wasWaiting)
            {
                waiting.add(plot);
                if (unupdatedPlots == null)
                {
                    unupdated.put(key, unupdatedPlots = new HashSet<>(3));
                }
                unupdatedPlots.add(plot);
                continue;
            }
            if (unupdatedPlots != null)
            {
                unupdatedPlots.remove(plot);
                if (unupdatedPlots.isEmpty())
                {
                    unupdated.remove(key);
                }
            }

            final BlockState state = location.getBlock().getState();
            if (state instanceof Sign)
            {
                final Sign sign = (Sign) state;
                plot.updateSign(sign);
            }
            else
            {
                this.plugin.getLogger().severe("[HouseDeeds] plot id '" + plot.getName() + "' sign location is not a " + "sign!");
            }
        }
        if (! this.active)
        {
            this.plugin.getLogger().severe("[HouseDeeds] Sign task stopped.");
            return;
        }
        if (! plots.hasNext())
        {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.update(this.manager.getPlots().values().iterator(), new ArrayDeque<>(100)), interval);
        }
        else
        {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.update(plots, waiting), interval);
        }
    }

//    /**
//     * @deprecated Use {@link #schedule()} instead.
//     */
//    @Override
//    @Deprecated
//    public void run()
//    {
//
//        for (final Plot plot : this.manager.getPlots().values())
//        {
//            if ((plot.getExpiry() > - 1) && (System.currentTimeMillis() >= plot.getExpiry()))
//            {
//                plot.reset(plot.getProtectedRegion(this.plugin));
//            }
//            final Location location = plot.getSignLocation();
//            if (location == null)
//            {
//                continue;
//            }
//
//            final BlockState state = location.getBlock().getState();
//            if (state instanceof Sign)
//            {
//                final Sign sign = (Sign) state;
//                plot.updateSign(sign);
//            }
//            else
//            {
//
//                this.plugin.getLogger().severe("[HouseDeeds] plot id '" + plot.getName() + "' sign location is not a " +
//                                                       "sign!");
//            }
//
//        }
//    }

    /**
     * Attemps to schedule this task
     *
     * @throws IllegalStateException thrown if the task is already scheduled
     */
    public void schedule() throws IllegalStateException
    {

        if (this.isActive())
        {
            throw new IllegalStateException("Can not schedule task if it is active.");
        }

        this.task = new BukkitRunnable()
        {
            @SuppressWarnings("deprecation")
            @Override
            public void run()
            {
                SignUpdater.this.update(SignUpdater.this.manager.getPlots().values().iterator(), new ArrayDeque<>(100));
            }
        };

        this.task.runTaskLater(this.plugin, interval);
        this.active = true;
    }

    private boolean active = false;

    /**
     * Attempts to cancel this task.
     *
     * @throws IllegalStateException thrown if the task isn't scheduled
     */
    public void cancel() throws IllegalStateException
    {

        if (! this.isActive())
        {
            throw new IllegalStateException("Can not cancel task if it isn't active.");
        }
        this.task.cancel();
        this.active = false;
    }

    public BukkitRunnable getTask()
    {
        return this.task;
    }

    /**
     * Checks whether this task is active.
     *
     * @return true if this task is active.
     */
    public boolean isActive()
    {
        return this.task != null;
    }

    public static long getInterval()
    {
        return interval;
    }

    public static void setInterval(final long interval)
    {
        SignUpdater.interval = interval;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("manager", this.manager).append("task", this.task).toString();
    }
}
