import {
  BodyShort,
  Button,
  ErrorMessage,
  Heading,
  HelpText,
  HStack,
  MonthPicker,
  useMonthpicker,
  VStack,
} from '@navikt/ds-react'
import React, { useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { formaterStringDato } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Informasjon, Vurdering } from '../soeknadsoversikt/styled'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears, subYears } from 'date-fns'
import { LovtekstMedLenke } from '../soeknadsoversikt/LovtekstMedLenke'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/SoeknadsoversiktTextArea'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/virkningstidspunkt/utils'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

export interface Hjemmel {
  lenke: string
  tittel: string
}

const Virkningstidspunkt = (props: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
  hjemler: Hjemmel[]
  beskrivelse: string
  children?: { info: React.ReactNode }
  erBosattUtland: boolean
}) => {
  const { behandling, erBosattUtland } = props
  const avdoede = usePersonopplysninger()?.avdoede.find((po) => po)
  const dispatch = useAppDispatch()
  const [, fastsettVirkningstidspunktRequest, resetToInitial] = useApiCall(fastsettVirkningstidspunkt)

  const [vurdert, setVurdert] = useState<boolean>(behandling.virkningstidspunkt !== null)
  const [virkningstidspunkt, setVirkningstidspunkt] = useState<Date | null>(
    behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null
  )
  const [begrunnelse, setBegrunnelse] = useState<string>(behandling.virkningstidspunkt?.begrunnelse ?? '')
  const [kravdato, setKravdato] = useState<Date | null>(
    behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null
  )

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

  const { monthpickerProps, inputProps } = useMonthpicker({
    fromDate: hentMinimumsVirkningstidspunkt(
      avdoede?.opplysning?.doedsdato,
      getSoeknadMottattDato(),
      behandling.sakType
    ),
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
        begrunnelse: begrunnelse,
        kravdato: kravdato,
      },
      (res) => {
        dispatch(oppdaterVirkningstidspunkt(res))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      },
      (error) =>
        setErrorTekst(
          `Kunne ikke sette virkningstidspunkt. ${
            error.detail ? error.detail : 'Last siden på nytt og prøv igjen, meld sak hvis problemet vedvarer'
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
    setVurdert(behandling.virkningstidspunkt !== null)
    onSuccess?.()
  }

  return (
    <>
      <LovtekstMedLenke
        tittel="Virkningstidspunkt"
        hjemler={props.hjemler}
        status={Boolean(behandling.virkningstidspunkt) ? 'success' : 'warning'}
      >
        <VStack gap="2">
          <Informasjon>{props.beskrivelse}</Informasjon>
          <div>{props.children?.info}</div>
        </VStack>

        <Vurdering>
          {!vurdert ? (
            <Button variant="secondary" onClick={() => setVurdert(true)}>
              Legg til vurdering
            </Button>
          ) : (
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
                          ? formaterStringDato(behandling.virkningstidspunkt.kravdato)
                          : 'Ikke fastsatt'}
                      </BodyShort>
                    </div>
                  )}
                  <div>
                    <Heading size="xsmall">Virkningstidspunkt</Heading>
                    <BodyShort spacing>
                      {behandling.virkningstidspunkt
                        ? formaterStringDato(behandling.virkningstidspunkt.dato)
                        : 'Ikke fastsatt'}
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
              redigerbar={props.redigerbar}
              lagreklikk={fastsett}
              avbrytklikk={reset}
              kommentar={behandling.virkningstidspunkt?.begrunnelse}
              defaultRediger={behandling.virkningstidspunkt === null}
            >
              <VStack gap="4">
                <Heading level="3" size="small">
                  Hva er virkningstidspunkt for behandlingen?
                </Heading>

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
                  placeholder="Forklar brgrunnelsen"
                  value={begrunnelse}
                  onChange={(e) => setBegrunnelse(e.target.value)}
                />

                {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}
              </VStack>
            </VurderingsboksWrapper>
          )}
        </Vurdering>
      </LovtekstMedLenke>
    </>
  )
}

export default Virkningstidspunkt
