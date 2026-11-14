import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class SingleCoreSimulator {
    private static final int TASK_COUNT = 4;
    private static final int MIN_BURST = 1;
    private static final int MAX_BURST = 10;
    private static final int ROUND_ROBIN_QUANTUM = 5;

    private SingleCoreSimulator() {
    }

    public static void main(String[] args) throws InterruptedException {
        run(args);
    }

    public static void run(String[] args) throws InterruptedException {
        if (args.length != 1
                || !isSupportedAlgorithm(args[0])) {
            System.err.println("Usage: java SingleCoreSimulator <FCFS|RR|SJF|PSJF>");
            return;
        }

        run(args[0], ROUND_ROBIN_QUANTUM);
    }

    public static void run(String algorithm, int quantum) throws InterruptedException {
        if (!isSupportedAlgorithm(algorithm)) {
            System.err.println("Usage: java SingleCoreSimulator <FCFS|RR|SJF|PSJF>");
            return;
        }
        if (algorithm.equalsIgnoreCase("RR") && (quantum < 2 || quantum > 10)) {
            System.err.println("Round-robin quantum must be between 2 and 10.");
            return;
        }

        String selectedAlgorithm = algorithm.toUpperCase();
        Random random = new Random();
        Queue<SharedThread> readyQueue = new LinkedList<>();
        List<SharedThread> tasks = new ArrayList<>();
        Object readyQueueLock = new Object();

        System.out.println("Scheduler Algorithm Select: " + getAlgorithmName(selectedAlgorithm)
                + (selectedAlgorithm.equals("RR") ? ". Time Quantum = " + quantum : ""));
        System.out.println("# threads = " + TASK_COUNT);

        for (int id = 0; id < TASK_COUNT; id++) {
            int burstTime = random.nextInt(MAX_BURST - MIN_BURST + 1) + MIN_BURST;
            SharedThread task = new SharedThread(id, burstTime);
            tasks.add(task);
            synchronized (readyQueueLock) {
                readyQueue.add(task);
            }
            task.start();
            System.out.printf("%-15s | Creating process thread %d%n", "Main thread", id);
        }
        printReadyQueue(readyQueue, readyQueueLock);

        AtomicReference<Throwable> dispatcherFailure = new AtomicReference<>();
        System.out.printf("%-15s | Forking dispatcher 0%n", "Main thread");
        Thread dispatcher = new Thread(() -> {
            try {
                dispatch(selectedAlgorithm, quantum, readyQueue, readyQueueLock);
            } catch (InterruptedException | RuntimeException e) {
                dispatcherFailure.set(e);
            }
        }, "Dispatcher");
        System.out.printf("%-15s | Using CPU 0%n", "Dispatcher 0");
        System.out.printf("%-15s | Now releasing dispatchers.%n%n", "Dispatcher 0");
        dispatcher.start();
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
        System.out.printf("%-15s | Exiting.%n", "Main thread");
    }

    private static void dispatch(String algorithm, int quantum, Queue<SharedThread> readyQueue,
                                 Object readyQueueLock) throws InterruptedException {
        String algorithmName = getAlgorithmName(algorithm);
        System.out.printf("%-15s | Running %s algorithm%s%n%n", "Dispatcher 0",
                algorithm.equals("RR") ? "RR" : algorithmName,
                algorithm.equals("RR") ? ", Time Quantum = " + quantum : "");
        while (true) {
            SharedThread task;
            synchronized (readyQueueLock) {
                task = selectTask(algorithm, readyQueue);
            }
            if (task == null) {
                return;
            }

            int cycles;
            if (algorithm.equals("RR")) {
                cycles = Math.min(quantum, task.getTimeLeft());
            } else if (algorithm.equals("PSJF")) {
                cycles = 1;
            } else {
                cycles = task.getTimeLeft();
            }
            System.out.printf("%-15s | Running process %d%n", "Dispatcher 0",
                    task.getThreadID());
            Main.CPU(task, cycles);
            System.out.println();

            if ((algorithm.equals("RR") || algorithm.equals("PSJF"))
                    && task.getTimeLeft() > 0) {
                synchronized (readyQueueLock) {
                    readyQueue.add(task);
                }
            }
        }
    }

    private static SharedThread selectTask(String algorithm, Queue<SharedThread> readyQueue) {
        if (algorithm.equals("SJF") || algorithm.equals("PSJF")) {
            SharedThread shortest = null;
            for (SharedThread candidate : readyQueue) {
                if (shortest == null || candidate.getTimeLeft() < shortest.getTimeLeft()) {
                    shortest = candidate;
                }
            }
            if (shortest != null) {
                readyQueue.remove(shortest);
            }
            return shortest;
        }
        return readyQueue.poll();
    }

    private static void printReadyQueue(Queue<SharedThread> readyQueue,
                                        Object readyQueueLock) {
        synchronized (readyQueueLock) {
            System.out.println("--------------- Ready Queue ---------------");
            for (SharedThread task : readyQueue) {
                System.out.printf("ID:%d, Max Burst:%d, Current Burst:%d%n",
                        task.getThreadID(), task.getBurstTime(), task.getCompletedBurst());
            }
            System.out.println("-------------------------------------------\n");
        }
    }

    private static boolean isSupportedAlgorithm(String algorithm) {
        return algorithm != null && (algorithm.equalsIgnoreCase("FCFS")
                || algorithm.equalsIgnoreCase("RR") || algorithm.equalsIgnoreCase("SJF")
                || algorithm.equalsIgnoreCase("PSJF"));
    }

    private static String getAlgorithmName(String algorithm) {
        switch (algorithm) {
            case "FCFS":
                return "FCFS";
            case "RR":
                return "Round Robin";
            case "SJF":
                return "Non Preemptive - Shortest Job First";
            case "PSJF":
                return "Preemptive - Shortest Job First";
            default:
                throw new IllegalArgumentException("Unsupported scheduling algorithm: " + algorithm);
        }
    }

}
