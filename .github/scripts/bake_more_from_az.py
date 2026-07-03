#!/usr/bin/env python3
"""Resolve the pasted more-from-az.json link list into a finished, baked manifest.

Input: more-from-az.json containing a list of GitHub repo links (one per line, any order; bare URLs
or an already-baked JSON are both accepted). Output: the same file rewritten as

    { "version": <bumped>, "apps": [ { name, iconUrl, description, github?, play?, web?, isPwa? }, ... ] }

with apps grouped by repository, Play links constructed+verified, website/PWA taken from the repo
homepage, WIP apps dropped, and Play-having apps sorted first.

The pure helpers (no network) are unit-tested in the repo's test suites.
"""
import json
import os
import re
import sys
import urllib.error
import urllib.request

MANIFEST = "more-from-az.json"
URL_RE = re.compile(r"""https?://[^\s"'<>,}\]]+""")
GH_RE = re.compile(r"github\.com/([^/]+)/([^/?#]+)", re.I)
ENTITIES = [("&amp;", "&"), ("&quot;", '"'), ("&#39;", "'"), ("&lt;", "<"), ("&gt;", ">")]


# ----------------------------- pure helpers (tested) -----------------------------

def extract_urls(raw):
    """All URLs in the file, de-duplicated, order preserved (trailing punctuation trimmed)."""
    seen, out = set(), []
    for u in URL_RE.findall(raw):
        u = u.rstrip(".,;")
        if u not in seen:
            seen.add(u)
            out.append(u)
    return out


def parse_repo(url):
    """(owner, repo) for a GitHub URL with a trailing .git stripped, else None."""
    m = GH_RE.search(url)
    if not m:
        return None
    owner, repo = m.group(1), re.sub(r"\.git$", "", m.group(2))
    return (owner, repo) if owner and repo else None


def derive_play_url(github_url):
    """Conventional Play URL com.<owner>.<repo> (lower-cased) for a GitHub repo, else None."""
    parsed = parse_repo(github_url)
    if not parsed:
        return None
    owner, repo = parsed
    return f"https://play.google.com/store/apps/details?id=com.{owner.lower()}.{repo.lower()}"


def is_wip_first_line(readme):
    """True if the first content README line contains the whole word WIP (case-insensitive).

    Leading blank lines and common non-content decorations (HTML comments, badges, images) are
    skipped so the check lands on the real first line of content (usually the title)."""
    for line in (readme or "").splitlines():
        s = line.strip()
        if not s or s.startswith(("<!--", "<img", "![", "[![")):
            continue
        return re.search(r"\bWIP\b", s, re.I) is not None
    return False


def app_links_from_input(raw):
    """Ordered anchor links to resolve, from either a baked JSON manifest or a raw pasted list.

    Crucially, when the input is already baked, only each app's GitHub link (or a standalone
    play/web) is re-emitted — the baked icon/name and a repo's derived play/web are NOT re-processed
    (the repo re-derives them), so re-baking is idempotent and never spawns duplicate/junk cards."""
    try:
        data = json.loads(raw)
        apps = data.get("apps") if isinstance(data, dict) else None
    except Exception:
        apps = None
    if isinstance(apps, list):
        out = []
        for e in apps:
            if isinstance(e, str) and e.strip():
                out.append(e.strip())
            elif isinstance(e, dict):
                anchor = e.get("github") or e.get("play") or e.get("web")
                if isinstance(anchor, str) and anchor:
                    out.append(anchor)
        return out
    # Not a structured manifest (raw paste) -> every URL in the text, one per line / any order.
    return extract_urls(raw)


def og(html, prop):
    """Extract an OpenGraph content value, tolerant of attribute order; decodes basic entities."""
    a = re.search(r"""<meta[^>]+property=["']og:%s["'][^>]+content=["']([^"']*)["']""" % prop, html, re.I)
    b = re.search(r"""<meta[^>]+content=["']([^"']*)["'][^>]+property=["']og:%s["']""" % prop, html, re.I)
    v = a.group(1) if a else (b.group(1) if b else None)
    if not v:
        return None
    for k, r in ENTITIES:
        v = v.replace(k, r)
    return v.strip() or None


def detect_pwa(html):
    """Heuristic: a page is a PWA if it links a web-app manifest."""
    return re.search(r"""rel=["'][^"']*\bmanifest\b[^"']*["']""", html or "", re.I) is not None


def sort_play_first(apps):
    """Stable sort: apps with a Play link first."""
    return sorted(apps, key=lambda a: 0 if a.get("play") else 1)


