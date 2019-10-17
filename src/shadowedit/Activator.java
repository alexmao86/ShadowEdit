package shadowedit;

import java.io.PrintStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import shadowedit.monitor.ScriptExecutionChangeListener;
import shadowedit.util.PluginUtil;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String METAFILE = "shadowedit.sdexml";
	// The plug-in ID
	public static final String PLUGIN_ID = "ShadowEdit"; //$NON-NLS-1$

	public static final String META_TEMPLATE=PLUGIN_ID+".xml";
	
	// The shared instance
	private static Activator plugin;
	private MessageConsole console;

	private ScriptExecutionChangeListener resourceChangeListener;

	public static PrintStream out = System.out;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		resourceChangeListener = new ScriptExecutionChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);

		console = new MessageConsole("ShadowEdit", null);
		// Add it to console manager
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });

		out = new PrintStream(console.newMessageStream());
		out.println("Showdit open, before edit it is suggested to use winscp to synchronize remote directory first.");

		new Thread(new Runnable() {
			@Override
			public void run() {
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				IProject[] projects = workspaceRoot.getProjects();
				for (int i = 0; i < projects.length; i++) {
					IProject project = projects[i];
					PluginUtil.createMetafileOptionally(project);
					PluginUtil.updateMetafileTimestamp(project);//update timestamp to triger watch
				}
			}

		}).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		resourceChangeListener.dispose();
		out.close();
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
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	protected static void init() {
		
	}
}
