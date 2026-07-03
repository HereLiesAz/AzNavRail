import React, { useEffect, useRef, useState } from 'react';
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
} from 'react-native';
import AzMarkdownNative from './AzMarkdownNative';
import { AzButton } from './AzButton';
import { AzLoad } from './AzLoad';
import { AzButtonShape } from '../types';
import { listDocs, fetchDoc, AzDocEntry } from '../services/githubDocs';
import { fetchMoreFromAz, AzMoreFromApp } from '../services/moreFromAz';

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
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';
  const [state, setState] = useState<DocsState>({ status: 'loading' });
  const [selected, setSelected] = useState<AzDocEntry | null>(null);
  const [body, setBody] = useState<string | null>(null);
  const [moreApps, setMoreApps] = useState<AzMoreFromApp[] | null>(null);

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (selected) { setSelected(null); return true; }
      onDismiss();
      return true;
    });
    return () => sub.remove();
  }, [selected, onDismiss]);

  useEffect(() => {
    let active = true;
    listDocs(repoUrl)
      .then((r) => active && setState({ status: 'loaded', entries: r.entries, offline: r.offline }))
      .catch(() => active && setState({ status: 'error' }));
    return () => { active = false; };
  }, [repoUrl]);

  useEffect(() => {
    if (!selected) { setBody(null); return; }
    let active = true;
    setBody(null);
    fetchDoc(selected).then((b) => active && setBody(b ?? '_Could not load this document._'));
    return () => { active = false; };
  }, [selected]);

  useEffect(() => {
    if (!moreFromAzEnabled) { setMoreApps([]); return; }
    let active = true;
    fetchMoreFromAz(moreFromAzJsonUrl)
      .then((r) => active && setMoreApps(r?.apps ?? []))
      .catch(() => active && setMoreApps([]));
    return () => { active = false; };
  }, [moreFromAzEnabled, moreFromAzJsonUrl]);

  return (
    <View style={[styles.overlay, { backgroundColor: surface }]}>
      <View style={styles.header}>
        {selected && (
          <TouchableOpacity onPress={() => setSelected(null)} accessibilityLabel="Back to contents">
            <Text style={[styles.icon, { color: accent }]}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={[styles.title, { color: accent }]} numberOfLines={1}>{selected ? selected.title : 'About'}</Text>
        <TouchableOpacity onPress={onDismiss} accessibilityLabel="Close">
          <Text style={[styles.icon, { color: accent }]}>✕</Text>
        </TouchableOpacity>
      </View>

      {selected ? (
        body === null ? <AzLoad /> : (
          <ScrollView style={styles.flex}><AzMarkdownNative markdown={body} accent={accent} /></ScrollView>
        )
      ) : (
        <>
          {/* TOP HALF — docs TOC. */}
          <View style={styles.half}>
            {state.status === 'loading' && <AzLoad />}
            {state.status === 'error' && <Text style={styles.empty}>Couldn't load documentation.</Text>}
            {state.status === 'loaded' && (
              <>
                {state.offline && <Text style={styles.banner}>Showing cached docs (offline or rate-limited).</Text>}
                {state.entries.length === 0 ? (
                  <Text style={styles.empty}>No documentation found in this repository.</Text>
                ) : (
                  <ScrollView style={styles.flex} contentContainerStyle={styles.toc}>
                    {state.entries.map((e) => (
                      <TouchableOpacity key={e.path} style={[styles.tocRow, { borderColor: accent }]} onPress={() => setSelected(e)}>
                        <Text style={[styles.tocText, { color: accent }]}>{e.title}</Text>
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
              <View style={styles.divider} />
              <MoreFromAzHeroCarousel apps={moreApps} accent={accent} />
            </View>
          )}
        </>
      )}
    </View>
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

  if (apps === null) return <View style={styles.flex}><AzLoad /></View>;
  if (apps.length === 0) return <Text style={styles.empty}>No apps to show right now.</Text>;

  const activeApp = apps[activeIndex];

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const offset = e.nativeEvent.contentOffset.x;
    const idx = Math.max(0, Math.min(apps.length - 1, Math.round(offset / snapInterval)));
    if (idx !== activeIndex) setActiveIndex(idx);
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
          decelerationRate="fast"
          keyExtractor={(_, i) => String(i)}
          contentContainerStyle={{ paddingHorizontal: sidePadding, alignItems: 'center' }}
          ItemSeparatorComponent={() => <View style={{ width: HERO_SPACING }} />}
          onScroll={onScroll}
          scrollEventThrottle={16}
          renderItem={({ item, index }) => {
            const distance = Math.abs(index - activeIndex);
            const size = distance === 0 ? HERO_LARGE : distance === 1 ? HERO_MEDIUM : HERO_SMALL;
            const isActive = index === activeIndex;
            return (
              <TouchableOpacity
                onPress={() => {
                  if (isActive) {
                    const url = item.webUrl || item.playStoreUrl || item.githubUrl;
                    if (url) Linking.openURL(url).catch(() => {});
                  } else {
                    listRef.current?.scrollToOffset({ offset: index * snapInterval, animated: true });
                    setActiveIndex(index);
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
                  <Image source={{ uri: item.iconUrl }} style={styles.heroImage} />
                ) : (
                  <Text style={[styles.heroInitials, { color: accent }]}>{item.name.slice(0, 2).toUpperCase()}</Text>
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

const ActiveAppPanel: React.FC<{ app: AzMoreFromApp; accent: string }> = ({ app, accent }) => {
  const open = (u?: string) => {
    if (!u) return;
    Linking.openURL(u).catch(() => {});
  };
  return (
    <ScrollView style={styles.flex} contentContainerStyle={{ paddingVertical: 12 }}>
      {app.bannerUrl ? (
        <Image source={{ uri: app.bannerUrl }} style={styles.banner96} resizeMode="cover" />
      ) : null}
      <Text style={styles.appName}>{app.name}</Text>
      {!!app.description && <Text style={styles.appDesc}>{app.description}</Text>}
      <View style={styles.appActions}>
        {app.playStoreUrl ? <AzButton text="Play" color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => open(app.playStoreUrl)} /> : null}
        {app.webUrl ? <AzButton text={app.isPwa ? 'Open' : 'Website'} color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => open(app.webUrl)} /> : null}
        {app.githubUrl ? <AzButton text="GitHub" color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => open(app.githubUrl)} /> : null}
      </View>
    </ScrollView>
  );
};

function isAppIcon(url: string): boolean {
  return !!url && !url.includes('avatars.githubusercontent.com');
}

const styles = StyleSheet.create({
  overlay: { ...StyleSheet.absoluteFillObject, zIndex: 3000, paddingTop: '6%', paddingBottom: '10%', paddingHorizontal: 20 },
  flex: { flex: 1 },
  half: { flex: 1 },
  divider: { height: 1, backgroundColor: '#0000001A', marginVertical: 8 },
  header: { flexDirection: 'row', alignItems: 'center' },
  title: { flex: 1, fontSize: 30, fontWeight: 'bold', marginHorizontal: 8 },
  icon: { fontSize: 22, paddingHorizontal: 6 },
  toc: { gap: 8, paddingVertical: 12 },
  tocRow: { borderWidth: 1, borderRadius: 12, paddingVertical: 14, paddingHorizontal: 16, marginBottom: 8 },
  tocText: { fontSize: 18 },
  empty: { opacity: 0.7, paddingVertical: 16, fontSize: 15, textAlign: 'center' },
  banner: { opacity: 0.6, paddingVertical: 8, fontSize: 12 },
  heroCard: { borderRadius: 20, overflow: 'hidden', justifyContent: 'center', alignItems: 'center', backgroundColor: '#00000008' },
  heroImage: { width: '100%', height: '100%' },
  heroInitials: { fontSize: 28, fontWeight: 'bold' },
  banner96: { width: '100%', height: 96, borderRadius: 12, marginBottom: 12 },
  appName: { fontSize: 20, fontWeight: '600' },
  appDesc: { fontSize: 14, opacity: 0.75, marginTop: 4 },
  appActions: { flexDirection: 'row', gap: 8, marginTop: 8, flexWrap: 'wrap' },
});
