package magnum;

import static javax.lang.System.*;
import static javax.util.List.*;
import static javax.util.Map.*;
import static javax.util.Properties.properties;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

import javax.io.File;
import javax.lang.Process;
import javax.util.List;
import javax.util.Map;
import javax.util.Properties;
import javax.util.Timestamp;

import magnum.core.CommandRunner;
import magnum.core.FileWatcher;

public class Main extends cilantro.Main {
	
	protected Watcher<?> watcher;
	
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
	
	public static class Version {
		
		private int major = 0;
		private int minor = 0;
		private int maintenance = 0;
		
		private Version() {}
		private Version(int major, int minor, int maintenance) {
			this.major = major;
			this.minor = minor;
			this.maintenance = maintenance;
		}
		
		public int getMajor() {
			return major;
		}
		
		public int getMinor() {
			return minor;
		}
		
		public int getMaintenance() {
			return maintenance;
		}
		
		
		public int compare(String version) {
			return this.compare(parse(version));
		}
		
		public int compare(Version version) {
			if (this.major != version.major) return this.major > version.major ? 1 : -1;
			if (this.minor != version.minor) return this.minor > version.minor ? 1 : -1;
			if (this.maintenance != version.maintenance) return this.maintenance > version.maintenance ? 1 : -1;
			return 0;
		}
		
		public boolean isGreater(String version) {
			return this.isGreater(parse(version));
		}
		
		public boolean isGreater(Version version) {
			return this.compare(version) == 1;
		}
		
		public boolean isLesser(String version) {
			return this.isLesser(parse(version));
		}
		
		public boolean isLesser(Version version) {
			return this.compare(version) == -1;
		}
		
		public String toString() {
			return this.major + "." + this.minor + "." + this.maintenance;
		}
		
		public static Version getVersion() {
			return parse(load("version.properties"));
		}
		
		public static Version parse(String text) {
			Version version = new Version();
			String[] tokens = text.split("\\.");
			
			if (tokens.length > 0)
				version.major = integer(tokens[0], 0);
			
			if (tokens.length > 1)
				version.minor = integer(tokens[1], 0);
			
			if (tokens.length > 2)
				version.maintenance = integer(tokens[2], 0);

			return version;
		}
		
		private static int integer(String value, int defaultValue) {
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				return defaultValue;
			}
		}
		
		private static String load(String name) {
			File file = new File(name);
			if (file.exists() == true)
				return load(file(file));
			else
				return load(Version.class.getClassLoader().getResourceAsStream(name));
		}

		private static String load(InputStream input) {
			Properties properties = properties(input);
			return properties.getProperty("major", "0") + "." + properties.getProperty("minor", "0") + "." + properties.getProperty("maintenance", "0") + "." + properties.getProperty("revision", "0");
		}
		
		private static InputStream file(File file) {
			try {
				return new FileInputStream(file.toFile());
			} catch (Exception e) {
				return new ByteArrayInputStream(new byte[0]);
			}
		}
	}
}
