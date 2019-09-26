package id.jred;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.util.ArrayList;
import java.util.List;

public class CmdLineArgs {
  private final JCommander jcmd;

  @Parameter(names={"-h", "--help"},
             description="This help",
             help=true)
  private boolean help;

  @Parameter(names={"-p", "--port"},
             description="Port to listen to/connect")
  private int port = 8080;

  @Parameter(names={"-s", "--server"},
             description="Start server daemon")
  private boolean server;

  @Parameter(description="command [positional arguments]")
  private List<String> positional = new ArrayList<>();

  public CmdLineArgs(String[] args) {
    this.jcmd = JCommander.newBuilder()
        .addObject(this)
        .columnSize(80)
        .programName("jred")
        .args(args)
        .build();
  }

  public boolean isHelp() { return help; }

  public void printHelp() {
    jcmd.usage();
  }
}
