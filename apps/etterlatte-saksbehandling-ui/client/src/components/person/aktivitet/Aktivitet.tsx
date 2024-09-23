import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitspliktVurderingForSak } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { Familiemedlem } from '~shared/types/familieOpplysninger'
import { VurderingAvAktivitetsplikt } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/VurderingAvAktivitetsplikt'

const velgDoedsdato = (avdoede: Familiemedlem[] | []): Date => {
  if (avdoede.length === 0) return new Date()
  else if (avdoede.length === 1) return avdoede[0].doedsdato!!
  else
    return avdoede.reduce((foersteAvdoed, andreAvdoed) =>
      foersteAvdoed.doedsdato!! < andreAvdoed.doedsdato!! ? foersteAvdoed : andreAvdoed
    ).doedsdato!!
}

export const Aktivitet = ({ fnr, sakResult }: { fnr: string; sakResult: Result<SakMedBehandlinger> }): ReactNode => {
  const [hentAktivitetspliktVurderingForSakResult, hentAktivitetspliktVurderingForSakRequest] = useApiCall(
    hentAktivitspliktVurderingForSak
  )
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentAktivitetspliktVurderingForSakRequest({ sakId: sakResult.data.sak.id })
      familieOpplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
    }
  }, [])

  return (
    <Box padding="8" maxWidth="70rem">
      <VStack gap="8">
        <VStack gap="4">
          <Heading size="medium">Aktivitetsplikt</Heading>
          <BodyShort>Gjenlevende sin tidslinje</BodyShort>

          {isSuccess(sakResult) &&
            mapResult(familieOpplysningerResult, {
              pending: <Spinner label="Henter opplysninger om avdød" />,
              error: (error) => (
                <ApiErrorAlert>{error.detail || 'Kunne ikke hente opplysninger om avdød'}</ApiErrorAlert>
              ),
              success: ({ avdoede }) => (
                <>
                  {avdoede && (
                    <AktivitetspliktTidslinje doedsdato={velgDoedsdato(avdoede)} sakId={sakResult.data.sak.id} />
                  )}
                </>
              ),
            })}
        </VStack>

        <hr style={{ width: '100%' }} />

        {mapResult(hentAktivitetspliktVurderingForSakResult, {
          pending: <Spinner label="Henter vurderinger..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente vurderinger'}</ApiErrorAlert>,
          success: (aktivitetspliktVurdering) =>
            aktivitetspliktVurdering ? (
              <VurderingAvAktivitetsplikt aktivitetspliktVurdering={aktivitetspliktVurdering} />
            ) : (
              <BodyShort>Ingen vurdering</BodyShort>
            ),
        })}
      </VStack>
    </Box>
  )
}
