import { BrevStatus } from '~shared/types/Brev'
import { Tag } from '@navikt/ds-react'

const mapBrevStatus = (status: BrevStatus) => {
  switch (status) {
    case BrevStatus.OPPRETTET:
      return 'Opprettet'
    case BrevStatus.OPPDATERT:
      return 'Oppdatert'
    case BrevStatus.FERDIGSTILT:
      return 'Ferdigstilt'
    case BrevStatus.JOURNALFOERT:
      return 'Journalført'
    case BrevStatus.DISTRIBUERT:
      return 'Distribuert'
    case BrevStatus.SLETTET:
      return 'Slettet'
    case BrevStatus.UTGAATT:
      return 'Utgått'
  }
}

const tagColors = (status: BrevStatus) => {
  switch (status) {
    case BrevStatus.OPPRETTET:
    case BrevStatus.OPPDATERT:
      return 'neutral'
    case BrevStatus.FERDIGSTILT:
    case BrevStatus.JOURNALFOERT:
      return 'info'
    case BrevStatus.DISTRIBUERT:
      return 'success'
    case BrevStatus.SLETTET:
    case BrevStatus.UTGAATT:
      return 'warning'
  }
}

export default function BrevStatusTag({ status }: { status: BrevStatus }) {
  return (
    <Tag variant={tagColors(status)} size="medium">
      {mapBrevStatus(status)}
    </Tag>
  )
}
