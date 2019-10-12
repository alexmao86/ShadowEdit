package shadowedit.monitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import shadowedit.Activator;
import shadowedit.model.ShadowEdit;
import shadowedit.util.CommanderUtil;
import shadowedit.util.XMLUtil;

/**
 * 由于封装的java file watch不友好，这里利用队列分析触发的命令，来模拟出
 * 
 * NEW: 对文件夹和文件有效： 无论文件或文件夹，只触发一次。windows explorer中触发两次，一次是新建.... 一次时修改
 * 
 * MODIFY: 仅对文件有效，表示内容被修改： 修改一个文件的内容时，所有从ROOT开始的文件夹均发烧changed视觉。
 * 
 * RENAME命令： 对文件夹和文件均有效： 对文件夹重命名，则发生很多add change move from等
 * 
 * @author maoanapex88@163.com alexmao86
 *
 */
public class ScriptExecutionChangeListener implements IResourceChangeListener, Runnable {
	// 命令的时间戳间隔在50毫秒内，则视为一个命令
	private final int DELTA = 50;

	private final static Logger LOGGER = LoggerFactory.getLogger(ScriptExecutionChangeListener.class);
	private Queue<FileAction> commandQueue = new LinkedList<>();
	private final ScheduledExecutorService shadowEditBackgroundExecutor;
	private final ScheduledExecutorService otherTaskExecutor;
	private Map<String, ShadowEdit> controls = new HashMap<>();

