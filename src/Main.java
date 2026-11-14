public class Main {

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration;
        try {
            configuration = parseArguments(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(2);
            return;
        }

        String algorithm;
        switch (configuration.algorithm) {
            case 1:
                algorithm = "FCFS";
                break;
            case 2:
                algorithm = "RR";
                break;
            case 3:
                algorithm = "SJF";
                break;
            case 4:
                algorithm = "PSJF";
                break;
            default:
                throw new IllegalStateException("Unsupported scheduling algorithm.");
        }
        if (configuration.cores == 1) {
            SingleCoreSimulator.run(algorithm, configuration.quantum);
        } else {
            MultiCoreSim.run(algorithm.equals("SJF") ? "NSJF" : algorithm,
                    configuration.quantum, configuration.cores);
        }
    }

    public static void CPU(SharedThread task, int cycles) throws InterruptedException {
        if (task.assignCPU(cycles)) {
            task.waitForCompletion();
        }
    }
    public static void CPU(SharedThread task) throws InterruptedException {
        task.waitForCompletion();
    }

    private static Configuration parseArguments(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("arguments are required.");
        }

        Integer algorithm = null;
        Integer quantum = null;
        Integer cores = null;

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if ("-S".equals(argument)) {
                if (algorithm != null) {
                    throw new IllegalArgumentException("-S may only be specified once.");
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("-S requires an algorithm parameter.");
                }
                algorithm = parseInteger(args[++i], "-S algorithm");
                if (algorithm < 1 || algorithm > 4) {
                    throw new IllegalArgumentException("the -S algorithm must be between 1 and 4.");
                }
                if (algorithm == 2) {
                    if (i + 1 >= args.length || args[i + 1].startsWith("-")) {
                        throw new IllegalArgumentException("-S 2 requires a time quantum.");
                    }
                    quantum = parseInteger(args[++i], "round-robin quantum");
                    if (quantum < 2 || quantum > 10) {
                        throw new IllegalArgumentException("the round-robin quantum must be between 2 and 10.");
                    }
                }
            } else if ("-C".equals(argument)) {
                if (cores != null) {
                    throw new IllegalArgumentException("-C may only be specified once.");
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("-C requires a core-count parameter.");
                }
                cores = parseInteger(args[++i], "-C core count");
                if (cores < 1 || cores > 4) {
                    throw new IllegalArgumentException("the -C core count must be between 1 and 4.");
                }
            } else {
                throw new IllegalArgumentException("unrecognized or unexpected argument: " + argument);
            }
        }

        if (algorithm == null) {
            throw new IllegalArgumentException("-S is required.");
        }
        return new Configuration(algorithm, quantum == null ? 5 : quantum,
                cores == null ? 1 : cores);
    }

    private static int parseInteger(String value, String parameterName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(parameterName + " must be an integer.");
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java Main -S <algorithm> [quantum] [-C <cores>]");
        System.err.println("  -S 1       First-come, first-served");
        System.err.println("  -S 2 <q>   Round-robin, with quantum 2-10");
        System.err.println("  -S 3       Non-preemptive shortest job first");
        System.err.println("  -S 4       Preemptive shortest job first");
        System.err.println("  -C <cores> Number of CPU cores, 1-4 (default: 1)");
    }

    private static final class Configuration {
        private final int algorithm;
        private final int quantum;
        private final int cores;

        private Configuration(int algorithm, int quantum, int cores) {
            this.algorithm = algorithm;
            this.quantum = quantum;
            this.cores = cores;
        }
    }
}
