import { Link } from 'react-router-dom'
import { PageContent, PageHeader } from '../components/layout/PageHeader'
import { Card, EmptyState } from '../components/ui'

export default function NotFoundPage() {
  return (
    <>
      <PageHeader title="없는 화면" />
      <PageContent>
        <Card>
          <EmptyState title="없는 화면이에요" action={<Link to="/">홈으로</Link>} />
        </Card>
      </PageContent>
    </>
  )
}
