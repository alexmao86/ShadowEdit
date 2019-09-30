package shadowedit.monitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	//命令的时间戳间隔在50毫秒内，则视为一个命令
	private final int DELTA=50;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ScriptExecutionChangeListener.class);
	private Queue<FileAction> commandQueue=new LinkedList<>();
	private final ScheduledExecutorService executor;
	
	public ScriptExecutionChangeListener(ScheduledExecutorService executor) {
		super();
		this.executor = executor;
		this.executor.scheduleWithFixedDelay(this, 2500, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(final IResourceDelta delta) throws CoreException {
					final IResource resource = delta.getResource();
					if (resource.getType() != IResource.FILE&&resource.getType() != IResource.FOLDER) {
						return true;
					} 
					
					String resourcePath = resource.getLocation().toFile().getAbsolutePath();
					String projectPath=(resource.getProject()==null?"":resource.getProject().getLocation().toFile().getAbsolutePath());
					
					if (delta.getKind() == IResourceDelta.ADDED) {
						LOGGER.debug("{} projectPath: {}, resource {} added", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.ADDED, projectPath, resourcePath));
					}
					if (delta.getKind() == IResourceDelta.CHANGED) {
						LOGGER.debug("{} projectPath: {}, resource {} changed", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.CHANGED, projectPath, resourcePath));
					}
					if (delta.getKind() == IResourceDelta.REMOVED) {
						LOGGER.debug("{} projectPath: {}, resource {} removed", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.REMOVED, projectPath, resourcePath));
					}
					if ((delta.getFlags()&IResourceDelta.MOVED_FROM)!=0) {
						LOGGER.debug("{} projectPath: {}, resource {} move from", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.MOVED_FROM, projectPath, resourcePath));
					}
					if ((delta.getFlags()&IResourceDelta.MOVED_TO)!=0) {
						LOGGER.debug("{} projectPath: {}, resource {} move to", System.currentTimeMillis(), projectPath, resourcePath);
						commandQueue.offer(new FileAction(IResourceDelta.MOVED_TO, projectPath, resourcePath));
					}
					return true;
				}
			});
		} catch (final CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		if(commandQueue.isEmpty()) {
			return ;
		}
		List<FileAction> sub=new ArrayList<>();
		FileAction temp=commandQueue.poll();
		sub.add(temp);
		while(!commandQueue.isEmpty()) {
			FileAction current=commandQueue.peek();
			if(current.getMillseconds()-temp.getMillseconds()<DELTA) {
				sub.add(current);
				commandQueue.poll();
			}
			else {
				break;
			}
		}
		
		if(sub.isEmpty()) {
			return ;
		}
		
		//判断
		int changes=0;
		int adds=0;
		int removes=0;
		int movetos=0;
		int movefroms=0;
		for(FileAction fa:sub) {
			if(fa.getEclipseKind()==IResourceDelta.ADDED) adds++;
			else if(fa.getEclipseKind()==IResourceDelta.CHANGED) changes++;
			else if(fa.getEclipseKind()==IResourceDelta.REMOVED) removes++;
			else if(fa.getEclipseKind()==IResourceDelta.MOVED_FROM) movefroms++;
			else if(fa.getEclipseKind()==IResourceDelta.MOVED_TO) movetos++;
		}
		
		//add files
		if(changes>0&&adds>0&&removes==0&&movetos==0&&movefroms==0) {
			resolveNews(sub);
		}
		else if(changes>0&&adds==0&&removes>0&&movetos==0&&movefroms==0) {//remove 文件或文件夹
			resolveRemoves(sub);
		}
		else if(changes==1&&adds==0&&removes==0&&movetos==0&&movefroms==0) {//在改文件
			resolveFileModify(sub.get(0));
		}
		else if(movetos>0&&movefroms>0) {
			resolveMoveto(sub);
		}
	}

	//只需在队列里面找到第一条moveto则是rename的newname，第一条move from 是oldname
	private void resolveMoveto(List<FileAction> sub) {
		FileAction newFile=null;
		FileAction oldFile=null;
		for(FileAction fa:sub) {
			if(fa.getEclipseKind()==IResourceDelta.MOVED_FROM&&newFile==null) {
				newFile=fa;
			}
			else if(fa.getEclipseKind()==IResourceDelta.MOVED_TO&&oldFile==null) {
				oldFile=fa;
			}
		}
		LOGGER.debug("resolveMoveto {}->{}", oldFile, newFile);
	}

	private void resolveNews(List<FileAction> sub) {
		for(FileAction fa:sub) {
			if(fa.getEclipseKind()==IResourceDelta.ADDED) {
				LOGGER.debug("resolveNews {}", fa);
			}
		}
	}

	private void resolveRemoves(List<FileAction> sub) {
		for(FileAction fa:sub) {
			if(fa.getEclipseKind()==IResourceDelta.REMOVED) {
				LOGGER.debug("resolveRemoves {}", fa);
			}
		}
	}

	private void resolveFileModify(FileAction fileAction) {
		LOGGER.debug("resolveFileModify {}", fileAction);
	}
}
