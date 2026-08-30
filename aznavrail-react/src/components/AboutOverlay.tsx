import React, { useEffect, useRef, useState, useMemo } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Linking,
  BackHandler,
  Image,
  FlatList,
  Dimensions,
  NativeScrollEvent,
  NativeSyntheticEvent,
  Animated,
  PanResponder,
} from 'react-native';
import { AzMotion } from '../AzNavRailDefaults';
import AzMarkdownNative from './AzMarkdownNative';
import { AzButton } from './AzButton';
import { AzLoad } from './AzLoad';
import { AzButtonShape } from '../types';
import { AzAuthorProfile, AzDocEntry } from '../services/githubDocs';
import { AzMoreFromApp } from '../services/moreFromAz';
import { azAboutPrefetch } from '../services/aboutPrefetch';
import { useAzAccent, AZ_ACCENT_FALLBACK } from '../AzRailPalette';
import { isSafeExternalUrl } from '../util/AzSafeUrl';

/**
 * The About reader's own palette: dark ground, light ink, in every theme.
 *
 * This is one of the few places in the library that does not follow the host's colour scheme, for
 * the reason given where `surface` is resolved. The host's accent still comes through on headings,
 * links, and the close affordance.
 */
export const AzAboutColors = {
  /** Near-black, very slightly cool, so the accent reads warm against it. */
  Ground: '#101014',
  /** Primary ink. Not pure white — pure white on near-black glares. */
  Ink: '#ECECF0',
  /** Secondary ink for supporting lines. */
  InkMuted: '#B4B4BE',
  /** The grab handle and hairline rules. */
  Hairline: '#3A3A44',
} as const;

/** How far the sheet must be pulled down before releasing it dismisses the reader. */
const AZ_ABOUT_DISMISS_THRESHOLD = 140;

/** Grows the header glyphs' touch targets to something a finger can actually find. */
const AZ_ABOUT_HIT_SLOP = { top: 12, bottom: 12, left: 12, right: 12 };

/** Where the About page's tip button sends anyone who wants to leave one. */
const AZ_DONATE_URL = 'https://paypal.me/HereLiesAz';

/** The free-forever pitch shown above the tip button. */
const AZ_TIP_PITCH =
  'Every single one of my apps is available on Github in full, without ads or conditions of ' +
  'any kind, for free and forever. But I never say no to just a the-tip.';

interface AboutOverlayProps {
  repoUrl: string;
  settings?: { activeColor?: string; translucentBackground?: string };
  moreFromAzEnabled?: boolean;
  moreFromAzJsonUrl: string;
  onDismiss: () => void;
}

type DocsState =
  | { status: 'loading' }
  | { status: 'loaded'; entries: AzDocEntry[]; offline: boolean }
  | { status: 'error' };

const HERO_LARGE = 132;
const HERO_MEDIUM = 96;
const HERO_SMALL = 64;
const HERO_SPACING = 12;
const CAROUSEL_ROW_HEIGHT = HERO_LARGE + 24;

/**
 * In-app About reader.
 *
 * Layout is vertically stacked, each section sharing the screen equally:
 *  - **Docs** — auto-generated table of contents of the app's markdown docs.
 *  - **More from Az** (when enabled) — a focused-hero carousel (small · medium · LARGE · medium ·
 *    small) with the active app's banner (when present), name, description, and link buttons.
 *  - **Tip jar, author, and contact** — the free-forever pitch and tip button, the author's GitHub
 *    avatar/name/bio, and the @HereLiesAz/Feedback/website links, stacked in large type. Scrolls
 *    independently of the sections above it.
 */
