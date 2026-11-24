const nf = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 })
export const fmtWeight = (n: number) => nf.format(n)
/** Значение для поля ввода: десятичная запятая, как на русской клавиатуре. */
export const toInput = (n: number) => String(n).replace('.', ',')
export const fromInput = (s: string) => Number(s.trim().replace(',', '.'))

// Цвета блинов для групп мышц: один и тот же группе всегда достаётся один и тот же цвет
const DOTS = ['bg-plate-red', 'bg-plate-blue', 'bg-plate-yellow', 'bg-plate-green']
export function groupDot(group: string) {
  let h = 0
  for (const ch of group) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return DOTS[h % DOTS.length]
}

export function isHttpUrl(v: string): boolean {
  try {
    const u = new URL(v)
    return u.protocol === 'http:' || u.protocol === 'https:'
  } catch {
    return false
  }
}

export const safeHttpUrl = (v: string | null | undefined) => (v && isHttpUrl(v) ? v : null)

/** Extract a video ID from supported YouTube URL formats. */
export function youtubeVideoId(value: string | null | undefined): string | null {
  const safeUrl = safeHttpUrl(value)
  if (!safeUrl) return null

  try {
    const url = new URL(safeUrl)
    const host = url.hostname.toLowerCase().replace(/^www\./, '')
    let id: string | null = null

    if (host === 'youtu.be') {
      id = url.pathname.split('/').filter(Boolean)[0] ?? null
    } else if (host === 'youtube.com' || host === 'm.youtube.com' || host === 'youtube-nocookie.com') {
      const parts = url.pathname.split('/').filter(Boolean)
      if (url.pathname === '/watch') id = url.searchParams.get('v')
      else if (['embed', 'shorts', 'live'].includes(parts[0] ?? '')) id = parts[1] ?? null
    }

    return id && /^[\w-]{11}$/.test(id) ? id : null
  } catch {
    return null
  }
}

/** Convert supported YouTube links into a privacy-enhanced embed URL. */
export function youtubeEmbedUrl(value: string | null | undefined): string | null {
  const id = youtubeVideoId(value)
  return id ? `https://www.youtube-nocookie.com/embed/${id}?rel=0` : null
}

export function plural(n: number, forms: [string, string, string]): string {
  const m = Math.abs(n) % 100
  const d = m % 10
  if (m > 10 && m < 20) return forms[2]
  if (d > 1 && d < 5) return forms[1]
  if (d === 1) return forms[0]
  return forms[2]
}

export const mmss = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
