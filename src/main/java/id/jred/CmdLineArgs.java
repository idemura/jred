package id.jred;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class CmdLineArgs {
    private final JCommander jcmd;

    @Parameter(names={"--help"}, help=true)
    private boolean help;

    @Parameter(
        names={"-h", "--host"},
        description="Host to listen to/connect")
    private String host;

    @Parameter(
        names={"-p", "--port"},
        description="Port to listen to/connect")
    private int port;

    @Parameter(description="command [positional arguments]")
    private List<String> positional = new ArrayList<>();

    public CmdLineArgs(String[] args) {
        this.jcmd = JCommander.newBuilder()
            .addObject(this)
            .programName("jred")
            .args(args)
            .build();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @NonNull
    public List<String> getPositional() {
        return positional;
    }

    public boolean isHelp() { return help; }

    public void printHelp() {
        jcmd.usage();
    }
}
