import BaseField from '../BaseField'

export default function SelectField({ value, setValue, label, placeholder, options = [], disabled, error }) {
  return (
    <BaseField label={label} error={error}>
      <select
        className="field-control"
        value={value || ''}
        disabled={disabled}
        onChange={(event) => setValue(event.target.value)}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map(option => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </BaseField>
  )
}
