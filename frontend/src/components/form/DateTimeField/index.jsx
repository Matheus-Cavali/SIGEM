import BaseField from '../BaseField'

function formatDateTime(value) {
  const digits = value.replace(/\D/g, '').slice(0, 12)
  return digits
    .replace(/^(\d{2})(\d)/, '$1/$2')
    .replace(/^(\d{2})\/(\d{2})(\d)/, '$1/$2/$3')
    .replace(/^(\d{2})\/(\d{2})\/(\d{4})(\d)/, '$1/$2/$3 $4')
    .replace(/^(\d{2})\/(\d{2})\/(\d{4}) (\d{2})(\d)/, '$1/$2/$3 $4:$5')
}

export default function DateTimeField({ value, setValue, label, placeholder, disabled, required, error }) {
  return (
    <BaseField label={label} error={error} required={required}>
      <input
        className="field-control"
        type="text"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder || 'dd/mm/aaaa hh:mm'}
        inputMode="numeric"
        maxLength={16}
        onChange={(event) => setValue(formatDateTime(event.target.value))}
      />
    </BaseField>
  )
}
