const TOKEN_KEY = 'barberpro_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

async function request(path, { method = 'GET', body, autenticado = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (autenticado) {
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const resp = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (resp.status === 204) return null;

  let data = null;
  const texto = await resp.text();
  if (texto) {
    try { data = JSON.parse(texto); } catch { data = texto; }
  }

  if (!resp.ok) {
    const mensagem = (data && data.mensagem) || `Erro ${resp.status}`;
    throw new Error(mensagem);
  }

  return data;
}

export const api = {
  get: (path) => request(path),
  post: (path, body, opts) => request(path, { method: 'POST', body, ...opts }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path) => request(path, { method: 'PATCH' }),
  delete: (path) => request(path, { method: 'DELETE' }),
  publica: {
    get: (path) => request(path, { autenticado: false }),
    post: (path, body) => request(path, { method: 'POST', body, autenticado: false }),
  },
};
