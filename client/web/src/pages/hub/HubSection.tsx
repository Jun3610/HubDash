import { MoreHorizontal, Pin, Plus } from 'lucide-react'
import { useRef, useState, type FormEvent } from 'react'
import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { hubCategories, hubLinks } from '../../api/hub'
import { useCreate, useRemove, useUpdate } from '../../api/resource'
import type { HubCategory, HubLink } from '../../api/types'
import {
  Button,
  ConfirmDialog,
  EmptyState,
  Field,
  FormError,
  IconButton,
  Input,
  Modal,
  Peek,
  QueryState,
  RowActions,
  Segmented,
  Select,
  Table,
} from '../../components/ui'
import { pinnedLinksStore } from '../../config/prefs'
import { useAllHubLinks } from '../../hooks/useHub'
import { datePart, formatShortDate } from '../../lib/date'
import { createStore, useStore } from '../../lib/storage'
import { sortBy } from '../../lib/select/range'
import { browserUrl, firstLetter, shortUrl, titleFromUrl } from '../../lib/url'
import { hasErrors, isUrl, maxLen, optStr, required, type Errors } from '../../lib/validate'
import s from './Hub.module.css'

const viewStore = createStore<'card' | 'list'>('hubdash.hubView', 'card')

function abbr(name: string): string {
  const latin = name.replace(/[^A-Za-z0-9]/g, '')
  return (latin.length >= 2 ? latin.slice(0, 2) : name.slice(0, 2)).toUpperCase()
}

/**
 * 허브 (이슈 #162에서 Study로 합침): 카테고리 상자를 누르면 작은 창에 그 카테고리의 링크 목록,
 * 링크를 누르면 노션이 새 탭으로 열린다. 링크·카테고리 추가/수정/삭제/고정도 창 안에서.
 * 주소: ?hub=<카테고리 id> (예전 /hub, /hub/:id 는 여기로 넘김)
 */
