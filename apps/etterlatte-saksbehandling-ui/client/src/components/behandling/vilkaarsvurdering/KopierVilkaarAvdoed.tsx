import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Box, Button, Heading, HStack, Link, Spacer, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon, PlusCircleIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ClickEvent, trackClickJaNei } from '~utils/analytics'
import { JaNei } from '~shared/types/ISvar'
import {
  hentKandidatForKopieringAvVilkaar,
  kopierVilkaarFraAnnenBehandling,
  Vilkaar,
} from '~shared/api/vilkaarsvurdering'
import { updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

export const KopierVilkaarAvdoed = ({ behandlingId, vilkaar }: { behandlingId: string; vilkaar: Vilkaar[] }) => {
  const [hentKandidatForKopieringAvVilkaarStatus, hentKandidatForKopieringAvVilkaarReq] = useApiCall(
    hentKandidatForKopieringAvVilkaar
  )
  const [kopierVilkaarStatus, kopierVilkaarReq] = useApiCall(kopierVilkaarFraAnnenBehandling)
  const [visDetaljer, setVisDetaljer] = useState<boolean | undefined>(undefined)
  const dispatch = useAppDispatch()
  const avdoede = usePersonopplysninger()
    ?.avdoede.map((p) => p.opplysning?.foedselsnummer)
    .join(', ')

  const harKopierteVilkaar = () => {
    return vilkaar.some((v) => v.kopiertFraVilkaarId != null)
  }

  // Viser detaljer dersom vilkår ikke allerede er kopiert
  const skalViseDetaljer = visDetaljer ?? !harKopierteVilkaar()

  const kopierVilkaar = (kildeBehandlingId: string) => {
    trackClickJaNei(ClickEvent.KOPIER_VILKAAR_FRA_BEHANDLING_MED_SAMME_AVDOED, JaNei.JA)
    kopierVilkaarReq(
      {
        behandlingId: behandlingId,
        kildeBehandlingId: kildeBehandlingId,
      },
      (vilkaarsvurdering) => {
        setVisDetaljer(false)
        dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
      }
    )
  }

  const ikkeKopierVilkaar = () => {
    trackClickJaNei(ClickEvent.KOPIER_VILKAAR_FRA_BEHANDLING_MED_SAMME_AVDOED, JaNei.NEI)
    setVisDetaljer(false)
  }

  useEffect(() => {
    hentKandidatForKopieringAvVilkaarReq(behandlingId)
  }, [])

  return (
    <>
      {mapResult(hentKandidatForKopieringAvVilkaarStatus, {
        error: (error) => (
          <ApiErrorAlert>
            En feil har oppstått ved henting av vilkårsvurdering for samme avdøde. {error.detail}
          </ApiErrorAlert>
        ),
        success: (behandlingId) => (
          <>
            {behandlingId && (
              <Box maxWidth="50rem" marginBlock="space-0 space-8">
                <Alert variant="info">
                  <HStack gap="space-6" align="center" justify="end" minWidth="45rem">
                    <Heading size="small" level="2">
                      Det finnes en annen sak tilknyttet samme avdøde
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
                        Det finnes en søskensak tilknyttet avdøde {avdoede}, der vilkår som omhandler avdøde allerede er
                        fylt inn. Ønsker du å benytte de samme vilkårsvurderingene for avdøde i denne behandlingen?
                        Dette overskriver det du eventuelt har registrert allerede.
                      </BodyShort>
                      <Link href={`/behandling/${behandlingId}/vilkaarsvurdering`} as="a" target="_blank">
                        Se vilkårsvurderingen for den andre behandlingen
                        <ExternalLinkIcon aria-hidden />
                      </Link>
                      <HStack gap="space-1">
                        <Button
                          variant="secondary"
                          size="small"
                          icon={<XMarkOctagonIcon aria-hidden />}
                          onClick={ikkeKopierVilkaar}
                        >
                          Nei, jeg ønsker å fylle ut manuelt
                        </Button>
                        <Button
                          variant="primary"
                          size="small"
                          icon={<PlusCircleIcon aria-hidden />}
                          onClick={() => kopierVilkaar(behandlingId)}
                        >
                          Ja, kopier og legg til vilkår for avdøde
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
        apiResult: kopierVilkaarStatus,
        errorMessage: 'En feil har oppstått ved kopiering av vilkår for avdøde',
      })}
    </>
  )
}
