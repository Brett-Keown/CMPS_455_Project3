import java.util.*;
import java.util.concurrent.Semaphore;

public class CPU extends Thread{

    private final int cpuId;
    private final String algorithm;
    private final Queue<SharedThread> readyQueue;
    private final int totalTasks;
    private final int[] completedTasks;
    private final Semaphore readyQueue_SEM;

    public CPU(int cpuId, String algorithm, Queue<SharedThread> readyQueue,
               Semaphore readyQueue_SEM, int[] completedTasks, int totalTasks){
        this.cpuId = cpuId;
        this.algorithm = algorithm;
        this.readyQueue = readyQueue;
        this.readyQueue_SEM = readyQueue_SEM;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
    }

    private void dispatch() throws InterruptedException{

        System.out.println("CPU " + cpuId + " running " + algorithm);
        while (true){
            SharedThread task = null;
            readyQueue_SEM.acquire();
           /* try {
                if (completedTasks[0] == totalTasks){
                    return;
                }
                if (algorithm.equals("NSJF")){
                    //task = S
                }
            }*/

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
