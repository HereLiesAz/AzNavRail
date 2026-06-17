package com.hereliesaz.aznavrail.model

/**
 * One entry in the auto-generated About table of contents: a markdown document discovered in the
 * consuming app's GitHub repository (its root or `docs/` folder).
 *
 * @param title Humanized title derived from the filename (e.g. `MIGRATION_GUIDE.md` -> "Migration Guide").
 * @param path Repository-relative path (e.g. `README.md`, `docs/API.md`). Used as a stable key.
 * @param downloadUrl Raw download URL returned by the GitHub contents API, fetched lazily on open.
 */
data class AzDocEntry(
    val title: String,
    val path: String,
    val downloadUrl: String
)

/**
 * A raw entry in the `more-from-az.json` manifest: **just links**. Everything else (name, icon,
 * description) is auto-populated at runtime by resolving these links — the Google Play listing's
 * OpenGraph tags when [play] is present, otherwise the GitHub repository's API metadata.
 *
 * @param github Optional GitHub repository link.
 * @param play Optional Google Play listing link.
 * @param web Optional website / PWA link.
 */
data class AzMoreFromLink(
    val github: String? = null,
    val play: String? = null,
    val web: String? = null
)

/**
 * A fully-resolved app shown in the "More from Az" carousel: the link entry enriched with metadata
 * fetched from the link itself.
 *
 * @param name Display name (Play title or GitHub repo name).
 * @param iconUrl Icon image URL (Play `og:image` or GitHub owner avatar), loaded with Coil.
 * @param description Short description (Play `og:description` or GitHub repo description).
 * @param githubUrl Optional GitHub repository link.
 * @param playStoreUrl Optional Google Play listing link.
 * @param webUrl Optional website / PWA link.
 */
data class AzMoreFromApp(
    val name: String,
    val iconUrl: String,
    val description: String,
    val githubUrl: String? = null,
    val playStoreUrl: String? = null,
    val webUrl: String? = null
)
