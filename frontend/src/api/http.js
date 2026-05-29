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
  const isFormData = options.body instanceof FormData
  const headers = { ...(options.headers || {}) }

  if (!isFormData) {
    headers['Content-Type'] = 'application/json'
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
    const error = new Error(data?.erro || data?.mensagem || 'Erro na requisicao')
    if (data?.erros) {
      error.fieldErrors = data.erros
      error.message = Object.values(data.erros).join('. ')
    }
    throw error
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

export function postForm(path, formData) {
  return request(path, { method: 'POST', body: formData })
}

export function del(path) {
  return request(path, { method: 'DELETE' })
}

export async function patch(path, body) {
  return request(path, {
    method: 'PATCH',
    body: JSON.stringify(body)
  })
}