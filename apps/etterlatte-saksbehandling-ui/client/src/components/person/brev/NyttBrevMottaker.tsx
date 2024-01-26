import { IBrev } from '~shared/types/Brev'
import { Alert, Heading, Panel } from '@navikt/ds-react'
import RedigerMottakerModal from '~components/person/brev/RedigerMottakerModal'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getVergeadresseForPerson, getVergeadresseFraGrunnlag } from '~shared/api/grunnlag'
import { getData, isSuccessOrNotFound } from '~shared/api/brev'
import { VergeFeilhaandtering } from '~components/person/VergeFeilhaandtering'

import { isSuccess } from '~shared/api/apiUtils'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'

export default function NyttBrevMottaker({ brev }: { brev: IBrev }) {
  const [brevState, setBrevState] = useState(brev)

  const mottaker = brevState.mottaker
  const adresse = mottaker?.adresse

  const [vergeadresse, getVergeadresse] = brev.behandlingId
    ? useApiCall(getVergeadresseFraGrunnlag)
    : useApiCall(getVergeadresseForPerson)

  useEffect(() => {
    if (brev.behandlingId) {
      getVergeadresse(brev.behandlingId)
    } else {
      getVergeadresse(brev.soekerFnr)
    }
  }, [brev])

  return (
    <div style={{ margin: '1rem' }}>
      {isSuccess(vergeadresse) && (
        <Alert variant="info" size="small" inline>
          Søker har verge
        </Alert>
      )}
      {isSuccessOrNotFound(vergeadresse) && (
        <Panel border>
          <Heading spacing level="2" size="medium">
            Mottaker
            <RedigerMottakerModal
              brev={brevState}
              oppdater={(val) => setBrevState({ ...brevState, mottaker: val })}
              vergeadresse={getData(vergeadresse)}
            />
          </Heading>

          <InfoWrapper>
            <Info
              wide
              label="Navn"
              tekst={
                /[a-zA-Z\s]/.test(mottaker.navn) ? (
                  mottaker.navn
                ) : (
                  <Alert variant="error" size="small" inline>
                    Navn mangler
                  </Alert>
                )
              }
            />
            {mottaker.foedselsnummer && <Info label="Fødselsnummer" tekst={mottaker.foedselsnummer.value} wide />}
            {mottaker.orgnummer && <Info label="Org.nr." tekst={mottaker.orgnummer} wide />}

            <Info
              wide
              label="Adresse"
              tekst={
                <>
                  {!adresse?.adresselinje1 && !adresse?.adresselinje2 && !adresse?.adresselinje3 ? (
                    <Alert variant="warning" size="small" inline>
                      Adresselinjer mangler
                    </Alert>
                  ) : (
                    [adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3]
                      .filter((linje) => !!linje)
                      .map((linje, i) => <div key={`adresselinje-${i}`}>{linje}</div>)
                  )}
                </>
              }
            />

            <Info
              wide
              label="Postnummer-/sted"
              tekst={
                !adresse?.postnummer && !adresse?.poststed ? (
                  <Alert variant="warning" size="small" inline>
                    Postnummer og -sted mangler
                  </Alert>
                ) : (
                  <>
                    {adresse?.postnummer} {adresse?.poststed}
                  </>
                )
              }
            />

            <Info
              wide
              label="Land"
              tekst={
                <>
                  {adresse?.land || (
                    <Alert variant="error" size="small" inline>
                      Land mangler
                    </Alert>
                  )}
                  {!!adresse?.landkode ? (
                    `(${adresse.landkode})`
                  ) : (
                    <Alert variant="error" size="small" inline>
                      Landkode mangler
                    </Alert>
                  )}
                </>
              }
            />
          </InfoWrapper>
        </Panel>
      )}
      {VergeFeilhaandtering(vergeadresse)}
    </div>
  )
}
