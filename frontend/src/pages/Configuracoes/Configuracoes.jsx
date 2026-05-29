import { useEffect, useState } from 'react'
import PageHeader from '../../components/PageHeader'
import Icon from '../../components/Icon'
import { CepField, CnpjField, ColorField, LogoUpload, PhoneField, SelectField, TextField } from '../../components/form'
import { useAuth } from '../../state/AuthContext'
import { useParameters } from '../../state/ParametersContext'
import './Configuracoes.scss'

const ESTADOS = [
  { value: 'AC', label: 'AC' }, { value: 'AL', label: 'AL' },
  { value: 'AP', label: 'AP' }, { value: 'AM', label: 'AM' },
  { value: 'BA', label: 'BA' }, { value: 'CE', label: 'CE' },
  { value: 'DF', label: 'DF' }, { value: 'ES', label: 'ES' },
  { value: 'GO', label: 'GO' }, { value: 'MA', label: 'MA' },
  { value: 'MT', label: 'MT' }, { value: 'MS', label: 'MS' },
  { value: 'MG', label: 'MG' }, { value: 'PA', label: 'PA' },
  { value: 'PB', label: 'PB' }, { value: 'PR', label: 'PR' },
  { value: 'PE', label: 'PE' }, { value: 'PI', label: 'PI' },
  { value: 'RJ', label: 'RJ' }, { value: 'RN', label: 'RN' },
  { value: 'RS', label: 'RS' }, { value: 'RO', label: 'RO' },
  { value: 'RR', label: 'RR' }, { value: 'SC', label: 'SC' },
  { value: 'SP', label: 'SP' }, { value: 'SE', label: 'SE' },
  { value: 'TO', label: 'TO' },
]

const initialForm = {
  nomeFantasia: '',
  cnpj: '',
  telefone: '',
  email: '',
  site: '',
  endereco: {
    id: null,
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    bairro: '',
    cidade: '',
    uf: '',
  },
  corPrimaria: '#1f4f82',
  corSecundaria: '#7d0a1e',
  caminhoLogo: '',
  caminhoLogoGrande: '',
  logo: null,
  logoGrande: null,
}

function absoluteLogoUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `http://localhost:8080${path}`
}

