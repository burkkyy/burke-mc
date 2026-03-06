package burkemc.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TickScheduler {
    private record Task(Runnable action, int ticksRemaining) {}

    private static final ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void schedule(Runnable action, int delayTicks) {
        tasks.add(new Task(action, delayTicks));
    }

    private static void tick() {
        int size = tasks.size();
        for (int i = 0; i < size; i++) {
            Task t = tasks.poll();
            if (t == null) break;
            int remaining = t.ticksRemaining() - 1;
            if (remaining <= 0) {
                t.action().run();
            } else {
                tasks.add(new Task(t.action(), remaining));
            }
        }
    }
}