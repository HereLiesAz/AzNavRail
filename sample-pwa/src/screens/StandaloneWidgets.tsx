import { useState } from 'react'
import {
  AzButton,
  AzToggle,
  AzCycler,
  AzDivider,
  AzLoad,
  AzRoller,
  AzButtonShape,
} from '@HereLiesAz/aznavrail-react'

const shapes = Object.values(AzButtonShape) as AzButtonShape[]

export default function StandaloneWidgets() {
  const [toggled, setToggled] = useState(false)
  const cyclerOpts = ['Alpha', 'Beta', 'Gamma', 'Delta']
  const [cyclerSel, setCyclerSel] = useState(cyclerOpts[0])
  const rollerOpts = ['Cherry', 'Bell', 'Bar', 'Seven', 'Diamond']
  const [rollerSel, setRollerSel] = useState(rollerOpts[0])

  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h2 style={{ margin: 0 }}>Standalone Widgets</h2>

      <Section title="AzButton — every shape">
        <Row>
          {shapes.map((shape) => (
            <AzButton
              key={shape}
              text={shape}
              shape={shape}
              onClick={() => console.log(`button[${shape}]`)}
            />
          ))}
        </Row>
      </Section>

      <Section title="AzToggle — every shape">
        <Row>
          {shapes.map((shape) => (
            <AzToggle
              key={shape}
              isChecked={toggled}
              onToggle={() => setToggled((v) => !v)}
              toggleOnText={`${shape} On`}
              toggleOffText={`${shape} Off`}
              shape={shape}
            />
          ))}
        </Row>
      </Section>

      <Section title="AzCycler with disabledOptions">
        <AzCycler
          options={cyclerOpts}
          selectedOption={cyclerSel}
          disabledOptions={['Gamma']}
          onCycle={() => {
            const idx = cyclerOpts.indexOf(cyclerSel)
            setCyclerSel(cyclerOpts[(idx + 1) % cyclerOpts.length])
          }}
          shape={AzButtonShape.RECTANGLE}
        />
      </Section>

      <Section title="AzRoller (filtering dropdown)">
        <AzRoller
          options={rollerOpts}
          selectedOption={rollerSel}
          onOptionSelected={setRollerSel}
          hint="Pick a symbol"
          enabled
        />
        <p style={{ fontSize: 13, opacity: 0.75 }}>Selected: {rollerSel}</p>
      </Section>

      <Section title="AzDivider">
        <AzDivider />
      </Section>

      <Section title="AzLoad">
        <div style={{ width: 80, height: 80 }}>
          <AzLoad />
        </div>
      </Section>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <div style={{ fontWeight: 600, marginBottom: 8 }}>{title}</div>
      {children}
    </div>
  )
}

function Row({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
      {children}
    </div>
  )
}
