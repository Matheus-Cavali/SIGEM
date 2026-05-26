export function dmyToISO(dmy) {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(dmy ?? '')
  return m ? `${m[3]}-${m[2]}-${m[1]}` : (dmy || '')
}