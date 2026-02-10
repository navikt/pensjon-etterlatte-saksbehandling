/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Alert, Box, Button, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { Vurderinger } from '~components/aktivitetsplikt/vurdering/Vurderinger'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { velgDoedsdatoFraPersonopplysninger } from '~components/person/aktivitet/AktivitetspliktSakoversikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { useNavigate } from 'react-router'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { AktivitetspliktVurderingOversikt } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurderingOversikt'
import { AktivitetspliktOppgaveVurderingType } from '~shared/types/Aktivitetsplikt'
import { hentSisteIverksattePersonopplysninger } from '~shared/api/sak'

export function VurderAktivitet() {
  const { sak } = useAktivitetspliktOppgaveVurdering()
  const [sistIverksattPersonopplysningerResult, sistIverksattPersonopplysningerFetch] = useApiCall(
    hentSisteIverksattePersonopplysninger
  )
  const [avdoedesDoedsdato, setAvdoedesDoedsdato] = useState<Date | undefined>()

  useEffect(() => {
    sistIverksattPersonopplysningerFetch({ sakId: sak.id, sakType: sak.sakType }, ({ avdoede }) => {
      if (avdoede) {
        setAvdoedesDoedsdato(velgDoedsdatoFraPersonopplysninger(avdoede))
      }
    })
  }, [])

  return (
    <>
      <AktivitetspliktVurderingOversikt />
      <Box paddingInline="space-16" paddingBlock="space-16" maxWidth="120rem">
        <VStack gap="space-4">
          {mapResult(sistIverksattPersonopplysningerResult, {
            pending: <Spinner label="Henter opplysninger om avdød" />,
            error: (error) => (
              <ApiErrorAlert>
                Kunne ikke hente avdødes dødsdato. Kan derfor ikke vise tidslinje for aktivitet. Feil: {error.detail}
              </ApiErrorAlert>
            ),
          })}

          {avdoedesDoedsdato && <AktivitetspliktTidslinje doedsdato={avdoedesDoedsdato} sakId={sak.id} />}
          <Vurderinger doedsdato={avdoedesDoedsdato} />
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
      if (
        vurderingType === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER &&
        aktiviteter.every((aktivitet) => !aktivitet.vurdertFra12Mnd)
      ) {
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
    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle" marginBlock="space-4">
      <VStack gap="space-6">
        {feilmeldingAktiviteter.length > 0 && <Alert variant="error">{feilmeldingAktiviteter}</Alert>}
        <HStack gap="space-4" justify="center">
          <Button onClick={gaaTilNeste}>Neste</Button>
        </HStack>
      </VStack>
    </Box>
  )
}
