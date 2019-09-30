package shadowedit.monitor;

public class FileAction {
	public static final int NEW=0;
	public static final int MODIFY=1;
	public static final int RENAME=2;
	
	private final long millseconds=System.currentTimeMillis();
	private int eclipseKind;
	private String projectPath;
	private String resourcePath;
	
	public FileAction(int eclipseKind, String projectPath, String resourcePath) {
		super();
		this.eclipseKind = eclipseKind;
		this.projectPath = projectPath;
		this.resourcePath = resourcePath;
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

	@Override
	public String toString() {
		return "FileAction [millseconds=" + millseconds + ", eclipseKind=" + eclipseKind + ", projectPath="
				+ projectPath + ", resourcePath=" + resourcePath + "]";
	}
}
