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
 * A fully-resolved app shown in the "More from Az" carousel. Resolution (name/icon/description and
 * the Play/website links) is performed in CI and baked into `more-from-az.json`; the rail just reads
 * these.
 *
 * @param name Display name (Play title or GitHub repo name).
 * @param iconUrl Icon image URL (Play `og:image` or GitHub owner avatar), loaded with Coil.
 * @param description Short description (Play `og:description` or GitHub repo description).
 * @param githubUrl Optional GitHub repository link.
 * @param playStoreUrl Optional Google Play listing link.
 * @param webUrl Optional website / PWA link.
 * @param isPwa When true, [webUrl] is a PWA (an "Open" launch button); otherwise it's a "Website".
 */
data class AzMoreFromApp(
    val name: String,
    val iconUrl: String,
    val description: String,
    val githubUrl: String? = null,
    val playStoreUrl: String? = null,
    val webUrl: String? = null,
    val isPwa: Boolean = false
)
