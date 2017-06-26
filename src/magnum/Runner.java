package magnum;

import javax.util.Map;

public interface Runner {
	public Process run(Map<String, String> options) throws Exception;
}
