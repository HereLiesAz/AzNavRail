import React, { useEffect, useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, Linking, BackHandler } from 'react-native';
import AzMarkdownNative from './AzMarkdownNative';
import { MoreFromAzOverlay } from './MoreFromAzOverlay';
import { AzButton } from './AzButton';
import { AzLoad } from './AzLoad';
import { AzButtonShape } from '../types';
import { listDocs, fetchDoc, AzDocEntry } from '../services/githubDocs';

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

/**
 * Full-screen in-app About reader for React Native. Auto-discovers the repo's markdown docs and
 * renders the selected one inline via {@link AzMarkdownNative}; a GitHub button is pinned at the
 * bottom and an optional "More from Az" entry opens the carousel. Themed to match the rail.
 */
export const AboutOverlay: React.FC<AboutOverlayProps> = ({ repoUrl, settings = {}, moreFromAzEnabled, moreFromAzJsonUrl, onDismiss }) => {
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';
  const [state, setState] = useState<DocsState>({ status: 'loading' });
  const [selected, setSelected] = useState<AzDocEntry | null>(null);
  const [body, setBody] = useState<string | null>(null);
  const [showMore, setShowMore] = useState(false);

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (showMore) { setShowMore(false); return true; }
      if (selected) { setSelected(null); return true; }
      onDismiss();
      return true;
    });
    return () => sub.remove();
  }, [selected, showMore, onDismiss]);

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
        <View style={styles.flex}>
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
                  {moreFromAzEnabled && (
                    <TouchableOpacity style={[styles.tocRow, styles.emph, { borderColor: accent }]} onPress={() => setShowMore(true)}>
                      <Text style={[styles.tocText, { color: accent, fontWeight: 'bold' }]}>More from Az</Text>
                    </TouchableOpacity>
                  )}
                </ScrollView>
              )}
              <View style={styles.bottomSpacer} />
              <AzButton text="View on GitHub" color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => Linking.openURL(repoUrl).catch(() => {})} />
            </>
          )}
        </View>
      )}

      {showMore && <MoreFromAzOverlay jsonUrl={moreFromAzJsonUrl} settings={settings} onDismiss={() => setShowMore(false)} />}
    </View>
  );
};

const styles = StyleSheet.create({
  overlay: { ...StyleSheet.absoluteFillObject, zIndex: 3000, paddingTop: '6%', paddingBottom: '10%', paddingHorizontal: 20 },
  flex: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center' },
  title: { flex: 1, fontSize: 30, fontWeight: 'bold', marginHorizontal: 8 },
  icon: { fontSize: 22, paddingHorizontal: 6 },
  toc: { gap: 8, paddingVertical: 12 },
  tocRow: { borderWidth: 1, borderRadius: 12, paddingVertical: 14, paddingHorizontal: 16, marginBottom: 8 },
  emph: { borderWidth: 2 },
  tocText: { fontSize: 18 },
  empty: { opacity: 0.7, paddingVertical: 16, fontSize: 15 },
  banner: { opacity: 0.6, paddingVertical: 8, fontSize: 12 },
  bottomSpacer: { height: 32 },
});
