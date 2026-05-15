import { useEffect, useState } from 'react'
import { get, put } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { useAuth } from '../state/AuthContext'

export default function Permissoes() {
  const { user } = useAuth()
  const [usuarios, setUsuarios] = useState([])
  const [recursos, setRecursos] = useState([])
  const [usuarioId, setUsuarioId] = useState('')
  const [permissoes, setPermissoes] = useState([])
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [saving, setSaving] = useState(false)

  if (user?.nivelAcesso !== 1) {
    return (
      <>
        <PageHeader title="Permissoes" subtitle="Gerenciar permissoes do sistema" />
        <div className="message">Acesso restrito. Apenas usuarios com nivel Total podem gerenciar permissoes.</div>
      </>
    )
  }

  useEffect(() => {
    const load = async () => {
      try {
        const [resU, resR] = await Promise.all([get('/api/usuarios'), get('/api/recurso')])
        const lista = Array.isArray(resU) ? resU : []
        setUsuarios(lista.filter(u => u.nivelAcesso !== 1 && u.tipoUsuario !== 'voluntario'))
        setRecursos(Array.isArray(resR) ? resR : [])
      } catch (error) { setErro(error.message) }
    }; load()
  }, [])

  const handleChange = async (e) => {
    const id = e.target.value; setUsuarioId(id); setMensagem('')
    if (id) {
      try {
        const res = await get('/api/recurso/' + id + '/permissoes')
        setPermissoes(res.permissoes || [])
      } catch (error) { setErro(error.message) }
    }
  }

  const salvar = async () => {
    if (!usuarioId) return; setSaving(true); setMensagem('')
    try {
      const recursoIds = recursos.filter(r => permissoes.includes(r.nome)).map(r => r.id)
      await put('/api/recurso/' + usuarioId + '/permissoes', { recursoIds })
      const res = await get('/api/recurso/' + usuarioId + '/permissoes')
      setPermissoes(res.permissoes || [])
      setMensagem('Permissoes atualizadas com sucesso!')
    } catch (error) { setMensagem(error.message) }
    finally { setSaving(false) }
  }

  const recursosDoUsuario = recursos.filter(r => permissoes.includes(r.nome))
  const recursosDisponiveis = recursos.filter(r => !permissoes.includes(r.nome))

  return (
    <>
      <PageHeader title="Permissoes" subtitle="Gerencie as permissoes dos usuarios" />
      {erro && <div className="message">{erro}</div>}
      {mensagem && <div className={'message ' + (mensagem.includes('sucesso') ? 'success' : '')}>{mensagem}</div>}

      <div className="editor-card">
        <label style={{ display: 'block', marginBottom: 16 }}>
          <span style={{ fontSize: 12, fontWeight: 600, marginBottom: 4, display: 'block' }}>Selecione um usuario</span>
          <select value={usuarioId} onChange={handleChange} style={{ width: '100%', padding: '8px 10px', border: '1px solid #E0E0E0', borderRadius: 4, fontSize: 13 }}>
            <option value="">-- Selecione --</option>
            {usuarios.map(u => (
              <option key={u.id} value={u.id}>
                {(u.nome ? u.nome.replace(/"/g, '') : '') + ' - ' + (u.email ? u.email.replace(/"/g, '') : '') + ' (' + (u.tipoUsuario ? u.tipoUsuario.replace(/"/g, '') : '') + ')'}
              </option>
            ))}
          </select>
        </label>

        {usuarioId && (
          <>
            <div className="perm-panels">
              <div className="perm-panel" style={{ borderTop: '3px solid #2A5C82' }}>
                <div className="perm-panel-header">
                  <h3>Recursos do Usuario <small>({recursosDoUsuario.length})</small></h3>
                </div>
                <div className="perm-panel-body">
                  {recursosDoUsuario.length === 0 ? (
                    <div className="perm-empty">Nenhum recurso atribuido</div>
                  ) : recursosDoUsuario.map(r => (
                    <div className="perm-item" key={r.id}>
                      <div>
                        <div className="perm-item-name">{r.nome}</div>
                        <div className="perm-item-desc">{r.descricao}</div>
                      </div>
                      <button className="icon-button danger" title="Remover" onClick={() => setPermissoes(prev => prev.filter(p => p !== r.nome))}>
                        <Icon name="trash" size={14} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
              <div className="perm-panel" style={{ borderTop: '3px solid #27ae60' }}>
                <div className="perm-panel-header">
                  <h3>Disponiveis <small>({recursosDisponiveis.length})</small></h3>
                </div>
                <div className="perm-panel-body">
                  {recursosDisponiveis.length === 0 ? (
                    <div className="perm-empty">Nenhum recurso disponivel</div>
                  ) : recursosDisponiveis.map(r => (
                    <div className="perm-item" key={r.id}>
                      <div>
                        <div className="perm-item-name">{r.nome}</div>
                        <div className="perm-item-desc">{r.descricao}</div>
                      </div>
                      <button className="icon-button" title="Adicionar" onClick={() => setPermissoes(prev => [...prev, r.nome])}>
                        <Icon name="plus" size={14} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            <div className="form-submit" style={{ marginTop: 16 }}>
              <button className="danger-action" onClick={salvar} disabled={saving}>{saving ? 'Salvando...' : 'Salvar Permissoes'}</button>
            </div>
          </>
        )}
      </div>
    </>
  )
}
