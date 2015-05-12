package io.github.stephenc.diffpatch;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies unified diff patches.
 */
@Mojo(name = "apply", threadSafe = true)
public class ApplyMojo extends AbstractMojo {
    @Parameter(defaultValue = "src/main/patches")
    private File patchDirectory;

    @Component
    private MavenProject project;

    /**
     * The character encoding scheme to be applied when applying patches.
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File[] patches = patchDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".patch");
            }
        });
        if (patches == null) {
            return;
        }
        for (File p : patches) {
            getLog().info(String.format("Applying patch %s", p));
            List<String> lines;
            try {
                lines = FileUtils.readLines(p, encoding);
            } catch (IOException e) {
                throw new MojoExecutionException(String.format("Could not read %s", p), e);
            }
            int linesCount = lines.size();
            List<String> diff = new ArrayList<String>(linesCount);
            String oldFile = null;
            String newFile = null;
            slurping:
            for (int i = 0; i < linesCount; i++) {
                String line = lines.get(i);
                if (line.startsWith("--- ") && (i + 1 < linesCount) && lines.get(i + 1).startsWith("+++ ")) {
                    // this is the start of a new header section, so apply any current diff from previous section.
                    apply(diff, oldFile, newFile);

                    int endIndex = line.indexOf(4, '\t');
                    // now start out the next section
                    diff.clear();
                    
                    oldFile = line.substring(4, endIndex == -1 ? line.length() : endIndex);
                    diff.add(line);
                    
                    // consume the +++ line also
                    line = lines.get(++i);
                    endIndex = line.indexOf(4, '\t');
                    newFile = line.substring(4, endIndex == -1 ? line.length() : endIndex);
                    diff.add(line);
                } else if (!line.isEmpty()) {
                    switch (line.charAt(0)) {
                        case '-':
                        case '@':
                        case '+':
                        case ' ':
                            diff.add(line);
                            continue slurping;
                        default:
                            break;
                    }
                }
            }
            apply(diff, oldFile, newFile);
        }
    }

    private void apply(List<String> diff, String oldFile, String newFile) throws MojoExecutionException {
        if (!diff.isEmpty() && newFile != null && oldFile != null) {
            Patch patch = DiffUtils.parseUnifiedDiff(diff);
            try {
                if (oldFile.equals(newFile)) {
                    getLog().info(String.format("  Patching %s", oldFile));
                } else {
                    getLog().info(String.format("  Patching %s to %s", oldFile, newFile));
                }
                FileUtils.writeLines(new File(project.getBasedir(), newFile), encoding, patch.applyTo(
                        FileUtils.readLines(new File(project.getBasedir(), oldFile), encoding)));
            } catch (IOException e) {
                throw new MojoExecutionException("Could not apply patch", e);
            } catch (PatchFailedException e) {
                throw new MojoExecutionException("Could not apply patch", e);
            }
        }
    }
}