export default function Configuracoes() {
  const { user } = useAuth()
  const { parameters, loading, error, updateParameters, uploadLogo, uploadLogoGrande } = useParameters()
  const [form, setForm] = useState(initialForm)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [localError, setLocalError] = useState('')
  const [logoPreview, setLogoPreview] = useState('')
  const [logoGrandePreview, setLogoGrandePreview] = useState('')

  useEffect(() => {
    const next = { ...initialForm, ...parameters, logo: null, logoGrande: null }
    if (parameters.endereco) {
      next.endereco = { ...initialForm.endereco, ...parameters.endereco }
    }
    setForm(next)
    setLogoPreview(absoluteLogoUrl(next.caminhoLogo))
    setLogoGrandePreview(absoluteLogoUrl(next.caminhoLogoGrande))
  }, [parameters])

  const disabled = !editing || saving
  const fieldErrors = {}

  function setField(field, value) {
    if (field.startsWith('endereco.')) {
      const enderecoField = field.replace('endereco.', '')
      setForm(prev => ({
        ...prev,
        endereco: { ...prev.endereco, [enderecoField]: value },
      }))
    } else {
      setForm(prev => ({
        ...prev,
        [field]: value,
      }))
    }
  }

  function handleCepChange(cep) {
    setField('endereco.cep', cep)
    const digits = cep.replace(/\D/g, '')
    if (digits.length === 8) {
      fetch(`https://viacep.com.br/ws/${digits}/json/`)
        .then(res => res.json())
        .then(data => {
          if (!data.erro) {
            setForm(prev => ({
              ...prev,
              endereco: {
                ...prev.endereco,
                logradouro: data.logradouro || '',
                bairro: data.bairro || '',
                cidade: data.localidade || '',
                uf: data.uf || '',
              },
            }))
          }
        })
        .catch(() => {})
    }
  }

  async function save() {
    setSaving(true)
    setMessage('')
    setLocalError('')

    try {
      let logoPath = form.caminhoLogo
      let logoGrandePath = form.caminhoLogoGrande

      if (form.logo instanceof File) {
        const logoResponse = await uploadLogo(form.logo)
        if (logoResponse && logoResponse.caminhoLogo) {
          logoPath = logoResponse.caminhoLogo
        }
      }

      if (form.logoGrande instanceof File) {
        const logoGrandeResponse = await uploadLogoGrande(form.logoGrande)
        if (logoGrandeResponse && logoGrandeResponse.caminhoLogoGrande) {
          logoGrandePath = logoGrandeResponse.caminhoLogoGrande
        }
      }

      const endereco = form.endereco.cep || form.endereco.logradouro || form.endereco.numero
        ? form.endereco
        : null

      const payload = {
        nomeFantasia: form.nomeFantasia,
        cnpj: form.cnpj,
        telefone: form.telefone,
        email: form.email,
        site: form.site,
        endereco,
        corPrimaria: form.corPrimaria,
        corSecundaria: form.corSecundaria,
        caminhoLogo: logoPath,
        caminhoLogoGrande: logoGrandePath,
      }

      const saved = await updateParameters(payload)

      setForm(prev => {
        const next = { ...initialForm, ...saved, logo: null, logoGrande: null }
        if (saved && saved.endereco) {
          next.endereco = { ...initialForm.endereco, ...saved.endereco }
        }
        return next
      })

      if (saved && saved.caminhoLogo) {
        setLogoPreview(absoluteLogoUrl(saved.caminhoLogo))
      } else {
        setLogoPreview('')
      }

      if (saved && saved.caminhoLogoGrande) {
        setLogoGrandePreview(absoluteLogoUrl(saved.caminhoLogoGrande))
      } else {
        setLogoGrandePreview('')
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
    const next = { ...initialForm, ...parameters, logo: null, logoGrande: null }
    if (parameters.endereco) {
      next.endereco = { ...initialForm.endereco, ...parameters.endereco }
    }
    setForm(next)
    setLogoPreview(absoluteLogoUrl(next.caminhoLogo))
    setLogoGrandePreview(absoluteLogoUrl(next.caminhoLogoGrande))
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
          <LogoUpload
            label="Logo Grande"
            value={form.logoGrande || form.caminhoLogoGrande}
            setValue={(value) => setField('logoGrande', value)}
            preview={logoGrandePreview}
            setPreview={setLogoGrandePreview}
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
        </div>
      </section>

      <section className="settings-card">
        <header className="settings-card-header">
          <h2><Icon name="map" size={18} /> Endereço</h2>
        </header>

        <div className="settings-grid">
          <CepField
            label="CEP"
            value={form.endereco.cep}
            setValue={handleCepChange}
            placeholder="00000-000"
            disabled={disabled}
            error={fieldErrors['endereco.cep']}
          />
          <TextField
            label="Logradouro"
            value={form.endereco.logradouro}
            setValue={(value) => setField('endereco.logradouro', value)}
            placeholder="Rua das Flores"
            disabled={disabled}
            error={fieldErrors['endereco.logradouro']}
          />
          <TextField
            label="Número"
            value={form.endereco.numero}
            setValue={(value) => setField('endereco.numero', value)}
            placeholder="123"
            disabled={disabled}
            error={fieldErrors['endereco.numero']}
          />
          <TextField
            label="Complemento"
            value={form.endereco.complemento}
            setValue={(value) => setField('endereco.complemento', value)}
            placeholder="Sala 2"
            disabled={disabled}
          />
          <TextField
            label="Bairro"
            value={form.endereco.bairro}
            setValue={(value) => setField('endereco.bairro', value)}
            placeholder="Centro"
            disabled={disabled}
            error={fieldErrors['endereco.bairro']}
          />
          <TextField
            label="Cidade"
            value={form.endereco.cidade}
            setValue={(value) => setField('endereco.cidade', value)}
            placeholder="Presidente Prudente"
            disabled={disabled}
            error={fieldErrors['endereco.cidade']}
          />
          <SelectField
            label="UF"
            value={form.endereco.uf}
            setValue={(value) => setField('endereco.uf', value)}
            placeholder="Selecione"
            options={ESTADOS}
            disabled={disabled}
            error={fieldErrors['endereco.uf']}
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
