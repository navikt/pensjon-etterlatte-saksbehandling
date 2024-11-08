import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { Vurderinger } from '~components/aktivitetsplikt/vurdering/Vurderinger'
import { isPending, mapFailure, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { velgDoedsdato } from '~components/person/aktivitet/Aktivitet'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { useNavigate } from 'react-router'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { opprettAktivitetspliktsbrev } from '~shared/api/aktivitetsplikt'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useDispatch } from 'react-redux'
import { setBrevid } from '~store/reducers/Aktivitetsplikt12mnd'

export function VurderAktivitet() {
  const { sak } = useAktivitetspliktOppgaveVurdering()
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

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
            success: ({ avdoede }) =>
              avdoede && (
                <>
                  <AktivitetspliktTidslinje doedsdato={velgDoedsdato(avdoede)} sakId={sak.id} />
                  <Vurderinger doedsdato={velgDoedsdato(avdoede)} />
                </>
              ),
          })}
        </VStack>
      </Box>
      <NesteEllerOpprettBrev />
    </>
  )
}

function NesteEllerOpprettBrev() {
  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const navigate = useNavigate()

  const [opprettBrevStatus, opprettBrevCall] = useApiCall(opprettAktivitetspliktsbrev)
  const dispatch = useDispatch()
  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)
  const skalOppretteBrev = erRedigerbar && aktivtetspliktbrevdata?.skalSendeBrev && !aktivtetspliktbrevdata.brevId

  function opprettBrev() {
    opprettBrevCall(
      {
        oppgaveId: oppgave.id,
      },
      (brevId) => {
        dispatch(setBrevid(brevId.brevId))
        navigate(`../${AktivitetspliktSteg.OPPSUMMERING_OG_BREV}`)
      }
    )
  }

  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      <HStack gap="4" justify="center">
        {mapFailure(opprettBrevStatus, (error) => (
          <ApiErrorAlert>Kunne ikke opprette brev: {error.detail}</ApiErrorAlert>
        ))}
        {skalOppretteBrev ? (
          <Button onClick={opprettBrev} loading={isPending(opprettBrevStatus)}>
            Opprett og gå til brev
          </Button>
        ) : (
          <Button onClick={() => navigate(`../${AktivitetspliktSteg.OPPSUMMERING_OG_BREV}`)}>Neste</Button>
        )}
      </HStack>
    </Box>
  )
}
