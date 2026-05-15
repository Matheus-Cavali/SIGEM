import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { get, postForm, put } from '../api/http'

const ParametersContext = createContext(null)

const emptyParameters = {
  nomeFantasia: '',
  razaoSocial: '',
  cnpj: '',
  caminhoLogo: '',
  corPrimaria: '#1f4f82',
  corSecundaria: '#7d0a1e',
  telefone: '',
  email: '',
  site: '',
  endereco: '',
}

export function ParametersProvider({ children }) {
  const [parameters, setParameters] = useState(emptyParameters)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchParameters = async () => {
    setLoading(true)
    setError('')

    try {
      const data = await get('/parameters')
      setParameters({ ...emptyParameters, ...(data || {}) })
      return data
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchParameters().catch(() => {})
  }, [])

  const updateParameters = async (payload) => {
    setError('')
    const data = await put('/parameters', payload)
    setParameters({ ...emptyParameters, ...(data || {}) })
    return data
  }

  const uploadLogo = async (file) => {
    if (!file) return null

    const formData = new FormData()
    formData.append('logo', file)

    const data = await postForm('/parameters/logo', formData)
    setParameters(prev => ({ ...prev, caminhoLogo: data?.caminhoLogo || prev.caminhoLogo }))
    return data
  }

  const value = useMemo(() => ({
    parameters,
    loading,
    error,
    fetchParameters,
    updateParameters,
    uploadLogo,
  }), [parameters, loading, error])

  return <ParametersContext.Provider value={value}>{children}</ParametersContext.Provider>
}

export function useParameters() {
  return useContext(ParametersContext)
}
