package magnum;

import static javax.lang.System.*;
import static javax.lang.Process.*;
import static javax.util.List.*;

import java.util.function.Consumer;

import javax.io.File;
import javax.lang.Strings;
import javax.util.List;
import javax.util.Timestamp;

public class Main {
	
	public static void main(String[] arguments) throws Exception {
		println("Magnum v1.0.0");
		println("--------------");
		println("Watching files...");
		watch(list("src", "resources"), file -> {
			try {
				println("Running " + Strings.join(arguments, " ") + "...");
				process(builder(arguments).redirectErrorStream(true)).future().onOutput((line, future) -> {
					println("  [" + list(arguments).get(0, "none") + "]: " + line);
				}).onTerminate((code, future) -> println("Execution complete [" + Timestamp.format(now()) + "]."));
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
	}
	
	public static void watch(List<String> locations, Consumer<File> handler) throws Exception {
		for (String location : locations) {
			new File(location).watch((file, operation) -> {
				println("Noticed " + file + " changed [" + Timestamp.format(now()) + "]...");
				handler.accept(file);
			});
		}
	}
}
