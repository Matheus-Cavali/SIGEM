import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { get, put } from '../api/http'
import { useAuth } from '../state/AuthContext'
import PageHeader from '../components/PageHeader'

export default function Permissoes() {
  const { user, can } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!can('GESTAO_PERMISSOES')) navigate('/investimentos', { replace: true })
  }, [user, can])
  const [usuarios, setUsuarios] = useState([])
  const [recursos, setRecursos] = useState([])
  const [usuarioId, setUsuarioId] = useState('')
  const [recursoIds, setRecursoIds] = useState([])
  const [erro, setErro] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [salvo, setSalvo] = useState('')

  const loadUsuarios = async () => {
    try {
      const data = await get('/api/usuarios?tipo=colaborador')
      setUsuarios(Array.isArray(data) ? data.filter(u => u.nivelAcesso !== 1) : [])
    } catch (error) {
      if (!error.fieldErrors) setErro(error.message)
    }
  }

  const loadRecursos = async () => {
    try {
      const data = await get('/api/recurso')
      const obsoletas = ['GESTAO_COLABORADORES', 'GESTAO_VOLUNTARIOS']
      setRecursos(Array.isArray(data) ? data.filter(r => !obsoletas.includes(r.nome)) : [])
    } catch (error) {
      if (!error.fieldErrors) setErro(error.message)
    }
  }

  useEffect(() => { loadUsuarios(); loadRecursos() }, [])

  const loadPermissoes = async (id) => {
    if (id) {
      try {
        const data = await get('/api/recurso/' + id + '/permissoes')
        if (data && Array.isArray(data.permissoes)) {
          const ids = data.permissoes.map(nome =>
            recursos.find(r => r.nome === nome)
          ).filter(Boolean).map(r => r.id)
          setRecursoIds(ids)
        } else {
          setRecursoIds([])
        }
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
        }
        setRecursoIds([])
      }
    } else {
      setRecursoIds([])
    }
  }

  useEffect(() => {
    if (recursos.length) loadPermissoes(usuarioId)
  }, [usuarioId, recursos.length])

  const toggleRecurso = (id) => {
    setRecursoIds(prev =>
      prev.includes(id) ? prev.filter(r => r !== id) : [...prev, id]
    )
  }

  const salvar = async () => {
    setErro(''); setSalvo(''); setFieldErrors({})
    if (usuarioId) {
      try {
        await put('/api/recurso/' + usuarioId + '/permissoes', { recursoIds })
        setSalvo('Permissões salvas com sucesso!')
      } catch (error) {
        if (error.fieldErrors) {
          setFieldErrors(error.fieldErrors)
        } else {
          setErro(error.message)
        }
      }
    } else {
      setFieldErrors({ usuarioId: 'Selecione um colaborador' })
    }
  }

  return (
    <>
      <PageHeader title="Permissões" subtitle="Gerencie quais páginas cada colaborador pode acessar" />

      <section className="editor-card">
        <div className="editor-title"><h2>Selecionar Colaborador</h2></div>
        {erro && <div className="message inline">{erro}</div>}
        {salvo && <div className="message success">{salvo}</div>}

        <div className="inline-form">
          <div className="field-container">
            <label className="field-label">Colaborador <span className="required-star">*</span></label>
            <select className={'field-control' + (fieldErrors.usuarioId ? ' is-invalid' : '')}
                    value={usuarioId} onChange={e => { setUsuarioId(e.target.value); setFieldErrors(prev => ({ ...prev, usuarioId: '' })) }}>
              <option value="">Selecione um colaborador...</option>
              {usuarios.map(u => (
                <option key={u.id} value={u.id}>{u.nome} ({u.email})</option>
              ))}
            </select>
            {fieldErrors.usuarioId && <span className="field-error">{fieldErrors.usuarioId}</span>}
          </div>
        </div>
      </section>

      {usuarioId && (
        <section className="editor-card">
          <div className="editor-title"><h2>Páginas e Recursos</h2></div>
          {!recursos.length && <p className="empty-state">Nenhum recurso encontrado.</p>}
          {!!recursos.length && (
            <div className="inline-form">
              {recursos.map(r => (
                <label className="checkbox-label" key={r.id}>
                  <input type="checkbox" checked={recursoIds.includes(r.id)}
                         onChange={() => toggleRecurso(r.id)} />
                  <span>{r.descricao || r.nome}</span>
                </label>
              ))}
              <div className="form-submit">
                <button className="primary-action" onClick={salvar}>Salvar Permissões</button>
              </div>
            </div>
          )}
        </section>
      )}
    </>
  )
}
