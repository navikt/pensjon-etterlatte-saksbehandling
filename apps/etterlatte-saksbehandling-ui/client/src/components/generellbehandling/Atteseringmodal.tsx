import { Alert, Button, Textarea } from '@navikt/ds-react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import React, { useState } from 'react'
import { attesterGenerellbehandling } from '~shared/api/generellbehandling'
import { hentSakOgNavigererTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtland'
import { useNavigate } from 'react-router-dom'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'

export const Atteseringmodal = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const [attesterStatus, attesterFetch] = useApiCall(attesterGenerellbehandling)
  const navigate = useNavigate()
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')

  const attesterWrapper = () => {
    attesterFetch(utlandsBehandling, () => {
      setTimeout(() => {
        hentSakOgNavigererTilSaksoversikt(utlandsBehandling.sakId, navigate)
      }, 4000)
    })
  }
  return (
    <>
      <Textarea
        label="Tilbakemelding fra attestant"
        size="small"
        value={fritekstgrunn}
        onChange={(e) => {
          setFritekstgrunn(e.target.value)
        }}
      />
      <Button style={{ marginTop: '1rem' }} onClick={() => attesterWrapper()} loading={isPending(attesterStatus)}>
        Attester
      </Button>
      {isSuccess(attesterStatus) && <Alert variant="success">Behandlingen ble attestert</Alert>}
      {isFailure(attesterStatus) && <Alert variant="error">Behandlingen ble ikke attestert</Alert>}
    </>
  )
}
