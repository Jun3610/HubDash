import { describe, expect, it } from 'vitest'
import { firstLetter, shortUrl, titleFromUrl } from './url'

describe('url', () => {
  it('짧은 주소', () => {
    expect(shortUrl('https://www.jenkins.io/doc/book/pipeline/')).toBe('jenkins.io/doc/book/pipeline')
    expect(shortUrl('https://docs.k3s.io/')).toBe('docs.k3s.io')
    expect(shortUrl('not a url')).toBe('not a url')
  })
  it('제목 후보', () => {
    expect(titleFromUrl('https://www.jenkins.io/doc/book/pipeline/')).toBe('Pipeline · jenkins.io')
    expect(titleFromUrl('https://docs.docker.com/build/building/multi-stage/')).toBe('Multi stage · docs.docker.com')
    expect(titleFromUrl('https://docs.k3s.io')).toBe('docs.k3s.io')
    expect(titleFromUrl('https://velog.io/@me/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%B0%B0%EC%B9%98')).toBe(
      '스프링 배치 · velog.io',
    )
    expect(titleFromUrl('nope')).toBe('')
  })
  it('노션 주소', () => {
    expect(titleFromUrl('https://www.notion.so/CI-CD-7122b9e1fd14833ca059019e1c5f1a65')).toBe('CI CD')
    expect(titleFromUrl('https://app.notion.com/p/7122b9e1fd14833ca059019e1c5f1a65')).toBe('노션 페이지')
  })
  it('첫 글자', () => {
    expect(firstLetter('jenkins')).toBe('J')
    expect(firstLetter('도커')).toBe('도')
    expect(firstLetter('')).toBe('?')
  })
})
