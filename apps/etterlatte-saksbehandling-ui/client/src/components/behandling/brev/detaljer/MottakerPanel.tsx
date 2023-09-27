import { Alert, BodyShort, Heading, Panel } from '@navikt/ds-react'
import { IBrev } from '~shared/types/Brev'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'

export default function MottakerPanel({ vedtaksbrev }: { vedtaksbrev: IBrev }) {
  const { soekerFnr, mottaker } = vedtaksbrev
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

      <InfoWrapper>
        <Info label="Navn" tekst={mottaker.navn || '-'} wide />
        {mottaker.foedselsnummer && <Info label="Fødselsnummer" tekst={mottaker.foedselsnummer.value} wide />}
        {mottaker.orgnummer && <Info label="Org.nr." tekst={mottaker.orgnummer} wide />}

        <Info
          wide
          label="Adresse"
          tekst={
            <BodyShort>
              {[adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3].join('\n')}
              <br />
              {adresse?.postnummer} {adresse?.poststed}
              <br />
              {adresse?.land} ({adresse?.landkode})
            </BodyShort>
          }
        />
      </InfoWrapper>
    </Panel>
  )
}
