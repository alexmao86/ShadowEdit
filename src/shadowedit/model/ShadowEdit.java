package shadowedit.model;

import java.util.ArrayList;
import java.util.List;

public class ShadowEdit {
	private boolean enabled=false;
	private List<String> includes=new ArrayList<String>();
	private List<String> excludes=new ArrayList<String>();
	private String onmoveto;
	private String onmodify;
	private String onremove;
	private String oncreate;
	private String mkdir;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public List<String> getIncludes() {
		return includes;
	}
	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}
	public List<String> getExcludes() {
		return excludes;
	}
	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}
	public String getOnmoveto() {
		return onmoveto;
	}
	public void setOnmoveto(String onmoveto) {
		this.onmoveto = onmoveto;
	}
	public String getOnmodify() {
		return onmodify;
	}
	public void setOnmodify(String onmodify) {
		this.onmodify = onmodify;
	}
	public String getOnremove() {
		return onremove;
	}
	public void setOnremove(String onremove) {
		this.onremove = onremove;
	}
	public String getOncreate() {
		return oncreate;
	}
	public void setOncreate(String oncreate) {
		this.oncreate = oncreate;
	}
	public String getMkdir() {
		return mkdir;
	}
	public void setMkdir(String mkdir) {
		this.mkdir = mkdir;
	}
}
