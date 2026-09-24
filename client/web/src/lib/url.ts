/** "https://www.jenkins.io/doc/book/pipeline/" → "jenkins.io/doc/book/pipeline" (카드에 보이는 짧은 주소) */
export function shortUrl(url: string): string {
  try {
    const u = new URL(url)
    const path = u.pathname.replace(/\/+$/, '')
    return u.host.replace(/^www\./, '') + (path.length > 1 ? path : '')
  } catch {
    return url
  }
}

/**
 * 주소만 보고 제목 후보를 만든다. 브라우저에서는 다른 사이트의 <title>을 읽을 수 없어서(CORS)
 * 경로 마지막 조각을 읽기 좋게 바꾼다. 노션 페이지 주소는 "제목-페이지ID" 모양이면 제목 부분만 쓴다.
 */
export function titleFromUrl(url: string): string {
  let u: URL
  try {
    u = new URL(url)
  } catch {
    return ''
  }
  const host = u.host.replace(/^www\./, '')
  const segs = u.pathname.split('/').filter(Boolean)
  let last = segs[segs.length - 1] ?? ''
  try {
    last = decodeURIComponent(last)
  } catch {
    // 잘못된 인코딩이면 그대로
  }
  last = last.replace(/\.(html?|php|aspx?)$/i, '')
  if (/notion\.(so|site|com)$/.test(host)) {
    const m = /^(.*?)-?[0-9a-f]{32}$/i.exec(last)
    const name = m ? m[1] : ''
    return name ? name.replace(/-/g, ' ') : '노션 페이지'
  }
  if (!last || /^[0-9a-f-]{16,}$/i.test(last)) return host
  const words = last.replace(/[-_+]+/g, ' ').trim()
  return `${words.charAt(0).toUpperCase()}${words.slice(1)} · ${host}`
}

export function firstLetter(title: string): string {
  const ch = [...title.trim()][0] ?? '?'
  return ch.toUpperCase()
}

/**
 * 노션 링크를 브라우저에서 열리는 주소로 바꾼다 (이슈 #132).
 * 이관된 링크는 `https://app.notion.com/p/<페이지ID>` 모양인데, 이 주소는 노션 앱으로 넘어간다.
 * 웹 주소 `https://www.notion.so/<페이지ID>`로 바꿔 연다. 저장된 값은 그대로 둔다.
 */
export function browserUrl(url: string): string {
  try {
    const u = new URL(url)
    if (u.host === 'app.notion.com') {
      const id = u.pathname.split('/').filter(Boolean).pop() ?? ''
      return `https://www.notion.so/${id}${u.search}${u.hash}`
    }
    if (u.protocol === 'notion:') {
      return `https://www.notion.so${u.pathname}${u.search}${u.hash}`
    }
    return url
  } catch {
    return url
  }
}
