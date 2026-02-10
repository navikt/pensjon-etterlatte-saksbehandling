import {
  hentKandidatForKopieringAvTrygdetid,
  ITrygdetid,
  kopierTrygdetidFraAnnenBehandling,
} from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Box, Button, Heading, HStack, Link, Spacer, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon, PlusCircleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ClickEvent, trackClickJaNei } from '~utils/analytics'
import { JaNei } from '~shared/types/ISvar'

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
      trygdetid?.trygdetidGrunnlag.find((grunnlag) => grunnlag.kilde.ident !== 'Gjenny')
    )
    return !!redigert
  }

  const [kopierTrygdetidStatus, kopierTrygdetidReq] = useApiCall(kopierTrygdetidFraAnnenBehandling)
  const [visDetaljer, setVisDetaljer] = useState<boolean | undefined>(undefined)

  const skalViseDetaljer = visDetaljer ?? !harRedigertTrygdetidGrunnlag()

  const kopierTrygdetid = (kildeBehandlingId: string) => {
    trackClickJaNei(ClickEvent.KOPIER_TRYGDETIDSGRUNNLAG_FRA_BEHANDLING_MED_SAMME_AVDOEDE, JaNei.JA)
    kopierTrygdetidReq(
      {
        behandlingId: behandlingId,
        kildeBehandlingId: kildeBehandlingId,
      },
      (trygdetider) => {
        setVisDetaljer(false)
        setTrygdetider(trygdetider)
      }
    )
  }

  const ikkeKopierTrygdetid = () => {
    trackClickJaNei(ClickEvent.KOPIER_TRYGDETIDSGRUNNLAG_FRA_BEHANDLING_MED_SAMME_AVDOEDE, JaNei.NEI)
    setVisDetaljer(false)
  }

  useEffect(() => {
    hentKandidatForKopieringAvTrygdetidReq(behandlingId)
  }, [trygdetider])

  return (
    <>
      {mapResult(hentKandidatForKopieringAvTrygdetidStatus, {
        error: (error) => (
          <ApiErrorAlert>En feil har oppstått ved henting av trygdetid for samme avdøde. {error.detail}</ApiErrorAlert>
        ),
        success: (behandlingId) => (
          <>
            {behandlingId && (
              <Box maxWidth="50rem">
                <Alert variant="info">
                  <HStack gap="space-6" align="center" justify="end" minWidth="45rem">
                    <Heading size="small" level="2">
                      Det finnes en annen sak tilknyttet avdøde
                    </Heading>
                    <Spacer />
                    {!skalViseDetaljer && (
                      <Button variant="tertiary" size="small" onClick={() => setVisDetaljer(true)}>
                        Les mer
                      </Button>
                    )}
                  </HStack>
                  {skalViseDetaljer && (
                    <VStack gap="space-2">
                      <BodyShort>
                        Det finnes en annen sak tilknyttet avdøde {avdoedesNavn()}, der trygdetiden allerede er fylt
                        inn. Ønsker du å benytte den samme trygdetiden i denne behandlingen? Dette overskriver det du
                        eventuelt har registrert allerede.
                      </BodyShort>
                      <Link href={`/behandling/${behandlingId}/trygdetid`} as="a" target="_blank">
                        Forhåndsvis trygdetiden
                        <ExternalLinkIcon aria-hidden />
                      </Link>
                      <HStack gap="space-1">
                        <Button
                          variant="secondary"
                          size="small"
                          icon={<XMarkOctagonIcon aria-hidden />}
                          onClick={ikkeKopierTrygdetid}
                        >
                          Nei, jeg ønsker å fylle ut manuelt
                        </Button>
                        <Button
                          variant="primary"
                          size="small"
                          icon={<PlusCircleIcon aria-hidden />}
                          onClick={() => kopierTrygdetid(behandlingId)}
                        >
                          Ja, kopier og legg til trygdetid
                        </Button>
                      </HStack>
                    </VStack>
                  )}
                </Alert>
              </Box>
            )}
          </>
        ),
      })}
      {isFailureHandler({
        apiResult: kopierTrygdetidStatus,
        errorMessage: 'En feil har oppstått ved kopiering av trygdetid fra annen behandling',
      })}
    </>
  )
}
