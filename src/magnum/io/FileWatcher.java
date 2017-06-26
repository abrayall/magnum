package magnum.io;

import java.util.function.Consumer;

import javax.io.File;
import javax.util.List;
import static javax.util.List.*;

import magnum.Watcher;

public class FileWatcher implements Watcher<File> {

	protected List<File> files;
	
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
		for (File file : this.files)
			file.watch((target, operation) -> callback.accept(target));
		
		return this;
	}

}
