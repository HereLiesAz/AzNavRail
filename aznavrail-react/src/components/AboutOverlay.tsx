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
import { AzDocEntry } from '../services/githubDocs';
import { AzMoreFromApp } from '../services/moreFromAz';
import { azAboutPrefetch } from '../services/aboutPrefetch';
import { AzFooterLabel } from './AzFooterLabel';
import { useAzAccent, AZ_ACCENT_FALLBACK } from '../AzRailPalette';

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
 * Layout is two vertically-stacked halves:
 *  - **Top half** — auto-generated table of contents of the app's markdown docs.
 *  - **Bottom half** — a focused-hero "More from Az" carousel (small · medium · LARGE · medium · small)
 *    with the active app's banner (when present), name, description, and link buttons under it.
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

          {/* BOTTOM HALF — focused-hero More-from-Az carousel + active-app info panel. */}
          {moreFromAzEnabled && (
            <View style={styles.half}>
              <View style={[styles.divider, { backgroundColor: accent }]} />
              <MoreFromAzHeroCarousel apps={moreApps} accent={accent} />
            </View>
          )}

          {/* The page ends where every other surface in this library ends: a way to write to the
              author, and the author. About is where someone goes to find out who made this, so
              making them close it and hunt through a menu for that would be a joke at their
              expense. */}
          <View style={[styles.divider, { backgroundColor: accent }]} />
          <View style={styles.pageFooter}>
            <AzFooterLabel
              text="@HereLiesAz"
              color={accent}
              style={styles.pageFooterCell}
              onPress={() =>
                Linking.openURL('https://instagram.com/HereLiesAz').catch(
                  () => {}
                )
              }
            />
            <AzFooterLabel
              text="Feedback"
              color={accent}
              style={styles.pageFooterCell}
              onPress={() =>
                Linking.openURL(
                  'mailto:hereliesaz@gmail.com?subject=Feedback'
                ).catch(() => {})
              }
            />
            <AzFooterLabel
              text="hereliesaz.com"
              color={accent}
              style={styles.pageFooterCell}
              onPress={() =>
                Linking.openURL('https://hereliesaz.com').catch(() => {})
              }
            />
          </View>
        </>
      )}
    </Animated.View>
  );
};

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
                    if (url) Linking.openURL(url).catch(() => {});
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
    if (!u) return;
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
  pageFooter: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  pageFooterCell: { flex: 1 },
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
