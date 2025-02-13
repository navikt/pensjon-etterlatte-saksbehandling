import {
  Alert,
  BodyShort,
  ConfirmationPanel,
  ErrorMessage,
  Heading,
  HelpText,
  HStack,
  MonthPicker,
  useMonthpicker,
  VStack,
} from '@navikt/ds-react'
import React, { ReactNode, useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { formaterDato } from '~utils/formatering/dato'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Informasjon, Vurdering } from '../soeknadsoversikt/styled'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears, subYears } from 'date-fns'
import { LovtekstMedLenke } from '../soeknadsoversikt/LovtekstMedLenke'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/SoeknadsoversiktTextArea'
import { hentMinimumsVirkningstidspunkt, Hjemmel } from '~components/behandling/virkningstidspunkt/utils'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { mapFailure } from '~shared/api/apiUtils'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

interface Props {
  behandling: IDetaljertBehandling
  redigerbar: boolean
  erBosattUtland: boolean
  hjemler: Array<Hjemmel>
  beskrivelse: string
  children?: ReactNode | Array<ReactNode>
}

const Virkningstidspunkt = ({ behandling, redigerbar, erBosattUtland, hjemler, beskrivelse, children }: Props) => {
  const dispatch = useAppDispatch()
  const [fastsettVirkStatus, fastsettVirkningstidspunktRequest, resetToInitial] = useApiCall(fastsettVirkningstidspunkt)
  const avdoede = usePersonopplysninger()?.avdoede
  const tidligVirkningstidspunktTillatt = useFeaturetoggle(FeatureToggle.tillate_tidlig_virkningstidspunkt)

  const [virkningstidspunkt, setVirkningstidspunkt] = useState<Date | null>(
    behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null
  )
  const [begrunnelse, setBegrunnelse] = useState<string>(behandling.virkningstidspunkt?.begrunnelse ?? '')
  const [kravdato, setKravdato] = useState<Date | null>(
    behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null
  )
  const [overstyr, setOverstyr] = useState(false)

  const [errorTekst, setErrorTekst] = useState<string>('')

  function getSoeknadMottattDato() {
    return erBosattUtland
      ? subYears(new Date(), 20)
      : behandling.soeknadMottattDato
        ? new Date(behandling.soeknadMottattDato)
        : new Date(2024, 0, 1)
    // For saker migrert fra Pesys har vi ikke tatt med søknad mottatt-dato
    // Disse kan ha tidligste virkningstidspunkt i Gjenny 1.1.24, altså da etterlattereformen tredde i kraft
    // Denne siste fallbacken er altså tenkt for disse sakene
  }

  function foersteDoedsdato(): Date | undefined {
    const mappetAvdoede = avdoede?.map((it) => it.opplysning.doedsdato!!)

    if (mappetAvdoede && mappetAvdoede.length > 0) {
      return mappetAvdoede.reduce((accumulator, doedsdatoAvdoed) =>
        doedsdatoAvdoed < accumulator ? doedsdatoAvdoed : accumulator
      )
    } else {
      return undefined
    }
  }

  const minimumVirkningstidspunkt = tidligVirkningstidspunktTillatt
    ? subYears(new Date(), 3)
    : hentMinimumsVirkningstidspunkt(foersteDoedsdato(), getSoeknadMottattDato(), behandling.sakType)

  const { monthpickerProps, inputProps } = useMonthpicker({
    fromDate: minimumVirkningstidspunkt,
    toDate: addMonths(new Date(), 4),
    onMonthChange: (date: Date) => setVirkningstidspunkt(date),
    inputFormat: 'dd.MM.yyyy',
    onValidate: (val) => {
      if (val.isBefore || val.isAfter) setErrorTekst('Virkningstidspunkt er ikke gyldig')
      else setErrorTekst('')
    },
    defaultSelected: virkningstidspunkt ?? undefined,
  } as UseMonthPickerOptions)

  const fastsett = (onSuccess?: () => void) => {
    setErrorTekst('')
    if (!virkningstidspunkt) {
      return setErrorTekst('Du må velge virkningstidspunkt')
    }
    if (begrunnelse.trim().length === 0) {
      return setErrorTekst('Begrunnelsen må fylles ut')
    }
    if (erBosattUtland && !kravdato) {
      return setErrorTekst('Kravdato kreves på bosatt utland saker')
    }

    return fastsettVirkningstidspunktRequest(
      {
        id: behandling.id,
        dato: virkningstidspunkt,
        begrunnelse,
        kravdato,
        overstyr,
      },
      (res) => {
        dispatch(oppdaterVirkningstidspunkt(res))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      },
      (error) =>
        setErrorTekst(
          `Kunne ikke sette virkningstidspunkt. ${
            error.detail || 'Last siden på nytt og prøv igjen, meld sak hvis problemet vedvarer'
          }`
        )
    )
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setVirkningstidspunkt(behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null)
    setKravdato(behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null)
    setBegrunnelse(behandling.virkningstidspunkt?.begrunnelse ?? '')
    setErrorTekst('')
    onSuccess?.()
  }

  return (
    <>
      <LovtekstMedLenke
        tittel="Virkningstidspunkt"
        hjemler={hjemler}
        status={Boolean(behandling.virkningstidspunkt) ? 'success' : 'warning'}
      >
        <VStack gap="2">
          <Informasjon>{beskrivelse}</Informasjon>
          <HStack gap="4">{children}</HStack>
        </VStack>

        <Vurdering>
          <VurderingsboksWrapper
            tittel="Hva er virkningstidspunkt for behandlingen?"
            subtittelKomponent={
              <VStack gap="4">
                {erBosattUtland && (
                  <div>
                    <Heading size="xsmall">
                      <HStack gap="1">
                        Kravdato utland
                        <HelpText placement="top">
                          Skriv inn kravdato for søknad i utlandet, som hentes fra SED P2100.
                        </HelpText>
                      </HStack>
                    </Heading>
                    <BodyShort>
                      {behandling.virkningstidspunkt?.kravdato
                        ? formaterDato(behandling.virkningstidspunkt.kravdato)
                        : 'Ikke fastsatt'}
                    </BodyShort>
                  </div>
                )}
                <div>
                  <Heading size="xsmall">Virkningstidspunkt</Heading>
                  <BodyShort spacing>
                    {behandling.virkningstidspunkt ? formaterDato(behandling.virkningstidspunkt.dato) : 'Ikke fastsatt'}
                  </BodyShort>
                </div>
              </VStack>
            }
            vurdering={
              behandling.virkningstidspunkt
                ? {
                    saksbehandler: behandling.virkningstidspunkt.kilde.ident,
                    tidspunkt: new Date(behandling.virkningstidspunkt.kilde.tidspunkt),
                  }
                : undefined
            }
            redigerbar={redigerbar}
            lagreklikk={fastsett}
            avbrytklikk={reset}
            kommentar={behandling.virkningstidspunkt?.begrunnelse}
            defaultRediger={behandling.virkningstidspunkt === null}
            visAvbryt={!!behandling.virkningstidspunkt}
          >
            <VStack gap="4">
              <Heading level="3" size="small">
                Hva er virkningstidspunkt for behandlingen?
              </Heading>

              {!foersteDoedsdato() && (
                <Alert variant="warning">Det er anbefalt å registrere dødsdato før du setter virkningstidspunkt.</Alert>
              )}

              {erBosattUtland && (
                <DatoVelger
                  label={
                    <HStack gap="1">
                      Kravdato utland
                      <HelpText placement="top">
                        Skriv inn kravdato for søknad i utlandet, som hentes fra SED P2100.
                      </HelpText>
                    </HStack>
                  }
                  onChange={(date) => setKravdato(date ?? null)}
                  value={kravdato ?? undefined}
                  fromDate={subYears(new Date(), 18)}
                  toDate={addYears(new Date(), 2)}
                />
              )}
              <MonthPicker {...monthpickerProps}>
                <MonthPicker.Input label="Virkningstidspunkt" {...inputProps} />
              </MonthPicker>

              <SoeknadsoversiktTextArea
                label="Begrunnelse"
                placeholder="Forklar begrunnelsen"
                value={begrunnelse}
                onChange={(e) => setBegrunnelse(e.target.value)}
              />

              {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}

              {mapFailure(
                fastsettVirkStatus,
                (error) =>
                  behandling.behandlingType === IBehandlingsType.REVURDERING &&
                  error.code &&
                  ['VIRK_FOER_FOERSTE_IVERKSATT_VIRK', 'VIRK_KAN_IKKE_VAERE_ETTER_OPPHOER'].includes(error.code) && (
                    <ConfirmationPanel
                      label="Ja, jeg er sikker"
                      checked={overstyr}
                      onChange={(e) => setOverstyr(e.target.checked)}
                    >
                      Er du sikker på at du vil sette dette virkningstidspunktet?
                    </ConfirmationPanel>
                  )
              )}
            </VStack>
          </VurderingsboksWrapper>
        </Vurdering>
      </LovtekstMedLenke>
    </>
  )
}

export default Virkningstidspunkt
