package shadowedit.monitor;

import java.text.MessageFormat;

import shadowedit.model.ShadowEdit;

public class FileAction {
	public static final int NEW=0;
	public static final int MODIFY=1;
	public static final int RENAME=2;
	
	private final long millseconds=System.currentTimeMillis();
	private int eclipseKind;
	private String projectPath;
	private String resourcePath;
	private String relativePath;
	private final ShadowEdit shadowEdit;
	
	public FileAction(int eclipseKind, String projectPath, String resourcePath, ShadowEdit edit) {
		super();
		
		this.eclipseKind = eclipseKind;
		this.projectPath = projectPath;
		this.resourcePath = resourcePath;
		this.shadowEdit=edit;
		
		this.projectPath=this.projectPath.replaceAll("\\\\", "/");
		this.resourcePath=this.resourcePath.replaceAll("\\\\", "/");
		this.relativePath=this.resourcePath.replaceAll(this.projectPath, "");
	}
	
	public int getEclipseKind() {
		return eclipseKind;
	}
	public void setEclipseKind(int eclipseKind) {
		this.eclipseKind = eclipseKind;
	}
	public String getProjectPath() {
		return projectPath;
	}
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	public String getResourcePath() {
		return resourcePath;
	}
	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}
	public long getMillseconds() {
		return millseconds;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public ShadowEdit getShadowEdit() {
		return shadowEdit;
	}

	@Override
	public String toString() {
		return MessageFormat.format("[{0}:{1} {2} {3} {4}]", millseconds, eclipseKind, projectPath, resourcePath, relativePath);
	}
	
}
