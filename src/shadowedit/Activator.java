package shadowedit;

import java.io.PrintStream;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shadowedit.monitor.ScriptExecutionChangeListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	private final static Logger LOGGER=LoggerFactory.getLogger(Activator.class); 

	public static final String METAFILE = "shadowedit.sdexml";
	// The plug-in ID
	public static final String PLUGIN_ID = "ShadowEdit"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private MessageConsole console;
	private PrintStream out;
	private PrintStream err;
	
	private ScriptExecutionChangeListener resourceChangeListener;
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		resourceChangeListener=new ScriptExecutionChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.PRE_DELETE|IResourceChangeEvent.POST_CHANGE);
		
		console = new MessageConsole("ShadowEdit", null);
		// Add it to console manager
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] {console});
        
        PrintStream printStream = new PrintStream(console.newMessageStream());
        
    	out=System.out;
    	err=System.err;
    	
        System.setOut(printStream);
        System.setErr(printStream);
        LOGGER.debug("workspace watched, plugin started");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		resourceChangeListener.dispose();
		LOGGER.debug("workspace disposed");
		System.setOut(out);
        System.setErr(err);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
