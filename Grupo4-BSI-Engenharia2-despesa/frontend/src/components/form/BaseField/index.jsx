import './BaseField.scss'

export default function BaseField({ label, error, required, children }) {
  const containerClass = 'field-container' + (error ? ' has-error' : '')

  return (
    <div className={containerClass}>
      {label && (
        <label className="field-label">
          {label}{required && <span className="required-star"> *</span>}
        </label>
      )}
      {children}
      {error && <span className="field-error">{error}</span>}
    </div>
  )
}
