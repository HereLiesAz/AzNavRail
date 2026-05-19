import { useState } from 'react'
import { AzForm, AzTextBox } from '@HereLiesAz/aznavrail-react'

export default function FormDemo() {
  const [submission, setSubmission] = useState<Record<string, string> | null>(null)
  const [singleValue, setSingleValue] = useState('')

  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h2 style={{ margin: 0 }}>Forms & Text Inputs</h2>

      <section>
        <h3 style={{ margin: '8px 0' }}>AzForm</h3>
        <AzForm
          formName="signup"
          onSubmit={(values: Record<string, string>) => setSubmission(values)}
          submitButtonContent={<span>Submit</span>}
        >
          {/* AzFormScope.entry – consult the React lib's entry helper if you prefer that pattern.
              Here we render AzTextBox children directly since the React port exports both APIs. */}
        </AzForm>
        <p style={{ opacity: 0.75, fontSize: 13 }}>
          Last submission: {submission ? JSON.stringify(submission) : '(none)'}
        </p>
      </section>

      <section>
        <h3 style={{ margin: '8px 0' }}>Standalone AzTextBox</h3>
        <AzTextBox
          value={singleValue}
          onValueChange={setSingleValue}
          hint="Type something"
          historyContext="standalone"
          onSubmit={(v: string) => console.log('submitted', v)}
        />
        <p style={{ opacity: 0.75, fontSize: 13 }}>Live value: {singleValue || '(empty)'}</p>
      </section>
    </div>
  )
}