export function HubSection() {
  const [params, setParams] = useSearchParams()
  const hub = useAllHubLinks()
  const view = useStore(viewStore)
  const pinned = useStore(pinnedLinksStore)
  const [catDialog, setCatDialog] = useState<{ category?: HubCategory } | null>(null)
  const [editLink, setEditLink] = useState<HubLink | null>(null)
  const [delLink, setDelLink] = useState<HubLink | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)
  const remove = useRemove(hubLinks)

  const catId = Number(params.get('hub')) || null
  const category = hub.categories.find((c) => c.id === catId) ?? null

  const setHub = (id: number | null) => {
    setFormOpen(false)
    setParams(
      (p) => {
        const n = new URLSearchParams(p)
        if (id === null) n.delete('hub')
        else n.set('hub', String(id))
        return n
      },
      { replace: id === null },
    )
  }
  const openForm = () => {
    setFormOpen(true)
    setTimeout(() => formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 0)
  }

  const links = category ? sortBy(hub.linksByCategory.get(category.id) ?? [], (l) => l.createdAt, 'desc') : []
  const togglePin = (l: HubLink) =>
    pinnedLinksStore.set((xs) => (xs.includes(l.id) ? xs.filter((x) => x !== l.id) : [...xs, l.id]))

  return (
    <section aria-label="허브" className={s.section}>
      <div className={s.sectionHead}>
        <h2>허브</h2>
        <span className="muted" style={{ fontSize: 12 }}>
          {hub.categories.length}개 카테고리 · {hub.links.length}개 링크 · 누르면 노션 글 목록
        </span>
        <Button size="sm" style={{ marginLeft: 'auto' }} onClick={() => setCatDialog({})}>
          Add Category
        </Button>
      </div>
      <QueryState
        loading={hub.isLoading && hub.categories.length === 0}
        error={hub.error}
        onRetry={hub.refetch}
        empty={hub.categories.length === 0}
        emptyView={
          <div className={s.box}>
            <EmptyState
              title="카테고리가 없어요"
              description="노션 글 링크를 모을 카테고리를 먼저 만들어 보세요."
              action={
                <Button variant="primary" onClick={() => setCatDialog({})}>
                  카테고리 추가
                </Button>
              }
            />
          </div>
        }
      >
        <div className={s.catGrid}>
          {hub.categories.map((c) => {
            const list = sortBy(hub.linksByCategory.get(c.id) ?? [], (l) => l.createdAt, 'desc')
            return (
              <button key={c.id} type="button" className={s.catCard} onClick={() => setHub(c.id)}>
                <div className={s.catCardHead}>
                  <span className={s.badge}>{abbr(c.name)}</span>
                  <span className={s.catName}>{c.name}</span>
                  <span className={s.catCount}>{list.length}</span>
                </div>
                {c.description && <span className={s.catDesc}>{c.description}</span>}
                <ul className={s.catLinks}>
                  {list.slice(0, 3).map((l) => (
                    <li key={l.id} className="ellipsis">
                      {l.title}
                    </li>
                  ))}
                  {list.length === 0 && <li className="muted">링크가 없어요</li>}
                </ul>
              </button>
            )
          })}
        </div>
      </QueryState>

      {category && (
        <Peek
          label={`${category.name} 링크`}
          onClose={() => setHub(null)}
          actions={
            <>
              <Button size="sm" onClick={() => setCatDialog({ category })}>
                카테고리 수정
              </Button>
              <Button size="sm" variant="primary" icon={<Plus size={13} />} onClick={openForm}>
                링크 추가
              </Button>
            </>
          }
        >
          <div className={s.head}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0 }}>
              <h1>{category.name}</h1>
              <span className="muted">{category.description ?? '설명이 없어요'}</span>
            </div>
            <span style={{ marginLeft: 'auto' }}>
              <Segmented
                label="보기"
                value={view}
                onChange={(v) => viewStore.set(v)}
                items={[
                  { key: 'card', label: '카드' },
                  { key: 'list', label: '목록' },
                ]}
              />
            </span>
          </div>
          {view === 'card' ? (
            <div className={s.cards}>
              {links.map((l) => (
                <LinkCard
                  key={l.id}
                  link={l}
                  pinned={pinned.includes(l.id)}
                  onEdit={() => setEditLink(l)}
                  onDelete={() => setDelLink(l)}
                  onPin={() => togglePin(l)}
                />
              ))}
              <button type="button" className={s.addCard} onClick={openForm}>
                <Plus size={14} />이 카테고리에 링크 추가
              </button>
            </div>
          ) : links.length === 0 ? (
            <div className={s.box}>
              <EmptyState title="링크가 없어요" action={<Button onClick={openForm}>링크 추가</Button>} />
            </div>
          ) : (
            <div className={s.box}>
              <Table>
                <thead>
                  <tr>
                    <th scope="col">제목</th>
                    <th scope="col" className={s.hideMobile}>
                      주소
                    </th>
                    <th scope="col" className={s.hideMobile}>
                      추가일
                    </th>
                    <th scope="col">
                      <span className="sr-only">동작</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {links.map((l) => (
                    <tr key={l.id} className="hover-row">
                      <td>
                        {pinned.includes(l.id) && (
                          <Pin size={11} style={{ marginRight: 4, color: 'var(--accent)' }} aria-label="고정됨" />
                        )}
                        <a
                          href={browserUrl(l.url)}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{ color: 'var(--text)' }}
                        >
                          {l.title}
                        </a>
                      </td>
                      <td className={`${s.hideMobile} mono muted`} style={{ fontSize: 12 }}>
                        {shortUrl(l.url)}
                      </td>
                      <td className={`${s.hideMobile} mono muted`} style={{ fontSize: 12 }}>
                        {formatShortDate(datePart(l.createdAt))}
                      </td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        <IconButton
                          label={pinned.includes(l.id) ? `${l.title} 고정 해제` : `${l.title} 고정`}
                          size="sm"
                          onClick={() => togglePin(l)}
                        >
                          <Pin size={13} />
                        </IconButton>
                        <RowActions label={l.title} onEdit={() => setEditLink(l)} onDelete={() => setDelLink(l)} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
          {formOpen && (
            <LinkForm
              formRef={formRef}
              categories={hub.categories}
              defaultCategoryId={category.id}
              onClose={() => setFormOpen(false)}
            />
          )}
        </Peek>
      )}

      {catDialog && (
        <CategoryModal
          category={catDialog.category}
          linkCount={catDialog.category ? (hub.linksByCategory.get(catDialog.category.id)?.length ?? 0) : 0}
          onClose={() => setCatDialog(null)}
          onSaved={(c) => setHub(c.id)}
          onDeleted={() => setHub(null)}
        />
      )}
      {editLink && <LinkModal link={editLink} categories={hub.categories} onClose={() => setEditLink(null)} />}
      <ConfirmDialog
        open={!!delLink}
        title="링크 삭제"
        message={delLink && `"${delLink.title}"을(를) 지울까요?`}
        busy={remove.isPending}
        onClose={() => setDelLink(null)}
        onConfirm={() =>
          delLink &&
          remove.mutate(delLink.id, {
            onSuccess: () => {
              pinnedLinksStore.set((xs) => xs.filter((x) => x !== delLink.id))
              setDelLink(null)
            },
          })
        }
      />
    </section>
  )
}

/** 예전 허브 주소(/hub, /hub/:id, /hub?category=)는 Study의 허브로 (이슈 #162) */
export function HubRedirect() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const cat = id ?? params.get('category')
  return <Navigate to={cat ? `/study?hub=${cat}` : '/study'} replace />
}

