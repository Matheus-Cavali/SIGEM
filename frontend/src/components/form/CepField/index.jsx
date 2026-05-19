import BaseField from '../BaseField'

function formatCep(value) {
  return value
    .replace(/\D/g, '')
    .replace(/^(\d{5})(\d)/, '$1-$2')
    .slice(0, 9)
}

export default function CepField({ value, setValue, label, placeholder, disabled, error }) {
  return (
    <BaseField label={label} error={error}>
      <input
        className="field-control"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder || '00000-000'}
        inputMode="numeric"
        maxLength={9}
        onChange={(event) => setValue(formatCep(event.target.value))}
      />
    </BaseField>
  )
}
