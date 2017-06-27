package magnum.core;

import javax.util.List;
import javax.util.Map;
import javax.lang.Process;

import static javax.lang.Process.*;
import static javax.lang.System.*;
import static javax.util.List.*;

import magnum.Runner;

public class CommandRunner implements Runner {
	public Process run(Map<String, String> options) throws Exception {
		List<String> parameters = list(options.get("parameters", "").split(" ")).map(parameter -> parameter.trim()).filter(parameter -> !parameter.equals(""));
		if (parameters.size() > 0)
			return run(parameters);
		else
			throw new Exception("Not valid command to run");
	}
	
	public Process run(List<String> parameters) throws Exception {
		println("Running " + parameters.join(" ") + "...");
		return process(builder(parameters.array()).redirectErrorStream(true));
	}
}
