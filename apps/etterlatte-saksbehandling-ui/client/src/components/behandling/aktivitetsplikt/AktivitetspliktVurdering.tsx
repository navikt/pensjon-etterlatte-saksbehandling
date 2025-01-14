import { BodyShort, Box, Button, Heading, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import {
  hentAktivitspliktVurderingForBehandling,
  opprettAktivitspliktAktivitetsgradOgUnntakForBehandling,
} from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { PlusIcon } from '@navikt/aksel-icons'
import {
  NyVurderingAktivitetsgradOgUnntak,
  VurderingAktivitetsgradOgUnntak,
} from '~components/aktivitetsplikt/vurdering/VurderingAktivitetsgradOgUnntak'
import { setAktivitetspliktVurdering } from '~store/reducers/AktivitetsplikReducer'
import { useDispatch } from 'react-redux'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { UnntakTabellBehandling } from '~components/behandling/aktivitetsplikt/unntak/UnntakTabellBehandling'
import { AktivitetsgradTabellBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/AktivitetsgradTabellBehandling'

export const AktivitetspliktVurdering = ({
  behandling,
  resetManglerAktivitetspliktVurdering,
  doedsdato,
}: {
  behandling: IDetaljertBehandling
  resetManglerAktivitetspliktVurdering: () => void
  doedsdato?: Date
}) => {
  //TODO: denne må være på nytt format med denne endringen.. ny dto skal bli IAktivitetspliktVurderingNyDto
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurderingNyDto>()
  //TODO: kun denne skal brukes og den skal lagre både aktivitetsgrad og unntak
  const [hentet, hent] = useApiCall(hentAktivitspliktVurderingForBehandling)
  //TODO: hentet res her må legges slik at det kan vises i visningen
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  useEffect(() => {
    if (!vurdering) {
      hent(
        { sakId: behandling.sakId, behandlingId: behandling.id },
        (result) => {
          setVurdering(result)
          if (result) resetManglerAktivitetspliktVurdering()
        },
        (error) => {
          if (error.status === 404) {
            //NO-OP
          }
        }
      )
    }
  }, [])

  if (isPending(hentet)) {
    return <Spinner label="Henter aktivitetspliktsvurdering" />
  }

  //TODO: ingen av visningene her har sletting eller redigering og det MÅ på plass
  return (
    <Box maxWidth="120rem" paddingBlock="4 0" borderWidth="1 0 0 0">
      <VStack gap="6">
        <VStack gap="2">
          <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
          <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
        </VStack>
        {isSuccess(hentet) && (
          <>
            {vurdering && (
              <>
                <AktivitetsgradTabellBehandling behandling={behandling} aktiviteter={vurdering?.aktivitet} />
                <UnntakTabellBehandling behandling={behandling} unntak={vurdering?.unntak} />
              </>
            )}
          </>
        )}
        {redigerbar && <VurderAktivitetspliktWrapper doedsdato={doedsdato} behandling={behandling} />}
      </VStack>
    </Box>
  )
}

//TODO flytte ut
function VurderAktivitetspliktWrapper(props: { doedsdato?: Date; behandling: IDetaljertBehandling }) {
  const { doedsdato, behandling } = props
  const [lagreStatus, lagreVurdering] = useApiCall(opprettAktivitspliktAktivitetsgradOgUnntakForBehandling)
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)
  const dispatch = useDispatch()

  function oppdaterStateVedLagring(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setAktivitetspliktVurdering(data)) //TODO: må verfisere at denne legger seg riktig i state for visning her
    setLeggerTilVurdering(false)
  }

  if (!leggerTilVurdering) {
    return (
      <Box>
        <Button size="small" icon={<PlusIcon aria-hidden />} onClick={() => setLeggerTilVurdering(true)}>
          Legg til ny vurdering av aktivitetsplikt
        </Button>
      </Box>
    )
  }

  function lagreOgOppdater(formdata: NyVurderingAktivitetsgradOgUnntak) {
    lagreVurdering(
      {
        sakId: behandling.sakId,
        behandlingId: behandling.id,
        request: {
          aktivitetsgrad: {
            id: undefined,
            //TODO: denne skal kanskje ikke sjekkes på? kan evt bare sette den til false? evt basere på dagens dato diff mot dødsdato
            vurdertFra12Mnd: formdata.typeVurdering === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER,
            skjoennsmessigVurdering: formdata.vurderingAvAktivitet.skjoennsmessigVurdering,
            aktivitetsgrad: formdata.vurderingAvAktivitet.aktivitetsgrad,
            fom: formdata.vurderingAvAktivitet.fom,
            beskrivelse: formdata.vurderingAvAktivitet.beskrivelse || '',
          },
          unntak: formdata.unntak,
        },
      },
      oppdaterStateVedLagring
    )
  }

  // typeVurdering skal sb kunne velge typeVurdering? eller skal den være basert på hvor lenge avdødeds dødsdato var?
  return (
    <VurderingAktivitetsgradOgUnntak
      lagreStatus={lagreStatus}
      onSubmit={lagreOgOppdater}
      typeVurdering={AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER}
      doedsdato={doedsdato}
      onAvbryt={() => {}}
    />
  )
}
