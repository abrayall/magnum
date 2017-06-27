package magnum.core;

import static javax.lang.Try.*;
import static javax.util.List.*;

import java.util.function.Consumer;

import javax.io.File;
import javax.util.List;

import magnum.Watcher;

public class FileWatcher implements Watcher<File> {
	
	protected List<File> files;
	protected List<File.FileWatcher> watchers;
	
	public FileWatcher(String... files) {
		this(list(files).map(File::new));
	}
	
	public FileWatcher(File... files) {
		this(list(files));
	}
	
	public FileWatcher(List<File> files) {
		this.files = files;
	}
	
	public FileWatcher watch(Consumer<File> callback) throws Exception {
		this.watchers = this.files.map(file -> {
			return attempt(() -> file.watcher((target, operation) -> callback.accept(target)).watch());
		}).each(watcher -> attempt(() -> watcher.join()));

		return this;
	}
	
	public FileWatcher stop() throws Exception {
		this.watchers.each(watcher -> attempt(() -> watcher.halt()));
		return this;
	}
}
