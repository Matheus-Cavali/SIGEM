import BaseField from '../BaseField'

function formatCurrency(value) {
  const digits = value.replace(/\D/g, '')
  const cents = Number(digits || 0) / 100

  return cents.toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
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