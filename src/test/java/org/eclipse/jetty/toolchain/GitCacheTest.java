package org.eclipse.jetty.toolchain;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GitCacheTest
{
    @Test
    public void testGitCache() throws IOException
    {
        Path cloneDir = Paths.get(System.getProperty("user.dir"));
        Git git = Git.open(cloneDir.toFile());
        GitCache cache = new GitCache(git);
        Set<String> diffPaths = cache.getPaths("2bde336b89bb79952b05fb629da7e57c453e0b5f");
        assertEquals(3, diffPaths.stream().filter((filename) -> filename.startsWith("src/")).count());
        Set<String> branchesContaining = cache.getBranchesContaining("2bde336b89bb79952b05fb629da7e57c453e0b5f");
        assertNotNull(branchesContaining);
    }
}
