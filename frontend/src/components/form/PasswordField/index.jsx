import BaseField from '../BaseField'

export default function PasswordField({ value, setValue, label, placeholder, disabled, error }) {
  return (
    <BaseField label={label} error={error}>
      <input
        className="field-control"
        type="password"
        value={value || ''}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(event) => setValue(event.target.value)}
      />
    </BaseField>
  )
}
