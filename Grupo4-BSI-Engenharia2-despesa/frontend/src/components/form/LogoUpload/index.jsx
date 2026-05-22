import BaseField from '../BaseField'
import './LogoUpload.scss'

export default function LogoUpload({ value, setValue, preview, setPreview, label, disabled, error }) {
  function onChange(event) {
    const files = event.target.files
    const file = files && files.length > 0 ? files[0] : null

    if (!file) return

    setValue(file)
    setPreview(URL.createObjectURL(file))
  }

  function getDisplayValue() {
    if (value instanceof File) return value.name
    return value || ''
  }

  return (
    <BaseField label={label} error={error}>
      <div className="logo-upload">
        <div className="logo-preview">
          {preview ? <img src={preview} alt="Logo da igreja" /> : <span>Logo</span>}
        </div>
        <div className="logo-upload-actions">
          <input
            className="field-control"
            value={getDisplayValue()}
            disabled
            placeholder="Nenhuma imagem selecionada"
          />
          <label className={'logo-upload-button' + (disabled ? ' disabled' : '')}>
            Selecionar imagem
            <input type="file" accept="image/*" disabled={disabled} onChange={onChange} />
          </label>
        </div>
      </div>
    </BaseField>
  )
}
