import { BodyShort, Box, Button, Heading, HStack } from '@navikt/ds-react'
import React, { useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import { forrigeSteg, kanSeOppsummering } from '~components/klage/stegmeny/KlageStegmeny'
import { useApiCall } from '~shared/hooks/useApiCall'
import { fattVedtakOmAvvistKlage, ferdigstillKlagebehandling } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'

import { isPending, mapFailure } from '~shared/api/apiUtils'
import { formaterKlageutfall, VisInnstilling, VisOmgjoering } from '~components/klage/vurdering/KlageVurderingFelles'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ApiErrorAlert } from '~ErrorBoundary'

export function KlageOppsummering({ kanRedigere }: { kanRedigere: boolean }) {
  const navigate = useNavigate()
  const klage = useKlage()
  const [ferdigstillStatus, ferdigstill] = useApiCall(ferdigstillKlagebehandling)
  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakOmAvvistKlage)
  const dispatch = useAppDispatch()

  useEffect(() => {
    if (klage !== null && !kanSeOppsummering(klage)) {
      navigate(`/klage/${klage.id}/`)
    }
  }, [klage])

  const ferdigstillKlage = useCallback(() => {
    ferdigstill(klage!!.id, (ferdigKlage) => {
      dispatch(addKlage(ferdigKlage))
      // Kanskje gi noe eksplisitt feedback? Evt håndtere oppsummering av en ferdig klage forskjellig.
      // Gjelder forsåvidt de andre stegene i behandlingen også, visning for utfylt skjema
    })
  }, [klage?.id])

  const fattVedtakKlage = useCallback(() => {
    fattVedtak(klage!!.id, () => {
      navigate('/person', { state: { fnr: klage!!.sak.ident } })
    })
  }, [klage?.id])

  if (!klage) {
    return <Spinner label="Henter klage" />
  }

  const { utfall, sak } = klage
  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading level="1" size="large">
          Oppsummering
        </Heading>
      </Box>

      <Box paddingBlock="space-8" paddingInline="space-16 space-8">
        <Heading size="medium" level="2">
          Utfall
        </Heading>
        <BodyShort spacing>
          Utfallet av klagen er <strong>{formaterKlageutfall(klage)}</strong>.
        </BodyShort>

        {utfall?.utfall === 'DELVIS_OMGJOERING' || utfall?.utfall === 'STADFESTE_VEDTAK' ? (
          <VisInnstilling innstilling={utfall.innstilling} sakId={sak.id} kanRedigere={kanRedigere} />
        ) : null}

        {(utfall?.utfall === 'DELVIS_OMGJOERING' ||
          utfall?.utfall === 'OMGJOERING' ||
          utfall?.utfall === 'AVVIST_MED_OMGJOERING') && (
          <VisOmgjoering omgjoering={utfall.omgjoering} kanRedigere={kanRedigere} />
        )}
      </Box>

      {isFailureHandler({
        apiResult: ferdigstillStatus,
        errorMessage:
          'Kunne ikke ferdigstille klagebehandling på grunn av en feil. Prøv igjen etter å ha ' +
          'lastet siden på nytt, og meld sak hvis problemet vedvarer.',
      })}

      {mapFailure(fattVedtakStatus, (error) => (
        <ApiErrorAlert>Kunne ikke fatte vedtak om avvist klage, på grunn av feil: {error.detail}.</ApiErrorAlert>
      ))}

      <HStack gap="space-4" justify="center">
        <Button variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'oppsummering'))}>
          Gå tilbake
        </Button>
        {kanRedigere &&
          (utfall?.utfall === 'AVVIST' ? (
            <Button variant="primary" onClick={fattVedtakKlage} loading={isPending(fattVedtakStatus)}>
              Send til attestering
            </Button>
          ) : (
            <Button variant="primary" onClick={ferdigstillKlage} loading={isPending(ferdigstillStatus)}>
              Ferdigstill klagen
            </Button>
          ))}
      </HStack>
    </>
  )
}
