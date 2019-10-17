package shadowedit.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.w3c.dom.Document;

import shadowedit.Activator;
import shadowedit.util.PluginUtil;
import shadowedit.util.XMLUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class ShadowEditHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IWorkbenchPage activePage = window.getActivePage();
		IEditorPart activeEditor = activePage.getActiveEditor();

		if (activeEditor != null) {
			IEditorInput input = activeEditor.getEditorInput();

			IProject project = input.getAdapter(IProject.class);
			if (project == null) {
				IResource resource = input.getAdapter(IResource.class);
				if (resource != null) {

					project = resource.getProject();
					if (project == null) {
						MessageDialog.openInformation(window.getShell(), "Info", "No project was selected");
						return null;
					} else {
						try {
							PluginUtil.createMetafileOptionally(project);
							IFile file = project.getFile(Activator.METAFILE);
							InputStream xmlin = file.getContents();
							Document doc = XMLUtil.createDocument(xmlin);
							xmlin.close();
							doc.getDocumentElement().setAttribute(" timestamp=", System.currentTimeMillis()+"");
							boolean enabled = true;
							String attr = doc.getDocumentElement().getAttribute("enabled");

							if ("true".equalsIgnoreCase(attr)) {
								doc.getDocumentElement().setAttribute("enabled", "false");
								enabled = false;
							} else {
								doc.getDocumentElement().setAttribute("enabled", "true");
								enabled = true;
							}

							ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
							XMLUtil.transfor(doc, out);
							InputStream in = new ByteArrayInputStream(out.toByteArray());

							file.setContents(in, true, false, null);
							in.close();
							out.close();
							
							MessageDialog.openInformation(window.getShell(), "Info", "shadow edit was "+(enabled?"enabled":"disabled"));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			else {
				MessageDialog.openInformation(window.getShell(), "Info", "No project was selected, please open project file");
			}
		}

		return null;
	}
}
