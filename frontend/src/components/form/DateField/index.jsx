import { useState, useRef, useEffect, useMemo } from 'react'
import BaseField from '../BaseField'
import './DateField.scss'

const TZ_OFFSET_MS = -3 * 60 * 60 * 1000

function nowUTC3() {
  const utcMs = Date.now()
  const localMs = utcMs + TZ_OFFSET_MS
  const d = new Date(localMs)
  return { year: d.getUTCFullYear(), month: d.getUTCMonth() + 1, day: d.getUTCDate() }
}

function safeDate(year, month, day) {
  return new Date(Date.UTC(year, month - 1, day))
}

function parseISO(iso) {
  if (!iso) return null
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  if (!m) return null
  const y = +m[1], mo = +m[2], d = +m[3]
  if (!isValidDate(y, mo, d)) return null
  return { year: y, month: mo, day: d }
}

function toISO({ year, month, day }) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function toDMY({ year, month, day }) {
  return `${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}/${year}`
}

function isLeapYear(y) {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0
}

function daysInMonth(year, month) {
  if (month === 2) return isLeapYear(year) ? 29 : 28
  return [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1]
}

function isValidDate(year, month, day) {
  if (year < 1 || month < 1 || month > 12) return false
  if (day < 1 || day > daysInMonth(year, month)) return false
  return true
}

function weeksOfMonth(year, month) {
  const firstDay = safeDate(year, month, 1).getUTCDay() // 0=Dom
  const total = daysInMonth(year, month)
  const cells = []

  for (let i = 0; i < firstDay; i++) cells.push(null)
  for (let d = 1; d <= total; d++) cells.push(d)

  const weeks = []
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7).concat(Array(7).fill(null)).slice(0, 7))
  }
  return weeks
}

const MONTHS_PT = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
]

const WEEKDAYS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

const MIN_DATE_DEFAULT = { year: 1920, month: 1, day: 1 }

