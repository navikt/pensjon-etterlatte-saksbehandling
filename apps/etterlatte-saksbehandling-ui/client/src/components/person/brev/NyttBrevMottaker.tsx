import { IBrev } from '~shared/types/Brev'
import { BodyShort, Heading, Label, Panel } from '@navikt/ds-react'
import RedigerMottakerModal from '~components/person/brev/RedigerMottakerModal'
import { useState } from 'react'

export default function NyttBrevMottaker({ brev }: { brev: IBrev }) {
  const [mottaker, setMottaker] = useState(brev.mottaker)

  const adresse = mottaker?.adresse

  return (
    <div style={{ margin: '1rem' }}>
      <Panel border>
        <Heading spacing level="2" size="medium">
          Mottaker
          <RedigerMottakerModal brev={brev} oppdater={(val) => setMottaker(val)} />
        </Heading>

        <>
          <BodyShort spacing size="small">
            <Label>Navn</Label>
            <br />
            {mottaker?.navn}
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
        </>
      </Panel>
    </div>
  )
}
