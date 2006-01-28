import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

import java.net.URL;
import java.util.Properties;
import java.util.Locale;
import java.io.InputStream;
import java.io.IOException;

/** Test internationalization/localization by showing that StringTemplate
 *  easily deals with multiple versions of the same string.  The StringTemplates
 *  and strings properties file are looked up using CLASSPATH.
 */
public class Test {
	static ClassLoader cl = Thread.currentThread().getContextClassLoader();
	public static void main(String[] args) throws IOException {
		// choose a skin or site "look" to present
		String skin = "blue";

		// use Locale to get 2 letter country code for this computer
		Locale locale = Locale.getDefault();
		String defaultLanguage = locale.getLanguage();
		// allow them to override language from argument on command line
		String language = defaultLanguage;
		if ( args.length>0 ) {
			language = args[0];
		}
		// load strings from a properties files like en.strings
		URL stringsFile = cl.getResource(language+".strings");
		if ( stringsFile==null ) {
			System.err.println("can't find strings for language: "+language);
			return;
		}
		Properties strings = new Properties();
		InputStream is = stringsFile.openStream();
		strings.load(is);

		// get a template group rooted at appropriate skin
		String absoluteSkinRootDirectoryName = cl.getResource(skin).getFile();
		StringTemplateGroup templates =
			new StringTemplateGroup("test", absoluteSkinRootDirectoryName);

		// generate some pages; every page gets strings table to pull strings from
		StringTemplate page1ST = templates.getInstanceOf("page1");
		page1ST.setAttribute("strings", strings);
		StringTemplate page2ST = templates.getInstanceOf("page2");
		page2ST.setAttribute("strings", strings);

		// render to text
		System.out.println(page1ST);
		System.out.println(page2ST);
	}
}
