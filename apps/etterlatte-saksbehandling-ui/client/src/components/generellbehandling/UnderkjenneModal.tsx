import { Alert, BodyShort, Button, Textarea } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useState } from 'react'
import { underkjennGenerellbehandling } from '~shared/api/generellbehandling'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { useNavigate } from 'react-router-dom'
import { hentSakOgNavigerTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtlandBehandling'

import { isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const UnderkjenneModal = ({
  utlandsBehandling,
}: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const [underkjennStatus, underkjennFetch] = useApiCall(underkjennGenerellbehandling)
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')
  const [error, setError] = useState<string>('')
  const navigate = useNavigate()

  return (
    <>
      <BodyShort>Vil du underkjenne kravpakken? Vennligst begrunn hvorfor nedenfor:</BodyShort>
      <Textarea
        label="Beskriv hvorfor"
        size="small"
        value={fritekstgrunn}
        onChange={(e) => {
          setFritekstgrunn(e.target.value)
          setError('')
        }}
      />
      {error && (
        <Alert variant="error" style={{ marginTop: '2rem' }}>
          Du må fylle inn en begrunnelse
        </Alert>
      )}
      <Button
        style={{ marginTop: '1rem', marginBottom: '1rem' }}
        disabled={isSuccess(underkjennStatus)}
        type="button"
        onClick={() => {
          if (fritekstgrunn === '') {
            setError('Du må fylle inn en begrunnelse')
          } else {
            underkjennFetch({ generellbehandling: utlandsBehandling, begrunnelse: fritekstgrunn }, () => {
              setTimeout(() => {
                hentSakOgNavigerTilSaksoversikt(utlandsBehandling.sakId, navigate)
              }, 4000)
            })
          }
        }}
      >
        Bekreft og send i retur
      </Button>
      {isSuccess(underkjennStatus) && (
        <Alert variant="success">Behandlingen ble underkjent, du blir straks sendt til saksoversikten</Alert>
      )}
      {isFailureHandler({
        apiResult: underkjennStatus,
        errorMessage: 'Behandlingen ble ikke underkjent',
      })}
    </>
  )
}
