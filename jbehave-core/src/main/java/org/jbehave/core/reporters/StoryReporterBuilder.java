package org.jbehave.core.reporters;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jbehave.core.parser.StoryLocation;
import org.jbehave.core.reporters.FilePrintStreamFactory.FileConfiguration;

/**
 * <p>
 * A <a href="http://en.wikipedia.org/wiki/Builder_pattern">Builder</a> for
 * {@link StoryReporter}s. It builds a {@link DelegatingStoryReporter}
 * with delegates for a number of formats - mostly file-based ones except
 * {@Format.CONSOLE}. It requires a
 * {@link FilePrintStreamFactory} and provides default delegate instances for
 * each format.
 * </p>
 * <p>
 * To build a reporter for a single story path with default and given formats:
 * <pre>
 * Class&lt;MyStory&gt; storyClass = MyStory.class;
 * StoryPathResolver resolver = new UnderscoredCamelCaseResolver();
 * String storyPath = resolver.resolve(storyClass);
 * FilePrintStreamFactory printStreamFactory = new FilePrintStreamFactory(storyPath);
 * StoryReporter reporter = new StoryReporterBuilder(printStreamFactory)
 * 								.outputLocationClass(storyClass)
 * 								.withDefaultFormats()
 * 								.withFormats(HTML, TXT)
 * 								.build(storyPath);
 * </pre> 
 * </p>
 * <p>The builder is configured to build with the {@link Format#STATS} as default format.  To change the default formats
 * the user can override the method:
 * <pre>
 * new StoryReporterBuilder(printStreamFactory){
 *    protected StoryReporterBuilder withDefaultFormats() {
 *       return withFormats(STATS);
 *    }
 *  }
 * </pre>
 * </p>
 * <p>The builder configures the file-based reporters to output to the default file directory {@link FileConfiguration#OUTPUT_DIRECTORY}
 * as relative to the output location class source. In some case, e.g. with Ant class loader, the code source location may not be 
 * properly set.  In this case, we may specify the absolute output directory.
 * To change the default:
 * <pre>
 * new StoryReporterBuilder(printStreamFactory).outputTo("my-reports").outputAsAbsolute(true)
 * 					.withDefaultFormats().withFormats(HTML,TXT)
 * 					.build(storyPath);
 * </pre>
 * </p> 
 * <p>The builder provides default instances for all reporters.  To change the reporter for a specific instance, 
 * e.g. to report format <b>TXT</b> to <b>.text</b> files and to inject other non-default parameters, 
 * such as keywords for a different locale:
 * <pre>
 * new StoryReporterBuilder(printStreamFactory){
 *   public StoryReporter reporterFor(String storyPath, Format format){
 *       FilePrintStreamFactory factory = new FilePrintStreamFactory(new StoryLocation(storyPath, codeLocationClass));
 *       switch (format) {
 *           case TXT:
 *               factory.useConfiguration(new FileConfiguration("text"));
 *               return new PrintStreamStoryReporter(factory.createPrintStream(), new Properties(), new I18nKeywords(Locale), true);
 *            default:
 *               return super.reporterFor(format);
 *   }
 * }
 * </pre>
 */
public class StoryReporterBuilder {

    public enum Format {
        CONSOLE, IDE_CONSOLE, TXT, HTML, XML, STATS
    }

    private List<Format> formats = new ArrayList<Format>();
    private String outputDirectory = new FileConfiguration().getOutputDirectory();
    private boolean outputAbsolute = new FileConfiguration().isOutputDirectoryAbsolute();
	private Class<?> ouputLocationClass = this.getClass();
	private Properties renderingResources = defaultRenderingResources();

    public StoryReporterBuilder() {
    }

    public static Properties defaultRenderingResources() {
        Properties resources = new Properties();
        resources.setProperty("index", "ftl/jbehave-reports-index.ftl");
        resources.setProperty("single", "ftl/jbehave-reports-single.ftl");
        resources.setProperty("renderedDirectory", "rendered");
        resources.setProperty("defaultFormats", "stats");
        return resources;
	}

	public StoryReporterBuilder outputTo(String outputDirectory){
        this.outputDirectory = outputDirectory;
        return this;
    }
    
    public StoryReporterBuilder outputAsAbsolute(boolean outputAbsolute) {
        this.outputAbsolute = outputAbsolute;
        return this;
    }
    
	public StoryReporterBuilder outputLocationClass(Class<?> outputLocationClass) {
		this.ouputLocationClass = outputLocationClass;
		return this;
	}

    public StoryReporterBuilder withDefaultFormats() {
    	return withFormats(Format.STATS);
    }

    public StoryReporterBuilder withFormats(Format... formats) {
    	this.formats.addAll(asList(formats));
        return this;
    }

    public StoryReporter build(String storyPath) {
        Map<Format, StoryReporter> delegates = new HashMap<Format, StoryReporter>();
    	for (Format format : formats ){
			delegates.put(format, reporterFor(storyPath, format));
    	}
    	return new DelegatingStoryReporter(delegates.values());
    }

	public Map<String,StoryReporter> build(List<String> storyPaths) {
		Map<String,StoryReporter> reporters = new HashMap<String, StoryReporter>();
		for ( String storyPath : storyPaths ){
			reporters.put(storyPath, build(storyPath));
		}
		return reporters;
	}

    public StoryReporter reporterFor(String storyPath, Format format) {
        FilePrintStreamFactory factory = filePrintStreamFactory(storyPath);
        switch (format) {
            case CONSOLE:
                return new ConsoleOutput();
	        case IDE_CONSOLE:
	            return new IdeOnlyConsoleOutput();
            case TXT:
                factory.useConfiguration(fileConfiguration("txt"));
                return new TxtOutput(factory.createPrintStream());
            case HTML:
                factory.useConfiguration(fileConfiguration("html"));
                return new HtmlOutput(factory.createPrintStream());
            case XML:
                factory.useConfiguration(fileConfiguration("xml"));
                return new XmlOutput(factory.createPrintStream());
            case STATS:
                factory.useConfiguration(fileConfiguration("stats"));
                return new PostStoryStatisticsCollector(factory.createPrintStream());
            default:
                throw new UnsupportedReporterFormatException(format);
        }
    }

	private FilePrintStreamFactory filePrintStreamFactory(String storyPath) {
		return new FilePrintStreamFactory(new StoryLocation(storyPath, ouputLocationClass));
	}
    
	public File outputDirectory() {
		return filePrintStreamFactory("").outputDirectory();
	}

	public List<String> formatNames(boolean toLowerCase) {
		List<String> names = new ArrayList<String>();
		for (Format format : formats) {
			String name = format.name();
			if ( toLowerCase ){
				name = name.toLowerCase();
			}
			names.add(name);			
		}
		return names;		
	}

    public Properties renderingResources() {
    	return renderingResources;
    }
    
    public StoryReporterBuilder useRenderingResources(Properties resources){
    	this.renderingResources = resources;
    	return this;
    }

    protected FileConfiguration fileConfiguration(String extension) {
        return new FileConfiguration(outputDirectory, outputAbsolute, extension);
    }

	@SuppressWarnings("serial")
	public static class UnsupportedReporterFormatException extends RuntimeException {

		public UnsupportedReporterFormatException(Format format) {
			super("StoryReporter format " + format + " not supported");
		}

	}

}