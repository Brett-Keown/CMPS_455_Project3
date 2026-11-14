import java.util.*;
import java.util.concurrent.Semaphore;

public class CPU extends Thread {

    private final int cpuId;
    private final String algorithm;
    private final Queue<SharedThread> readyQueue;
    private final int totalTasks;
    private final int[] completedTasks;
    private final Semaphore readyQueue_SEM;

    public CPU(int cpuId, String algorithm, Queue<SharedThread> readyQueue,
               Semaphore readyQueue_SEM, int[] completedTasks, int totalTasks) {
        this.cpuId = cpuId;
        this.algorithm = algorithm;
        this.readyQueue = readyQueue;
        this.readyQueue_SEM = readyQueue_SEM;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
    }

    private void dispatch() throws InterruptedException {

        int quantum = 5;
        System.out.println("CPU " + cpuId + " running " + algorithm);

        while (true) {
            SharedThread task = null;

            readyQueue_SEM.acquire();

            if (completedTasks[0] >= totalTasks) {
                readyQueue_SEM.release();
                return;
            }

            if (!readyQueue.isEmpty()) {
                if (algorithm.equals("NSJF")) {
                    for (SharedThread t : readyQueue) {
                        if (task == null || t.getTimeLeft() < task.getTimeLeft()) {
                            task = t;
                        }
                    }
                    readyQueue.remove(task);
                } else {
                    task = readyQueue.poll();
                }
            }

            readyQueue_SEM.release();


            if (task == null) {
                Thread.sleep(1);
                continue;
            }

            int cycles = algorithm.equals("RR")
                    ? Math.min(quantum, task.getTimeLeft())
                    : task.getTimeLeft();

            System.out.println("Dispatcher " + cpuId + " selected task " + task.getThreadID()
                    + " for " + cycles + " cycle(s).");

            if (task.assignCPU(cycles)) {
                task.waitForCompletion();
            }

            readyQueue_SEM.acquire();

            if (task.getTimeLeft() > 0) {
                readyQueue.add(task);
            }
            else {
                completedTasks[0]++;
            }
            readyQueue_SEM.release();
            MultiCoreSim.printReadyQueue(readyQueue, readyQueue_SEM);


        }
    }
}

   /* @Override
    public void run(){
        try {
            dispatch();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }*/
    //}
//}
