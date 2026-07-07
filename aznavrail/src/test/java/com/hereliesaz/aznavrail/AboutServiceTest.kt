package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.service.MoreFromAzRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pure-logic tests for the About reader + More-from-Az parsing/ordering
 * (no network).
 */
@RunWith(RobolectricTestRunner::class)
class AboutServiceTest {

    @Test
    fun `parseRepo extracts owner and repo`() {
        assertEquals("HereLiesAz" to "AzNavRail", GithubDocsRepository.parseRepo("https://github.com/HereLiesAz/AzNavRail"))
        assertEquals("HereLiesAz" to "AzNavRail", GithubDocsRepository.parseRepo("https://github.com/HereLiesAz/AzNavRail.git"))
        assertEquals("a" to "b", GithubDocsRepository.parseRepo("http://github.com/a/b/tree/main"))
    }

    @Test
    fun `parseRepo returns null for non-github`() {
        assertNull(GithubDocsRepository.parseRepo("https://gitlab.com/a/b"))
        assertNull(GithubDocsRepository.parseRepo("not a url"))
    }

    @Test
    fun `repoUrlFromPackage derives the host app repo from its namespace`() {
        assertEquals("https://github.com/hereliesaz/SampleApp", GithubDocsRepository.repoUrlFromPackage("com.hereliesaz.SampleApp"))
        // A trailing build-variant suffix (applicationIdSuffix) is stripped.
        assertEquals("https://github.com/hereliesaz/SampleApp", GithubDocsRepository.repoUrlFromPackage("com.hereliesaz.SampleApp.debug"))
        // The derived URL parses back to the same owner/repo via the existing parser.
        assertEquals("hereliesaz" to "SampleApp", GithubDocsRepository.parseRepo(GithubDocsRepository.repoUrlFromPackage("com.hereliesaz.SampleApp")!!))
    }

    @Test
    fun `repoUrlFromPackage returns null when the namespace is too short`() {
        assertNull(GithubDocsRepository.repoUrlFromPackage("com.example"))
        assertNull(GithubDocsRepository.repoUrlFromPackage("single"))
        assertNull(GithubDocsRepository.repoUrlFromPackage(""))
    }

    @Test
    fun `humanize turns filenames into titles`() {
        assertEquals("Migration Guide", GithubDocsRepository.humanize("MIGRATION_GUIDE.md"))
        assertEquals("Api", GithubDocsRepository.humanize("api.md"))
        assertEquals("Project Structure", GithubDocsRepository.humanize("project-structure.md"))
    }

    @Test
    fun `parseContents keeps only markdown files`() {
        val json = """
            [
              {"type":"file","name":"README.md","path":"README.md","download_url":"https://raw/README.md"},
              {"type":"file","name":"build.gradle","path":"build.gradle","download_url":"https://raw/build.gradle"},
              {"type":"dir","name":"docs","path":"docs","download_url":""},
              {"type":"file","name":"API.md","path":"API.md","download_url":"https://raw/API.md"}
            ]
        """.trimIndent()
        val docs = GithubDocsRepository.parseContents(json)
        assertEquals(2, docs.size)
        assertTrue(docs.all { it.path.endsWith(".md") })
    }

    @Test
    fun `azignore excludes listed docs from the TOC`() {
        val patterns = GithubDocsRepository.parseIgnore("# private\n\nCHANGELOG.md\ndocs/internal/\n*.draft.md\n")
        assertTrue(GithubDocsRepository.isIgnored("CHANGELOG.md", patterns))
        assertTrue(GithubDocsRepository.isIgnored("docs/internal/notes.md", patterns))
        assertTrue(GithubDocsRepository.isIgnored("docs/x.draft.md", patterns))
        assertFalse(GithubDocsRepository.isIgnored("README.md", patterns))
        assertFalse(GithubDocsRepository.isIgnored("docs/API.md", patterns))
    }

    @Test
    fun `findDownloadUrl locates a dotfile in the contents listing`() {
        val json = """[
          {"type":"file","name":"README.md","path":"README.md","download_url":"u-readme"},
          {"type":"file","name":".azignore","path":".azignore","download_url":"u-azignore"},
          {"type":"dir","name":"docs","path":"docs"}
        ]"""
        assertEquals("u-azignore", GithubDocsRepository.findDownloadUrl(json, ".azignore"))
        assertEquals(null, GithubDocsRepository.findDownloadUrl(json, ".aiexclude"))
    }

    @Test
    fun `orderToc puts README first`() {
        val root = GithubDocsRepository.parseContents(
            """[
              {"type":"file","name":"API.md","path":"API.md","download_url":"u"},
              {"type":"file","name":"README.md","path":"README.md","download_url":"u"}
            ]""".trimIndent()
        )
        val docs = GithubDocsRepository.parseContents(
            """[{"type":"file","name":"DSL.md","path":"docs/DSL.md","download_url":"u"}]""".trimIndent()
        )
        val toc = GithubDocsRepository.orderToc(root, docs)
        assertEquals("README", toc.first().title)
        assertEquals("docs/DSL.md", toc.last().path)
    }

    @Test
    fun `parse reads a baked manifest and sorts Play-first`() {
        val baked = """
            { "version": 7, "apps": [
              { "name": "NoPlay", "iconUrl": "", "description": "d", "github": "https://github.com/a/b" },
              { "name": "HasPlay", "iconUrl": "i", "description": "d",
                "github": "https://github.com/a/c",
                "play": "https://play.google.com/store/apps/details?id=com.a.c", "isPwa": true,
                "web": "https://c.example.com" }
            ] }
        """.trimIndent()
        val result = MoreFromAzRepository.parse(baked)
        assertEquals(7, result.version)
        assertEquals(2, result.apps.size)
        assertEquals("HasPlay", result.apps.first().name) // Play app sorted first
        assertEquals("https://c.example.com", result.apps.first().webUrl)
        assertTrue(result.apps.first().isPwa)
    }

    @Test
    fun `parse tolerates an un-baked github-link list (degraded cards)`() {
        val raw = """
            { "version": 2, "apps": [
              "https://github.com/HereLiesAz/AzNavRail",
              "https://github.com/HereLiesAz/CueDetat"
            ] }
        """.trimIndent()
        val result = MoreFromAzRepository.parse(raw)
        assertEquals(2, result.apps.size)
        assertEquals("AzNavRail", result.apps[0].name)
        assertEquals("https://github.com/HereLiesAz/CueDetat", result.apps[1].githubUrl)
    }
}
