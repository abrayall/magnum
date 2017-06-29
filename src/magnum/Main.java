package magnum;

import static javax.lang.System.*;
import static javax.util.List.*;
import static javax.util.Map.*;

import java.util.function.Consumer;

import javax.io.File;
import javax.util.List;
import javax.util.Timestamp;
import javax.lang.Process;

import magnum.core.CommandRunner;
import magnum.core.FileWatcher;

public class Main extends cilantro.Main {
	
	protected Watcher<?> watcher;
	
	public Integer execute() throws Exception {
		println("${format(Magnum v1.0.0, blue, bold)}");
		println("--------------");
		println("Watching files...");
		
		this.watcher = watch(list("src", "resources"), file -> {
			try {
				run(parameters).future().onOutput((line, future) -> {
					println("  [" + parameters.get(0, "none") + "]: " + line);
				}).onTerminate((code, future) -> println("Execution complete [" + Timestamp.format(now()) + "]."));
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
	
	public static void main(String[] arguments) throws Exception {
		main(Main.class, arguments);
	}
}
