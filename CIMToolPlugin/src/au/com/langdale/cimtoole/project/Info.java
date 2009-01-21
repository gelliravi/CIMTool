/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;

import au.com.langdale.cimtoole.CIMToolPlugin;
/**
 * A set of utilities that define the file locations, file types,  
 * properties and preferences used in a CIMTool project. 
 */
public class Info {

	// properties and preferences
	public static final QualifiedName PROFILE_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "profile_path");
	public static final QualifiedName BASE_MODEL_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "base_model_path");
	public static final QualifiedName PROFILE_ENVELOPE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "profile_envelope");
	public static final QualifiedName SCHEMA_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "schema_namespace");
	public static final QualifiedName PROFILE_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "profile_namespace");
	public static final QualifiedName INSTANCE_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "instance_namespace");
	public static final QualifiedName MERGED_SCHEMA_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "merged_schema_path");
	public static final QualifiedName PRESERVE_NAMESPACES = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "preserve_namespaces");
	public static final QualifiedName USE_PACKAGE_NAMES = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "user_package_names");
	public static final QualifiedName PROBLEM_PER_SUBJECT = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "problem_per_subject");
	
	public static final String WIZARD_JOBS = "WIZARD_JOBS";

	public static boolean isProfile(IResource resource) {
		return isFile(resource, "Profiles", "owl", "n3");
	}

	public static boolean isRuleSet(IResource resource) {
		return isFile(resource, "Profiles", "split-rules", "inc-rules");
	}

	public static boolean isRuleSet(IResource resource, String type) {
		return isFile(resource, "Profiles", type, null);
	}

	public static boolean isSchema(IResource resource) {
		return isFile(resource, "Schema", "owl", "xmi");
	}

	public static boolean isSchemaFolder(IResource resource) {
		return isFolder(resource, "Schema");
	}

	public static boolean isInstance(IResource resource) {
		return isFile(resource, "Instances", "rdf", "xml");
	}

	public static boolean isSplitInstance(IResource resource) {
		return isFolder(resource, "Instances");
	}

	public static boolean isIncremental(IResource resource) {
		return isFolder(resource, "Incremental");
	}

	public static boolean isDiagnostic(IResource resource) {
		return isFile(resource, "Instances", "diagnostic", null);
	}

	public static boolean isFile(IResource resource, String location, String type1, String type2) {
		IPath path = resource.getProjectRelativePath();
		String ext = path.getFileExtension();
		return (resource instanceof IFile) && path.segmentCount() == 2 && path.segment(0).equals(location)
				&& ext != null && (ext.equals(type1) || type2 != null && ext.equals(type2));
	}

	public static boolean isFolder(IResource resource, String location) {
		IPath path = resource.getProjectRelativePath();
		return (resource instanceof IFolder) && path.segmentCount() == 2 && path.segment(0).equals(location);
	}
/*
	public static boolean isXMI(IFile file) {
		String ext = file.getFileExtension();
		if( ext == null)
			return false;
		ext = ext.toLowerCase();
		return ext.equals("xmi");
	}
*/
	public static boolean isParseable(IFile file) {
		String ext = file.getFileExtension();
		if( ext == null)
			return false;
		ext = ext.toLowerCase();
		
		return 
			ext.equals("xmi") || 
			ext.equals("owl") || 
			ext.equals("n3") || 
			ext.equals("simple-owl") || 
			ext.equals("merged-owl") || 
			ext.equals("diagnostic");
	}
	
	public static IFile findMasterFor(IFile file) {
		String ext = file.getFileExtension();
		if( ext != null &&  ext.equalsIgnoreCase("annotation")) {
			IFile master = getRelated(file, "xmi");
			if( master.exists())
				return master;
		}
		return null;
	}

	public static IFolder getRelatedFolder(IResource file) {
		IPath path = file.getFullPath().removeFileExtension();
		return file.getWorkspace().getRoot().getFolder(path);
	}

	public static IFile getRelated(IResource file, String ext) {
		IPath path = file.getFullPath().removeFileExtension().addFileExtension(ext);
		return file.getWorkspace().getRoot().getFile(path);
	}

	public static IFolder getSchemaFolder(IProject project) {
		return project != null? project.getFolder("Schema"): null;
	}

	public static IFolder getProfileFolder(IProject project) {
		return project != null? project.getFolder("Profiles"): null;
	}

	public static IFolder getInstanceFolder(IProject project) {
		return project != null? project.getFolder("Instances"): null;
	}

	public static IFolder getIncrementalFolder(IProject project) {
		return project != null? project.getFolder("Incremental"): null;
	}
	
	public static IResource getInstanceFor(IResource result) {
		IResource instance = getRelated(result, "ttl");
		if( !instance.exists())
			instance = getRelated(result, "rdf");
		if( !instance.exists())
			instance = getRelated(result, "xml");
		if( !instance.exists())
			instance = getRelatedFolder(result);
		if( ! instance.exists()) {
			instance = null;
		}
		return instance;
		
	}

	public static IFile getProfileFor(IResource resource) throws CoreException {
		IResource instance = getBaseModelFor(resource);
		if( instance != null)
			resource = instance;
		
		String path = getProperty(Info.PROFILE_PATH, resource);
		if( path.length() == 0)
			return null;
		
		IFile profile = getProfileFolder(resource.getProject()).getFile(path);
		if( ! profile.exists())
			return null;
		
		return profile;
	}
	
	public static IFile getRulesFor(IResource resource) throws CoreException {
		IFile profile = getProfileFor(resource);
		if(profile == null)
			return null;
		
		String type;
		if( isInstance(resource))
			type = "simple-rules";
		else if( isSplitInstance(resource))
			type = "split-rules";
		else if( isIncremental(resource))
			type = "inc-rules";
		else
			return null;
		
		IFile rules = getRelated(profile, type);
		if( ! rules.exists())
			return null;
		
		return rules;
	}
	
	public static IResource getBaseModelFor(IResource resource) throws CoreException {
		String path = getProperty(Info.BASE_MODEL_PATH, resource);
		if( path.length() == 0)
			return null;
		
		IFolder instance = getInstanceFolder(resource.getProject()).getFolder(path);
		if( ! instance.exists())
			return null;
		
		return instance;
	}

	public static String getInheritedProperty(QualifiedName symbol, IFile file)	throws CoreException {
		String path = file.getPersistentProperty(symbol);
		if( path == null || path.trim().length() == 0)
			path = file.getParent().getPersistentProperty(symbol);
		if( path == null || path.trim().length() == 0)
			path = getPreference(symbol);
		return path;
	}

	public static String getProperty(QualifiedName symbol, IResource resource) throws CoreException {
		String value = resource.getPersistentProperty(symbol);
		if(value != null && value.trim().length() > 0)
			return value;
		value = getPreference(symbol);
		resource.setPersistentProperty(symbol, value);
		return value;
	}

	public static String getPreference(QualifiedName symbol) {
		return CIMToolPlugin.getDefault().getPluginPreferences().getString(symbol.getLocalName());
	}

	public static boolean getPreferenceOption(QualifiedName symbol) {
		return CIMToolPlugin.getDefault().getPluginPreferences().getBoolean(symbol.getLocalName());
	}

	public static CoreException error(String m) {
		return new CoreException(new Status(Status.ERROR, CIMToolPlugin.PLUGIN_ID, m));
	}

	public static CoreException error(String m, Exception e) {
		return new CoreException(new Status(Status.ERROR, CIMToolPlugin.PLUGIN_ID, m, e));
	}
}
