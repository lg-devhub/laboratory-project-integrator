const enderecoApi = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '')

export async function requisicaoApi(caminho: string, opcoes: RequestInit = {}): Promise<Response> {
  const headers = new Headers(opcoes.headers)
  headers.set('Accept', 'application/json')

  if (!['GET', 'HEAD', 'OPTIONS'].includes((opcoes.method || 'GET').toUpperCase())) {
    headers.set('X-STOCKA-Request', 'true')
  }

  try {
    return await fetch(`${enderecoApi}${caminho}`, {
      ...opcoes,
      headers,
      credentials: 'include',
      signal: opcoes.signal ?? AbortSignal.timeout(8000),
    })
  } catch {
    throw new Error('Não foi possível acessar o servidor. Tente novamente mais tarde.')
  }
}