	public ScriptExecutionChangeListener() {
		super();
		this.shadowEditBackgroundExecutor = Executors.newSingleThreadScheduledExecutor();
		this.otherTaskExecutor = Executors.newSingleThreadScheduledExecutor();
		this.otherTaskExecutor.scheduleWithFixedDelay(this, 2500, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(final IResourceDelta delta) throws CoreException {
					System.out.println("visiting changes "+delta);
					final IResource resource = delta.getResource();
					if (resource.getType() == IResource.PROJECT) {
						IProject projectRoot = resource.getProject();
						if (!controls.containsKey(projectRoot.getName())) {
							generateOptionallyAndWatchMetafile(resource.getProject());
						}
						return true;
					}
					if (resource.getType() != IResource.FILE && resource.getType() != IResource.FOLDER) {
						return true;
					}
					
					String resourcePath = resource.getLocation().toFile().getAbsolutePath();
					String projectPath = (resource.getProject() == null ? "" : resource.getProject().getLocation().toFile().getAbsolutePath());
					
					if(resource.getName().equals(Activator.METAFILE)) {
						try {
							parseMetafile(resource.getProject());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					ShadowEdit config = controls.get(projectPath);
					if (config == null) {
						LOGGER.warn("no metafile for {}", projectPath);
						return true;
					}

					if (!config.isEnabled()) {
						LOGGER.warn("{} shadowedit is disabled********", projectPath);
						return true;
					}
					
					LOGGER.warn("{} shadowedit is enabled", projectPath);
					//把多个命令放入队列进行分析
					if (delta.getKind() == IResourceDelta.ADDED) {
						LOGGER.debug("{} projectPath: {}, resource {} added", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.ADDED, projectPath, resourcePath, controls.get(projectPath)));
					}
					if (delta.getKind() == IResourceDelta.CHANGED) {
						LOGGER.debug("{} projectPath: {}, resource {} changed", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.CHANGED, projectPath, resourcePath, controls.get(projectPath)));
					}
					if (delta.getKind() == IResourceDelta.REMOVED) {
						LOGGER.debug("{} projectPath: {}, resource {} removed", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.REMOVED, projectPath, resourcePath, controls.get(projectPath)));
					}
					if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
						LOGGER.debug("{} projectPath: {}, resource {} move from", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.MOVED_FROM, projectPath, resourcePath, controls.get(projectPath)));
					}
					if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
						LOGGER.debug("{} projectPath: {}, resource {} move to", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.MOVED_TO, projectPath, resourcePath, controls.get(projectPath)));
					}
					return true;
				}
			});
		} catch (final CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param projectRoot
	 */
	private void generateOptionallyAndWatchMetafile(final IProject project) {
		LOGGER.debug("generating shadow edit metafile");
		IFile file = project.getFile(Activator.METAFILE);
		
		if (!file.exists()) {
			LOGGER.debug("not exists, auto generate metafile");
			try {
				InputStream in = ScriptExecutionChangeListener.class.getResourceAsStream("ShadowEdit.xml");
				file.create(in, true, null);
				in.close();
			} catch (IOException | CoreException e) {
				e.printStackTrace();
			}
		}

		try {
			parseMetafile(project);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseMetafile(final IProject project) throws Exception {
		IFile file = project.getFile(Activator.METAFILE);
		InputStream in=file.getContents();
		Document doc = XMLUtil.createDocument(in);
		in.close();
		
		ShadowEdit bean = new ShadowEdit();
		bean.setEnabled("true".equalsIgnoreCase(doc.getDocumentElement().getAttribute("enabled")));

		Element fileset = (Element) doc.getDocumentElement().getElementsByTagName("fileset").item(0);
		NodeList includes = fileset.getElementsByTagName("include");
		if (includes != null) {
			for (int i = 0; i < includes.getLength(); i++) {
				String txt = includes.item(i).getTextContent().trim();
				if (txt.isEmpty()) {
					continue;
				}
				bean.getIncludes().add(txt);
			}
		}

		NodeList excludes = fileset.getElementsByTagName("exclude");
		if (excludes != null) {
			for (int i = 0; i < excludes.getLength(); i++) {
				String txt = excludes.item(i).getTextContent().trim();
				if (txt.isEmpty()) {
					continue;
				}
				bean.getExcludes().add(txt);
			}
		}

		bean.setMkdir(doc.getElementsByTagName("mkdir").item(0).getTextContent().trim());
		bean.setOncreate(doc.getElementsByTagName("oncreate").item(0).getTextContent().trim());
		bean.setOnmodify(doc.getElementsByTagName("onmodify").item(0).getTextContent().trim());
		bean.setOnmoveto(doc.getElementsByTagName("onmoveto").item(0).getTextContent().trim());
		bean.setOnremove(doc.getElementsByTagName("onremove").item(0).getTextContent().trim());

		controls.put(project.getName(), bean);
	}

	@Override
	public void run() {
		LOGGER.debug("background watching");
		//System.out.println("watching ...");
		if (commandQueue.isEmpty()) {
			return;
		}
		List<FileAction> sub = new ArrayList<>();
		FileAction temp = commandQueue.poll();
		sub.add(temp);
		while (!commandQueue.isEmpty()) {
			FileAction current = commandQueue.peek();
			if (current.getMillseconds() - temp.getMillseconds() < DELTA) {
				sub.add(current);
				commandQueue.poll();
			} else {
				break;
			}
		}

		if (sub.isEmpty()) {
			return;
		}

		// 判断
		int changes = 0;
		int adds = 0;
		int removes = 0;
		int movetos = 0;
		int movefroms = 0;
		for (FileAction fa : sub) {
			if (fa.getEclipseKind() == IResourceDelta.ADDED)
				adds++;
			else if (fa.getEclipseKind() == IResourceDelta.CHANGED)
				changes++;
			else if (fa.getEclipseKind() == IResourceDelta.REMOVED)
				removes++;
			else if (fa.getEclipseKind() == IResourceDelta.MOVED_FROM)
				movefroms++;
			else if (fa.getEclipseKind() == IResourceDelta.MOVED_TO)
				movetos++;
		}

		// add files
		if (changes >= 0 && adds > 0 && removes == 0 && movetos == 0 && movefroms == 0) {
			doCreate(sub);
		} else if (changes >= 0 && adds == 0 && removes > 0 && movetos == 0 && movefroms == 0) {// remove 文件或文件夹
			doRemoval(sub);
		} else if (changes == 1 && adds == 0 && removes == 0 && movetos == 0 && movefroms == 0) {// 在改文件
			doContentChange(sub.get(0));
		} else if (movetos > 0 && movefroms > 0) {
			doMoveTo(sub);
		}
	}

	// 只需在队列里面找到第一条moveto则是rename的newname，第一条move from 是oldname
	private void doMoveTo(List<FileAction> sub) {
		this.shadowEditBackgroundExecutor.execute(new Runnable() {
			@Override
			public void run() {
				List<FileAction> movetoList=new ArrayList<>(sub.size()/2);
				List<FileAction> movefromList=new ArrayList<>(sub.size()/2);
				
				for (FileAction fa : sub) {
					if (fa.getEclipseKind() == IResourceDelta.MOVED_FROM) {
						movefromList.add(fa);
					} 
					else if (fa.getEclipseKind() == IResourceDelta.MOVED_TO) {
						movetoList.add(fa);
					}
				}
				for(int i=0;i<movetoList.size();i++) {
					FileAction newFile = movetoList.get(i);
					FileAction oldFile = movefromList.get(i);
					ShadowEdit edit = oldFile.getShadowEdit();
					List<String> result = CommanderUtil.execute(MessageFormat.format(edit.getOnmoveto(), oldFile.getMillseconds() + "", oldFile.getProjectPath(), oldFile.getResourcePath(), oldFile.getRelativePath(), newFile.getProjectPath(), newFile.getResourcePath(), newFile.getRelativePath()));
					for (String l : result) {
						System.out.println(l);
					}
				}
			}
		});

	}

	private void doCreate(List<FileAction> sub) {
		Collections.sort(sub, new java.util.Comparator<FileAction>() {
			@Override
			public int compare(FileAction o1, FileAction o2) {
				return o1.getResourcePath().length() - o2.getResourcePath().length();
			}
		});

		for (final FileAction fa : sub) {
			if (fa.getEclipseKind() == IResourceDelta.ADDED) {
				this.shadowEditBackgroundExecutor.execute(new Runnable() {
					@Override
					public void run() {
						ShadowEdit edit = fa.getShadowEdit();
						boolean isFile = (new File(fa.getResourcePath()).isFile());
						String commandMessage = edit.getMkdir();
						if (isFile) {
							commandMessage = edit.getOncreate();
						}
						List<String> result = CommanderUtil.execute(MessageFormat.format(commandMessage, fa.getMillseconds() + "", fa.getProjectPath(), fa.getResourcePath(), fa.getRelativePath()));
						for (String l : result) {
							System.out.println(l);
						}
					}
				});
			}
		}
	}

	private void doRemoval(List<FileAction> sub) {
		Collections.sort(sub, new java.util.Comparator<FileAction>() {
			@Override
			public int compare(FileAction o1, FileAction o2) {
				return o2.getResourcePath().length() - o1.getResourcePath().length();
			}
		});
		for (final FileAction fa : sub) {
			if (fa.getEclipseKind() == IResourceDelta.REMOVED) {
				this.shadowEditBackgroundExecutor.execute(new Runnable() {
					@Override
					public void run() {
						ShadowEdit edit = fa.getShadowEdit();
						List<String> result = CommanderUtil.execute(MessageFormat.format(edit.getOnremove(), fa.getMillseconds() + "", fa.getProjectPath(), fa.getResourcePath(), fa.getRelativePath()));
						for (String l : result) {
							System.out.println(l);
						}
					}
				});
			}
		}
	}

	private void doContentChange(final FileAction fa) {
		this.shadowEditBackgroundExecutor.execute(new Runnable() {
			@Override
			public void run() {
				ShadowEdit edit = fa.getShadowEdit();
				List<String> result = CommanderUtil.execute(MessageFormat.format(edit.getOnmodify(), fa.getMillseconds() + "", fa.getProjectPath(), fa.getResourcePath(), fa.getRelativePath()));
				for (String l : result) {
					System.out.println(l);
				}
			}
		});
	}

	public void dispose() {
		this.shadowEditBackgroundExecutor.shutdown();
		this.otherTaskExecutor.shutdown();
	}
}
