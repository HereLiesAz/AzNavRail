package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.service.MoreFromAzRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-logic tests for the About reader + More-from-Az parsing/ordering (no network). */
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
    fun `parseLinks reads version and link-only entries`() {
        val (version, links) = MoreFromAzRepository.parseLinks(
            """{ "version": 7, "apps": [ {"github":"https://github.com/a/b"}, {"play":"p","web":"w"}, {} ] }"""
        )
        assertEquals(7, version)
        assertEquals(2, links.size) // the empty entry is dropped
        assertEquals("https://github.com/a/b", links[0].github)
        assertEquals("w", links[1].web)
    }

    @Test
    fun `extractOg pulls opengraph content regardless of attribute order`() {
        val html = """<meta property="og:title" content="My App - Apps on Google Play">""" +
            """<meta content="https://img/icon.png" property="og:image">"""
        assertEquals("My App - Apps on Google Play", MoreFromAzRepository.extractOg(html, "title"))
        assertEquals("https://img/icon.png", MoreFromAzRepository.extractOg(html, "image"))
        assertNull(MoreFromAzRepository.extractOg(html, "description"))
    }
}
