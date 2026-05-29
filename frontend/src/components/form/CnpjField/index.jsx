import BaseField from '../BaseField'

function formatCnpj(value) {
  return value
    .replace(/\D/g, '')
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2')
    .slice(0, 18)
}

export default function CnpjField({ value, setValue, label, placeholder, disabled, error }) {
  return (
    <BaseField label={label} error={error}>
      <input
        className="field-control"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder}
        inputMode="numeric"
        onChange={(event) => setValue(formatCnpj(event.target.value))}
      />
    </BaseField>
  )
}