export const AboutOverlay: React.FC<AboutOverlayProps> = ({
  repoUrl,
  settings = {},
  moreFromAzEnabled,
  moreFromAzJsonUrl,
  onDismiss,
}) => {
  // The reader wears the rail's colour, not the app's theme. `activeColor` when the developer set
  // one; failing that, the colour the rail's own items are drawn in (see `resolveRailAccent`).
  const railAccent = useAzAccent(AZ_ACCENT_FALLBACK);
  const accent = settings.activeColor || railAccent;
  // Dark ground, light ink, in every theme. The reader is a full-screen surface the user has
  // stepped *aside* into — long-form prose, a document list, a carousel — and long-form reading on
  // a bright white field is the wrong call regardless of what the surrounding app is doing. It used
  // to default to '#ffffff'. A host-supplied translucentBackground still wins outright.
  // `translucentBackground` supplies the hue; its alpha is not honoured, because a see-through
  // full-screen reader is an unreadable one.
  const surface = settings.translucentBackground || AzAboutColors.Ground;
  const [state, setState] = useState<DocsState>({ status: 'loading' });
  const [selected, setSelected] = useState<AzDocEntry | null>(null);
  const [body, setBody] = useState<string | null>(null);
  const [moreApps, setMoreApps] = useState<AzMoreFromApp[] | null>(null);
  const [authorProfile, setAuthorProfile] = useState<AzAuthorProfile | null>(
    azAboutPrefetch.authorProfile
  );

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (selected) {
        setSelected(null);
        return true;
      }
      onDismiss();
      return true;
    });
    return () => sub.remove();
  }, [selected, onDismiss]);

  // The reader reads what the rail warmed in the background (see `aboutPrefetch`) rather than
  // starting its own fetch and making the user watch it. A page opened while a cold-start fetch is
  // still in flight fills itself in the moment it lands.
  useEffect(() => {
    let active = true;
    const apply = () => {
      const warmed = azAboutPrefetch.docsFor(repoUrl);
      if (!active || !warmed) return false;
      setState({
        status: 'loaded',
        entries: warmed.entries,
        offline: warmed.offline,
      });
      return true;
    };
    if (!apply()) {
      const unsubscribe = azAboutPrefetch.subscribe(apply);
      azAboutPrefetch
        .warmDocs(repoUrl)
        .then(() => {
          if (active && !apply()) setState({ status: 'error' });
        })
        .catch(() => active && setState({ status: 'error' }));
      return () => {
        active = false;
        unsubscribe();
      };
    }
    return () => {
      active = false;
    };
  }, [repoUrl]);

  useEffect(() => {
    if (!selected) {
      setBody(null);
      return;
    }
    let active = true;
    // The warmed body when there is one (the first doc always is), so the common case renders on
    // the frame the row is tapped.
    const warmed = azAboutPrefetch.bodyFor(selected);
    setBody(warmed);
    if (warmed) return;
    azAboutPrefetch
      .warmDoc(selected)
      .then(
        () =>
          active &&
          setBody(
            azAboutPrefetch.bodyFor(selected) ??
              '_Could not load this document._'
          )
      );
    return () => {
      active = false;
    };
  }, [selected]);

  useEffect(() => {
    if (!moreFromAzEnabled) {
      setMoreApps([]);
      return;
    }
    let active = true;
    const warmed = azAboutPrefetch.moreAppsFor(moreFromAzJsonUrl);
    if (warmed) {
      setMoreApps(warmed);
      return () => {
        active = false;
      };
    }
    azAboutPrefetch
      .warmMoreFromAz(moreFromAzJsonUrl)
      .then(
        () =>
          active &&
          setMoreApps(azAboutPrefetch.moreAppsFor(moreFromAzJsonUrl) ?? [])
      )
      .catch(() => active && setMoreApps([]));
    return () => {
      active = false;
    };
  }, [moreFromAzEnabled, moreFromAzJsonUrl]);

  // The author profile (avatar + bio), fetched live from GitHub. Read whatever was already warmed,
  // subscribe for it landing mid-flight, and kick off the fetch if nothing warmed it yet.
  useEffect(() => {
    const apply = () => setAuthorProfile(azAboutPrefetch.authorProfile);
    apply();
    const unsubscribe = azAboutPrefetch.subscribe(apply);
    void azAboutPrefetch.warmAuthorProfile();
    return unsubscribe;
  }, []);

  // Drag-down-to-dismiss. A full-screen reader that can only be left through one small glyph is a
  // room with a keyhole for a door; this gives the whole surface a way out. The sheet follows the
  // finger, springs back if the pull was not committed, and leaves if it was.
  const dragY = useRef(new Animated.Value(0)).current;
  const dragValue = useRef(0);
  const dismissResponder = useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: (_e, g) =>
          g.dy > 6 && Math.abs(g.dy) > Math.abs(g.dx),
        onPanResponderMove: (_e, g) => {
          dragValue.current = Math.max(0, g.dy);
          dragY.setValue(dragValue.current);
        },
        onPanResponderRelease: () => {
          if (dragValue.current > AZ_ABOUT_DISMISS_THRESHOLD) {
            onDismiss();
            return;
          }
          Animated.timing(dragY, {
            toValue: 0,
            duration: AzMotion.PanelDurationMs,
            useNativeDriver: true,
          }).start();
          dragValue.current = 0;
        },
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [onDismiss]
  );

  return (
    <Animated.View
      {...dismissResponder.panHandlers}
      style={[
        styles.overlay,
        { backgroundColor: surface, transform: [{ translateY: dragY }] },
      ]}
    >
      {/* The grab handle — the visible half of the drag-to-dismiss gesture. Without it the gesture
          exists but nothing announces it. */}
      <View style={styles.grabHandle} />
      <View style={styles.header}>
        {selected && (
          <TouchableOpacity
            onPress={() => setSelected(null)}
            accessibilityLabel="Back to contents"
            accessibilityRole="button"
            hitSlop={AZ_ABOUT_HIT_SLOP}
            style={styles.iconTarget}
          >
            <Text style={[styles.icon, { color: accent }]}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={[styles.title, { color: accent }]} numberOfLines={1}>
          {selected ? selected.title : 'About'}
        </Text>
        <TouchableOpacity
          onPress={onDismiss}
          accessibilityLabel="Close"
          accessibilityRole="button"
          hitSlop={AZ_ABOUT_HIT_SLOP}
          style={styles.iconTarget}
        >
          <Text style={[styles.icon, { color: accent }]}>✕</Text>
        </TouchableOpacity>
      </View>

      {selected ? (
        body === null ? (
          <AzLoad />
        ) : (
          <ScrollView style={styles.flex}>
            <AzMarkdownNative markdown={body} accent={accent} />
          </ScrollView>
        )
      ) : (
        <>
          {/* TOP HALF — docs TOC. */}
          <View style={styles.half}>
            {state.status === 'loading' && <AzLoad />}
            {state.status === 'error' && (
              <Text style={styles.empty}>Couldn't load documentation.</Text>
            )}
            {state.status === 'loaded' && (
              <>
                {state.offline && (
                  <Text style={styles.banner}>
                    Showing cached docs (offline or rate-limited).
                  </Text>
                )}
                {state.entries.length === 0 ? (
                  <Text style={styles.empty}>
                    No documentation found in this repository.
                  </Text>
                ) : (
                  <ScrollView
                    style={styles.flex}
                    contentContainerStyle={styles.toc}
                  >
                    {state.entries.map((e) => (
                      <TouchableOpacity
                        key={e.path}
                        style={[styles.tocRow, { borderColor: accent }]}
                        onPress={() => setSelected(e)}
                      >
                        <Text style={[styles.tocText, { color: accent }]}>
                          {e.title}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </ScrollView>
                )}
              </>
            )}
          </View>

          {/* MIDDLE — focused-hero More-from-Az carousel + active-app info panel ("the app links"). */}
          {moreFromAzEnabled && (
            <View style={styles.half}>
              <View style={[styles.divider, { backgroundColor: accent }]} />
              <MoreFromAzHeroCarousel apps={moreApps} accent={accent} />
            </View>
          )}

          {/* BOTTOM — the tip pitch, the author, and the way to reach them. Scrolls on its own so it
              always has room for all three regardless of how much the sections above claim; the page
              ends where every other surface in this library ends: a way to write to the author, and
              the author. About is where someone goes to find out who made this, so making them hunt
              through a menu for that would be a joke at their expense. */}
          <View style={[styles.divider, { backgroundColor: accent }]} />
          <ScrollView
            style={styles.half}
            contentContainerStyle={styles.footerScroll}
          >
            <AzTipJar accent={accent} />
            <View style={styles.tipToAuthorGap} />
            <AzAuthorHeader accent={accent} profile={authorProfile} />
            <View style={[styles.divider, { backgroundColor: accent }]} />
            <View style={styles.pageFooter}>
              <AzAboutFooterLink
                text="@HereLiesAz"
                color={accent}
                onPress={() =>
                  Linking.openURL('https://instagram.com/HereLiesAz').catch(
                    () => {}
                  )
                }
              />
              <AzAboutFooterLink
                text="Feedback"
                color={accent}
                onPress={() =>
                  Linking.openURL(
                    'mailto:hereliesaz@gmail.com?subject=Feedback'
                  ).catch(() => {})
                }
              />
              <AzAboutFooterLink
                text="hereliesaz.com"
                color={accent}
                onPress={() =>
                  Linking.openURL('https://hereliesaz.com').catch(() => {})
                }
              />
            </View>
          </ScrollView>
        </>
      )}
    </Animated.View>
  );
};

/**
 * The free-forever pitch and its tip button — sits between the app carousel and the author header,
 * where "here's more of my work" naturally turns into "here's how you can thank me for it".
 */
const AzTipJar: React.FC<{ accent: string }> = ({ accent }) => (
  <View style={styles.tipJar}>
    <Text style={styles.tipText}>{AZ_TIP_PITCH}</Text>
    <View style={styles.tipButtonGap} />
    <AzButton
      text="Leave a Tip"
      color={accent}
      shape={AzButtonShape.RECTANGLE}
      onClick={() => Linking.openURL(AZ_DONATE_URL).catch(() => {})}
    />
  </View>
);

/**
 * The author header: GitHub avatar, name, and bio. The avatar and bio are fetched live from the
 * GitHub users API (see `azAboutPrefetch.warmAuthorProfile`) rather than baked in, so this stays
 * current with no release needed; the name is fixed — it is always "Az" regardless of what GitHub's
 * `name` field says.
 */
const AzAuthorHeader: React.FC<{
  accent: string;
  profile: AzAuthorProfile | null;
}> = ({ accent, profile }) => (
  <View style={styles.authorHeader}>
    <View style={[styles.authorAvatar, { borderColor: accent }]}>
      {profile?.avatarUrl ? (
        <Image
          source={{ uri: profile.avatarUrl }}
          style={styles.authorAvatarImage}
        />
      ) : (
        <Text style={[styles.authorAvatarFallback, { color: accent }]}>AZ</Text>
      )}
    </View>
    <Text style={styles.authorName}>Az</Text>
    {!!profile?.bio && <Text style={styles.authorBio}>{profile.bio}</Text>}
  </View>
);

/** One big, centered link row in the About page's own footer — see `AboutOverlay`. */
const AzAboutFooterLink: React.FC<{
  text: string;
  color: string;
  onPress: () => void;
}> = ({ text, color, onPress }) => (
  <TouchableOpacity onPress={onPress} style={styles.footerLinkRow}>
    <Text style={[styles.footerLinkText, { color }]}>{text}</Text>
  </TouchableOpacity>
);

const MoreFromAzHeroCarousel: React.FC<{
  apps: AzMoreFromApp[] | null;
  accent: string;
}> = ({ apps, accent }) => {
  const [activeIndex, setActiveIndex] = useState(0);
  const listRef = useRef<FlatList<AzMoreFromApp>>(null);
  const width = Dimensions.get('window').width;
  const snapInterval = HERO_LARGE + HERO_SPACING;
  const sidePadding = Math.max(0, (width - HERO_LARGE) / 2);

  if (apps === null)
    return (
      <View style={styles.flex}>
        <AzLoad />
      </View>
    );
  if (apps.length === 0)
    return <Text style={styles.empty}>No apps to show right now.</Text>;

  const activeApp = apps[activeIndex];

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const offset = e.nativeEvent.contentOffset.x;
    const idx = Math.max(
      0,
      Math.min(apps.length - 1, Math.round(offset / snapInterval))
    );
    if (idx !== activeIndex) setActiveIndex(idx);
  };

  /** Pulls whatever the gesture left half-centred the rest of the way in. */
  const onSettle = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const offset = e.nativeEvent.contentOffset.x;
    const idx = Math.max(
      0,
      Math.min(apps.length - 1, Math.round(offset / snapInterval))
    );
    const drift = Math.abs(offset - idx * snapInterval);
    if (drift > 1) {
      listRef.current?.scrollToOffset({
        offset: idx * snapInterval,
        animated: true,
      });
    }
    if (idx !== activeIndex) setActiveIndex(idx);
  };

  /** Tapping a card that isn't centred brings it to the centre. */
  const centerOn = (index: number) => {
    listRef.current?.scrollToOffset({
      offset: index * snapInterval,
      animated: true,
    });
    setActiveIndex(index);
  };

  return (
    <View style={styles.flex}>
      <View style={{ height: CAROUSEL_ROW_HEIGHT, justifyContent: 'center' }}>
        <FlatList
          ref={listRef}
          data={apps}
          horizontal
          showsHorizontalScrollIndicator={false}
          snapToInterval={snapInterval}
          snapToAlignment="start"
          // One flick should hand focus to the next app or two, not spin the row past a dozen:
          // `disableIntervalMomentum` stops the fling at the neighbouring card instead of letting
          // momentum carry it, which is what makes the carousel land ON an app rather than between.
          disableIntervalMomentum
          onMomentumScrollEnd={onSettle}
          decelerationRate="fast"
          keyExtractor={(_, i) => String(i)}
          contentContainerStyle={{
            paddingHorizontal: sidePadding,
            alignItems: 'center',
          }}
          ItemSeparatorComponent={() => (
            <View style={{ width: HERO_SPACING }} />
          )}
          onScroll={onScroll}
          scrollEventThrottle={16}
          renderItem={({ item, index }) => {
            const distance = Math.abs(index - activeIndex);
            const size =
              distance === 0
                ? HERO_LARGE
                : distance === 1
                  ? HERO_MEDIUM
                  : HERO_SMALL;
            const isActive = index === activeIndex;
            return (
              <TouchableOpacity
                onPress={() => {
                  if (isActive) {
                    const url =
                      item.webUrl || item.playStoreUrl || item.githubUrl;
                    if (url && isSafeExternalUrl(url)) {
                      Linking.openURL(url).catch(() => {});
                    }
                  } else {
                    centerOn(index);
                  }
                }}
                style={[
                  styles.heroCard,
                  {
                    width: size,
                    height: size,
                    borderColor: isActive ? accent : accent + '66',
                    borderWidth: isActive ? 2 : 1,
                  },
                ]}
              >
                {isAppIcon(item.iconUrl) ? (
                  <Image
                    source={{ uri: item.iconUrl }}
                    style={styles.heroImage}
                  />
                ) : (
                  <Text style={[styles.heroInitials, { color: accent }]}>
                    {item.name.slice(0, 2).toUpperCase()}
                  </Text>
                )}
              </TouchableOpacity>
            );
          }}
        />
      </View>
      {activeApp && <ActiveAppPanel app={activeApp} accent={accent} />}
    </View>
  );
};

const ActiveAppPanel: React.FC<{ app: AzMoreFromApp; accent: string }> = ({
  app,
  accent,
}) => {
  const open = (u?: string) => {
    if (!u || !isSafeExternalUrl(u)) return;
    Linking.openURL(u).catch(() => {});
  };
  return (
    <ScrollView
      style={styles.flex}
      contentContainerStyle={{ paddingVertical: 12 }}
    >
      {app.bannerUrl ? (
        <Image
          source={{ uri: app.bannerUrl }}
          style={styles.banner96}
          resizeMode="cover"
        />
      ) : null}
      <Text style={styles.appName}>{app.name}</Text>
      {!!app.description && (
        <Text style={styles.appDesc}>{app.description}</Text>
      )}
      <View style={styles.appActions}>
        {app.playStoreUrl ? (
          <AzButton
            text="Play"
            color={accent}
            shape={AzButtonShape.RECTANGLE}
            onClick={() => open(app.playStoreUrl)}
          />
        ) : null}
        {app.webUrl ? (
          <AzButton
            text={app.isPwa ? 'Open' : 'Website'}
            color={accent}
            shape={AzButtonShape.RECTANGLE}
            onClick={() => open(app.webUrl)}
          />
        ) : null}
        {app.githubUrl ? (
          <AzButton
            text="GitHub"
            color={accent}
            shape={AzButtonShape.RECTANGLE}
            onClick={() => open(app.githubUrl)}
          />
        ) : null}
      </View>
    </ScrollView>
  );
};

function isAppIcon(url: string): boolean {
  return !!url && !url.includes('avatars.githubusercontent.com');
}

const styles = StyleSheet.create({
  grabHandle: {
    alignSelf: 'center',
    width: 36,
    height: 4,
    borderRadius: 2,
    marginBottom: 10,
    backgroundColor: AzAboutColors.Hairline,
  },
  iconTarget: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  overlay: {
    ...StyleSheet.absoluteFill,
    zIndex: 3000,
    paddingTop: '6%',
    paddingBottom: '10%',
    paddingHorizontal: 20,
  },
  flex: { flex: 1 },
  half: { flex: 1 },
  divider: { height: 1, marginVertical: 8 },
  footerScroll: { paddingVertical: 16 },
  tipJar: { alignItems: 'center' },
  tipText: {
    color: AzAboutColors.InkMuted,
    fontSize: 17,
    textAlign: 'center',
    lineHeight: 24,
  },
  tipButtonGap: { height: 14 },
  tipToAuthorGap: { height: 28 },
  authorHeader: { alignItems: 'center' },
  authorAvatar: {
    width: 88,
    height: 88,
    borderRadius: 44,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  authorAvatarImage: { width: '100%', height: '100%' },
  authorAvatarFallback: { fontSize: 28, fontWeight: 'bold' },
  authorName: {
    color: AzAboutColors.Ink,
    fontSize: 28,
    fontWeight: 'bold',
    marginTop: 14,
  },
  authorBio: {
    color: AzAboutColors.InkMuted,
    fontSize: 17,
    textAlign: 'center',
    marginTop: 6,
    lineHeight: 24,
  },
  pageFooter: { alignItems: 'center', gap: 22, marginTop: 4 },
  footerLinkRow: { paddingVertical: 4 },
  footerLinkText: { fontSize: 24, fontWeight: '600' },
  header: { flexDirection: 'row', alignItems: 'center' },
  title: { flex: 1, fontSize: 30, fontWeight: 'bold', marginHorizontal: 8 },
  icon: { fontSize: 22, paddingHorizontal: 6 },
  toc: { gap: 8, paddingVertical: 12 },
  tocRow: {
    borderWidth: 1,
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  tocText: { fontSize: 18 },
  empty: {
    opacity: 0.7,
    paddingVertical: 16,
    fontSize: 15,
    textAlign: 'center',
  },
  banner: { opacity: 0.6, paddingVertical: 8, fontSize: 12 },
  heroCard: {
    borderRadius: 20,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#00000008',
  },
  heroImage: { width: '100%', height: '100%' },
  heroInitials: { fontSize: 28, fontWeight: 'bold' },
  banner96: { width: '100%', height: 96, borderRadius: 12, marginBottom: 12 },
  appName: { fontSize: 20, fontWeight: '600' },
  appDesc: { fontSize: 14, opacity: 0.75, marginTop: 4 },
  appActions: { flexDirection: 'row', gap: 8, marginTop: 8, flexWrap: 'wrap' },
});
