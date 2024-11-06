import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { Vurderinger } from '~components/aktivitetsplikt/vurdering/Vurderinger'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { velgDoedsdato } from '~components/person/aktivitet/Aktivitet'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { useNavigate } from 'react-router'
import { handlinger } from '~components/behandling/handlinger/typer'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { IBrevAktivitetspliktDto, opprettAktivitetspliktsbrev } from '~shared/api/aktivitetsplikt'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'

export function VurderAktivitet({ fetchOppgave }: { fetchOppgave: () => void }) {
  const { sak, oppgave } = useAktivitetspliktOppgaveVurdering()
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  const navigate = useNavigate()
  useEffect(() => {
    familieOpplysningerFetch({ ident: sak.ident, sakType: sak.sakType })
  }, [])

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4" maxWidth="120rem">
        <Heading level="1" size="large">
          Oppfølging av aktivitet
        </Heading>
      </Box>
      <Box paddingInline="16" paddingBlock="16" maxWidth="120rem">
        <VStack gap="4">
          {mapResult(familieOpplysningerResult, {
            pending: <Spinner label="Henter opplysninger om avdød" />,
            error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente opplysninger om avdød'}</ApiErrorAlert>,
            success: ({ avdoede }) => (
              <>{avdoede && <AktivitetspliktTidslinje doedsdato={velgDoedsdato(avdoede)} sakId={sak.id} />}</>
            ),
          })}
          <Vurderinger />
        </VStack>
      </Box>
      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        <HStack gap="4" justify="center">
          <Button
            onClick={() => {
              fetchOppgave()
              navigate(`../${AktivitetspliktSteg.BREV}`)
            }}
          >
            {handlinger.NESTE.navn}
          </Button>
        </HStack>
      </Box>
    </>
  )
}

function hvorErVi(
  brevdata?: IBrevAktivitetspliktDto,
  redigerbar: boolean
): 'SKAL_OPPRETTE' | 'NESTE' | 'SKAL_FERDIGSTILLE' | 'MANGLER_UTFYLLING' {
  if (!redigerbar) {
    return 'NESTE'
  }
  if (!brevdata) {
    return 'MANGLER_UTFYLLING'
  }
  if (brevdata.skalSendeBrev) {
    if (brevdata.brevId) {
      return 'NESTE'
    } else {
      return 'SKAL_OPPRETTE'
    }
  } else {
    return 'SKAL_FERDIGSTILLE'
  }
}

// TODO: bruk denne for å velge handling / knapp i stedet for default neste
function NesteEllerOpprettEllerFerdigstill(props: { fetchOppgave: () => void }) {
  const { fetchOppgave } = props
  const { sak, oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const navigate = useNavigate()

  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettAktivitetspliktsbrev)
  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

  const hvor = hvorErVi(aktivtetspliktbrevdata, erRedigerbar)

  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      <HStack gap="4" justify="center">
        {hvor === 'SKAL_OPPRETTE' && <Button onClick={}>Opprett infobrev</Button>}
        <Button
          onClick={() => {
            fetchOppgave()
            navigate(`../${AktivitetspliktSteg.BREV}`)
          }}
        >
          {/*  TODO: Vis ferdigstilling av oppgave som en mulighet hvis vi har valgt at vi ikke skal sende brevet */}
          Ferdigstill
        </Button>
      </HStack>
    </Box>
  )
}
