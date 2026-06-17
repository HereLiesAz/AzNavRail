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
    """True if the first non-empty README line contains the whole word WIP (case-insensitive)."""
    for line in (readme or "").splitlines():
        if line.strip():
            return re.search(r"\bWIP\b", line, re.I) is not None
    return False


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


def resolve_github(github_url):
    parsed = parse_repo(github_url)
    if not parsed:
        return None
    owner, repo = parsed
    status, body = http_get(f"https://api.github.com/repos/{owner}/{repo}", token=True)
    if status != 200:
        return None
    meta = json.loads(body)
    branch = meta.get("default_branch") or "HEAD"

    # README — used only for the WIP check.
    for name in ("README.md", "readme.md", "Readme.md", "README.markdown"):
        rs, readme = http_get(f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{name}")
        if rs == 200:
            if is_wip_first_line(readme):
                return None  # exclude WIP apps
            break

    app = {
        "name": meta.get("name") or repo,
        "iconUrl": meta.get("owner", {}).get("avatar_url", ""),
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

    # Website / PWA from the repo homepage.
    home = (meta.get("homepage") or "").strip()
    if home and "github.com" not in home and "play.google.com" not in home:
        if not home.startswith(("http://", "https://")):
            home = "https://" + home
        ws, whtml = http_get(home)
        app["web"] = home
        app["isPwa"] = detect_pwa(whtml) if ws == 200 else False

    return app


# ----------------------------- main -----------------------------

def bake(raw):
    """Resolve the raw manifest text into (version, apps). Performs network I/O."""
    m = re.search(r'"version"\s*:\s*(\d+)', raw)
    version = (int(m.group(1)) if m else 0) + 1

    apps, seen_repo = [], set()
    for url in extract_urls(raw):
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
