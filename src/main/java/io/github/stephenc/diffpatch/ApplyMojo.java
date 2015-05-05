package io.github.stephenc.diffpatch;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephen Connolly
 */
@Mojo(name="apply",threadSafe = true)
public class ApplyMojo extends AbstractMojo {
    @Parameter(defaultValue = "src/main/patches")
    private File patchDirectory;
    
    /**
     * The character encoding scheme to be applied when applying patches.
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        File[] patches = patchDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".patch");
            }
        });
        if (patches == null) return;
        for (File p: patches) {
            List<String> lines;
            try {
                lines = FileUtils.readLines(p, encoding);
            } catch (IOException e) {
                throw new MojoExecutionException(String.format("Could not read %s", p), e);
            }
            List<String> diff = new ArrayList<String>(lines.size());
            String oldFile = null;
            String newFile = null;
            slurping:
            for (String line: lines) {
                if (newFile == null && line.startsWith("+++ ")) {
                    int endIndex = line.indexOf(4, '\t');
                    newFile = line.substring(4, endIndex == -1 ? line.length() : endIndex);
                    diff.add(line);
                    continue;
                }
                if (oldFile == null && line.startsWith("--- ")) {
                    int endIndex = line.indexOf(4, '\t');
                    oldFile = line.substring(4, endIndex == -1 ? line.length() : endIndex);
                    continue;
                }
                if (!line.isEmpty()) {
                    switch (line.charAt(0)) {
                        case '@':
                        case '+':
                        case '-':
                        case ' ':
                            diff.add(line);
                            continue slurping;
                        default:
                            break;
                    }
                }
                if (!diff.isEmpty()) {
                    Patch patch = DiffUtils.parseUnifiedDiff(diff);
                    try {
                        getLog().info(String.format("Patching %s to %s", oldFile, newFile));
                        FileUtils.writeLines(new File(newFile), encoding, patch.applyTo(
                                FileUtils.readLines(new File(oldFile), encoding)));
                    } catch (IOException e) {
                        throw new MojoExecutionException("Could not apply patch", e);
                    } catch (PatchFailedException e) {
                        throw new MojoExecutionException("Could not apply patch", e);
                    }
                }
                diff.clear();
                oldFile = null;
                newFile = null;
            }
            if (!diff.isEmpty()) {
                Patch patch = DiffUtils.parseUnifiedDiff(diff);
                try {
                    getLog().info(String.format("Patching %s to %s", oldFile, newFile));
                    FileUtils.writeLines(new File(newFile), encoding, patch.applyTo(
                            FileUtils.readLines(new File(oldFile), encoding)));
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not apply patch", e);
                } catch (PatchFailedException e) {
                    throw new MojoExecutionException("Could not apply patch", e);
                }
            }
        }
    }
}
