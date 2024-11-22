import {
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktVurderingNyDto,
  teksterAktivitetspliktSkjoennsmessigVurdering,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetspliktVurdering } from '~shared/api/aktivitetsplikt'
import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { setAktivitetspliktVurdering } from '~store/reducers/Aktivitetsplikt12mnd'
import { VurderingAktivitetsgradForm } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/VurderingAktivitetsgradForm'
import { BodyShort, Button, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function VisAktivitetsgrad(props: { aktivitet: IAktivitetspliktAktivitetsgrad }) {
  const { aktivitet } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()

  const [slettStatus, slettSpesifikkAktivitet] = useApiCall(slettAktivitetspliktVurdering)
  const [redigerer, setRedigerer] = useState<boolean>(false)

  const dispatch = useDispatch()
  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

  function oppdaterTilstandLagretVurdering(data: IAktivitetspliktVurderingNyDto) {
    setRedigerer(false)
    dispatch(setAktivitetspliktVurdering(data))
  }

  function slettAktivitetsgradIOppgave(aktivitet: IAktivitetspliktAktivitetsgrad) {
    slettSpesifikkAktivitet(
      {
        sakId: aktivitet.sakId,
        oppgaveId: oppgave.id,
        vurderingId: aktivitet.id,
      },
      (data) => {
        dispatch(setAktivitetspliktVurdering(data))
        setRedigerer(false)
      }
    )
  }
  if (redigerer) {
    return (
      <VurderingAktivitetsgradForm
        onSuccess={oppdaterTilstandLagretVurdering}
        onAvbryt={() => setRedigerer(false)}
        aktivitet={aktivitet}
      />
    )
  }

  return (
    <VStack gap="6" maxWidth="50rem">
      <Heading size="small">
        Vurdering gjort for aktiviteten fra {aktivitet.vurdertFra12Mnd ? '12 måneder' : '6 måneder'}
      </Heading>
      <VStack gap="2">
        <Label>Beskrivelse</Label>
        <BodyShort>{aktivitet.beskrivelse}</BodyShort>
      </VStack>

      {aktivitet.skjoennsmessigVurdering && (
        <VStack gap="4">
          <Label>Trenger bruker ekstra oppfølging?</Label>
          <BodyShort>{teksterAktivitetspliktSkjoennsmessigVurdering[aktivitet.skjoennsmessigVurdering]}</BodyShort>
        </VStack>
      )}

      {erRedigerbar && (
        <HStack gap="4">
          <Button size="xsmall" variant="secondary" onClick={() => setRedigerer(true)} icon={<PencilIcon />}>
            Rediger
          </Button>
          <Button
            size="xsmall"
            variant="secondary"
            icon={<TrashIcon />}
            loading={isPending(slettStatus)}
            onClick={() => slettAktivitetsgradIOppgave(aktivitet)}
          >
            Slett
          </Button>
        </HStack>
      )}

      {isFailure(slettStatus) && (
        <ApiErrorAlert>
          Kunne ikke slette aktivitetsvurderingen, på grunn av feil: {slettStatus.error.detail}
        </ApiErrorAlert>
      )}
    </VStack>
  )
}
