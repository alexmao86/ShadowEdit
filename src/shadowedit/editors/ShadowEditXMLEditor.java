package shadowedit.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class ShadowEditXMLEditor extends TextEditor {

	private ColorManager colorManager;

	public ShadowEditXMLEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
		setDocumentProvider(new XMLDocumentProvider());
	}
	@Override
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