function LinkCard({
  link,
  pinned,
  categoryName,
  onEdit,
  onDelete,
  onPin,
}: {
  link: HubLink
  pinned: boolean
  categoryName?: string
  onEdit: () => void
  onDelete: () => void
  onPin: () => void
}) {
  const [menu, setMenu] = useState(false)
  const act = (fn: () => void) => () => {
    setMenu(false)
    fn()
  }
  return (
    // 카드 아무 곳이나 누르면 새 탭으로 (메뉴 버튼 제외, 이슈 #132)
    <article
      className={`${s.card} ${s.cardClickable}`}
      onClick={(e) => {
        if ((e.target as HTMLElement).closest('button, a, [role=menu]')) return
        window.open(browserUrl(link.url), '_blank', 'noopener,noreferrer')
      }}
    >
      <div className={s.cardHead}>
        <span className={s.letter} aria-hidden="true">
          {firstLetter(link.title)}
        </span>
        <div className={s.titleCol}>
          {/* 결정(이슈 #105): 노션 링크 포함 모든 링크는 브라우저 새 탭 */}
          <a
            href={browserUrl(link.url)}
            target="_blank"
            rel="noopener noreferrer"
            className={s.linkTitle}
            title={link.url}
          >
            {link.title}
          </a>
          <span className={s.domain}>{shortUrl(link.url)}</span>
        </div>
        <IconButton label={`${link.title} 메뉴`} size="sm" aria-expanded={menu} onClick={() => setMenu((v) => !v)}>
          <MoreHorizontal size={15} />
        </IconButton>
      </div>
      {link.description && <span className={s.desc}>{link.description}</span>}
      <span className={s.at}>
        {formatShortDate(datePart(link.createdAt))} 추가
        {categoryName && <span>· {categoryName}</span>}
        {pinned && (
          <span style={{ color: 'var(--accent)', display: 'inline-flex', alignItems: 'center', gap: 2 }}>
            <Pin size={11} /> 고정됨
          </span>
        )}
      </span>
      {menu && (
        <>
          <div style={{ position: 'fixed', inset: 0, zIndex: 9 }} onClick={() => setMenu(false)} />
          <div className={s.menu} role="menu">
            <button type="button" role="menuitem" onClick={act(onPin)}>
              {pinned ? '고정 해제' : '사이드바에 고정'}
            </button>
            <button type="button" role="menuitem" onClick={act(onEdit)}>
              수정
            </button>
            <button type="button" role="menuitem" className={s.danger} onClick={act(onDelete)}>
              삭제
            </button>
          </div>
        </>
      )}
    </article>
  )
}

