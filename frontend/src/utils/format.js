export function somenteDigitos(value) {
  return (value || '').toString().replace(/\D/g, '')
}

export function formatarCpf(value) {
  const digits = somenteDigitos(value).slice(0, 11)
  let result = digits

  if (digits.length > 9) {
    result = digits.replace(/(\d{3})(\d{3})(\d{3})(\d{0,2})/, '$1.$2.$3-$4')
  } else if (digits.length > 6) {
    result = digits.replace(/(\d{3})(\d{3})(\d{0,3})/, '$1.$2.$3')
  } else if (digits.length > 3) {
    result = digits.replace(/(\d{3})(\d{0,3})/, '$1.$2')
  }

  return result
}

export function formatarData(value) {
  const digits = somenteDigitos(value).slice(0, 8)
  let result = digits

  if (digits.length > 4) {
    result = digits.slice(0, 2) + '/' + digits.slice(2, 4) + '/' + digits.slice(4)
  } else if (digits.length > 2) {
    result = digits.slice(0, 2) + '/' + digits.slice(2)
  }

  return result
}

export function dataParaBackend(value) {
  return formatarData(value)
}

export function moeda(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(value || 0))
}

export const valorParaNumero = (valor) => {
  if (!valor) return 0;
  if (typeof valor === 'number') return valor;

  // Remove todos os pontos de milhar e troca a vírgula decimal por ponto
  const valorLimpo = String(valor).replace(/\./g, '').replace(',', '.');

  return parseFloat(valorLimpo) || 0;
}
