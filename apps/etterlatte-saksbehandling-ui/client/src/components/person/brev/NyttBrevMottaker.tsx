import { Adresse, IBrev } from '~shared/types/Brev'
import { Alert, BodyShort, Heading, Label, Panel } from '@navikt/ds-react'
import RedigerMottakerModal from '~components/person/brev/RedigerMottakerModal'
import React, { useEffect, useState } from 'react'
import { isPendingOrInitial, useApiCall } from '~shared/hooks/useApiCall'
import { getVergeadresseFraGrunnlag } from '~shared/api/grunnlag'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePersondata } from '~shared/types/kilde'

export default function NyttBrevMottaker({ brev }: { brev: IBrev }) {
  const [brevState, setBrevState] = useState(brev)

  const mottaker = brevState.mottaker
  const adresse = mottaker?.adresse

  const [vergeAdresseResult, getVergeadresse] = useApiCall(getVergeadresseFraGrunnlag)
  const [vergeAdresse, setVergeadresse] = useState<Grunnlagsopplysning<Adresse, KildePersondata> | undefined>(undefined)

  useEffect(() => {
    if (brev.behandlingId) {
      getVergeadresse(
        brev.behandlingId,
        (result) => {
          setVergeadresse(result)
        },
        (error) => {
          if (error.status == 404) {
            setVergeadresse(undefined)
          } else {
            throw error
          }
        }
      )
    }
  }, [brev])

  return (
    <div style={{ margin: '1rem' }}>
      {vergeAdresse && (
        <Alert variant="info" size="small" inline>
          Søker har verge
        </Alert>
      )}
      {!isPendingOrInitial(vergeAdresseResult) && (
        <Panel border>
          <Heading spacing level="2" size="medium">
            Mottaker
            <RedigerMottakerModal
              brev={brevState}
              oppdater={(val) => setBrevState({ ...brevState, mottaker: val })}
              vergeadresse={vergeAdresse}
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
    </div>
  )
}
