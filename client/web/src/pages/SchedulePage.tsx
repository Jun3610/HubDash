import { PageContent, PageHeader } from '../components/layout/PageHeader'
import { Card, EmptyState } from '../components/ui'

export default function SchedulePage() {
  return (
    <>
      <PageHeader title="일정" />
      <PageContent>
        <Card>
          <EmptyState title="화면 준비 중" description="곧 구현됩니다." />
        </Card>
      </PageContent>
    </>
  )
}
