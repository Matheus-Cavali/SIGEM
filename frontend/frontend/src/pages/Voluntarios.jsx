import { useEffect, useState } from 'react'
import { get } from '../api/http'
import PageHeader from '../components/PageHeader'
import Icon from '../components/Icon'
import { formatarCpf } from '../utils/format'

export default function Voluntarios() {
  const [items, setItems] = useState([]); const [erro, setErro] = useState('')

  const load = async () => {
    try { const data = await get('/api/voluntarios'); setItems(Array.isArray(data) ? data : []) }
    catch (error) { setErro(error.message) }
  }

  useEffect(() => { load() }, [])

  return (
    <>
      <PageHeader title="Voluntarios" subtitle="Voluntarios da igreja" />
      {erro && <div className="message">{erro}</div>}
      <div className="cards-list">
        {items.map(item => (
          <article className="finance-card" key={item.id}>
            <div>
              <h3>{item.nome ? item.nome.replace(/"/g, '') : ''}</h3>
              <div className="meta-row">
                <span>{item.email ? item.email.replace(/"/g, '') : ''}</span>
                <span>{item.cpf ? formatarCpf(item.cpf.replace(/"/g, '')) : ''}</span>
                <span>{item.celular || '-'}</span>
                <span className={'badge ' + (item.statusAtivo ? 'badge-success' : 'badge-error')}>{item.statusAtivo ? 'Ativo' : 'Inativo'}</span>
                <span style={{ color: '#888' }}>Inicio: {item.dataInicio || '-'}</span>
                {item.dataDesligamento && <span style={{ color: '#c0392b' }}>Deslig: {item.dataDesligamento}</span>}
              </div>
            </div>
          </article>
        ))}
        {items.length === 0 && !erro && <p style={{ textAlign: 'center', color: '#888', padding: 32 }}>Nenhum voluntario</p>}
      </div>
    </>
  )
}
