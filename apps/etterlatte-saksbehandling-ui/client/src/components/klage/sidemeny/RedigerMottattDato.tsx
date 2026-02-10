import { useKlage } from '~components/klage/useKlage'
import { useState } from 'react'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'
import { Box, Button, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterMottattDatoForKlage } from '~shared/api/klage'
import { useDispatch } from 'react-redux'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { addKlage } from '~store/reducers/KlageReducer'
import { ApiErrorAlert } from '~ErrorBoundary'

export function RedigerMottattDato() {
  const klage = useKlage()
  const [redigererDato, setRedigererDato] = useState<boolean>(false)
  const dispatch = useDispatch()
  const eksisterendeDato = klage?.innkommendeDokument?.mottattDato
    ? new Date(klage.innkommendeDokument.mottattDato)
    : undefined
  const [datoUnderRedigering, setDatoUnderRedigering] = useState<Date | undefined>(eksisterendeDato)
  const [oppdaterDatoStatus, oppdaterDato] = useApiCall(oppdaterMottattDatoForKlage)
  const [feilmelding, setFeilmelding] = useState('')

  function oppdaterMottattDato() {
    setFeilmelding('')
    if (!klage || isPending(oppdaterDatoStatus)) {
      return
    }
    if (!datoUnderRedigering) {
      setFeilmelding('Du må velge en ny klagedato.')
      return
    }
    oppdaterDato({ klageId: klage.id, mottattDato: datoUnderRedigering?.toISOString() }, (klage) => {
      dispatch(addKlage(klage))
      setRedigererDato(false)
      setDatoUnderRedigering(undefined)
    })
  }

  if (redigererDato) {
    return (
      <VStack gap="space-4">
        <DatoVelger
          error={feilmelding}
          value={datoUnderRedigering}
          onChange={setDatoUnderRedigering}
          label="Klagedato"
          toDate={new Date()}
        />
        <Box>
          <Button size="small" onClick={oppdaterMottattDato} loading={isPending(oppdaterDatoStatus)}>
            Lagre
          </Button>
        </Box>
        {isFailure(oppdaterDatoStatus) && (
          <ApiErrorAlert>
            Kunne ikke oppdatere dato klage mottatt, på grunn av feil: {oppdaterDatoStatus.error.detail}
          </ApiErrorAlert>
        )}
      </VStack>
    )
  }

  return (
    <Box>
      <Button
        size="small"
        icon={<PencilIcon aria-hidden />}
        iconPosition="left"
        variant="secondary"
        onClick={() => setRedigererDato(true)}
      >
        Rediger klagedato
      </Button>
    </Box>
  )
}
