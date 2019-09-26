package id.jred;

public class App {
    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp()) {
            cmdLineArgs.printHelp();
            return;
        }
        var positional = cmdLineArgs.getPositional();
        if (positional.isEmpty() ||
            positional.get(0).equalsIgnoreCase("client")) {
            System.out.println("jred client");
        } else if (positional.get(0).equalsIgnoreCase("server")) {
            System.out.println("jred server");
        } else {
            System.out.println("jred: invalid mode " + positional.get(0));
        }
    }
}
