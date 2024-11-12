import {
  hentKandidatForKopieringAvTrygdetid,
  ITrygdetid,
  kopierTrygdetidFraAnnenBehandling,
} from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Box, Button, Heading, HStack, Link, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon, PlusCircleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { trackClick } from '~utils/amplitude'

export const TrygdetidIAnnenBehandlingMedSammeAvdoede = ({
  behandlingId,
  trygdetider,
  setTrygdetider,
  mapNavn,
}: {
  behandlingId: string
  trygdetider: ITrygdetid[]
  setTrygdetider: (trygdetider: ITrygdetid[]) => void
  mapNavn: (fnr: string, visFnr: boolean) => string
}) => {
  const [hentKandidatForKopieringAvTrygdetidStatus, hentKandidatForKopieringAvTrygdetidReq] = useApiCall(
    hentKandidatForKopieringAvTrygdetid
  )

  const avdoedesNavn = () => trygdetider.map((t) => mapNavn(t.ident, false)).join(' og ')

  const harRedigertTrygdetidGrunnlag = () => {
    const redigert = trygdetider.find((trygdetid) =>
      trygdetid?.trygdetidGrunnlag.find((grunnlag) => grunnlag.kilde.ident != 'Gjenny')
    )
    return !!redigert
  }

  const [kopierTrygdetidStatus, kopierTrygdetidReq] = useApiCall(kopierTrygdetidFraAnnenBehandling)
  const [visDetaljer, setVisDetaljer] = useState(false)
  const [skjult, setSkjult] = useState(false)
  const [trygdetidKopiert, setTrygdetidKopiert] = useState(false)

  const kopierTrygdetid = (kildeBehandlingId: string) => {
    trackClick('kopier-trygdetidsgrunnlag-fra-behandling-med-samme-avdoede')
    kopierTrygdetidReq(
      {
        behandlingId: behandlingId,
        kildeBehandlingId: kildeBehandlingId,
      },
      (trygdetider) => {
        setTrygdetidKopiert(true)
        setTrygdetider(trygdetider)
      }
    )
  }

  useEffect(() => {
    hentKandidatForKopieringAvTrygdetidReq(behandlingId)
  }, [])

  useEffect(() => {
    setVisDetaljer(!harRedigertTrygdetidGrunnlag())
    setSkjult(trygdetidKopiert)
    setTrygdetidKopiert(false)
  }, [trygdetider])

  if (skjult) {
    return <></>
  }

  return (
    <>
      {mapResult(hentKandidatForKopieringAvTrygdetidStatus, {
        success: (behandlingId) => (
          <>
            {behandlingId ? (
              <Box paddingBlock="2">
                <Alert variant="info">
                  <HStack gap="6" justify="space-between">
                    <Heading size="small" level="2">
                      Det finnes en annen behandling tilknyttet samme avdøde
                    </Heading>
                    {!visDetaljer && (
                      <Link href="#" onClick={() => setVisDetaljer(true)}>
                        Les mer
                      </Link>
                    )}
                  </HStack>
                  {visDetaljer && (
                    <VStack gap="3">
                      <BodyShort>
                        Det finnes en annen behandling tilknyttet avdøde {avdoedesNavn()}, der trygdetiden allerede er
                        fylt inn. Ønsker du å benytte den samme trygdetiden i denne behandlingen? Dette overskriver det
                        du eventuelt har registrert allerede.
                      </BodyShort>
                      <Link href={`/behandling/${behandlingId}/trygdetid`} as="a" target="_blank">
                        Gå til behandlingen
                        <ExternalLinkIcon>Gå til behandlingen</ExternalLinkIcon>
                      </Link>
                      <HStack gap="4" justify="space-between">
                        <Button
                          variant="primary"
                          icon={<PlusCircleIcon fontSize="1.5rem" />}
                          onClick={() => kopierTrygdetid(behandlingId)}
                        >
                          Ja, kopier og legg til trygdetid
                        </Button>
                        <Button
                          variant="secondary"
                          icon={<XMarkOctagonIcon fontSize="1.5rem" />}
                          onClick={() => setVisDetaljer(false)}
                        >
                          Nei, jeg ønsker å fylle ut manuelt
                        </Button>
                      </HStack>
                    </VStack>
                  )}
                </Alert>
              </Box>
            ) : (
              <></>
            )}
          </>
        ),
        error: (error) => (
          <ApiErrorAlert>En feil har oppstått ved henting av trygdetid for samme avdøde. {error.detail}</ApiErrorAlert>
        ),
      })}
      {isFailureHandler({
        apiResult: kopierTrygdetidStatus,
        errorMessage: 'En feil har oppstått ved kopiering av trygdetid fra annen behandling',
      })}
    </>
  )
}
