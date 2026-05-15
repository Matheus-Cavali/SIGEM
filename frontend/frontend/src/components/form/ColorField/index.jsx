import BaseField from '../BaseField'
import './ColorField.scss'

function normalizeColor(value) {
  const clean = value.replace(/[^0-9a-fA-F#]/g, '')
  const withHash = clean.startsWith('#') ? clean : `#${clean}`

  return withHash.slice(0, 7)
}

export default function ColorField({ value, setValue, label, placeholder, disabled, error }) {
  const color = /^#[0-9a-fA-F]{6}$/.test(value || '') ? value : '#23598d'

  return (
    <BaseField label={label} error={error}>
      <div className="color-field">
        <input
          className="color-picker"
          type="color"
          value={color}
          disabled={disabled}
          onChange={(event) => setValue(event.target.value)}
          aria-label={label}
        />
        <input
          className="field-control color-input"
          value={value || ''}
          disabled={disabled}
          placeholder={placeholder}
          onChange={(event) => setValue(normalizeColor(event.target.value))}
        />
      </div>
    </BaseField>
  )
}
