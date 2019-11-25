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
import vermouth.Version;

public class Main extends cilantro.Main {

	protected Watcher<?> watcher;
	protected Process process;

	public Integer execute(List<String> parameters, Map<String, String> options) throws Exception {
		println("-----------------------------");
		println("${format(Magnum - CI Build Tool, blue, bold)} ${format(v" + Version.getVersion() + ", yellow)}");
		println("-----------------------------");

		if (parameters.size() == 0)
			return help();

		return watch(parameters, options);
	}

	public Integer watch(List<String> parameters, Map<String, String> options) throws Exception {
		println("Watching files...");
		this.watcher = watch(list("src", "resources"), file -> {
			new Thread(new Runnable() {
				public void run() {
					try {
						if (process != null)
							kill(process);

						process = execute(parameters);
						process.future().onOutput((line, future) -> {
							println("  ${yellow([" + parameters.get(0, "none") + "]:)} " + line);
						}).onTerminate((code, future) -> println("\n${format(Execution complete [" + Timestamp.format(now()) + "]., green, bold)}"));
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}).start();
		});

		return 0;
	}

	public Watcher<?> watch(List<String> locations, Consumer<File> handler) throws Exception {
		return new FileWatcher(locations.array()).watch(file -> {
			println("Noticed " + file + " changed [" + Timestamp.format(now()) + "]...");
			handler.accept(file);
		});
	}

	public Process execute(List<String> parameters) throws Exception {
		return new CommandRunner().run(map(entry("parameters", parameters.join(" "))));
	}

	public void shutdown() throws Exception {
		this.watcher.stop();
		this.kill(this.process);
	}

	public void kill(Process process) {
		if (process != null && process.isAlive()) {
			process.destroyForcibly();
			while (process.isAlive() == true)
				try { Thread.sleep(100); } catch (Exception e) {}
		}
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
