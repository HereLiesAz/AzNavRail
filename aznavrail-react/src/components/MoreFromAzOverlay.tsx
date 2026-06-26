import React, { useEffect, useState } from 'react';
import { View, Text, Image, FlatList, TouchableOpacity, StyleSheet, Linking, BackHandler } from 'react-native';
import { AzLoad } from './AzLoad';
import { fetchMoreFromAz, AzMoreFromApp } from '../services/moreFromAz';

interface MoreFromAzOverlayProps {
  jsonUrl: string;
  settings?: { activeColor?: string; translucentBackground?: string };
  onDismiss: () => void;
}

/** The app a card opens when tapped: prefer the website/PWA, then Play, then the GitHub repo. */
const primaryUrl = (a: AzMoreFromApp): string | undefined => a.webUrl || a.playStoreUrl || a.githubUrl;
/** True only for a genuine app icon — never the owner's GitHub avatar (which is not an app icon). */
const isAppIcon = (url: string): boolean => !!url && !url.includes('avatars.githubusercontent.com');

/**
 * Full-screen "More from Az" overlay for React Native: a horizontal carousel of the author's other
 * apps. The cards are not a selection model — **tapping a card opens that app** (its website/PWA, else
 * Play, else GitHub). Each card shows that app's own icon (never the owner's GitHub avatar — a
 * blank/avatar icon falls back to the app's initials) and its name.
 */
export const MoreFromAzOverlay: React.FC<MoreFromAzOverlayProps> = ({ jsonUrl, settings = {}, onDismiss }) => {
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';
  const [apps, setApps] = useState<AzMoreFromApp[] | null>(null);

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => { onDismiss(); return true; });
    return () => sub.remove();
  }, [onDismiss]);

  useEffect(() => {
    let active = true;
    fetchMoreFromAz(jsonUrl).then((r) => active && setApps(r?.apps ?? []));
    return () => { active = false; };
  }, [jsonUrl]);

  const open = (url?: string) => url && Linking.openURL(url).catch(() => {});

  return (
    <View style={[styles.overlay, { backgroundColor: surface }]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onDismiss} accessibilityLabel="Back"><Text style={[styles.icon, { color: accent }]}>←</Text></TouchableOpacity>
        <Text style={[styles.title, { color: accent }]}>More from Az</Text>
      </View>

      {apps === null && <AzLoad />}
      {apps && apps.length === 0 && <Text style={styles.empty}>Couldn't load apps right now.</Text>}

      {apps && apps.length > 0 && (
        <FlatList
          data={apps}
          horizontal
          showsHorizontalScrollIndicator={false}
          keyExtractor={(a, i) => a.githubUrl || a.playStoreUrl || a.webUrl || String(i)}
          contentContainerStyle={styles.carousel}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.cardWrap} onPress={() => open(primaryUrl(item))} accessibilityLabel={item.name}>
              <View style={[styles.card, { borderColor: `${accent}66` }]}>
                {isAppIcon(item.iconUrl) ? (
                  <Image source={{ uri: item.iconUrl }} style={styles.cardImg} resizeMode="cover" />
                ) : (
                  <Text style={{ color: accent, fontSize: 28 }}>{item.name.slice(0, 2).toUpperCase()}</Text>
                )}
              </View>
              <Text style={[styles.cardName]} numberOfLines={1}>{item.name}</Text>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  overlay: { ...StyleSheet.absoluteFillObject, zIndex: 3100, paddingTop: '6%', paddingBottom: '10%', paddingHorizontal: 20 },
  flex: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', marginBottom: 16 },
  title: { fontSize: 30, fontWeight: 'bold', marginLeft: 8 },
  icon: { fontSize: 22, paddingHorizontal: 6 },
  carousel: { gap: 12, paddingVertical: 4 },
  cardWrap: { width: 132, alignItems: 'center' },
  card: { width: 132, height: 132, borderRadius: 20, borderWidth: 1, overflow: 'hidden', alignItems: 'center', justifyContent: 'center' },
  cardImg: { width: '100%', height: '100%' },
  cardName: { marginTop: 8, fontSize: 16, fontWeight: '600' },
  empty: { opacity: 0.7, paddingVertical: 16 },
});
