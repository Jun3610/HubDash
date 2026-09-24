import { MoreHorizontal, Plus, Save, Search, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { memos } from '../api/memo'
import { BIG_PAGE, useCreate, useList, useRemove, useUpdate } from '../api/resource'
import type { Memo } from '../api/types'
import { PageHeader } from '../components/layout/PageHeader'
import { Button, ConfirmDialog, EmptyState, IconButton, openable, Peek, QueryState, Segmented } from '../components/ui'
import { useNow } from '../hooks/useToday'
import { datePart, formatShortDate, parseLocalDateTime } from '../lib/date'
import { joinTags, splitTags } from '../lib/format'
import { Markdown } from '../components/ui/Markdown'
import { plainSnippet } from '../lib/markdown'
import { HabitsSummary, LifeSection, ReadingSummary, type LifeSectionKey } from './life/LifeSections'
import s from './memo/Memo.module.css'

const LIMITS = { title: 200, content: 5000, tags: 300 }

/** "방금" / "12분 전" / "3시간 전" / "2일 전" / 일주일 넘으면 "09.12" */
function relTime(at: string, now: Date): string {
  const diff = (now.getTime() - parseLocalDateTime(at).getTime()) / 60000
  if (diff < 1) return '방금'
  if (diff < 60) return `${Math.floor(diff)}분 전`
  if (diff < 60 * 24) return `${Math.floor(diff / 60)}시간 전`
  if (diff < 60 * 24 * 7) return `${Math.floor(diff / 60 / 24)}일 전`
  return formatShortDate(datePart(at))
}

/**
 * 메모: 페이지는 목록 한 화면, 메모 편집·습관·독서는 작은 창 (이슈 #148).
 * 주소: ?id=<메모> / ?new=1 새 메모 / ?tab=habits|reading
 */
export default function MemoPage() {
  const now = useNow()
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState('')
  const [tag, setTag] = useState<string | null>(null)
  const list = useList(memos, { size: BIG_PAGE, sort: 'updatedAt,desc' })
  const all = useMemo(() => list.data?.content ?? [], [list.data])

  const allTags = useMemo(() => {
    const counts = new Map<string, number>()
    for (const m of all) for (const t of splitTags(m.tags)) counts.set(t, (counts.get(t) ?? 0) + 1)
    return [...counts.entries()].sort((a, b) => b[1] - a[1]).map(([t]) => t)
  }, [all])

  const needle = q.trim().toLowerCase()
  const shown = all.filter(
    (m) =>
      (!tag || splitTags(m.tags).includes(tag)) &&
      (!needle || m.title.toLowerCase().includes(needle) || m.content.toLowerCase().includes(needle)),
  )

  const tabParam = params.get('tab')
  const life: LifeSectionKey | null = tabParam === 'habits' || tabParam === 'reading' ? tabParam : null
  const isNew = params.get('new') === '1'
  const idParam = params.get('id')
  // 새 메모가 처음 저장돼 주소가 ?id=로 바뀌어도 같은 편집기를 유지한다 (다시 그리면 입력 중이던 내용이 끊김)
  const [createdId, setCreatedId] = useState<string | null>(null)
  const continuing = !!createdId && createdId === idParam
  const selected = isNew ? null : (all.find((m) => String(m.id) === idParam) ?? null)
  const open = isNew || continuing || !!selected

  const pick = (next: { id?: number; new?: boolean } | null) => {
    setCreatedId(null)
    go(next)
  }
  const go = (next: { id?: number; new?: boolean } | null) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p)
        n.delete('id')
        n.delete('new')
        if (next?.id) n.set('id', String(next.id))
        if (next?.new) n.set('new', '1')
        return n
      },
      { replace: !next || continuing },
    )
  const setLife = (k: LifeSectionKey | null) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p)
        if (k) n.set('tab', k)
        else n.delete('tab')
        return n
      },
      { replace: !k },
    )

  return (
    <>
      <PageHeader title="Memo">
        <Button variant="primary" icon={<Plus size={14} />} onClick={() => pick({ new: true })}>
          새 메모
        </Button>
      </PageHeader>
      <div className={s.board}>
        <div className={s.lifeRow}>
          <div {...openable('습관 열기', () => setLife('habits'))}>
            <HabitsSummary />
          </div>
          <div {...openable('독서 열기', () => setLife('reading'))}>
            <ReadingSummary onOpen={() => setLife('reading')} />
          </div>
        </div>

        <div className={s.filters}>
          <label className={s.search}>
            <Search size={14} />
            <input
              type="text"
              placeholder="제목 · 내용 검색"
              aria-label="메모 검색"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
            {q && (
              <IconButton label="검색어 지우기" size="sm" onClick={() => setQ('')}>
                <X size={13} />
              </IconButton>
            )}
          </label>
          {allTags.length > 0 && (
            <div className={s.tagFilters} role="group" aria-label="태그 필터">
              <button type="button" className={s.tagFilter} aria-pressed={!tag} onClick={() => setTag(null)}>
                전체
              </button>
              {allTags.map((t) => (
                <button
                  key={t}
                  type="button"
                  className={s.tagFilter}
                  aria-pressed={tag === t}
                  onClick={() => setTag(tag === t ? null : t)}
                >
                  {t}
                </button>
              ))}
            </div>
          )}
        </div>

        <QueryState
          loading={list.isLoading}
          error={list.error}
          onRetry={() => void list.refetch()}
          empty={shown.length === 0}
          emptyView={
            <EmptyState
              title={all.length ? '조건에 맞는 메모가 없어요' : '메모가 없어요'}
              action={
                all.length ? undefined : (
                  <Button variant="primary" onClick={() => pick({ new: true })}>
                    새 메모
                  </Button>
                )
              }
            />
          }
        >
          <section aria-label="메모 목록" className={s.cards}>
            {shown.map((m) => (
              <button key={m.id} type="button" className={s.card} onClick={() => pick({ id: m.id })}>
                <div className={s.itemHead}>
                  <span className={s.itemTitle}>{m.title}</span>
                  <span className={s.itemAt}>{relTime(m.updatedAt, now)}</span>
                </div>
                <span className={s.snippet}>{plainSnippet(m.content) || ' '}</span>
                {splitTags(m.tags).length > 0 && (
                  <span className={s.chips}>
                    {splitTags(m.tags).map((t) => (
                      <span key={t} className={s.chip}>
                        {t}
                      </span>
                    ))}
                  </span>
                )}
              </button>
            ))}
          </section>
        </QueryState>
      </div>

      {open && (
        <Peek label={isNew ? '새 메모' : (selected?.title ?? '메모')} onClose={() => pick(null)}>
          <Editor
            key={isNew || continuing ? 'new' : selected!.id}
            memo={selected ?? undefined}
            now={now}
            onCreated={(m) => {
              setCreatedId(String(m.id))
              go({ id: m.id })
            }}
            onDeleted={() => {
              setCreatedId(null)
              go(null)
            }}
          />
        </Peek>
      )}
      {life && (
        <Peek label={life === 'habits' ? '습관' : '독서'} onClose={() => setLife(null)}>
          <LifeSection section={life} />
        </Peek>
      )}
    </>
  )
}

