import './BaseField.scss'

export default function BaseField({ label, error, children }) {
  return (
    <div className="field-container">
      {label && <label className="field-label">{label}</label>}
      {children}
      {error && <span className="field-error">{error}</span>}
    </div>
  )
}
