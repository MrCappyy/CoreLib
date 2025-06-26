package net.mrcappy.corelib.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A sane wrapper around Bukkit's scheduler.
 * 
 * Why does this exist? Because BukkitScheduler's API
 * was designed by someone who hates developers.
 * 
 * Features:
 * - Static access (CoreScheduler.runLater())
 * - Automatic task cleanup
 * - Cancel tokens that actually work
 * - No more fucking task IDs to keep track of
 */
public class CoreScheduler {
    
    private static CoreScheduler instance;
    private final Plugin plugin;
    private final Map<Integer, ScheduledTask> activeTasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    
    public CoreScheduler(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }    
    /**
     * Clean up all active tasks.
     * Called on plugin disable.
     */
    public void shutdown() {
        activeTasks.values().forEach(ScheduledTask::cancel);
        activeTasks.clear();
    }
    
    // Static convenience methods
    // Because typing CoreLibPlugin.getInstance().getCoreScheduler() is cancer
    
    /**
     * Run a task on the next tick.
     * Basically runTask but with a better name.
     */
    public static ScheduledTask run(Runnable task) {
        return instance.runTask(task);
    }
    
    /**
     * Run a task after a delay.
     * @param task The shit to run
     * @param delay Delay in ticks (20 ticks = 1 second for you noobs)
     */
    public static ScheduledTask runLater(Runnable task, long delay) {
        return instance.runTaskLater(task, delay);
    }
    
    /**
     * Run a task repeatedly.
     * @param task The shit to run
     * @param delay Initial delay in ticks
     * @param period Repeat interval in ticks
     */
    public static ScheduledTask runTimer(Runnable task, long delay, long period) {
        return instance.runTaskTimer(task, delay, period);
    }
    
    /**
     * Run a task asynchronously.
     * For database queries and other blocking shit.
     * DO NOT TOUCH BUKKIT API FROM ASYNC THREADS YOU MUPPET.
     */
    public static ScheduledTask runAsync(Runnable task) {
        return instance.runTaskAsync(task);
    }
    
    /**
     * Run a task asynchronously after a delay.
     */
    public static ScheduledTask runAsyncLater(Runnable task, long delay) {
        return instance.runTaskAsyncLater(task, delay);
    }
    
    /**
     * Run a task asynchronously and repeatedly.
     * For the truly insane.
     */
    public static ScheduledTask runAsyncTimer(Runnable task, long delay, long period) {
        return instance.runTaskAsyncTimer(task, delay, period);
    }    
    // Instance methods that actually do the work
    
    private ScheduledTask runTask(Runnable task) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        });
        
        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }
    
    private ScheduledTask runTaskLater(Runnable task, long delay) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        }, delay);
        
        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }
    
    private ScheduledTask runTaskTimer(Runnable task, long delay, long period) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        
        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }    
    private ScheduledTask runTaskAsync(Runnable task) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        });
        
        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }
    
    private ScheduledTask runTaskAsyncLater(Runnable task, long delay) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                task.run();
            } finally {
                activeTasks.remove(id);
            }
        }, delay);
        
        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }
    
    private ScheduledTask runTaskAsyncTimer(Runnable task, long delay, long period) {
        int id = taskCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(id);
        
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin, task, delay, period
        );
        
        scheduled.setBukkitTask(bukkitTask);
        activeTasks.put(id, scheduled);
        return scheduled;
    }    
    /**
     * Represents a scheduled task that can be cancelled.
     * Way better than keeping track of task IDs like a caveman.
     */
    public class ScheduledTask {
        private final int id;
        private BukkitTask bukkitTask;
        private volatile boolean cancelled = false;
        
        ScheduledTask(int id) {
            this.id = id;
        }
        
        void setBukkitTask(BukkitTask task) {
            this.bukkitTask = task;
        }
        
        /**
         * Cancel this task.
         * Idempotent - can be called multiple times safely.
         */
        public void cancel() {
            if (!cancelled && bukkitTask != null) {
                bukkitTask.cancel();
                cancelled = true;
                activeTasks.remove(id);
            }
        }
        
        /**
         * Check if this task is still running.
         * Returns false if cancelled or completed.
         */
        public boolean isActive() {
            return !cancelled && bukkitTask != null && 
                   Bukkit.getScheduler().isCurrentlyRunning(bukkitTask.getTaskId());
        }
        
    public int getTaskId() {
            return bukkitTask != null ? bukkitTask.getTaskId() : -1;
        }
    }
    
    /**
     * Run a task on the main thread.
     * Use this from async tasks to touch Bukkit API.
     */
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}