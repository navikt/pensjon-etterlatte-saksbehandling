import { Alert, BodyShort, Box, Button, Heading, HStack, Textarea, VStack } from '@navikt/ds-react'
import { useLocation, useParams } from 'react-router-dom'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad, hentOppgave } from '~shared/api/oppgaver'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { opprettRevurderingEtteroppgjoer } from '~shared/api/revurdering'
import { OppgaveDTO } from '~shared/types/oppgave'
import { navigerTilPersonOversikt } from '~components/person/lenker/navigerTilPersonOversikt'
import { PersonOversiktFane } from '~components/person/Person'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Opprinnelse } from '~shared/types/IDetaljertBehandling'
import { Sidebar } from '~shared/components/Sidebar'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'

export const SvarPaaEtteroppgjoer = () => {
  useSidetittel('Svar på etteroppgjør')

  const { oppgaveId } = useParams()

  const {
    state: { opprinnelse },
  } = useLocation()

  if (!oppgaveId) {
    return <Alert variant="error">Oppgave ID ligger ikke med i URL</Alert>
  }

  const [begrunnelse, setBegrunnelse] = useState<string>('')

  const [hentOppgaveResult, hentOppgaveFetch] = useApiCall(hentOppgave)

  const [ferdigstillOppgaveResult, ferdigstillOppgaveRequest] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [opprettRevurderingResult, opprettRevurderingRequest] = useApiCall(opprettRevurderingEtteroppgjoer)

  const opprettRevurdering = (oppgave: OppgaveDTO) => {
    opprettRevurderingRequest(
      { sakId: oppgave.sakId, opprinnelse: !!opprinnelse ? opprinnelse : Opprinnelse.UKJENT },
      () => {
        avsluttOppgave(oppgave)
      }
    )
  }

  const avsluttOppgave = (oppgave: OppgaveDTO) => {
    ferdigstillOppgaveRequest({ id: oppgave.id, merknad: begrunnelse }, () => {
      navigerTilPersonOversikt(oppgave.fnr!, PersonOversiktFane.SAKER)
    })
  }

  useEffect(() => {
    hentOppgaveFetch(oppgaveId!)
  }, [oppgaveId])

  return mapResult(hentOppgaveResult, {
    pending: <Spinner label="Henter oppgaver..." />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente oppgave, på grunn av feil: {error.detail}</ApiErrorAlert>,
    success: (oppgave) => (
      <>
        <StatusBar ident={oppgave.fnr} />
        <HStack height="100%" minHeight="100vh" wrap={false}>
          <Box paddingInline="16" paddingBlock="16 4" width="100%">
            <VStack gap="4" maxWidth="50rem">
              <Heading size="medium" spacing>
                Behandling av svar på etteroppgjøret
              </Heading>

              <Heading size="small" level="3">
                Revurdering blir knyttet til en forbehandling
              </Heading>

              <BodyShort>
                Hvis bruker har gitt nok informasjon kan revurderingen for etteroppgjøret opprettes, ellers kan oppgaven
                avsluttes. Ved opprettelse av revurdering vil den automatisk knyttes til den siste ferdigstilte
                forbehandlingen.
              </BodyShort>

              <Textarea
                label="Begrunnelse (valgfri)"
                value={begrunnelse || ''}
                onChange={(e) => setBegrunnelse(e.target.value)}
              />

              {isFailureHandler({
                apiResult: ferdigstillOppgaveResult,
                errorMessage: 'Feil under ferdigstilling av oppgave',
              })}

              {isFailureHandler({
                apiResult: opprettRevurderingResult,
                errorMessage: 'Feil under opprettelse av revurdering',
              })}
              <HStack justify="space-between">
                <Button
                  variant="secondary"
                  onClick={() => navigerTilPersonOversikt(oppgave.fnr!, PersonOversiktFane.SAKER)}
                >
                  Avbryt
                </Button>
                <HStack gap="4">
                  <Button
                    loading={isPending(opprettRevurderingResult) || isPending(ferdigstillOppgaveResult)}
                    onClick={() => opprettRevurdering(oppgave)}
                  >
                    Opprett revurdering
                  </Button>
                  <Button
                    loading={isPending(opprettRevurderingResult) || isPending(ferdigstillOppgaveResult)}
                    onClick={() => avsluttOppgave(oppgave)}
                  >
                    Avslutt oppgave
                  </Button>
                </HStack>
              </HStack>
            </VStack>
          </Box>
          <Sidebar>
            <DokumentlisteLiten fnr={oppgave.fnr!} />
          </Sidebar>
        </HStack>
      </>
    ),
  })
}
