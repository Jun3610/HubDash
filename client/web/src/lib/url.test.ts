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

import { browserUrl } from './url'

describe('노션 링크를 브라우저 주소로', () => {
  it('app.notion.com/p/<id> → www.notion.so/<id>', () => {
    expect(browserUrl('https://app.notion.com/p/7122b9e1fd14833ca059019e1c5f1a65')).toBe(
      'https://www.notion.so/7122b9e1fd14833ca059019e1c5f1a65',
    )
  })
  it('쿼리·해시는 유지', () => {
    expect(browserUrl('https://app.notion.com/p/abc?pvs=4#h1')).toBe('https://www.notion.so/abc?pvs=4#h1')
  })
  it('notion:// 스킴도 웹 주소로', () => {
    expect(browserUrl('notion://www.notion.so/abc')).toBe('https://www.notion.so/abc')
  })
  it('다른 주소는 그대로', () => {
    expect(browserUrl('https://www.notion.so/abc')).toBe('https://www.notion.so/abc')
    expect(browserUrl('https://github.com/x')).toBe('https://github.com/x')
    expect(browserUrl('not a url')).toBe('not a url')
  })
})
