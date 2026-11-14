import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class SingleCoreSimulator {
    private static final int MIN_TASKS = 1;
    private static final int MAX_TASKS = 25;
    private static final int MIN_BURST = 1;
    private static final int MAX_BURST = 50;
    private static final int ROUND_ROBIN_QUANTUM = 5;

    private static boolean arrivalsComplete = false;
    private static SharedThread currentTask = null;

    private SingleCoreSimulator() {
    }

    public static void main(String[] args) throws InterruptedException {
        run(args);
    }

    public static void run(String[] args) throws InterruptedException {
        if (args.length != 1
                || !(args[0].equalsIgnoreCase("FCFS")
                || args[0].equalsIgnoreCase("RR")
                || args[0].equalsIgnoreCase("NSJF")
                || args[0].equalsIgnoreCase("PSJF"))) {
            System.err.println("Usage: java SingleCoreSimulator <FCFS|RR|NSJF>");
            return;
        }

        String algorithm = args[0].toUpperCase();
        Random random = new Random();
        int taskCount = random.nextInt(MAX_TASKS - MIN_TASKS + 1) + MIN_TASKS;
        Queue<SharedThread> readyQueue = new LinkedList<>();
        List<SharedThread> tasks = new ArrayList<>();
        Object readyQueueLock = new Object();

        System.out.println("Scheduling algorithm: " + algorithm
                + (algorithm.equals("RR") ? " (quantum: " + ROUND_ROBIN_QUANTUM + ")" : ""));
        System.out.println("Creating " + taskCount + " task threads.");

        AtomicReference<Throwable> dispatcherFailure = new AtomicReference<>();
        Thread dispatcher = new Thread(() -> {
            try {
                dispatch(algorithm, readyQueue, readyQueueLock);
            } catch (InterruptedException | RuntimeException e) {
                dispatcherFailure.set(e);
            }
        }, "Dispatcher");

        if (!algorithm.equals("PSJF")) {
            for (int id = 1; id <= taskCount; id++) {
                int burstTime = random.nextInt(MAX_BURST - MIN_BURST + 1) + MIN_BURST;
                SharedThread task = new SharedThread(id, burstTime);
                tasks.add(task);
                synchronized (readyQueueLock) {
                    readyQueue.add(task);
                }
                task.start();
                System.out.println("Created task " + id + " with CPU burst " + burstTime + " cycles.");
            }
        }
            else {
                for (int id = 1; id <= taskCount; id++) {
                    int burstTime = random.nextInt(MAX_BURST - MIN_BURST + 1) + MIN_BURST;

                    SharedThread task = new SharedThread(id, burstTime);
                    tasks.add(task);

                    synchronized (readyQueueLock) {
                        readyQueue.add(task);
                        if (currentTask != null &&
                                task.getTimeLeft() < currentTask.getTimeLeft()) {

                            System.out.println("Task " + task.getThreadID()
                                    + " preempting Task " + currentTask.getThreadID()
                                    + " (remaining: " + currentTask.getTimeLeft() + ")");

                            currentTask.preemption();
                        }
                    }

                    task.start();

                    System.out.println("Task " + id + " arrived with burst " + burstTime);

                    if (id == 1) {
                        dispatcher.start();
                    }

                    Thread.sleep(random.nextInt(41) + 10);
                }
                synchronized (readyQueueLock) {
                    arrivalsComplete = true;
                }
            }

        printReadyQueue(readyQueue, readyQueueLock);

        if (!algorithm.equals("PSJF")) {
            dispatcher.start();
        }
        dispatcher.join();

        Throwable failure = dispatcherFailure.get();
        if (failure instanceof InterruptedException) {
            throw (InterruptedException) failure;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }

        for (SharedThread task : tasks) {
            task.join();
        }
        System.out.println("All tasks have finished.");
    }

    private static void dispatch(String algorithm, Queue<SharedThread> readyQueue,
                                 Object readyQueueLock) throws InterruptedException {
        System.out.println("Dispatcher running " + algorithm + ".");
        while (true) {
            SharedThread task;

            synchronized (readyQueueLock) {
                if (algorithm.equals("NSJF")) {
                    task = NSJF(readyQueue);
                } else if (algorithm.equals("PSJF")) {
                    task = PSJF(readyQueue);
                } else {
                    task = readyQueue.poll();
                }

                if (task == null) {
                    if (algorithm.equals("PSJF") && !arrivalsComplete) {
                        continue;
                    }
                    return;
                }
                if (algorithm.equals("PSJF")) {
                    currentTask = task;
                    task.assignCPU(task.getTimeLeft());
                }
            }

            int cycles = algorithm.equals("RR")
                    ? Math.min(ROUND_ROBIN_QUANTUM, task.getTimeLeft())
                    : task.getTimeLeft();

            System.out.println("Dispatcher selected task " + task.getThreadID() + " for " + cycles + " cycle(s).");
            if (algorithm.equals("PSJF")) {
                Main.CPU(task);
            } else {
                Main.CPU(task, cycles);
            }
            if (algorithm.equals("PSJF")) {
                synchronized (readyQueueLock) {
                    currentTask = null;
                    if (task.getTimeLeft() > 0) {
                        readyQueue.add(task);
                    }
                }
            }

            if (algorithm.equals("RR") && task.getTimeLeft() > 0) {
                synchronized (readyQueueLock) {
                    readyQueue.add(task);
                }
            }
            printReadyQueue(readyQueue, readyQueueLock);
        }
    }

    public static SharedThread NSJF(Queue<SharedThread> readyQueue) {
        int shortestBurstTime;
        int leastBurstTime = 999;
        SharedThread leastTimeTask = null;


        for (SharedThread task : readyQueue) {
            shortestBurstTime = task.getBurstTime();
            if (shortestBurstTime < leastBurstTime) {
                leastTimeTask = task;
                leastBurstTime = shortestBurstTime;
            }
        }

        if (leastTimeTask != null) {
            readyQueue.remove(leastTimeTask);
        }

        return leastTimeTask;
    }

    public static SharedThread PSJF(Queue<SharedThread> readyQueue) {
        int leastBurstTime = 999;
        SharedThread leastTimeTask = null;

        for (SharedThread task : readyQueue) {
            if (task.getTimeLeft() < leastBurstTime) {
                leastTimeTask = task;
                leastBurstTime = task.getTimeLeft();
            }
        }

        if (leastTimeTask != null) {
            readyQueue.remove(leastTimeTask);
        }

        return leastTimeTask;
    }

    private static void printReadyQueue(Queue<SharedThread> readyQueue,
                                        Object readyQueueLock) {
        synchronized (readyQueueLock) {
            StringBuilder contents = new StringBuilder("[");
            for (SharedThread task : readyQueue) {
                if (contents.length() > 1) {
                    contents.append(", ");
                }
                contents.append(task.getThreadID());
            }
            contents.append(']');
            System.out.println("Ready queue: " + contents);
        }
    }
}