const API_URL = 'http://localhost:8080'

function getSession() {
  let session = null
  const stored = localStorage.getItem('sigem_session')

  if (stored) {
    try {
      session = JSON.parse(stored)
    } catch (error) {
      localStorage.removeItem('sigem_session')
    }
  }

  return session
}

export async function request(path, options = {}) {
  const session = getSession()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }

  if (session?.token) {
    headers.Authorization = `Bearer ${session.token}`
  }

  const response = await fetch(API_URL + path, {
    ...options,
    headers,
  })
  const text = await response.text()
  let data = null

  if (text) {
    try {
      data = JSON.parse(text)
    } catch (error) {
      data = text
    }
  }

  if (!response.ok) {
    const message = data?.erro || data?.mensagem || 'Erro na requisicao'
    throw new Error(message)
  }

  return data
}

export function get(path) {
  return request(path)
}

export function post(path, body) {
  return request(path, { method: 'POST', body: JSON.stringify(body) })
}

export function put(path, body) {
  return request(path, { method: 'PUT', body: JSON.stringify(body) })
}

export function del(path) {
  return request(path, { method: 'DELETE' })
}