// ---- 링크 추가 (본문 아래 폼) ----

interface LinkDraft {
  url: string
  title: string
  categoryId: string
  description: string
}

function validateLink(d: LinkDraft): Errors<keyof LinkDraft> {
  return {
    url: required(d.url, '주소') ?? isUrl(d.url.trim()) ?? maxLen(d.url, 1000),
    title: required(d.title, '제목') ?? maxLen(d.title, 200),
    categoryId: required(d.categoryId, '카테고리'),
    description: maxLen(d.description, 500),
  }
}

function LinkForm({
  formRef,
  categories,
  defaultCategoryId,
  onClose,
}: {
  formRef: React.Ref<HTMLFormElement>
  categories: HubCategory[]
  defaultCategoryId: number
  onClose: () => void
}) {
  const [d, setD] = useState<LinkDraft>({ url: '', title: '', categoryId: String(defaultCategoryId), description: '' })
  const [errors, setErrors] = useState<Errors<keyof LinkDraft>>({})
  const [titleTouched, setTitleTouched] = useState(false)
  const create = useCreate(hubLinks)
  const setUrl = (url: string) => {
    // 제목을 직접 고치기 전까지는 주소에서 제목 후보를 채운다
    setD((cur) => ({ ...cur, url, title: titleTouched ? cur.title : titleFromUrl(url.trim()) }))
  }
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = validateLink(d)
    setErrors(errs)
    if (hasErrors(errs)) return
    create.mutate(
      {
        categoryId: Number(d.categoryId),
        url: d.url.trim(),
        title: d.title.trim(),
        description: optStr(d.description),
      },
      {
        onSuccess: () => {
          setD({ url: '', title: '', categoryId: d.categoryId, description: '' })
          setTitleTouched(false)
          setErrors({})
        },
      },
    )
  }
  return (
    <form ref={formRef} aria-label="링크 추가" className={s.form} onSubmit={submit} noValidate>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>링크 추가</h2>
        <span className="muted" style={{ marginLeft: 'auto', fontSize: 12 }}>
          주소를 붙여넣으면 제목을 먼저 채워둬요
        </span>
      </div>
      <div className={s.formRow}>
        <Field label="주소" required error={errors.url}>
          <Input
            mono
            autoFocus
            type="url"
            placeholder="https://"
            value={d.url}
            onChange={(e) => setUrl(e.target.value)}
          />
        </Field>
        <Field label="제목" required error={errors.title}>
          <Input
            value={d.title}
            onChange={(e) => {
              setTitleTouched(true)
              setD({ ...d, title: e.target.value })
            }}
          />
        </Field>
        <Field label="카테고리" required error={errors.categoryId}>
          <Select value={d.categoryId} onChange={(e) => setD({ ...d, categoryId: e.target.value })}>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </Field>
      </div>
      <Field label="설명 (선택)" error={errors.description}>
        <Input value={d.description} onChange={(e) => setD({ ...d, description: e.target.value })} />
      </Field>
      <FormError error={create.error} />
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button onClick={onClose}>닫기</Button>
        <Button type="submit" variant="primary" disabled={create.isPending}>
          {create.isPending ? '저장 중…' : '저장'}
        </Button>
      </div>
    </form>
  )
}

// ---- 모달 ----

