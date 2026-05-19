import { useState } from 'react'
import {
  AzTutorialProvider,
  useAzTutorialController,
} from '@HereLiesAz/aznavrail-react'
import type { AzTutorial } from '@HereLiesAz/aznavrail-react'

/**
 * Sample tutorials wired into the React port's tutorial controller. The Android sample app
 * exercises the equivalent set via `azAdvanced(tutorials = SampleTutorials)`; this file is the
 * React-side counterpart and covers every public `AzAdvanceCondition` and `AzHighlight` variant.
 */
const sampleTutorials: Record<string, AzTutorial> = {
  'showcase-tour': {
    scenes: [
      {
        id: 'intro',
        content: () => <Backdrop label="Scene: intro" tint="#1565C0" />,
        cards: [
          {
            title: 'Welcome',
            text: 'This card uses AzAdvanceCondition.Button — tap the action to continue.',
            highlight: { type: 'FullScreen' },
            advanceCondition: { type: 'Button' },
            actionText: 'Next',
          },
          {
            title: 'Tap-anywhere',
            text: 'Tap anywhere on the screen to advance past this card.',
            highlight: { type: 'None' },
            advanceCondition: { type: 'TapAnywhere' },
          },
        ],
      },
      {
        id: 'targeting',
        content: () => <Backdrop label="Scene: targeting" tint="#2E7D32" />,
        cards: [
          {
            title: 'Highlight: Item',
            text: "AzHighlight.Item points at a nav-rail item by id. The 'home' rail item is currently highlighted.",
            highlight: { type: 'Item', id: 'home' },
            advanceCondition: { type: 'Button' },
          },
          {
            title: 'Tap target',
            text: 'AzAdvanceCondition.TapTarget requires the user to tap the highlighted area.',
            highlight: { type: 'Item', id: 'showcase-home' },
            advanceCondition: { type: 'TapTarget' },
          },
        ],
      },
      {
        id: 'events',
        content: () => <Backdrop label="Scene: events" tint="#EF6C00" />,
        cards: [
          {
            title: 'Event-driven advance',
            text: 'This card waits for the event named "tutorial-go". Fire it from the controls below.',
            advanceCondition: { type: 'Event', name: 'tutorial-go' },
            highlight: { type: 'FullScreen' },
          },
          {
            title: 'Checklist card',
            text: 'Cards can carry a checklist for users to tick off before moving on.',
            checklistItems: ['Inspect the highlight', 'Read the body text', 'Press Next when ready'],
            advanceCondition: { type: 'Button' },
          },
          {
            title: 'Media card',
            text: 'Cards can host arbitrary media — images, video, anything that renders.',
            mediaContent: () => (
              <div
                style={{
                  width: '100%',
                  height: 80,
                  background: '#FFD54F',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 'bold',
                  color: '#222',
                }}
              >
                Live media content
              </div>
            ),
            advanceCondition: { type: 'Button' },
            actionText: 'Finish',
          },
        ],
      },
    ],
  },
  'branching-demo': {
    scenes: [
      {
        id: 'branch-root',
        content: () => <Backdrop label="Scene: branch-root" tint="#6A1B9A" />,
        cards: [
          {
            title: 'Branching',
            text: "Starting this tutorial with variables = { path: 'a' | 'b' } routes the next scene at runtime.",
            advanceCondition: { type: 'Button' },
          },
        ],
        branchVar: 'path',
        branches: { a: 'branch-a', b: 'branch-b' },
      },
      {
        id: 'branch-a',
        content: () => <Backdrop label="Branch A" tint="#C2185B" />,
        cards: [{ title: 'Path A', text: 'You started this tutorial with path=a.' }],
      },
      {
        id: 'branch-b',
        content: () => <Backdrop label="Branch B" tint="#0277BD" />,
        cards: [{ title: 'Path B', text: 'You started this tutorial with path=b.' }],
      },
    ],
  },
}

function Backdrop({ label, tint }: { label: string; tint: string }) {
  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        background: tint + '40',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <p style={{ color: tint, fontWeight: 'bold' }}>{label}</p>
    </div>
  )
}

function Inner() {
  const controller = useAzTutorialController()
  const [branchChoice, setBranchChoice] = useState<'a' | 'b'>('a')

  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ margin: 0 }}>Interactive Tutorials</h2>
      <p style={{ opacity: 0.8 }}>
        Tutorials are plain JS objects of scenes and cards. Each card declares its highlight
        target and advance condition. Read-state is persisted via AsyncStorage when the optional
        dependency is installed.
      </p>

      <p>
        <strong>Active tutorial:</strong> {controller.activeTutorialId ?? '(none)'}
        <br />
        <strong>Marked-read:</strong>{' '}
        {controller.readTutorials.length ? controller.readTutorials.join(', ') : '(none)'}
      </p>

      <Row>
        <button style={btn} onClick={() => controller.startTutorial('showcase-tour')}>
          Start showcase-tour
        </button>
        <button style={btn} onClick={() => controller.fireEvent('tutorial-go')}>
          fireEvent("tutorial-go")
        </button>
      </Row>

      <Row>
        <button
          style={btn}
          onClick={() => controller.startTutorial('branching-demo', { path: branchChoice })}
        >
          Start branching-demo (path={branchChoice})
        </button>
        <button style={btn} onClick={() => setBranchChoice((c) => (c === 'a' ? 'b' : 'a'))}>
          Toggle branch path
        </button>
      </Row>

      <Row>
        <button style={btn} onClick={() => controller.markTutorialRead('showcase-tour')}>
          markTutorialRead("showcase-tour")
        </button>
        <button style={btn} onClick={() => controller.endTutorial()}>
          endTutorial()
        </button>
      </Row>

      <p style={{ opacity: 0.7, fontSize: 13 }}>
        Note: the React port currently exposes the controller, the data model, and AsyncStorage
        persistence. The overlay UI is registered automatically by{' '}
        <code>AzNavRail</code> when a tutorial is active and is rendered above the rail.
      </p>

      <details style={{ marginTop: 12 }}>
        <summary style={{ cursor: 'pointer' }}>Available sample tutorials</summary>
        <pre style={{ background: 'rgba(255,255,255,0.06)', padding: 8, overflowX: 'auto' }}>
          {Object.keys(sampleTutorials).join('\n')}
        </pre>
      </details>
    </div>
  )
}

const btn: React.CSSProperties = {
  padding: '6px 12px',
  borderRadius: 6,
  border: '1px solid rgba(255,255,255,0.2)',
  background: 'rgba(255,255,255,0.08)',
  color: 'inherit',
  cursor: 'pointer',
  font: 'inherit',
}

function Row({ children }: { children: React.ReactNode }) {
  return <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>{children}</div>
}

export default function TutorialDemo() {
  return (
    <AzTutorialProvider>
      <Inner />
    </AzTutorialProvider>
  )
}

export { sampleTutorials }