# ----------------------------- network -----------------------------

def http_get(url, token=False):
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "AzNavRail-bot")
    if "api.github.com" in url:
        req.add_header("Accept", "application/vnd.github+json")
    tok = os.environ.get("GITHUB_TOKEN")
    if token and tok:
        req.add_header("Authorization", f"Bearer {tok}")
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception:
        return None, ""


def resolve_play(url):
    separator = "&" if "?" in url else "?"
    if "hl=" not in url:
        url = f"{url}{separator}hl=en"
    status, html = http_get(url)
    title = og(html, "title")
    if not title:
        return None
    return {
        "name": re.sub(r" - Apps on Google Play$", "", title).strip(),
        "iconUrl": og(html, "image") or "",
        "description": og(html, "description") or "",
        "play": url,
    }


def resolve_web(url):
    status, html = http_get(url)
    if status != 200:
        return None
    title = og(html, "title")
    if not title:
        return None
    return {
        "name": title,
        "iconUrl": og(html, "image") or "",
        "description": og(html, "description") or "",
        "web": url,
        "isPwa": detect_pwa(html),
    }


def _head_ok(url):
    """True if `url` is reachable with a 200-class status. Uses GET (some CDNs 405 on HEAD)."""
    status, _ = http_get(url)
    return status == 200


# Standard Android launcher-icon paths, densities from highest to lowest, webp first for size.
_LAUNCHER_ICON_PATHS = [
    "app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp",
    "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
    "app/src/main/res/mipmap-xxhdpi/ic_launcher.webp",
    "app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
    "app/src/main/res/mipmap-xhdpi/ic_launcher.webp",
    "app/src/main/res/mipmap-xhdpi/ic_launcher.png",
    "app/src/main/res/mipmap-hdpi/ic_launcher.png",
]

# Common banner names (either / or). Case-sensitive lookup — GitHub raw is case-sensitive.
_BANNER_NAMES = [
    "docs/banner.png", "docs/banner.webp", "docs/banner.jpg", "docs/banner.jpeg",
    "docs/Banner.png", "docs/Banner.webp",
    "docs/hero.png", "docs/hero.webp",
]


def resolve_repo_icon(owner, repo, branch):
    """Try to find an app-launcher icon in a GitHub repo. Returns a raw.githubusercontent URL or None.

    Order: standard mipmap paths → GitHub contents API tree walk for `ic_launcher*.{png,webp}` under
    any `res/mipmap-*` dir → adaptive-icon xml parse → repo social preview as a last resort."""
    raw_root = f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}"
    for path in _LAUNCHER_ICON_PATHS:
        url = f"{raw_root}/{path}"
        if _head_ok(url):
            return url

    # Tree-walk the contents API — cheap when the standard paths miss (Compose Multiplatform layouts,
    # non-`app/` roots).  We ask for the whole tree once.
    status, body = http_get(
        f"https://api.github.com/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", token=True,
    )
    if status == 200:
        try:
            tree = json.loads(body).get("tree") or []
        except Exception:
            tree = []
        candidates = []
        for entry in tree:
            path = (entry.get("path") or "") if isinstance(entry, dict) else ""
            if not path or entry.get("type") != "blob":
                continue
            lower = path.lower()
            if "/mipmap-" in lower and lower.rsplit("/", 1)[-1].startswith("ic_launcher") and (
                lower.endswith(".png") or lower.endswith(".webp")
            ):
                candidates.append(path)
        # Prefer highest density.
        def density_key(p):
            for i, d in enumerate(("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")):
                if d in p:
                    return i
            return 99
        candidates.sort(key=density_key)
        if candidates:
            return f"{raw_root}/{candidates[0]}"

        # Adaptive-icon fallback: parse foreground drawable name from the XML.
        for entry in tree:
            path = (entry.get("path") or "") if isinstance(entry, dict) else ""
            if path.endswith("mipmap-anydpi-v26/ic_launcher.xml"):
                status2, xml = http_get(f"{raw_root}/{path}")
                if status2 == 200:
                    m = re.search(r'android:drawable="@(?:mipmap|drawable)/([^"]+)"', xml or "")
                    if m:
                        name = m.group(1)
                        base = path.rsplit("mipmap-anydpi-v26", 1)[0]
                        for ext in ("png", "webp"):
                            for kind in ("drawable", "mipmap-xxxhdpi", "mipmap-xxhdpi"):
                                candidate = f"{base}{kind}/{name}.{ext}"
                                if _head_ok(f"{raw_root}/{candidate}"):
                                    return f"{raw_root}/{candidate}"

    # Last resort: repo social preview (always 200, but is the repo card, not the app icon).
    return f"https://opengraph.githubassets.com/1/{owner}/{repo}"