export default function DateField({
  value,
  setValue, 
  label,
  placeholder,
  disabled,
  required,
  error,
  minDate,      // padrão: "1920-01-01"
  maxDate,      // padrão: data de hoje no fusu-horário UTC-3
}) {
  const [open, setOpen] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const [viewYear, setViewYear] = useState(null)
  const [viewMonth, setViewMonth] = useState(null)
  const [yearSelectOpen, setYearSelectOpen] = useState(false)

  const containerRef = useRef(null)
  const inputRef = useRef(null)

  const minParsed = useMemo(() => parseISO(minDate) ?? MIN_DATE_DEFAULT, [minDate])
  const maxParsed = useMemo(() => {
    if (maxDate) return parseISO(maxDate)
    const t = nowUTC3()
    return t
  }, [maxDate])

  const selected = useMemo(() => parseISO(value), [value])

  useEffect(() => {
    if (selected) {
      setInputValue(toDMY(selected))
    } else {
      setInputValue('')
    }
  }, [value])

  useEffect(() => {
    if (open) {
      const base = selected ?? nowUTC3()
      setViewYear(base.year)
      setViewMonth(base.month)
      setYearSelectOpen(false)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    function handleClick(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    function handleKey(e) {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('mousedown', handleClick)
      document.removeEventListener('keydown', handleKey)
    }
  }, [open])

  function isWithinRange(date, min, max) {
    const d = safeDate(date.year, date.month, date.day)
    const mn = safeDate(min.year, min.month, min.day)
    const mx = max ? safeDate(max.year, max.month, max.day) : null
    if (d < mn) return false
    if (mx && d > mx) return false
    return true
  }

  function isDayDisabled(day) {
    if (!day) return true
    return !isWithinRange({ year: viewYear, month: viewMonth, day }, minParsed, maxParsed)
  }

  function isDaySelected(day) {
    if (!day || !selected) return false
    return selected.year === viewYear && selected.month === viewMonth && selected.day === day
  }

  function isDayToday(day) {
    if (!day) return false
    const t = nowUTC3()
    return t.year === viewYear && t.month === viewMonth && t.day === day
  }

  function selectDay(day) {
    if (isDayDisabled(day)) return
    const candidate = { year: viewYear, month: viewMonth, day }
    setValue(toISO(candidate))
    setOpen(false)
  }

  function prevMonth() {
    if (viewMonth === 1) { setViewMonth(12); setViewYear(y => y - 1) }
    else setViewMonth(m => m - 1)
  }

  function nextMonth() {
    if (viewMonth === 12) { setViewMonth(1); setViewYear(y => y + 1) }
    else setViewMonth(m => m + 1)
  }

  function canGoPrev() {
    return viewYear > minParsed.year || (viewYear === minParsed.year && viewMonth > minParsed.month)
  }

  function canGoNext() {
    if (!maxParsed) return true
    return viewYear < maxParsed.year || (viewYear === maxParsed.year && viewMonth < maxParsed.month)
  }

  const availableYears = useMemo(() => {
    const start = minParsed.year
    const end = maxParsed ? maxParsed.year : nowUTC3().year
    const years = []
    for (let y = end; y >= start; y--) years.push(y)
    return years
  }, [minParsed, maxParsed])

  const weeks = useMemo(() => {
    if (viewYear == null || viewMonth == null) return []
    return weeksOfMonth(viewYear, viewMonth)
  }, [viewYear, viewMonth])


  return (
    <BaseField label={label} error={error} required={required}>
      <div className={`date-field${disabled ? ' date-field--disabled' : ''}`} ref={containerRef}>
        <div className="date-field__input-wrapper">
          <input
            ref={inputRef}
            className="field-control date-field__input"
            type="text"
            value={inputValue}
            disabled={disabled}
            placeholder={placeholder || 'dd/mm/aaaa'}
            inputMode="numeric"
            maxLength={10}
            autoComplete="off"
            readOnly
            style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
            onFocus={() => { if (!disabled) setOpen(true) }}
            aria-label={label || 'Data'}
            aria-expanded={open}
            aria-haspopup="dialog"
          />
          <button
            type="button"
            className="date-field__icon-btn"
            disabled={disabled}
            tabIndex={-1}
            aria-label="Abrir calendário"
            onClick={() => {
              if (!disabled) {
                setOpen(o => !o)
                inputRef.current?.focus()
              }
            }}
          >
            <CalendarIcon />
          </button>
        </div>

        {open && !disabled && viewYear != null && (
          <div
            className="date-field__popup"
            role="dialog"
            aria-label="Calendário"
            onMouseDown={e => e.preventDefault()}
          >
            <div className="date-field__cal-header">
              <button
                type="button"
                className="date-field__nav-btn"
                onClick={prevMonth}
                disabled={!canGoPrev()}
                aria-label="Mês anterior"
              >
                ‹
              </button>

              <div className="date-field__month-year">
                <span className="date-field__month-label">
                  {MONTHS_PT[viewMonth - 1]}
                </span>

                <button
                  type="button"
                  className="date-field__year-btn"
                  onClick={() => setYearSelectOpen(o => !o)}
                  aria-label="Selecionar ano"
                >
                  {viewYear}
                  <span className="date-field__year-caret">{yearSelectOpen ? '▴' : '▾'}</span>
                </button>
              </div>

              <button
                type="button"
                className="date-field__nav-btn"
                onClick={nextMonth}
                disabled={!canGoNext()}
                aria-label="Próximo mês"
              >
                ›
              </button>
            </div>

            {/* Year selector dropdown */}
            {yearSelectOpen && (
              <div className="date-field__year-list" role="listbox" aria-label="Anos">
                {availableYears.map(y => (
                  <button
                    key={y}
                    type="button"
                    role="option"
                    aria-selected={y === viewYear}
                    className={`date-field__year-option${y === viewYear ? ' date-field__year-option--selected' : ''}`}
                    onClick={() => {
                      setViewYear(y)
                      // Ajusta mês se necessário nos limites
                      if (maxParsed && y === maxParsed.year && viewMonth > maxParsed.month) {
                        setViewMonth(maxParsed.month)
                      }
                      if (y === minParsed.year && viewMonth < minParsed.month) {
                        setViewMonth(minParsed.month)
                      }
                      setYearSelectOpen(false)
                    }}
                  >
                    {y}
                  </button>
                ))}
              </div>
            )}

            {!yearSelectOpen && (
              <>
                <div className="date-field__weekdays">
                  {WEEKDAYS.map(d => (
                    <span key={d} className="date-field__weekday">{d}</span>
                  ))}
                </div>

                <div className="date-field__days">
                  {weeks.map((week, wi) => (
                    <div key={wi} className="date-field__week">
                      {week.map((day, di) => (
                        <button
                          key={di}
                          type="button"
                          className={[
                            'date-field__day',
                            !day ? 'date-field__day--empty' : '',
                            isDayDisabled(day) ? 'date-field__day--disabled' : '',
                            isDaySelected(day) ? 'date-field__day--selected' : '',
                            isDayToday(day) ? 'date-field__day--today' : '',
                          ].filter(Boolean).join(' ')}
                          disabled={!day || isDayDisabled(day)}
                          onClick={() => selectDay(day)}
                          aria-label={day ? `${day} de ${MONTHS_PT[viewMonth - 1]} de ${viewYear}` : undefined}
                          aria-pressed={isDaySelected(day)}
                          tabIndex={!day || isDayDisabled(day) ? -1 : 0}
                        >
                          {day || ''}
                        </button>
                      ))}
                    </div>
                  ))}
                </div>

                {selected && (
                  <div className="date-field__footer">
                    <button
                      type="button"
                      className="date-field__clear-btn"
                      onClick={() => { setValue(''); setOpen(false) }}
                    >
                      Limpar
                    </button>
                    <span className="date-field__selected-label">
                      {toDMY(selected)}
                    </span>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </BaseField>
  )
}

function CalendarIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
      <rect x="1" y="3" width="14" height="12" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
      <path d="M1 7h14" stroke="currentColor" strokeWidth="1.4"/>
      <path d="M5 1v4M11 1v4" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
      <circle cx="5" cy="10.5" r="0.9" fill="currentColor"/>
      <circle cx="8" cy="10.5" r="0.9" fill="currentColor"/>
      <circle cx="11" cy="10.5" r="0.9" fill="currentColor"/>
      <circle cx="5" cy="13" r="0.9" fill="currentColor"/>
      <circle cx="8" cy="13" r="0.9" fill="currentColor"/>
    </svg>
  )
}