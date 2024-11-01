import { Box, Button, Heading, VStack } from '@navikt/ds-react'
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

export function VurderAktivitet() {
  const oppgave = useAktivitetspliktOppgaveVurdering()
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)
  const navigate = useNavigate()
  useEffect(() => {
    familieOpplysningerFetch({ ident: oppgave.sak.ident, sakType: oppgave.sak.sakType })
  }, [])

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading level="1" size="large">
          Oppfølging av aktivitet
        </Heading>
      </Box>
      <Box paddingInline="16" paddingBlock="16">
        <VStack gap="4">
          {mapResult(familieOpplysningerResult, {
            pending: <Spinner label="Henter opplysninger om avdød" />,
            error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente opplysninger om avdød'}</ApiErrorAlert>,
            success: ({ avdoede }) => (
              <>{avdoede && <AktivitetspliktTidslinje doedsdato={velgDoedsdato(avdoede)} sakId={oppgave.sak.id} />}</>
            ),
          })}
          <Vurderinger />
          <Box width="6">
            <Button
              onClick={() => {
                navigate(`../${AktivitetspliktSteg.BREV}`)
              }}
            >
              {handlinger.NESTE.navn}
            </Button>
          </Box>
        </VStack>
      </Box>
    </>
  )
}
