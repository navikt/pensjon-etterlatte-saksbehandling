import { isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { BodyShort, Box, Button, Heading, Label, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitspliktVurderingForSak, hentOppfoelgingsoppgaver } from '~shared/api/aktivitetsplikt'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { Familiemedlem } from '~shared/types/familieOpplysninger'
import { VurderingAvAktivitetspliktSak } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/VurderingAvAktivitetspliktSak'
import { AktivitetspliktStatusTagOgGyldig } from '~shared/tags/AktivitetspliktStatusOgGyldig'
import {
  AktivitetspliktOppfoelgingsOppgave,
  AktivitetspliktOppfoelgingsOppgaver,
  harVurdering,
} from '~shared/types/Aktivitetsplikt'
import { Oppgavetype } from '~shared/types/oppgave'

export const velgDoedsdato = (avdoede: Familiemedlem[] | []): Date => {
  if (avdoede.length === 0) return new Date()
  else if (avdoede.length === 1) return avdoede[0].doedsdato!!
  else
    return avdoede.reduce((foersteAvdoed, andreAvdoed) =>
      foersteAvdoed.doedsdato!! < andreAvdoed.doedsdato!! ? foersteAvdoed : andreAvdoed
    ).doedsdato!!
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

  const [hentRes, hentOppfoelgingsoppgaverForSak] = useApiCall(hentOppfoelgingsoppgaver)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentAktivitetspliktVurderingForSakRequest({ sakId: sakResult.data.sak.id })
      familieOpplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
      hentOppfoelgingsoppgaverForSak({ sakId: sakResult.data.sak.id })
    }
  }, [sakResult])

  return (
    <Box padding="8" maxWidth="70rem">
      {mapResult(hentAktivitetspliktVurderingForSakResult, {
        pending: <Spinner label="Henter vurderinger..." />,
        error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente vurderinger'}</ApiErrorAlert>,
        success: (aktivitetspliktVurdering) => (
          <>
            <VStack gap="8">
              <VStack gap="4">
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

              {harVurdering(aktivitetspliktVurdering) ? (
                <VurderingAvAktivitetspliktSak aktivitetspliktVurdering={aktivitetspliktVurdering} />
              ) : (
                <BodyShort>Ingen vurdering</BodyShort>
              )}
            </VStack>
          </>
        ),
      })}

      <Box>
        {mapResult(hentRes, {
          pending: <Spinner label="Henter oppfølgingsoppgave status for sak" />,
          error: (error) => (
            <ApiErrorAlert>{error.detail || 'Kunne ikke oppfølgingsoppgave status for sak'}</ApiErrorAlert>
          ),
          success: (oppgaver) => (
            <>
              {kanOppretteOppgaveAvType(oppgaver, Oppgavetype.AKTIVITETSPLIKT) && (
                <>
                  <Button>Opprett 6 månderers oppfølgingsoppgave</Button>
                </>
              )}
              {kanOppretteOppgaveAvType(oppgaver, Oppgavetype.AKTIVITETSPLIKT_12MND) &&
                har6MndVurdering(oppgaver, Oppgavetype.AKTIVITETSPLIKT_12MND) && (
                  <>
                    <Button>Opprett 12 månderers oppfølgingsoppgave</Button>
                  </>
                )}
            </>
          ),
        })}
        <Button>Opprett 6 månderers oppfølgingsoppgave</Button>
      </Box>
    </Box>
  )
}

function har6MndVurdering(
  oppgaver: AktivitetspliktOppfoelgingsOppgave[],
  aksOppgavetype: AktivitetspliktOppfoelgingsOppgaver
): boolean {
  return oppgaver.filter((o) => o.oppgaveType == aksOppgavetype).some((o) => o.erFerdigstilt)
}

function kanOppretteOppgaveAvType(
  oppgaver: AktivitetspliktOppfoelgingsOppgave[],
  aksOppgavetype: AktivitetspliktOppfoelgingsOppgaver
): boolean {
  return oppgaver.filter((o) => o.oppgaveType == aksOppgavetype).every((o) => o.kanOpprette)
}
