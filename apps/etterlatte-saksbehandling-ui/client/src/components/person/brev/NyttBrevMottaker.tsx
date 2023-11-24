import { IBrev } from '~shared/types/Brev'
import { Alert, BodyShort, Heading, Label, Panel } from '@navikt/ds-react'
import RedigerMottakerModal from '~components/person/brev/RedigerMottakerModal'
import React, { useEffect, useState } from 'react'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { getVergeadresseFraGrunnlag } from '~shared/api/grunnlag'
import { getData, isSuccessOrNotFound } from '~shared/api/brev'
import { handleHentVergeadresseError } from '~components/person/Vergeadresse'

export default function NyttBrevMottaker({ brev }: { brev: IBrev }) {
  const [brevState, setBrevState] = useState(brev)

  const mottaker = brevState.mottaker
  const adresse = mottaker?.adresse

  const [vergeadresse, getVergeadresse] = useApiCall(getVergeadresseFraGrunnlag)

  useEffect(() => {
    if (brev.behandlingId) {
      getVergeadresse(brev.behandlingId)
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
      )}
      {isFailure(vergeadresse) && handleHentVergeadresseError(vergeadresse)}
    </div>
  )
}
