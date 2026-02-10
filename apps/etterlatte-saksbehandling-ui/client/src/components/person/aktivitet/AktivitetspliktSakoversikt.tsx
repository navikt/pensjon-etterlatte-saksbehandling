import { isFailure, isPending, isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { Alert, BodyShort, Box, Button, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitspliktVurderingForSak, opprettOppfoelgingsoppgave } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { Familiemedlem } from '~shared/types/familieOpplysninger'
import { VurderingAvAktivitetspliktSak } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/VurderingAvAktivitetspliktSak'
import { AktivitetspliktStatusTagOgGyldig } from '~shared/tags/AktivitetspliktStatusOgGyldig'
import { AktivitetspliktOppgaveVurderingType, harVurdering } from '~shared/types/Aktivitetsplikt'
import { ApiError } from '~shared/api/apiClient'
import { Personopplysning } from '~shared/types/grunnlag'

export const velgDoedsdato = (avdoede: Familiemedlem[] | []): Date => {
  if (avdoede.length === 0) return new Date()
  else if (avdoede.length === 1) return avdoede[0].doedsdato!!
  else
    return avdoede.reduce((foersteAvdoed, andreAvdoed) =>
      foersteAvdoed.doedsdato!! < andreAvdoed.doedsdato!! ? foersteAvdoed : andreAvdoed
    ).doedsdato!!
}

export const velgDoedsdatoFraPersonopplysninger = (avdoede: Personopplysning[]): Date | undefined => {
  if (avdoede.length === 0) {
    return undefined
  } else if (avdoede.length === 1) {
    return avdoede[0].opplysning.doedsdato
  } else {
    return avdoede
      .map((person) => person.opplysning.doedsdato)
      .filter((doedsdato) => !!doedsdato)
      .sort()[0]
  }
}

export const AktivitetspliktSakoversikt = ({
  fnr,
  sakResult,
}: {
  fnr: string
  sakResult: Result<SakMedBehandlinger>
}): ReactNode => {
  const [hentAktivitetspliktVurderingForSakResult, hentAktivitetspliktVurderingForSakRequest] = useApiCall(
    hentAktivitspliktVurderingForSak
  )
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  const [svaropprettOppgave, opprettOppfoelgingsoppgaveReq] = useApiCall(opprettOppfoelgingsoppgave)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentAktivitetspliktVurderingForSakRequest({ sakId: sakResult.data.sak.id })
      familieOpplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
    }
  }, [sakResult])

  const feilkodehaandtering = (error: ApiError) => {
    switch (error.status) {
      case 404:
        return <ApiWarningAlert>Kan ikke hente aktivitetsplikt: {error.detail}</ApiWarningAlert>
      case 403:
        return <ApiWarningAlert>Du mangler tilgang til saken {error.detail}</ApiWarningAlert>
      default:
        return <ApiErrorAlert>{error.detail || 'Feil ved henting av sak'}</ApiErrorAlert>
    }
  }

  if (isFailure(sakResult)) {
    return <Box padding="space-8">{feilkodehaandtering(sakResult.error)}</Box>
  }

  return (
    <VStack gap="space-4">
      <Box padding="space-8" maxWidth="70rem">
        {mapResult(hentAktivitetspliktVurderingForSakResult, {
          pending: <Spinner label="Henter vurderinger..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente vurderinger'}</ApiErrorAlert>,
          success: (aktivitetspliktVurdering) => (
            <VStack gap="space-8">
              <VStack gap="space-4">
                <Heading size="medium">Aktivitetsplikt</Heading>

                <AktivitetspliktStatusTagOgGyldig aktivitetspliktVurdering={aktivitetspliktVurdering} />
                <Label>Gjenlevende sin tidslinje</Label>

                {mapSuccess(sakResult, (data) =>
                  mapResult(familieOpplysningerResult, {
                    pending: <Spinner label="Henter opplysninger om avdød" />,
                    error: (error) => (
                      <ApiErrorAlert>{error.detail || 'Kunne ikke hente opplysninger om avdød'}</ApiErrorAlert>
                    ),
                    success: ({ avdoede }) =>
                      avdoede && <AktivitetspliktTidslinje doedsdato={velgDoedsdato(avdoede)} sakId={data.sak.id} />,
                  })
                )}
              </VStack>

              <hr style={{ width: '100%' }} />
              <Box>
                {harVurdering(aktivitetspliktVurdering) ? (
                  <VurderingAvAktivitetspliktSak aktivitetspliktVurdering={aktivitetspliktVurdering} />
                ) : (
                  <BodyShort>Ingen vurdering</BodyShort>
                )}
              </Box>
            </VStack>
          ),
        })}
      </Box>
      <VStack paddingInline="space-8 space-16" paddingBlock="space-0 space-16" maxWidth="70rem" gap="space-4">
        <Heading level="3" size="small">
          Informasjonsbrev og vurdering manuelt
        </Heading>
        <Box maxWidth="42.5rem">
          <BodyShort>
            Hvis det er behov for å sende ut informasjon om aktivitetsplikt manuelt kan en egen oppgave for infomasjon
            om aktivitetsplikt opprettes.
          </BodyShort>
        </Box>

        {isFailure(svaropprettOppgave) && (
          <Alert variant="error">Kunne ikke opprett oppfølgingsoppgave: {svaropprettOppgave.error.detail}</Alert>
        )}

        {isSuccess(svaropprettOppgave) && <Alert variant="success">Oppgaven ble opprettet.</Alert>}

        {mapSuccess(sakResult, (sakOgBehandlinger) => (
          <>
            <Heading size="xsmall">Opprett oppgave</Heading>
            <HStack gap="space-4">
              <div>
                <Button
                  variant="secondary"
                  loading={isPending(svaropprettOppgave)}
                  onClick={() =>
                    opprettOppfoelgingsoppgaveReq({
                      sakId: sakOgBehandlinger.sak.id,
                      vurderingType: AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER,
                    })
                  }
                >
                  Aktivitetsplikt fra 6 måneder
                </Button>
              </div>
              <div>
                <Button
                  variant="secondary"
                  loading={isPending(svaropprettOppgave)}
                  onClick={() =>
                    opprettOppfoelgingsoppgaveReq({
                      sakId: sakOgBehandlinger.sak.id,
                      vurderingType: AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER,
                    })
                  }
                >
                  Akvitivetsplikt fra 12 måneder
                </Button>
              </div>
            </HStack>
          </>
        ))}
      </VStack>
    </VStack>
  )
}
