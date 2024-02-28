import { BrevStatus, IBrev } from '~shared/types/Brev'
import { BodyShort, Box, Heading, Label, Tag } from '@navikt/ds-react'

const mapStatusTilString = (status: BrevStatus) => {
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
  }
}

export default function BrevStatusPanel({ brev }: { brev: IBrev }) {
  return (
    <Box padding="4" borderWidth="1" borderRadius="small" style={{ margin: '1rem' }}>
      <Heading size="medium" spacing>
        Oversikt
      </Heading>

      <BodyShort spacing size="small">
        <Label>ID:</Label>
        <br />
        {brev.id}
      </BodyShort>

      <BodyShort spacing size="small">
        <Label>Søker fnr:</Label>
        <br />
        {brev.soekerFnr}
      </BodyShort>

      <BodyShort spacing size="small">
        <Label>Status:</Label>
        <br />
        <Tag variant="info" size="small">
          {mapStatusTilString(brev.status)}
        </Tag>
      </BodyShort>
    </Box>
  )
}
