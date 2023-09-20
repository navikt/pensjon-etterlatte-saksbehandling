import { BodyShort, Heading, Label, Panel } from '@navikt/ds-react'
import { IBrev } from '~shared/types/Brev'

export default function ({ vedtaksbrev }: { vedtaksbrev: IBrev }) {
  const adresse = vedtaksbrev?.mottaker?.adresse

  return (
    <Panel border>
      <Heading spacing level="2" size="medium">
        Mottaker
      </Heading>

      <BodyShort spacing size="small">
        <Label>Navn</Label>
        <br />
        {vedtaksbrev.mottaker?.navn}
      </BodyShort>

      <BodyShort size="small">
        <Label>Adresse</Label>
        <br />
        {[adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3].join('\n')}
        <br />
        {adresse?.postnummer} {adresse?.poststed}
        <br />
        {adresse?.land} ({adresse?.landkode})
      </BodyShort>
    </Panel>
  )
}
