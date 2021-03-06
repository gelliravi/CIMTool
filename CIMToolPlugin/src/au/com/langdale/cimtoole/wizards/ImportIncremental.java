/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.FileField;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.SplitModelImporter;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;
import au.com.langdale.workspace.ResourceUI.InstanceBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class ImportIncremental extends Wizard implements IImportWizard {
	private String pathname = "";
	private String suggestion = "";
	private IFolder base;
	private IResource destin;
	private String namespace;
	private IFolder incrementals;
	
	private ProjectBinding projects = new ProjectBinding();
	private InstanceBinding models = new InstanceBinding();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		projects.setSelected(selection);
		models.setSelected(selection);
	}

	private FurnishedWizardPage select = new FurnishedWizardPage("select", "Import an Incremental Model", null) {
		{
			setDescription(
				"Import an Incremental CIM/XML file."
			);
		}

		@Override
		protected Content createContent() {

			return new Content() {

				final String NAMESPACE = Info.getPreference(Info.INSTANCE_NAMESPACE); 
				
				@Override
				protected Template define() {
					return Grid(
						Group(FileField("source", "File to import:", new String[]{"*.xml", "*.rdf"})),
						Group(
							Grid(
								Group(Label("Namespace URI:"), Field("namespace", NAMESPACE)))),
						Group(Label("Project"), Label("Base Model")),
						Group(CheckboxTableViewer("projects"), CheckboxTableViewer("models"))

					);
				}
				
				@Override
				protected void addBindings() {
					projects.bind("projects", this);
					models.bind("models", this, projects);
				}

				@Override
				public String validate() {
					
					// TODO: replace with TextBinding.
					// the source file
					pathname = getText("source").getText().trim();
					if( pathname.length() == 0)
						return "A file to import must be chosen";
					File source = new File(pathname);
					if( ! source.canRead())
						return "The chosen file cannot be read";
					
					// TODO: replace with TextBinding.
					// the namespace
					namespace = getText("namespace").getText();
					String error = Validators.NAMESPACE.validate(namespace);
					if( error != null)
						return error;
					
					// TODO: replace with TextBinding.
					// choose a profile
					if( !(models.getValue() instanceof IFolder))
						return "A base model for the imported incremental model must be selected.";
					base = (IFolder) models.getValue();
					incrementals = Info.getIncrementalFolder(projects.getProject());
					
					return null;
				}
			};
		}
	};
	
	private FurnishedWizardPage detail = new FurnishedWizardPage("detail", "Model Details", null) {
		{
			setDescription(
				"Create the incremental model."
			);
		}

		@Override
		protected Content createContent() {

			return new Content() {

				@Override
				protected Template define() {
					return Grid(
						Group(Label("Incremental Model file name:"), Field("filename")),
						Group(Label("size", "Source size unknown.")),
						Group(CheckBox("replace", "Replace existing incremental model"))
					);
				}
				
				@Override
				public String validate() {
					// display size
					File source = new File(pathname);
					long length = source.length();
					getLabel("size").setText("Size of source is " + Long.toString(length) + " bytes.");
					
					
					// setup the destination resource
					String filename = getText("filename").getText();
					if( (filename.equals(suggestion)) && pathname.length() > 0) {
						Path path = new Path(pathname);
						suggestion = path.removeFileExtension().lastSegment().replaceAll("[^0-9a-zA-Z._-]", "");
						filename = suggestion;
						setTextValue("filename", filename);
					}
					
					if(filename.length() == 0)
						return "A name is required for the imported incremental model.";
					
					destin = incrementals.getFolder(filename);
					
					// overwrite model
					boolean exists = destin.exists();
					getButton("replace").setEnabled(exists);
					if( exists && ! getButton("replace").getSelection())
						return "An incremental model named " + filename + " already exists. " +
								"Check option to replace.";
					
					return null;
				}
			};
		}
	};
	
	@Override
	public void addPages() {
		addPage(select);
		addPage(detail);
	}

	@Override
	public boolean performFinish() {
		IWorkspaceRunnable op = new SplitModelImporter((IFolder)destin, pathname, namespace, null, base);
		Jobs.runJob(op, incrementals, ("Importing incremental model " + destin.getName()));
		return true;
	}
}
