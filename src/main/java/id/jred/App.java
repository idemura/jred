package id.jred;

import lombok.NonNull;

public class App {
    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp()) {
            cmdLineArgs.printHelp();
            return;
        }
        System.out.println("jred");
    }
}
