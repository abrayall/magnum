package magnum;

import java.util.function.Consumer;

public interface Watcher<T> {
	public Watcher<T> watch(Consumer<T> callback) throws Exception;
}
