import { IBrev } from '~shared/types/Brev'
import { BodyShort, Box, Heading, Label } from '@navikt/ds-react'
import BrevStatusTag from '~components/person/brev/BrevStatusTag'

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
        <BrevStatusTag status={brev.status} />
      </BodyShort>
    </Box>
  )
}
