package shadowedit.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;

import shadowedit.Activator;
import shadowedit.monitor.ScriptExecutionChangeListener;

public class PluginUtil {
	private PluginUtil(){
		
	}
	public static void createMetafileOptionally(final IProject project) {
		IFile file = project.getFile(Activator.METAFILE);
		
		if (!file.exists()) {
			Activator.out.printf("Project %s does not contains shadow edit metafile %s, create it.\n", project.getName(), Activator.METAFILE);
			try {
				InputStream in = ScriptExecutionChangeListener.class.getResourceAsStream(Activator.META_TEMPLATE);
				file.create(in, true, null);
				in.close();
			} catch (IOException | CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateMetafileTimestamp(IProject project) {
		try {
			IFile file = project.getFile(Activator.METAFILE);
			InputStream fin=file.getContents();
			Document doc = XMLUtil.createDocument(fin);
			fin.close();
			doc.getDocumentElement().setAttribute("timestamp", System.currentTimeMillis()+"");
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
			XMLUtil.transfor(doc, out);
			InputStream in = new ByteArrayInputStream(out.toByteArray());

			file.setContents(in, true, false, null);
			in.close();
			out.close();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
