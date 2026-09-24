import { useQueryClient } from '@tanstack/react-query'
import { KeyRound, WifiOff } from 'lucide-react'
import { Link } from 'react-router-dom'
import { connectionStatus } from '../../api/client'
import { useStore } from '../../lib/storage'
import { Button } from '../ui'
import s from '../ui/Feedback.module.css'
import { cx } from '../ui'
import { usePeekTo } from './peek'

/** 401·서버 꺼짐 전역 배너. 서버를 필요할 때만 켜는 구조라 offline은 자주 뜬다 — 앱은 그대로 둔다 */
export function GlobalBanners() {
  const peekTo = usePeekTo()
  const status = useStore(connectionStatus)
  const qc = useQueryClient()

  if (status === 'unauthorized') {
    return (
      <div className={cx(s.banner, s.bannerError)} role="alert">
        <KeyRound size={15} className={s.bannerIcon} />
        <span className={s.bannerCode}>401</span>
        <span>API 키를 확인해 주세요</span>
        <div className={s.bannerActions}>
          <Link to={peekTo('settings', 'api')}>설정 열기</Link>
        </div>
      </div>
    )
  }
  if (status === 'offline') {
    return (
      <div className={cx(s.banner, s.bannerWarn)} role="alert">
        <WifiOff size={15} className={s.bannerIcon} />
        <span>서버에 연결할 수 없어요</span>
        <span className="muted" style={{ fontSize: 12 }}>
          로컬 서버가 꺼져 있을 수 있어요
        </span>
        <div className={s.bannerActions}>
          <Button size="sm" onClick={() => void qc.refetchQueries({ type: 'active' })}>
            다시 시도
          </Button>
        </div>
      </div>
    )
  }
  return null
}
