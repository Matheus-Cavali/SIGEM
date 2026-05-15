import BaseField from '../BaseField'

function formatPhone(value) {
  const digits = value.replace(/\D/g, '').slice(0, 11)

  if (digits.length <= 10) {
    return digits
      .replace(/^(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{4})(\d)/, '$1-$2')
  }

  return digits
    .replace(/^(\d{2})(\d)/, '($1) $2')
    .replace(/(\d{5})(\d)/, '$1-$2')
}

export default function PhoneField({ value, setValue, label, placeholder, disabled, error }) {
  return (
    <BaseField label={label} error={error}>
      <input
        className="field-control"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder}
        inputMode="tel"
        onChange={(event) => setValue(formatPhone(event.target.value))}
      />
    </BaseField>
  )
}
