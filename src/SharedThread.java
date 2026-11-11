import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SharedThread extends Thread {
    private final int ThreadID;
    private final int burstTime;
    private int timeLeft;
    private int quantum;

    // this boolean tracks whether a task is actively assigned to a cpu
    // in multi core implementation prevents duplicate assignment
    private boolean assigned = false;
    // this boolean is specifically for PSJF and serves to interrupt the active task
    private boolean psjfInterrupt = false;

    // execute semaphore allows the dispatcher to control when the individual task can execute
    private final Semaphore execute = new Semaphore(0);
    // done semaphore is used to indicate to the dispatcher when a task's finishes its execution (limited by quantum)
    private final Semaphore done = new Semaphore(0);
    // mutex used to protect both timeLeft and assigned
    private final Lock mutex = new ReentrantLock();


    public SharedThread(int id, int burstTime) {
        this.ThreadID = id;
        this.burstTime = burstTime;
        this.timeLeft = burstTime;
        this.quantum = 0;
    }

    // getThreadID, getBurstTime, getTimeLeft will be used by dispatcher to choose task/thread to execute
    public int getThreadID() {
        return ThreadID;
    }
    public int getBurstTime() {
        return burstTime;
    }
    public int getTimeLeft() {
        mutex.lock();
        try {
            return timeLeft;
        } finally {
            mutex.unlock();
        }
    }

    // this function is used in main to make the dispatcher wait until the current task is completed to assign another
    public void waitForCompletion() throws InterruptedException {
        done.acquire();

        mutex.lock();

        try {
            assigned = false;
        } finally {
            mutex.unlock();
        }
    }

    // allows execution on cpu if it is not already assigned or completed
    // duration based on quantumVal
    public boolean assignCPU(int quantumVal) {
        mutex.lock();

        try {
            if (assigned || timeLeft == 0) {
                return false;
            }

            assigned = true;
            psjfInterrupt = false;
            quantum = Math.min(quantumVal, timeLeft);
            execute.release();
            return true;

        } finally {
            mutex.unlock();
        }
    }

    // should allow premption for psjf algorithm
    // still needs to be tested
    public void preemption() {
        mutex.lock();

        try {
            if (assigned) {
                psjfInterrupt = true;
            }
        } finally {
            mutex.unlock();
        }
    }

    // while the thread has time left for execution, gains cpu permission and executes based on quantum value
    @Override
    public void run() {
        while (getTimeLeft() > 0) {

            try {
                execute.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < quantum; i++) {
                mutex.lock();

                try {
                    if (timeLeft == 0 || psjfInterrupt) {
                        break;
                    }
                    timeLeft--;

                } finally {
                    mutex.unlock();
                }
            }
            done.release();
        }
    }
}
