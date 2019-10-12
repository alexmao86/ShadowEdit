package shadowedit.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 * ClassName: CommanderUtil <br/>
 * 
 * @version
 * @since JDK 1.8
 */
public class CommanderUtil {
	private CommanderUtil() {

	}

	/**
	 * execute given command, the default timeout is 10 seconds
	 * 
	 * @param cmd
	 * @return
	 * @since JDK 1.8
	 */
	public static final List<String> execute(String cmd) {
		return execute(cmd, 30000);
	}

	/**
	 * 
	 * @param cmd
	 * @param timeout
	 *            in millseconds
	 * @return
	 * @since JDK 1.8
	 */
	public static final List<String> execute(String cmd, final int timeout) {
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

		List<String> ret = new ArrayList<String>();

		StringTokenizer st = new StringTokenizer(cmd);
		String[] cmdarray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++) {
			cmdarray[i] = st.nextToken();
		}

		final AtomicReference<Process> refer = new AtomicReference<>();
		scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				if (refer.get() != null) {
					refer.get().destroy();
				}
			}
		}, timeout, TimeUnit.MILLISECONDS);

		try {
			final Process proc = new ProcessBuilder(cmdarray).directory(new File(".")).redirectErrorStream(true)
					.start();
			refer.set(proc);

			InputStream in = proc.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				ret.add(line);
			}
			reader.close();
			in.close();
			proc.destroy();
			refer.set(null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}
}
