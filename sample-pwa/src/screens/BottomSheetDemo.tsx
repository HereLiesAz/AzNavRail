import { useState } from 'react'
import {
  AzBottomSheet,
  AzBottomSheetInsetAware,
  useAzSheetController,
  AzSheetDetent,
} from '@HereLiesAz/aznavrail-react'

export default function BottomSheetDemo() {
  const controller = useAzSheetController(AzSheetDetent.PEEK)
  const [horizontalSwipeEnabled, setHorizontalSwipe] = useState(true)
  const [collapseOnBack, setCollapseOnBack] = useState(true)
  const [handleVisible, setHandleVisible] = useState(true)
  const [animateInTree, setAnimate] = useState(true)
  const [insetAware, setInsetAware] = useState(false)
  const [swipeLog, setSwipeLog] = useState('(no swipes yet)')
  const [swipeCount, setSwipeCount] = useState(0)

  const config = {
    horizontalSwipeEnabled,
    collapseOnBack,
    handleVisible,
    animateInTree,
  }

  const onSwipeLeft = () => {
    setSwipeCount((n) => n + 1)
    setSwipeLog(`left @ ${Date.now() % 100000}`)
  }
  const onSwipeRight = () => {
    setSwipeCount((n) => n + 1)
    setSwipeLog(`right @ ${Date.now() % 100000}`)
  }

  const Sheet = insetAware ? AzBottomSheetInsetAware : AzBottomSheet

  return (
    <div style={{ padding: 24, position: 'relative', minHeight: '100%' }}>
      <h2 style={{ margin: 0 }}>Bottom Sheets</h2>
      <p style={{ opacity: 0.8 }}>
        Four detents (HIDDEN / PEEK / HALF / FULL). Drag the handle, tap snapTo, or step through with the controller.
        The HIDDEN strip is 28dp tall — tap it to reveal PEEK, or drag up.
      </p>

      <p>
        <strong>Detent:</strong> {controller.detent} · <strong>Enabled:</strong> {String(controller.isEnabled)}
      </p>

      <Section title="snapTo">
        <Row>
          {(['HIDDEN', 'PEEK', 'HALF', 'FULL'] as const).map((d) => (
            <button
              key={d}
              onClick={() => controller.snapTo(AzSheetDetent[d])}
              style={btn}
            >
              {d}
            </button>
          ))}
        </Row>
      </Section>

      <Section title="Step">
        <Row>
          <button onClick={controller.stepUp} style={btn}>stepUp()</button>
          <button onClick={controller.stepDown} style={btn}>stepDown()</button>
        </Row>
      </Section>

      <Section title="Config">
        <Toggle label="Horizontal swipe enabled" value={horizontalSwipeEnabled} onChange={setHorizontalSwipe} />
        <Toggle label="Collapse on back press" value={collapseOnBack} onChange={setCollapseOnBack} />
        <Toggle label="Drag handle visible" value={handleVisible} onChange={setHandleVisible} />
        <Toggle label="Animate detent transitions" value={animateInTree} onChange={setAnimate} />
        <Toggle label="Use inset-aware variant" value={insetAware} onChange={setInsetAware} />
        <Toggle label="Controller isEnabled" value={controller.isEnabled} onChange={controller.setEnabled} />
      </Section>

      <Section title="Swipe log">
        <p style={{ fontSize: 13, opacity: 0.75 }}>{swipeCount} swipes — last: {swipeLog}</p>
      </Section>

      {/* The sheet renders absolutely-positioned over this screen, anchored to the bottom. */}
      <Sheet
        controller={controller}
        config={config}
        onSwipeLeft={onSwipeLeft}
        onSwipeRight={onSwipeRight}
      >
        <SheetBody detent={controller.detent} />
      </Sheet>
    </div>
  )
}

function SheetBody({ detent }: { detent: AzSheetDetent }) {
  return (
    <div style={{ padding: 16, color: '#222', height: '100%', overflowY: 'auto' }}>
      <strong>Sheet contents</strong>
      <p style={{ margin: '6px 0' }}>Detent: {detent}</p>
      <p>
        Drag the handle up/down to step through detents. Each gesture advances exactly one step,
        matching the LogKitty accumulated-delta model.
      </p>
      {Array.from({ length: 20 }, (_, i) => (
        <p key={i} style={{ margin: '4px 0', fontSize: 13 }}>
          Body line {i + 1} — content scrolls independently at HALF / FULL.
        </p>
      ))}
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ marginTop: 16 }}>
      <div style={{ fontWeight: 600, marginBottom: 8 }}>{title}</div>
      {children}
    </div>
  )
}

function Row({ children }: { children: React.ReactNode }) {
  return <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>{children}</div>
}

function Toggle({
  label, value, onChange,
}: { label: string; value: boolean; onChange: (v: boolean) => void }) {
  return (
    <label style={{ display: 'flex', gap: 8, alignItems: 'center', padding: '4px 0' }}>
      <input type="checkbox" checked={value} onChange={(e) => onChange(e.target.checked)} />
      <span>{label}</span>
    </label>
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
