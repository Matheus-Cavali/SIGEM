import BaseField from '../BaseField'
import './LogoUpload.scss'

export default function LogoUpload({ value, setValue, preview, setPreview, label, disabled, error }) {
  const onChange = (event) => {
    const file = event.target.files?.[0]

    if (!file) return

    setValue(file)
    setPreview(URL.createObjectURL(file))
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
            value={value instanceof File ? value.name : value || ''}
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