type Draft = { title: string; content: string; tags: string[] }
const key = (d: Draft) => JSON.stringify([d.title.trim(), d.content, joinTags(d.tags)])

function Editor({
  memo,
  now,
  onCreated,
  onDeleted,
}: {
  memo?: Memo
  now: Date
  onCreated: (m: Memo) => void
  onDeleted: () => void
}) {
  const [d, setD] = useState<Draft>({
    title: memo?.title ?? '',
    content: memo?.content ?? '',
    tags: splitTags(memo?.tags),
  })
  const [mode, setMode] = useState<'edit' | 'preview'>(memo ? 'preview' : 'edit')
  const [savedKey, setSavedKey] = useState(memo ? key(d) : '')
  const [adding, setAdding] = useState(false)
  const [tagDraft, setTagDraft] = useState('')
  const [confirm, setConfirm] = useState(false)
  const idRef = useRef<number | undefined>(memo?.id)
  const create = useCreate(memos)
  const update = useUpdate(memos)
  const remove = useRemove(memos)
  const pending = create.isPending || update.isPending
  const error = create.error ?? update.error

  const tagsStr = joinTags(d.tags)
  const problem = !d.title.trim()
    ? '제목을 입력하면 저장돼요'
    : !d.content.trim()
      ? '내용을 입력하면 저장돼요'
      : d.title.length > LIMITS.title
        ? `제목은 ${LIMITS.title}자까지예요`
        : d.content.length > LIMITS.content
          ? `내용은 ${LIMITS.content}자까지예요`
          : tagsStr.length > LIMITS.tags
            ? `태그는 합쳐서 ${LIMITS.tags}자까지예요`
            : null
  const current = key(d)
  const dirty = current !== savedKey

  // 방금 보낸 내용 — 저장 중 표시가 풀린 뒤 저장됨 표시가 늦게 갱신되는 사이에 같은 내용을 또 보내지 않도록 (이슈 #136)
  const sentKey = useRef(savedKey)

  const save = (draft: Draft) => {
    const body = { title: draft.title.trim(), content: draft.content, tags: joinTags(draft.tags) || null }
    const k = key(draft)
    if (k === sentKey.current) return
    sentKey.current = k
    const failed = () => {
      sentKey.current = ''
    }
    if (idRef.current) {
      update.mutate({ id: idRef.current, body }, { onSuccess: () => setSavedKey(k), onError: failed })
    } else if (!create.isPending) {
      create.mutate(body, {
        onSuccess: (m) => {
          idRef.current = m.id
          setSavedKey(k)
          onCreated(m)
        },
        onError: failed,
      })
    }
  }

  // 입력이 멈추고 0.8초 뒤 자동 저장
  useEffect(() => {
    if (!dirty || problem || pending) return
    const t = setTimeout(() => save(d), 800)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, problem, pending])

  const latest = useRef({ d, dirty, problem })
  latest.current = { d, dirty, problem }

  // ⌘S / Ctrl+S로 바로 저장
  useEffect(() => {
    const onKey = (e: globalThis.KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 's') {
        e.preventDefault()
        const l = latest.current
        if (l.dirty && !l.problem) save(l.d)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 다른 메모로 바꾸거나 화면을 떠날 때 저장하지 않은 내용이 있으면 바로 저장
  useEffect(() => {
    const warn = (e: BeforeUnloadEvent) => {
      if (latest.current.dirty) e.preventDefault()
    }
    window.addEventListener('beforeunload', warn)
    return () => {
      window.removeEventListener('beforeunload', warn)
      const l = latest.current
      if (l.dirty && !l.problem && idRef.current) save(l.d)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const addTag = () => {
    const t = tagDraft.trim().replace(/,/g, '')
    if (t && !d.tags.includes(t)) setD({ ...d, tags: [...d.tags, t] })
    setTagDraft('')
    setAdding(false)
  }
  const onTagKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      addTag()
    } else if (e.key === 'Escape') {
      setTagDraft('')
      setAdding(false)
    }
  }

  const status = pending
    ? { color: 'var(--yellow)', text: '저장 중…' }
    : error
      ? { color: 'var(--red)', text: '저장 실패' }
      : dirty
        ? problem
          ? { color: 'var(--text-muted)', text: problem }
          : { color: 'var(--yellow)', text: '저장 안 됨 · 곧 자동 저장' }
        : { color: 'var(--green)', text: '저장됨' }

  return (
    <div className={s.editor}>
      <div className={s.meta}>
        <span className="mono">
          {memo
            ? `생성 ${formatShortDate(datePart(memo.createdAt))} · 수정 ${relTime(memo.updatedAt, now)}`
            : '새 메모'}
        </span>
        <span className={s.status} role="status">
          <span className={s.dot} style={{ background: status.color }} />
          {status.text}
          {error && (
            <Button variant="link" size="sm" onClick={() => save(d)} disabled={!!problem}>
              다시 시도
            </Button>
          )}
        </span>
        <Button
          size="sm"
          variant="primary"
          icon={<Save size={13} />}
          style={{ marginLeft: 'auto' }}
          disabled={!dirty || !!problem || pending}
          title="⌘S"
          onClick={() => save(d)}
        >
          저장
        </Button>
        <span>
          <Segmented
            label="모드"
            value={mode}
            onChange={setMode}
            items={[
              { key: 'edit', label: '편집' },
              { key: 'preview', label: '미리보기' },
            ]}
          />
        </span>
        {idRef.current && (
          <IconButton label="메모 삭제" size="sm" onClick={() => setConfirm(true)}>
            <MoreHorizontal size={15} />
          </IconButton>
        )}
      </div>

      <input
        className={s.titleInput}
        aria-label="제목"
        placeholder="제목"
        value={d.title}
        maxLength={LIMITS.title + 50}
        autoFocus={!memo}
        onChange={(e) => setD({ ...d, title: e.target.value })}
      />

      <div className={s.tags} aria-label="태그">
        {d.tags.map((t) => (
          <span key={t} className={s.tag}>
            {t}
            <button
              type="button"
              aria-label={`${t} 태그 빼기`}
              onClick={() => setD({ ...d, tags: d.tags.filter((x) => x !== t) })}
            >
              <X size={11} />
            </button>
          </span>
        ))}
        {adding ? (
          <input
            className={s.tagInput}
            aria-label="새 태그"
            autoFocus
            value={tagDraft}
            onChange={(e) => setTagDraft(e.target.value)}
            onKeyDown={onTagKey}
            onBlur={addTag}
            maxLength={30}
          />
        ) : (
          <button type="button" className={s.addTag} onClick={() => setAdding(true)}>
            + 태그
          </button>
        )}
      </div>

      <div className={s.divider} />

      {mode === 'edit' ? (
        <>
          <textarea
            className={s.body}
            aria-label="본문 (마크다운)"
            placeholder={'마크다운으로 적어요\n\n# 제목\n- 목록\n**굵게**'}
            value={d.content}
            onChange={(e) => setD({ ...d, content: e.target.value })}
          />
          <span className={s.counter} data-over={d.content.length > LIMITS.content}>
            {d.content.length.toLocaleString()} / {LIMITS.content.toLocaleString()}
          </span>
        </>
      ) : d.content.trim() ? (
        <Markdown source={d.content} />
      ) : (
        <EmptyState compact title="내용이 없어요 — 편집으로 바꿔 적어 보세요" />
      )}

      <ConfirmDialog
        open={confirm}
        title="메모 삭제"
        message={`"${d.title || '제목 없음'}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() =>
          idRef.current &&
          remove.mutate(idRef.current, {
            onSuccess: () => {
              setConfirm(false)
              setSavedKey(current) // 지운 뒤 떠날 때 다시 저장하지 않도록
              latest.current.dirty = false
              idRef.current = undefined
              onDeleted()
            },
          })
        }
      />
    </div>
  )
}
