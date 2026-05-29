import BaseField from '../BaseField'

function formatCurrency(value) {
  const str = value || ''
  const hasMinus = str.includes('-')
  const digits = str.replace(/\D/g, '')
  const cents = Number(digits || 0) / 100

  if (digits.length === 0 && hasMinus) return '-'

  const formatted = cents.toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })

  if (cents === 0) return formatted

  return hasMinus ? '-' + formatted : formatted
}

export default function CurrencyField({ value, setValue, label, placeholder, disabled, error, required }) {
  return (
    <BaseField label={label} error={error} required={required}>
      <input
        className="field-control"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder}
        inputMode="decimal"
        onChange={(event) => setValue(formatCurrency(event.target.value))}
      />
    </BaseField>
  )
}