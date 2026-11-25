import { useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Heading, HGrid, Label, Loader, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { addMonths, addYears, isValid } from 'date-fns'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterRevurderingInfo,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { erBehandlingRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { formaterDato } from '~utils/formatering/dato'
import { usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'
import { ForlengetOmsUtdanning } from './ForlengetOmsUtdanning'
import { ForlengetOmsArbeidsrettetTiltak } from './ForlengetOmsArbeidsrettetTiltak'
import { ForlengetOmsIngenAktivitet } from './ForlengetOmsIngenAktivitet'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import {
  ForlengetOmsVurdering,
  hentForlengetOmsVurdering,
  LagreForlengetOmsVurdering,
  lagreForlengetOmsVurdering,
} from '~shared/api/forlengetOms'
import { isPending, isSuccess, mapFailure, mapResult, Result } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

export const ForlengetOmsAktivitet = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const dispatch = useAppDispatch()
  const saksbehandler = useInnloggetSaksbehandler()
  const avdoede = usePersonopplysningerOmsAvdoede()
  const doedsdato = avdoede?.opplysning.doedsdato
  const gyldigDoedsdato = doedsdato && isValid(new Date(doedsdato)) ? new Date(doedsdato) : undefined
  const viktigeDatoer = [
    { tittel: 'Avdødes dødsdato', dato: gyldigDoedsdato },
    { tittel: '6 mnd etter dødsfall', dato: gyldigDoedsdato ? addMonths(gyldigDoedsdato, 6) : undefined },
    { tittel: '12 mnd etter dødsfall', dato: gyldigDoedsdato ? addMonths(gyldigDoedsdato, 12) : undefined },
    { tittel: '3 år etter dødsfall', dato: gyldigDoedsdato ? addYears(gyldigDoedsdato, 3) : undefined },
  ]
  const redigerbar = erBehandlingRedigerbar(behandling.status, behandling.sakEnhetId, saksbehandler.skriveEnheter)
  const [utdanningEllerTiltak, setUtdanningEllerTiltak] = useState('')
  const [lagreStatus, lagreVurdering] = useApiCall(lagreForlengetOmsVurdering)
  const lagrer = isPending(lagreStatus)
  const [vurderingStatus, setVurderingStatus] = useState<Result<ForlengetOmsVurdering | null>>({ status: 'initial' })

  useEffect(() => {
    let aktiv = true
    setVurderingStatus({ status: 'pending' })
    hentForlengetOmsVurdering(behandling.id).then((response) => {
      if (!aktiv) return
      if (response.ok) {
        setVurderingStatus({ status: 'success', data: response.data })
        setUtdanningEllerTiltak(response.data?.aktivitet.type ?? '')
      } else {
        setVurderingStatus({ status: 'error', error: response })
      }
    })
    return () => {
      aktiv = false
    }
  }, [behandling.id])

  if (!isSuccess(vurderingStatus)) {
    return mapResult(vurderingStatus, {
      initial: <Loader title="Henter vurdering av aktivitet" />,
      pending: <Loader title="Henter vurdering av aktivitet" />,
      error: (error) => <ApiErrorAlert>Kunne ikke hente vurdering av aktivitet: {error.detail}</ApiErrorAlert>,
    })
  }
  const vurdering = vurderingStatus.data ?? undefined
  const onLagre = (request: LagreForlengetOmsVurdering) => {
    lagreVurdering({ behandlingId: behandling.id, ...request }, (lagret) => {
      setVurderingStatus({ status: 'success', data: lagret.vurdering })
      dispatch(updateVilkaarsvurdering(lagret.vilkaarsvurdering))
      dispatch(
        oppdaterRevurderingInfo(lagret.periode ? { revurderingInfo: lagret.periode, begrunnelse: undefined } : null)
      )
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
    })
  }

  return (
    <>
      <Box paddingInline="space-64" paddingBlock="space-48 space-16">
        <Heading size="large" level="1">
          Vurdering av aktivitet
        </Heading>
      </Box>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-16" gap="space-24">
        <VStack gap="space-16">
          <Label as="p">Viktige datoer</Label>
          <HGrid columns={4} gap="space-24">
            {viktigeDatoer.map(({ tittel, dato }) => (
              <VStack key={tittel} gap="space-8">
                <Label as="p">{tittel}</Label>
                <BodyShort>{dato ? formaterDato(dato) : 'Ukjent dato'}</BodyShort>
              </VStack>
            ))}
          </HGrid>
          {!gyldigDoedsdato && (
            <Alert variant="warning">
              Dødsdato mangler eller er ugyldig. Datoene etter dødsfall kan ikke beregnes.
            </Alert>
          )}
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Tar bruker utdanning eller deltar i arbeidsrettet tiltak?"
            value={utdanningEllerTiltak}
            onChange={setUtdanningEllerTiltak}
            disabled={!redigerbar || lagrer || !!behandling.vilkaarsvurdering?.resultat}
          >
            <Radio value="UTDANNING">Ja, bruker tar utdanning</Radio>
            <Radio value="ARBEIDSRETTET_TILTAK">Ja, bruker deltar i arbeidsrettet tiltak</Radio>
            <Radio value="NEI">Nei</Radio>
          </RadioGroup>
        </VStack>
        {mapFailure(lagreStatus, (error) => (
          <ApiErrorAlert>Kunne ikke lagre vurderingen av aktivitet: {error.detail}</ApiErrorAlert>
        ))}
      </VStack>
      {utdanningEllerTiltak === 'UTDANNING' && (
        <ForlengetOmsUtdanning
          behandling={behandling}
          redigerbar={redigerbar}
          vurdering={vurdering}
          lagrer={lagrer}
          onLagre={onLagre}
        />
      )}
      {utdanningEllerTiltak === 'ARBEIDSRETTET_TILTAK' && (
        <ForlengetOmsArbeidsrettetTiltak
          behandling={behandling}
          redigerbar={redigerbar}
          vurdering={vurdering}
          lagrer={lagrer}
          onLagre={onLagre}
        />
      )}
      {utdanningEllerTiltak === 'NEI' && (
        <ForlengetOmsIngenAktivitet
          behandling={behandling}
          redigerbar={redigerbar}
          vurdering={vurdering}
          lagrer={lagrer}
          onLagre={onLagre}
        />
      )}
      {!utdanningEllerTiltak && (
        <ResultatForlengetOms behandling={behandling} redigerbar={redigerbar} lagrer={lagrer} onLagre={onLagre} />
      )}
    </>
  )
}
