import { useEffect, useState } from 'react'
import PageHeader from '../../components/PageHeader'
import Icon from '../../components/Icon'
import { CnpjField, ColorField, LogoUpload, PhoneField, TextAreaField, TextField } from '../../components/form'
import { useAuth } from '../../state/AuthContext'
import { useParameters } from '../../state/ParametersContext'
import './Configuracoes.scss'

const initialForm = {
  nomeFantasia: '',
  cnpj: '',
  telefone: '',
  email: '',
  site: '',
  endereco: '',
  corPrimaria: '#1f4f82',
  corSecundaria: '#7d0a1e',
  caminhoLogo: '',
  logo: null,
}

function absoluteLogoUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `http://localhost:8080${path}`
}

export default function Configuracoes() {
  const { user } = useAuth()
  const { parameters, loading, error, updateParameters, uploadLogo } = useParameters()
  const [form, setForm] = useState(initialForm)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [localError, setLocalError] = useState('')
  const [logoPreview, setLogoPreview] = useState('')

  useEffect(() => {
    const next = { ...initialForm, ...parameters, logo: null }
    setForm(next)
    setLogoPreview(absoluteLogoUrl(next.caminhoLogo))
  }, [parameters])

  const disabled = !editing || saving
  const fieldErrors = {}

  function setField(field, value) {
    setForm(prev => ({
      ...prev,
      [field]: value,
    }))
  }

  async function save() {
    setSaving(true)
    setMessage('')
    setLocalError('')

    try {
      let logoPath = form.caminhoLogo

      if (form.logo instanceof File) {
        const logoResponse = await uploadLogo(form.logo)

        if (logoResponse && logoResponse.caminhoLogo) {
          logoPath = logoResponse.caminhoLogo
        }
      }

      const saved = await updateParameters({
        nomeFantasia: form.nomeFantasia,
        cnpj: form.cnpj,
        telefone: form.telefone,
        email: form.email,
        site: form.site,
        endereco: form.endereco,
        corPrimaria: form.corPrimaria,
        corSecundaria: form.corSecundaria,
        caminhoLogo: logoPath,
      })

      setForm({ ...initialForm, ...saved, logo: null })

      if (saved && saved.caminhoLogo) {
        setLogoPreview(absoluteLogoUrl(saved.caminhoLogo))
      } else {
        setLogoPreview('')
      }

      setEditing(false)
      setMessage('Configurações salvas com sucesso.')
    } catch (err) {
      setLocalError(err.message)
    } finally {
      setSaving(false)
    }
  }

  function cancel() {
    const next = { ...initialForm, ...parameters, logo: null }
    setForm(next)
    setLogoPreview(absoluteLogoUrl(next.caminhoLogo))
    setEditing(false)
    setLocalError('')
    setMessage('')
  }

  return (
    <>
      <PageHeader
        title="Configurações"
        subtitle="Gerencie as informações da igreja"
        actionLabel={!editing && user?.nivelAcesso === 1 ? 'Editar' : ''}
        actionIcon="edit"
        onAction={() => setEditing(true)}
      />

      {loading && <div className="message inline">Carregando configurações...</div>}
      {(error || localError) && <div className="message inline">{error || localError}</div>}
      {message && <div className="message success inline">{message}</div>}

      <section className="settings-card">
        <header className="settings-card-header">
          <h2><Icon name="church" size={18} /> Informações da Igreja</h2>
        </header>

        <div className="settings-grid">
          <TextField
            label="Nome Fantasia"
            value={form.nomeFantasia}
            setValue={(value) => setField('nomeFantasia', value)}
            placeholder="Igreja Batista Central"
            disabled={disabled}
            error={fieldErrors.nomeFantasia}
          />
          <CnpjField
            label="CNPJ"
            value={form.cnpj}
            setValue={(value) => setField('cnpj', value)}
            placeholder="12.345.678/0001-90"
            disabled={disabled}
            error={fieldErrors.cnpj}
          />
          <LogoUpload
            label="Logo"
            value={form.logo || form.caminhoLogo}
            setValue={(value) => setField('logo', value)}
            preview={logoPreview}
            setPreview={setLogoPreview}
            disabled={disabled}
          />
          <div className="settings-colors">
            <ColorField
              label="Cor Primária"
              value={form.corPrimaria}
              setValue={(value) => setField('corPrimaria', value)}
              placeholder="#1f4f82"
              disabled={disabled}
            />
            <ColorField
              label="Cor Secundária"
              value={form.corSecundaria}
              setValue={(value) => setField('corSecundaria', value)}
              placeholder="#7d0a1e"
              disabled={disabled}
            />
          </div>
        </div>
      </section>

      <section className="settings-card">
        <header className="settings-card-header">
          <h2><Icon name="phone" size={18} /> Contato</h2>
        </header>

        <div className="settings-grid">
          <PhoneField
            label="Telefone"
            value={form.telefone}
            setValue={(value) => setField('telefone', value)}
            placeholder="(11) 3456-7890"
            disabled={disabled}
          />
          <TextField
            label="E-mail"
            value={form.email}
            setValue={(value) => setField('email', value)}
            placeholder="contato@igreja.org.br"
            disabled={disabled}
          />
          <TextField
            label="Site"
            value={form.site}
            setValue={(value) => setField('site', value)}
            placeholder="https://www.igreja.org.br"
            disabled={disabled}
          />
          <TextAreaField
            label="Endereço"
            value={form.endereco}
            setValue={(value) => setField('endereco', value)}
            placeholder="Rua das Flores, 123"
            disabled={disabled}
          />
        </div>
      </section>

      {editing && (
        <div className="settings-actions">
          <button className="secondary-action" type="button" onClick={cancel} disabled={saving}>Cancelar</button>
          <button className="primary-action" type="button" onClick={save} disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar'}
          </button>
        </div>
      )}
    </>
  )
}
