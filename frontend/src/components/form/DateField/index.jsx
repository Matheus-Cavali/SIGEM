import BaseField from '../BaseField'

function formatDate(value) {
  const digits = value.replace(/\D/g, '').slice(0, 8)
  return digits
    .replace(/^(\d{2})(\d)/, '$1/$2')
    .replace(/^(\d{2})\/(\d{2})(\d)/, '$1/$2/$3')
}

export default function DateField({ value, setValue, label, placeholder, disabled, required, error }) {
  return (
    <BaseField label={label} error={error} required={required}>
      <input
        className="field-control"
        type="text"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder || 'dd/mm/aaaa'}
        inputMode="numeric"
        maxLength={10}
        onChange={(event) => setValue(formatDate(event.target.value))}
      />
    </BaseField>
  )
}
