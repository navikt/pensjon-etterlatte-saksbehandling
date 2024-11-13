import { Alert, Box, Button, HStack, VStack } from '@navikt/ds-react'
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
import { AktivitetspliktVurdering12MndOversikt } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering12MndOversikt'

export function VurderAktivitet() {
  const { sak } = useAktivitetspliktOppgaveVurdering()
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  useEffect(() => {
    familieOpplysningerFetch({ ident: sak.ident, sakType: sak.sakType })
  }, [])

  return (
    <>
      <AktivitetspliktVurdering12MndOversikt />
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
  const { vurdering, vurderingType } = useAktivitetspliktOppgaveVurdering()
  const aktiviteter = vurdering.aktivitet
  const navigate = useNavigate()
  const [feilmeldingAktiviteter, setFeilmeldingAktiviteter] = useState('')
  const gaaTilNeste = () => {
    setFeilmeldingAktiviteter('')
    if (aktiviteter?.length) {
      if (vurderingType === 'TOLV_MAANEDER' && aktiviteter.every((aktivitet) => !aktivitet.vurdertFra12Mnd)) {
        setFeilmeldingAktiviteter('Du må gjøre en ny vurdering fra 12 måneder for å gå videre')
      } else {
        navigate(`../${AktivitetspliktSteg.BREVVALG}`)
      }
    } else {
      setFeilmeldingAktiviteter('Du må registrere en aktivitet for å gå videre')
    }
  }
  useEffect(() => {
    setFeilmeldingAktiviteter('')
  }, [vurdering.aktivitet])

  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle" marginBlock="4">
      <VStack gap="6">
        {feilmeldingAktiviteter.length > 0 && <Alert variant="error">{feilmeldingAktiviteter}</Alert>}
        <HStack gap="4" justify="center">
          <Button onClick={gaaTilNeste}>Neste</Button>
        </HStack>
      </VStack>
    </Box>
  )
}
