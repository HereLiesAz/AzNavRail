import React, { useEffect, useState } from 'react';
import { View, Text, Image, FlatList, TouchableOpacity, StyleSheet, Linking, BackHandler } from 'react-native';
import { AzButton } from './AzButton';
import { AzLoad } from './AzLoad';
import { AzButtonShape } from '../types';
import { fetchMoreFromAz, AzMoreFromApp } from '../services/moreFromAz';

interface MoreFromAzOverlayProps {
  jsonUrl: string;
  settings?: { activeColor?: string; translucentBackground?: string };
  onDismiss: () => void;
}

/**
 * Full-screen "More from Az" overlay for React Native: a horizontal, paging carousel of app-icon
 * cards with a detail pane below. Cards reuse the rail's transparent-shape-with-colored-stroke look;
 * data comes from the link-only manifest (metadata auto-resolved).
 */
export const MoreFromAzOverlay: React.FC<MoreFromAzOverlayProps> = ({ jsonUrl, settings = {}, onDismiss }) => {
  const accent = settings.activeColor || '#6200ee';
  const surface = settings.translucentBackground || '#ffffff';
  const [apps, setApps] = useState<AzMoreFromApp[] | null>(null);
  const [selected, setSelected] = useState(0);

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => { onDismiss(); return true; });
    return () => sub.remove();
  }, [onDismiss]);

  useEffect(() => {
    let active = true;
    fetchMoreFromAz(jsonUrl).then((r) => active && setApps(r?.apps ?? []));
    return () => { active = false; };
  }, [jsonUrl]);

  const current = apps && apps.length ? apps[Math.min(selected, apps.length - 1)] : null;
  const open = (url?: string) => url && Linking.openURL(url).catch(() => {});

  return (
    <View style={[styles.overlay, { backgroundColor: surface }]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onDismiss} accessibilityLabel="Back"><Text style={[styles.icon, { color: accent }]}>←</Text></TouchableOpacity>
        <Text style={[styles.title, { color: accent }]}>More from Az</Text>
      </View>

      {apps === null && <AzLoad />}
      {apps && apps.length === 0 && <Text style={styles.empty}>Couldn't load apps right now.</Text>}

      {current && (
        <View style={styles.flex}>
          <FlatList
            data={apps!}
            horizontal
            showsHorizontalScrollIndicator={false}
            keyExtractor={(a, i) => a.githubUrl || a.playStoreUrl || a.webUrl || String(i)}
            contentContainerStyle={styles.carousel}
            renderItem={({ item, index }) => (
              <TouchableOpacity
                style={[styles.card, { borderColor: index === selected ? accent : `${accent}66`, borderWidth: index === selected ? 3 : 1 }]}
                onPress={() => setSelected(index)}
              >
                {item.iconUrl ? (
                  <Image source={{ uri: item.iconUrl }} style={styles.cardImg} resizeMode="cover" />
                ) : (
                  <Text style={{ color: accent, fontSize: 28 }}>{item.name.slice(0, 2).toUpperCase()}</Text>
                )}
              </TouchableOpacity>
            )}
          />

          <View style={styles.detail}>
            <Text style={styles.name}>{current.name}</Text>
            {!!current.description && <Text style={styles.desc}>{current.description}</Text>}
            <View style={styles.actions}>
              {current.webUrl ? <AzButton text={current.isPwa ? 'Open' : 'Website'} color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => open(current.webUrl)} /> : null}
              {current.playStoreUrl ? <AzButton text="Play Store" color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => open(current.playStoreUrl)} /> : null}
              {current.githubUrl ? <AzButton text="GitHub" color={accent} shape={AzButtonShape.RECTANGLE} onClick={() => open(current.githubUrl)} /> : null}
            </View>
          </View>
        </View>
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
  card: { width: 132, height: 132, borderRadius: 20, overflow: 'hidden', alignItems: 'center', justifyContent: 'center' },
  cardImg: { width: '100%', height: '100%' },
  detail: { marginTop: 20 },
  name: { fontSize: 22, fontWeight: 'bold' },
  desc: { marginTop: 6, opacity: 0.85, fontSize: 15 },
  actions: { flexDirection: 'row', gap: 12, marginTop: 16 },
  empty: { opacity: 0.7, paddingVertical: 16 },
});
