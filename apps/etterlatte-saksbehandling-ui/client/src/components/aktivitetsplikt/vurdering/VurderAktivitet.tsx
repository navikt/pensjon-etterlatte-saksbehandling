import { Alert, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
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
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'

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
      <NesteKnapp />
    </>
  )
}

function NesteKnapp() {
  const { vurdering } = useAktivitetspliktOppgaveVurdering()
  const aktiviteter = vurdering.aktivitet
  const navigate = useNavigate()
  const [manglerAktiviteter, setManglerAktiviteter] = useState(false)
  const gaaTilNeste = () => {
    if (aktiviteter?.length) {
      navigate(`../${AktivitetspliktSteg.BREVVALG}`)
      setManglerAktiviteter(false)
    } else {
      setManglerAktiviteter(true)
    }
  }
  useEffect(() => {
    setManglerAktiviteter(false)
  }, [vurdering.aktivitet])

  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle" marginBlock="4">
      <HStack gap="4" justify="center">
        <Button onClick={gaaTilNeste}>Neste</Button>
        {manglerAktiviteter && <Alert variant="error">Du må registrere en aktivitet for å gå videre</Alert>}
      </HStack>
    </Box>
  )
}