function LinkModal({ link, categories, onClose }: { link: HubLink; categories: HubCategory[]; onClose: () => void }) {
  const [d, setD] = useState<LinkDraft>({
    url: link.url,
    title: link.title,
    categoryId: String(link.categoryId),
    description: link.description ?? '',
  })
  const [errors, setErrors] = useState<Errors<keyof LinkDraft>>({})
  const update = useUpdate(hubLinks)
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = validateLink(d)
    setErrors(errs)
    if (hasErrors(errs)) return
    update.mutate(
      {
        id: link.id,
        body: {
          categoryId: Number(d.categoryId),
          url: d.url.trim(),
          title: d.title.trim(),
          description: optStr(d.description),
        },
      },
      { onSuccess: onClose },
    )
  }
  return (
    <Modal
      open
      onClose={onClose}
      title="링크 수정"
      footer={
        <>
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="link-form" disabled={update.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form id="link-form" onSubmit={submit} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <Field label="주소" required error={errors.url}>
          <Input mono value={d.url} onChange={(e) => setD({ ...d, url: e.target.value })} />
        </Field>
        <Field label="제목" required error={errors.title}>
          <Input value={d.title} onChange={(e) => setD({ ...d, title: e.target.value })} />
        </Field>
        <Field label="카테고리" required>
          <Select value={d.categoryId} onChange={(e) => setD({ ...d, categoryId: e.target.value })}>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="설명" error={errors.description}>
          <Input value={d.description} onChange={(e) => setD({ ...d, description: e.target.value })} />
        </Field>
        <FormError error={update.error} />
      </form>
    </Modal>
  )
}

function CategoryModal({
  category,
  linkCount,
  onClose,
  onSaved,
  onDeleted,
}: {
  category?: HubCategory
  linkCount: number
  onClose: () => void
  onSaved: (c: HubCategory) => void
  onDeleted: () => void
}) {
  const [name, setName] = useState(category?.name ?? '')
  const [description, setDescription] = useState(category?.description ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [confirm, setConfirm] = useState(false)
  const create = useCreate(hubCategories)
  const update = useUpdate(hubCategories)
  const remove = useRemove(hubCategories)
  const m = category ? update : create
  const submit = (e: FormEvent) => {
    e.preventDefault()
    const errs = { name: required(name, '이름') ?? maxLen(name, 100), description: maxLen(description, 500) }
    setErrors(errs)
    if (hasErrors(errs)) return
    const body = { name: name.trim(), description: optStr(description) }
    const done = (c: HubCategory) => {
      onSaved(c)
      onClose()
    }
    if (category) update.mutate({ id: category.id, body }, { onSuccess: done })
    else create.mutate(body, { onSuccess: done })
  }
  return (
    <Modal
      open
      onClose={onClose}
      title={category ? '카테고리 수정' : '카테고리 추가'}
      footer={
        <>
          {category && (
            <Button variant="danger" style={{ marginRight: 'auto' }} onClick={() => setConfirm(true)}>
              삭제
            </Button>
          )}
          <Button onClick={onClose}>취소</Button>
          <Button variant="primary" type="submit" form="category-form" disabled={m.isPending}>
            저장
          </Button>
        </>
      }
    >
      <form
        id="category-form"
        onSubmit={submit}
        noValidate
        style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
      >
        <Field label="이름" required error={errors.name}>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="CI/CD" />
        </Field>
        <Field label="설명" error={errors.description}>
          <Input value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <FormError error={m.error ?? remove.error} />
      </form>
      <ConfirmDialog
        open={confirm}
        title="카테고리 삭제"
        message={
          linkCount
            ? `링크 ${linkCount}개가 남아 있어요. 서버가 거부할 수 있어요. 지울까요?`
            : '이 카테고리를 지울까요?'
        }
        busy={remove.isPending}
        onClose={() => setConfirm(false)}
        onConfirm={() =>
          category &&
          remove.mutate(category.id, {
            onSuccess: () => {
              onDeleted()
              onClose()
            },
            onError: () => setConfirm(false),
          })
        }
      />
    </Modal>
  )
}
