import { Alert, Heading, Panel } from '@navikt/ds-react'
import { IBrev, Mottaker } from '~shared/types/Brev'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import React from 'react'
import RedigerMottakerModal from '~components/person/brev/RedigerMottakerModal'

export default function MottakerPanel({
  vedtaksbrev,
  oppdater,
  redigerbar,
}: {
  vedtaksbrev: IBrev
  oppdater: (mottaker: Mottaker) => void
  redigerbar: Boolean
}) {
  const soekerFnr = vedtaksbrev.soekerFnr

  const mottaker = vedtaksbrev.mottaker
  const adresse = mottaker.adresse

  const soekerErIkkeMottaker = soekerFnr !== mottaker.foedselsnummer?.value

  return (
    <Panel border>
      <Heading spacing level="2" size="medium">
        Mottaker
      </Heading>

      {soekerErIkkeMottaker && (
        <Alert variant="info" size="small" inline>
          Søker er ikke mottaker av brevet
        </Alert>
      )}
      <br />

      {redigerbar && <RedigerMottakerModal brev={vedtaksbrev} oppdater={oppdater} />}
      <InfoWrapper>
        <Info label="Navn" tekst={mottaker.navn || '-'} wide />
        {mottaker.foedselsnummer && <Info label="Fødselsnummer" tekst={mottaker.foedselsnummer.value} wide />}
        {mottaker.orgnummer && <Info label="Org.nr." tekst={mottaker.orgnummer} wide />}

        <Info
          wide
          label="Adresse"
          tekst={
            <>
              {[adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3].join('\n')}
              <br />
              {adresse?.postnummer} {adresse?.poststed}
              <br />
              {adresse?.land} ({adresse?.landkode})
            </>
          }
        />
      </InfoWrapper>
    </Panel>
  )
}
