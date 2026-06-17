import React from 'react';
import { Text, View, Linking, StyleSheet } from 'react-native';

/**
 * A tiny dependency-free Markdown renderer for the React Native About reader, themed to AzNavRail.
 * Covers the common doc subset (headings, paragraphs, bold/italic, inline code, fenced code blocks,
 * lists, block-quotes, horizontal rules, links). `accent` colors links and headings.
 */
const INLINE = /(`([^`]+)`)|(\*\*([^*]+)\*\*)|(__([^_]+)__)|(\*([^*]+)\*)|(_([^_]+)_)|(\[([^\]]+)\]\(([^)]+)\))/g;

function renderInline(text: string, accent: string): React.ReactNode[] {
  const nodes: React.ReactNode[] = [];
  let last = 0;
  let m: RegExpExecArray | null;
  let k = 0;
  INLINE.lastIndex = 0;
  while ((m = INLINE.exec(text)) !== null) {
    if (m.index > last) nodes.push(text.slice(last, m.index));
    if (m[2]) nodes.push(<Text key={k} style={styles.code}>{m[2]}</Text>);
    else if (m[4]) nodes.push(<Text key={k} style={styles.bold}>{m[4]}</Text>);
    else if (m[6]) nodes.push(<Text key={k} style={styles.bold}>{m[6]}</Text>);
    else if (m[8]) nodes.push(<Text key={k} style={styles.italic}>{m[8]}</Text>);
    else if (m[10]) nodes.push(<Text key={k} style={styles.italic}>{m[10]}</Text>);
    else if (m[12]) {
      const url = m[13];
      nodes.push(
        <Text key={k} style={[styles.link, { color: accent }]} onPress={() => Linking.openURL(url).catch(() => {})}>
          {m[12]}
        </Text>
      );
    }
    last = m.index + m[0].length;
    k++;
  }
  if (last < text.length) nodes.push(text.slice(last));
  return nodes;
}

export default function AzMarkdownNative({ markdown, accent }: { markdown: string; accent: string }) {
  const lines = (markdown || '').replace(/\r\n/g, '\n').split('\n');
  const blocks: React.ReactNode[] = [];
  let para: string[] = [];
  let i = 0;
  let key = 0;

  const flushPara = () => {
    if (para.length) {
      const text = para.join(' ').trim();
      if (text) blocks.push(<Text key={key++} style={styles.p}>{renderInline(text, accent)}</Text>);
      para = [];
    }
  };

  while (i < lines.length) {
    const line = lines[i].replace(/\s+$/, '');
    if (line.trimStart().startsWith('```')) {
      flushPara();
      const code: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trimStart().startsWith('```')) { code.push(lines[i]); i++; }
      blocks.push(<View key={key++} style={styles.pre}><Text style={styles.preText}>{code.join('\n')}</Text></View>);
    } else if (/^(---|\*\*\*|___)\s*$/.test(line.trim())) {
      flushPara();
      blocks.push(<View key={key++} style={[styles.hr, { backgroundColor: accent }]} />);
    } else if (/^#{1,6} /.test(line)) {
      flushPara();
      const level = (line.match(/^#+/) as RegExpMatchArray)[0].length;
      blocks.push(
        <Text key={key++} style={[styles.heading, headingSize(level), level <= 2 ? { color: accent } : null]}>
          {renderInline(line.replace(/^#+\s/, ''), accent)}
        </Text>
      );
    } else if (line.trimStart().startsWith('>')) {
      flushPara();
      blocks.push(
        <View key={key++} style={[styles.quote, { borderLeftColor: accent }]}>
          <Text style={styles.p}>{renderInline(line.trimStart().replace(/^>\s?/, ''), accent)}</Text>
        </View>
      );
    } else if (/^\s*([-*+] |\d+\. )/.test(lines[i])) {
      flushPara();
      const ordered = /^\s*\d+\. /.test(lines[i]);
      let n = 1;
      while (i < lines.length && /^\s*([-*+] |\d+\. )/.test(lines[i])) {
        const content = lines[i].trimStart().replace(/^([-*+] |\d+\. )/, '');
        const marker = ordered ? `${n}. ` : '• ';
        blocks.push(
          <View key={key++} style={styles.li}>
            <Text style={[styles.p, { color: accent }]}>{marker}</Text>
            <Text style={[styles.p, styles.liText]}>{renderInline(content, accent)}</Text>
          </View>
        );
        n++; i++;
      }
      i--;
    } else if (line.trim() === '') {
      flushPara();
    } else {
      para.push(line.trim());
    }
    i++;
  }
  flushPara();

  return <View>{blocks}</View>;
}

function headingSize(level: number) {
  switch (level) {
    case 1: return { fontSize: 26 };
    case 2: return { fontSize: 22 };
    case 3: return { fontSize: 19 };
    default: return { fontSize: 16 };
  }
}

const styles = StyleSheet.create({
  p: { fontSize: 15, lineHeight: 22, marginBottom: 8 },
  bold: { fontWeight: 'bold' },
  italic: { fontStyle: 'italic' },
  link: { textDecorationLine: 'underline' },
  code: { fontFamily: 'monospace', backgroundColor: 'rgba(127,127,127,0.15)' },
  pre: { backgroundColor: 'rgba(127,127,127,0.12)', borderRadius: 8, padding: 12, marginBottom: 8 },
  preText: { fontFamily: 'monospace', fontSize: 13 },
  heading: { fontWeight: 'bold', marginTop: 12, marginBottom: 6 },
  quote: { borderLeftWidth: 3, paddingLeft: 12, marginBottom: 8 },
  hr: { height: 1, opacity: 0.4, marginVertical: 12 },
  li: { flexDirection: 'row', marginBottom: 2, paddingLeft: 8 },
  liText: { flex: 1 },
});
