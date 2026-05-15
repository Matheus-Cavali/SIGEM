import BaseField from '../BaseField'

export default function TextAreaField({ value, setValue, label, placeholder, disabled, error }) {
  return (
    <BaseField label={label} error={error}>
      <textarea
        className="field-control"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(event) => setValue(event.target.value)}
      />
    </BaseField>
  )
}
