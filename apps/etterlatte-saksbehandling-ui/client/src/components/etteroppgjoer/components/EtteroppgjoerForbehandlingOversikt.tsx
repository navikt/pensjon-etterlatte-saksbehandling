import { Dispatch, SetStateAction, useState } from 'react'
import { Alert, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { TabellForBeregnetEtteroppgjoerResultat } from '~components/etteroppgjoer/components/resultatAvForbehandling/TabellForBeregnetEtteroppgjoerResultat'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import {
  OpphoerSkyldesDoedsfall,
  OpphoerSkyldesDoedsfallSkjema,
} from '~components/etteroppgjoer/components/opphoerSkyldesDoedsfall/OpphoerSkyldesDoedsfall'
import {
  BeregnetEtteroppgjoerResultatDto,
  EtteroppgjoerForbehandling,
  EtteroppgjoerResultatType,
  IInformasjonFraBruker,
  erForbehandlingRedigerbar,
} from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'
import { FieldErrors } from 'react-hook-form'
import { FastsettFaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { erBehandlingRedigerbar, enhetErSkrivbar, erFerdigBehandlet } from '~components/behandling/felles/utils'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { FerdigstillEtteroppgjoerForbehandlingUtenBrev } from '~components/etteroppgjoer/components/FerdigstillEtteroppgjoerForbehandlingUtenBrev'
import { Link } from 'react-router-dom'
import { EtteroppjoerForbehandlingSteg } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { IDetaljertBehandling, Opprinnelse } from '~shared/types/IDetaljertBehandling'
import { InformasjonFraBruker } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBruker'
import { AvsluttEtteroppgjoerRevurderingModal } from '~components/etteroppgjoer/revurdering/AvsluttEtteroppgjoerRevurderingModal'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import Spinner from '~shared/Spinner'
import { SammendragAvSkjemaFeil } from '~shared/sammendragAvSkjemaFeil/SammendragAvSkjemaFeil'

export enum EtteroppgjoerKontekstType {
  FORBEHANDLING = 'forbehandling',
  REVURDERING = 'revurdering',
}

type ForbehandlingKontekst = { type: EtteroppgjoerKontekstType.FORBEHANDLING }
type RevurderingKontekst = { type: EtteroppgjoerKontekstType.REVURDERING; behandling: IDetaljertBehandling }
type EtteroppgjoerKontekst = ForbehandlingKontekst | RevurderingKontekst

interface Props {
  kontekst: EtteroppgjoerKontekst
}

function erRedigerbart(
  kontekst: EtteroppgjoerKontekst,
  forbehandling: EtteroppgjoerForbehandling,
  skriveEnheter: string[]
): boolean {
  if (kontekst.type === EtteroppgjoerKontekstType.REVURDERING) {
    return erBehandlingRedigerbar(kontekst.behandling.status, kontekst.behandling.sakEnhetId, skriveEnheter)
  }
  return erForbehandlingRedigerbar(forbehandling.status) && enhetErSkrivbar(forbehandling.sak.enhet, skriveEnheter)
}

function kanRedigereFaktiskInntektForKontekst(
  erRedigerbar: boolean,
  kontekst: EtteroppgjoerKontekst,
  forbehandling: EtteroppgjoerForbehandling
): boolean {
  if (!erRedigerbar) return false
  if (kontekst.type === EtteroppgjoerKontekstType.FORBEHANDLING) return true
  // Revurdering: manuelt opprettet kan alltid redigeres, automatisk kun med ny info som ikke er til ugunst
  return (
    kontekst.behandling.opprinnelse !== Opprinnelse.AUTOMATISK_JOBB ||
    (forbehandling.harMottattNyInformasjon === JaNei.JA && forbehandling.endringErTilUgunstForBruker !== JaNei.JA)
  )
}

export function EtteroppgjoerOversikt({ kontekst }: Props) {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const etteroppgjoerForbehandling = useEtteroppgjoerForbehandling()

  const [opphoerDoedsfallErrors, setOpphoerDoedsfallErrors] = useState<FieldErrors<OpphoerSkyldesDoedsfallSkjema>>()
  const [faktiskInntektErrors, setFaktiskInntektErrors] = useState<FieldErrors<FastsettFaktiskInntektSkjema>>()
  const [informasjonFraBrukerErrors, setInformasjonFraBrukerErrors] = useState<FieldErrors<IInformasjonFraBruker>>()
  const [valideringFeilmelding, setValideringFeilmelding] = useState<string>('')

  if (!etteroppgjoerForbehandling) {
    return <Spinner label="Laster etteroppgjør forbehandling" />
  }

  const { forbehandling, beregnetEtteroppgjoerResultat } = etteroppgjoerForbehandling
  const erRevurdering = kontekst.type === EtteroppgjoerKontekstType.REVURDERING
  const erRedigerbar = erRedigerbart(kontekst, forbehandling, innloggetSaksbehandler.skriveEnheter)
  const doedsfallIEtteroppgjoersaaret = forbehandling.opphoerSkyldesDoedsfallIEtteroppgjoersaar === JaNei.JA
  const kanRedigereFaktiskInntekt = kanRedigereFaktiskInntektForKontekst(erRedigerbar, kontekst, forbehandling)

  const visBeregnetResultat = erRevurdering
    ? forbehandling.endringErTilUgunstForBruker !== JaNei.JA
    : !!beregnetEtteroppgjoerResultat && !doedsfallIEtteroppgjoersaaret

  return (
    <VStack gap="10" paddingInline="16" paddingBlock="16 4">
      <Heading size="xlarge" level="1">
        Etteroppgjør for {forbehandling.aar}
      </Heading>

      <Inntektsopplysninger forbehandling={forbehandling} />

      {erRevurdering && (
        <RevurderingSpesifikkeSeksjoner
          behandling={kontekst.behandling}
          forbehandling={forbehandling}
          setInformasjonFraBrukerErrors={setInformasjonFraBrukerErrors}
          setValideringFeilmelding={setValideringFeilmelding}
        />
      )}

      {forbehandling.harVedtakAvTypeOpphoer && (
        <OpphoerSkyldesDoedsfall
          erRedigerbar={erRedigerbar}
          setOpphoerSkyldesDoedsfallSkjemaErrors={setOpphoerDoedsfallErrors}
        />
      )}

      {!doedsfallIEtteroppgjoersaaret && (
        <FastsettFaktiskInntekt
          erRedigerbar={kanRedigereFaktiskInntekt}
          setFastsettFaktiskInntektSkjemaErrors={setFaktiskInntektErrors}
        />
      )}

      {visBeregnetResultat && (
        <>
          <TabellForBeregnetEtteroppgjoerResultat />
          <ResultatAvForbehandling />
        </>
      )}

      <Box maxWidth="42.5rem">
        <VStack gap="8">
          {informasjonFraBrukerErrors && <SammendragAvSkjemaFeil errors={informasjonFraBrukerErrors} />}
          {opphoerDoedsfallErrors && <SammendragAvSkjemaFeil errors={opphoerDoedsfallErrors} />}
          {faktiskInntektErrors && <SammendragAvSkjemaFeil errors={faktiskInntektErrors} />}
          {valideringFeilmelding && <Alert variant="error">{valideringFeilmelding}</Alert>}
        </VStack>
      </Box>

      {!erRevurdering && doedsfallIEtteroppgjoersaaret && (
        <Alert size="small" variant="info">
          Siden bruker er død i etteroppgjørsåret, skal etteroppgjøret ferdigstilles uten brev og endringer.
        </Alert>
      )}

      {erRevurdering ? (
        <RevurderingNavigasjon
          behandling={kontekst.behandling}
          forbehandling={forbehandling}
          harSkjemaErrors={
            !!(
              (opphoerDoedsfallErrors && Object.keys(opphoerDoedsfallErrors).length > 0) ||
              (informasjonFraBrukerErrors && Object.keys(informasjonFraBrukerErrors).length > 0) ||
              (faktiskInntektErrors && Object.keys(faktiskInntektErrors).length > 0)
            )
          }
          setValideringFeilmelding={setValideringFeilmelding}
        />
      ) : (
        <ForbehandlingNavigasjon
          forbehandling={forbehandling}
          beregnetEtteroppgjoerResultat={beregnetEtteroppgjoerResultat}
        />
      )}
    </VStack>
  )
}

function RevurderingSpesifikkeSeksjoner({
  behandling,
  forbehandling,
  setInformasjonFraBrukerErrors,
  setValideringFeilmelding,
}: {
  behandling: IDetaljertBehandling
  forbehandling: EtteroppgjoerForbehandling
  setInformasjonFraBrukerErrors: (errors: FieldErrors<IInformasjonFraBruker> | undefined) => void
  setValideringFeilmelding: Dispatch<SetStateAction<string>>
}) {
  if (behandling.opprinnelse !== Opprinnelse.AUTOMATISK_JOBB) {
    return null
  }

  return (
    <>
      <InformasjonFraBruker
        behandling={behandling}
        setInformasjonFraBrukerSkjemaErrors={setInformasjonFraBrukerErrors}
        setValideringFeilmedling={setValideringFeilmelding}
      />

      {forbehandling.endringErTilUgunstForBruker === JaNei.JA && !erFerdigBehandlet(behandling.status) && (
        <Box maxWidth="42.5rem">
          <Alert variant="info">
            <Heading spacing size="small" level="3">
              Revurderingen skal avsluttes og det skal opprettes en ny forbehandling
            </Heading>
            Du har vurdert at endringen kommer til ugunst for bruker. Revurderingen skal derfor avsluttes, og en ny
            forbehandling for etteroppgjøret skal opprettes.
          </Alert>
        </Box>
      )}
    </>
  )
}

function RevurderingNavigasjon({
  behandling,
  forbehandling,
  harSkjemaErrors,
  setValideringFeilmelding,
}: {
  behandling: IDetaljertBehandling
  forbehandling: EtteroppgjoerForbehandling
  harSkjemaErrors: boolean
  setValideringFeilmelding: Dispatch<SetStateAction<string>>
}) {
  const { next } = useBehandlingRoutes()

  const manglerOpphoerSkyldesDoedsfallVurdering =
    forbehandling.harVedtakAvTypeOpphoer && !forbehandling.opphoerSkyldesDoedsfall

  const manglerInformasjonFraBrukerVurdering =
    behandling.opprinnelse === Opprinnelse.AUTOMATISK_JOBB && !forbehandling.harMottattNyInformasjon

  const manglerFastsattInntekt =
    forbehandling.harMottattNyInformasjon === JaNei.JA && forbehandling.kopiertFra === undefined

  const validerForNavigering = (): string | undefined => {
    if (manglerOpphoerSkyldesDoedsfallVurdering) return 'Du må ta stilling til om opphør skyldes dødsfall'
    if (manglerInformasjonFraBrukerVurdering) return 'Du må ta stilling til informasjon fra bruker'
    if (manglerFastsattInntekt) return 'Du må gjøre en endring i fastsatt inntekt'
    return undefined
  }

  const navigerTilNesteSteg = () => {
    if (harSkjemaErrors) {
      return
    }

    const feilmelding = validerForNavigering()
    if (feilmelding) {
      setValideringFeilmelding(feilmelding)
    } else {
      setValideringFeilmelding('')
      next()
    }
  }

  return (
    <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
      <HStack width="100%" justify="center">
        <VStack gap="4" align="center">
          {forbehandling.endringErTilUgunstForBruker === JaNei.JA ? (
            <AvsluttEtteroppgjoerRevurderingModal
              behandling={behandling}
              beskrivelseAvUgunst={forbehandling.beskrivelseAvUgunst}
            />
          ) : (
            <div>
              <Button type="button" onClick={navigerTilNesteSteg}>
                Neste side
              </Button>
            </div>
          )}
          <AvbrytBehandling />
        </VStack>
      </HStack>
    </Box>
  )
}

function ForbehandlingNavigasjon({
  forbehandling,
  beregnetEtteroppgjoerResultat,
}: {
  forbehandling: EtteroppgjoerForbehandling
  beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto | undefined
}) {
  const ferdigstillUtenBrev =
    beregnetEtteroppgjoerResultat?.resultatType === EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING ||
    forbehandling.opphoerSkyldesDoedsfall === JaNei.JA

  return (
    <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
      <HStack width="100%" justify="center">
        {ferdigstillUtenBrev ? (
          <FerdigstillEtteroppgjoerForbehandlingUtenBrev />
        ) : (
          <Button as={Link} to={`/etteroppgjoer/${forbehandling.id}/${EtteroppjoerForbehandlingSteg.BREV}`}>
            Gå til brev
          </Button>
        )}
      </HStack>
    </Box>
  )
}