def resolve_repo_banner(owner, repo, branch):
    """Return a URL for docs/banner.* (or docs/hero.*) if the repo has one, else None."""
    raw_root = f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}"
    for name in _BANNER_NAMES:
        url = f"{raw_root}/{name}"
        if _head_ok(url):
            return url
    return None


def resolve_github(github_url):
    parsed = parse_repo(github_url)
    if not parsed:
        return None
    owner, repo = parsed
    status, body = http_get(f"https://api.github.com/repos/{owner}/{repo}", token=True)
    if status != 200:
        return None
    try:
        meta = json.loads(body)
    except Exception:
        return None
    if not isinstance(meta, dict):
        return None
    branch = meta.get("default_branch") or "HEAD"

    # README — used only for the WIP check.
    for name in ("README.md", "readme.md", "Readme.md", "README.markdown"):
        rs, readme = http_get(f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{name}")
        if rs == 200:
            if is_wip_first_line(readme):
                return None  # exclude WIP apps
            break

    # Icon must be THAT APP's icon — never the owner's GitHub avatar. Sourced (in order) from the
    # Play listing's og:image, else the app website's og:image, else the launcher icon shipped in the
    # repo itself; blank falls back to initials in-app.
    app = {
        "name": meta.get("name") or repo,
        "iconUrl": "",
        "description": meta.get("description") or "",
        "github": github_url,
    }

    # Play link: construct from the package convention, keep only if it resolves to a real listing.
    play_url = derive_play_url(github_url)
    if play_url:
        play = resolve_play(play_url)
        if play:
            app["play"] = play_url
            if play.get("iconUrl"):
                app["iconUrl"] = play["iconUrl"]

    # Website / PWA from the repo homepage — also the icon source for non-Play apps (its og:image).
    home = (meta.get("homepage") or "").strip()
    if home and "github.com" not in home and "play.google.com" not in home:
        if not home.startswith(("http://", "https://")):
            home = "https://" + home
        ws, whtml = http_get(home)
        app["web"] = home
        app["isPwa"] = detect_pwa(whtml) if ws == 200 else False
        if not app["iconUrl"] and ws == 200:
            app["iconUrl"] = og(whtml, "image") or ""

    # Repo fallback for the icon — walk the launcher-icon paths and adaptive-icon xml. Runs whenever
    # Play/website resolvers left the iconUrl blank; also runs when the current value is the owner's
    # avatar (which the runtime blocks) so we replace it with something usable.
    if not app["iconUrl"] or "avatars.githubusercontent.com" in app["iconUrl"]:
        repo_icon = resolve_repo_icon(owner, repo, branch)
        if repo_icon:
            app["iconUrl"] = repo_icon

    # Banner from the repo's docs folder — displayed at the top of the app's info in the About page.
    banner = resolve_repo_banner(owner, repo, branch)
    if banner:
        app["bannerUrl"] = banner

    return app


# ----------------------------- main -----------------------------

def bake(raw):
    """Resolve the raw manifest text into (version, apps). Performs network I/O."""
    m = re.search(r'"version"\s*:\s*(\d+)', raw)
    version = (int(m.group(1)) if m else 0) + 1

    apps, seen_repo = [], set()
    for url in app_links_from_input(raw):
        parsed = parse_repo(url)
        if parsed:
            key = f"{parsed[0].lower()}/{parsed[1].lower()}"
            if key in seen_repo:
                continue
            seen_repo.add(key)
            app = resolve_github(url)
            if app:
                apps.append(app)
        elif "play.google.com" in url:
            a = resolve_play(url)
            if a:
                apps.append(a)
        else:
            a = resolve_web(url)
            if a:
                apps.append(a)
    return version, sort_play_first(apps)


def main():
    raw = open(MANIFEST).read()
    try:
        old = json.loads(raw).get("apps")
    except Exception:
        old = None
    version, apps = bake(raw)
    if apps == old:
        print("more-from-az: no change after resolution; leaving file untouched.")
        return
    with open(MANIFEST, "w") as f:
        f.write(json.dumps({"version": version, "apps": apps}, indent=2) + "\n")
    print(f"Baked {len(apps)} apps; version -> {version}")


if __name__ == "__main__":
    sys.exit(main())
