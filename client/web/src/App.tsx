import { useEffect } from 'react'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { useSettings } from './api/user'
import { AppLayout } from './components/layout/AppLayout'
import { accentStore, applyAccent, applyBorder, applyTheme, avatarColorStore, borderStore } from './config/prefs'
import { useStore } from './lib/storage'
import HealthPage from './pages/HealthPage'
import HomePage from './pages/HomePage'
import HubPage from './pages/HubPage'
import MemoPage from './pages/MemoPage'
import NotFoundPage from './pages/NotFoundPage'
import { CourseRedirect } from './pages/CoursePage'
import PknuPage from './pages/PknuPage'
import SchedulePage from './pages/SchedulePage'
import { SettingsRedirect } from './pages/SettingsPage'
import StudyPage from './pages/StudyPage'

const router = createBrowserRouter(
  [
    {
      element: <AppLayout />,
      children: [
        { path: '/', element: <HomePage /> },
        { path: '/health', element: <HealthPage /> },
        { path: '/pknu', element: <PknuPage /> },
        // 세부 화면은 작은 창으로 (이슈 #148) — 예전 주소는 해당 창이 열린 화면으로
        { path: '/pknu/courses/:id', element: <CourseRedirect /> },
        { path: '/study', element: <StudyPage /> },
        // 생활·습관은 메모의 탭으로 합쳤다 (이슈 #136)
        { path: '/life', element: <Navigate to="/memo?tab=habits" replace /> },
        { path: '/schedule', element: <SchedulePage /> },
        { path: '/memo', element: <MemoPage /> },
        { path: '/hub', element: <HubPage /> },
        // 리마인더는 화면에서 뺐다(이슈 #135) — 예전 주소는 홈으로
        { path: '/reminders', element: <Navigate to="/" replace /> },
        { path: '/settings', element: <SettingsRedirect /> },
        { path: '*', element: <NotFoundPage /> },
      ],
    },
  ],
  {
    future: {
      v7_relativeSplatPath: true,
      v7_fetcherPersist: true,
      v7_normalizeFormMethod: true,
      v7_partialHydration: true,
      v7_skipActionErrorRevalidation: true,
    },
  },
)

/** 서버 설정의 테마와 localStorage 강조색을 <html>에 반영 */
function ThemeSync() {
  const settings = useSettings()
  const accent = useStore(accentStore)
  const avatar = useStore(avatarColorStore)
  const theme = settings.data?.theme
  useEffect(() => applyTheme(theme), [theme])
  useEffect(() => applyAccent(accent), [accent])
  const border = useStore(borderStore)
  useEffect(() => applyBorder(border), [border])
  useEffect(() => {
    document.documentElement.style.setProperty('--avatar', avatar)
    document.documentElement.style.setProperty('--sb-avatar', avatar)
  }, [avatar])
  useEffect(() => {
    if (theme?.toLowerCase() !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: light)')
    const onChange = () => applyTheme('system')
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [theme])
  return null
}

export default function App() {
  return (
    <>
      <ThemeSync />
      <RouterProvider router={router} future={{ v7_startTransition: true }} />
    </>
  )
}
