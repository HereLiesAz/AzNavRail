import React from 'react';

/**
 * A tiny dependency-free Markdown renderer for the web About reader, themed to AzNavRail.
 * Covers the common doc subset: ATX headings, paragraphs, bold/italic, inline code, fenced code
 * blocks, lists, block-quotes, horizontal rules, and links. `accent` colors links and accents.
 */
const INLINE = /(`([^`]+)`)|(\*\*([^*]+)\*\*)|(__([^_]+)__)|(\*([^*]+)\*)|(_([^_]+)_)|(\[([^\]]+)\]\(([^)]+)\))/g;

function renderInline(text, accent, keyPrefix) {
  const nodes = [];
  let last = 0;
  let m;
  let k = 0;
  INLINE.lastIndex = 0;
  while ((m = INLINE.exec(text)) !== null) {
    if (m.index > last) nodes.push(text.slice(last, m.index));
    if (m[2]) nodes.push(<code key={`${keyPrefix}-${k}`} className="az-md-code">{m[2]}</code>);
    else if (m[4]) nodes.push(<strong key={`${keyPrefix}-${k}`}>{m[4]}</strong>);
    else if (m[6]) nodes.push(<strong key={`${keyPrefix}-${k}`}>{m[6]}</strong>);
    else if (m[8]) nodes.push(<em key={`${keyPrefix}-${k}`}>{m[8]}</em>);
    else if (m[10]) nodes.push(<em key={`${keyPrefix}-${k}`}>{m[10]}</em>);
    else if (m[12]) nodes.push(
      <a key={`${keyPrefix}-${k}`} href={m[13]} target="_blank" rel="noreferrer" style={{ color: accent }}>{m[12]}</a>
    );
    last = m.index + m[0].length;
    k++;
  }
  if (last < text.length) nodes.push(text.slice(last));
  return nodes;
}

export default function AzMarkdownWeb({ markdown, accent }) {
  const lines = (markdown || '').replace(/\r\n/g, '\n').split('\n');
  const blocks = [];
  let para = [];
  let i = 0;
  let key = 0;

  const flushPara = () => {
    if (para.length) {
      const text = para.join(' ').trim();
      if (text) blocks.push(<p key={key++} className="az-md-p">{renderInline(text, accent, `p${key}`)}</p>);
      para = [];
    }
  };

  while (i < lines.length) {
    const raw = lines[i];
    const line = raw.replace(/\s+$/, '');
    if (line.trimStart().startsWith('```')) {
      flushPara();
      const code = [];
      i++;
      while (i < lines.length && !lines[i].trimStart().startsWith('```')) { code.push(lines[i]); i++; }
      blocks.push(<pre key={key++} className="az-md-pre"><code>{code.join('\n')}</code></pre>);
    } else if (/^(---|\*\*\*|___)\s*$/.test(line.trim())) {
      flushPara();
      blocks.push(<hr key={key++} className="az-md-hr" style={{ borderColor: accent }} />);
    } else if (/^#{1,6} /.test(line)) {
      flushPara();
      const level = line.match(/^#+/)[0].length;
      const Tag = `h${Math.min(level, 6)}`;
      const color = level <= 2 ? accent : undefined;
      blocks.push(<Tag key={key++} className="az-md-h" style={{ color }}>{renderInline(line.replace(/^#+\s/, ''), accent, `h${key}`)}</Tag>);
    } else if (line.trimStart().startsWith('>')) {
      flushPara();
      blocks.push(
        <blockquote key={key++} className="az-md-quote" style={{ borderLeftColor: accent }}>
          {renderInline(line.trimStart().replace(/^>\s?/, ''), accent, `q${key}`)}
        </blockquote>
      );
    } else if (/^\s*([-*+] |\d+\. )/.test(raw)) {
      flushPara();
      const items = [];
      const ordered = /^\s*\d+\. /.test(raw);
      while (i < lines.length && /^\s*([-*+] |\d+\. )/.test(lines[i])) {
        const content = lines[i].trimStart().replace(/^([-*+] |\d+\. )/, '');
        items.push(<li key={items.length}>{renderInline(content, accent, `li${key}-${items.length}`)}</li>);
        i++;
      }
      i--;
      blocks.push(ordered ? <ol key={key++} className="az-md-list">{items}</ol> : <ul key={key++} className="az-md-list">{items}</ul>);
    } else if (line.trim() === '') {
      flushPara();
    } else {
      para.push(line.trim());
    }
    i++;
  }
  flushPara();

  return <div className="az-markdown">{blocks}</div>;
}
