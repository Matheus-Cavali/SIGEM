import BaseField from '../BaseField'

export default function TextField({ value, setValue, label, placeholder, disabled, required, error }) {
  return (
    <BaseField label={label} required={required} error={error}>
      <input
        className="field-control"
        type="text"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(event) => setValue(event.target.value)}
      />
    </BaseField>
  )
}
