package magnum;

import static javax.lang.System.*;
import static javax.util.List.*;
import static javax.util.Map.*;

import java.util.function.Consumer;

import javax.io.File;
import javax.lang.Process;
import javax.util.List;
import javax.util.Map;
import javax.util.Timestamp;

import magnum.core.CommandRunner;
import magnum.core.FileWatcher;

public class Main extends cilantro.Main {
	
	protected Watcher<?> watcher;
	
	public Integer execute(List<String> parameters, Map<String, String> options) throws Exception {
		println("-----------------------------");
		println("${format(Magnum - CI Build Tool, blue, bold)} ${format(v1.0.1, yellow)}");
		println("-----------------------------");
		
		if (parameters.size() == 0)
			return help();
					
		return watch(parameters, options);
	}
	
	public Integer watch(List<String> parameters, Map<String, String> options) throws Exception {
		println("Watching files...");
		this.watcher = watch(list("src", "resources"), file -> {
			try {
				run(parameters).future().onOutput((line, future) -> {
					println("  ${yellow([" + parameters.get(0, "none") + "]:)} " + line);
				}).onTerminate((code, future) -> println("\n${format(Execution complete [" + Timestamp.format(now()) + "]., green, bold)}"));
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
		
		return 1;
	}
	
	public Watcher<?> watch(List<String> locations, Consumer<File> handler) throws Exception {
		return new FileWatcher(locations.array()).watch(file -> {
			println("Noticed " + file + " changed [" + Timestamp.format(now()) + "]...");
			handler.accept(file);
		});
	}
	
	public Process run(List<String> parameters) throws Exception {
		return new CommandRunner().run(map(entry("parameters", parameters.join(" "))));
	}
	
	public void shutdown() throws Exception {
		this.watcher.stop();
	}
	
	protected Integer help() {
		println();
		println("Usage: magnum [command]");
		println("  - command:  command and parameters that should be executed when file changes");
		println();
		return 0;
	}
	
	public static void main(String[] arguments) throws Exception {
		main(Main.class, arguments);
	}
}
