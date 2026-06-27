import { useAzGuidanceController } from '@HereLiesAz/aznavrail-react'

/**
 * Demonstrates the status-driven **guidance** framework (the reactive replacement for the scripted
 * scene/card tutorial). The statuses, edges and goals are declared in the rail DSL in `App.tsx`
 * (`<AzStatus>` / `<AzEdge>` / `<AzGoal>`); this screen only drives the controller. The framework
 * figures out — live — which instruction to show next and places each as a callout next to the control
 * you'd use. There is no Next button; performing the action flips a status and the callout advances.
 */
export default function TutorialDemo({
  taskDone,
  onMarkTaskDone,
  onResetTask,
}: {
  taskDone: boolean
  onMarkTaskDone: () => void
  onResetTask: () => void
}) {
  const guidance = useAzGuidanceController()

  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ margin: 0 }}>Status-driven guidance</h2>
      <p style={{ opacity: 0.8 }}>
        Guidance describes the app as a flowchart of statuses and edges. You activate goals (target
        statuses); the framework always shows the instruction to reach the next status on the way
        there, auto-advancing the instant a target becomes true and routing around wherever you
        actually are. Several goals can guide at once — each callout sits next to its own control.
      </p>

      <p>
        <strong>Guidance enabled:</strong> {String(guidance.enabled)}
        <br />
        <strong>Active goals:</strong> {guidance.activeGoals.length ? guidance.activeGoals.join(', ') : '(none)'}
        <br />
        <strong>Completed goals:</strong>{' '}
        {guidance.completedGoals.length ? guidance.completedGoals.join(', ') : '(none)'}
        <br />
        <strong>Custom status guide_task_done:</strong> {String(taskDone)}
      </p>

      <h3 style={{ margin: '8px 0 0' }}>Master switch</h3>
      <Row>
        <button style={btn} onClick={() => guidance.enable()}>enable()</button>
        <button style={btn} onClick={() => guidance.disable()}>disable()</button>
      </Row>

      <h3 style={{ margin: '8px 0 0' }}>Activate a single goal</h3>
      <Row>
        <button style={btn} onClick={() => guidance.activate('guide_onboarding')}>
          Guide me to Bottom Sheets
        </button>
        <button style={btn} onClick={() => guidance.activate('guide_expand_host')}>
          Guide me to expand Rail Host
        </button>
        <button style={btn} onClick={() => guidance.activate('guide_custom_task')}>
          Guide me through the custom task
        </button>
      </Row>

      <h3 style={{ margin: '8px 0 0' }}>Two goals at once</h3>
      <p style={{ opacity: 0.7, fontSize: 13, margin: 0 }}>
        Activates both the host-expand goal and the custom-task goal. Both instructions show
        simultaneously, each as a callout next to its own control.
      </p>
      <Row>
        <button
          style={btn}
          onClick={() => {
            guidance.activate('guide_expand_host')
            guidance.activate('guide_custom_task')
          }}
        >
          Activate both (host + task)
        </button>
      </Row>

      <h3 style={{ margin: '8px 0 0' }}>Satisfy / reset the custom status</h3>
      <Row>
        <button style={btn} onClick={onMarkTaskDone}>Mark task done (flip guide_task_done)</button>
        <button style={btn} onClick={onResetTask}>Reset task</button>
      </Row>

      <h3 style={{ margin: '8px 0 0' }}>Stop guiding</h3>
      <Row>
        <button style={btn} onClick={() => guidance.activeGoals.slice().forEach((g) => guidance.deactivate(g))}>
          Deactivate all goals
        </button>
      </Row>
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
