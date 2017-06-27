package magnum.core;

import javax.util.Map;
import static javax.lang.Process.*;
import static javax.util.List.*;

import magnum.Runner;

public class CommandRunner implements Runner {
	public Process run(Map<String, String> options) throws Exception {
		return process(builder(list(options.get("parameters", "").split(" ")).array()).redirectErrorStream(true));
	}
}
