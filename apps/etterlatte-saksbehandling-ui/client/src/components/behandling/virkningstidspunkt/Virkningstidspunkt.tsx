import { BodyShort, ErrorMessage, Heading, HelpText, MonthPicker, useMonthpicker, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { formaterStringDato } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { InfobokserWrapper, InfoWrapper, VurderingsContainerWrapper, VurderingsTitle } from '../soeknadsoversikt/styled'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears, subYears } from 'date-fns'
import { LovtekstMedLenke } from '../soeknadsoversikt/LovtekstMedLenke'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/LeggTilVurderingButton'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/SoeknadsoversiktTextArea'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/virkningstidspunkt/utils'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { DatoVelger } from '~shared/DatoVelger'
import styled from 'styled-components'
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

  const [vurdert, setVurdert] = useState(behandling.virkningstidspunkt !== null)
  const [virkningstidspunkt, setVirkningstidspunkt] = useState<Date | null>(
    behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null
  )
  const [begrunnelse, setBegrunnelse] = useState<string>(behandling.virkningstidspunkt?.begrunnelse ?? '')
  const [kravdato, setKravdato] = useState<Date | null>(
    behandling.virkningstidspunkt?.kravdato ? new Date(behandling.virkningstidspunkt.kravdato) : null
  )

  const [errorTekst, setErrorTekst] = useState<string>('')

  const avdoedDoedsdato = avdoede?.opplysning?.doedsdato
  const tittel = 'Hva er virkningstidspunkt for behandlingen?'

  const { monthpickerProps, inputProps } = useMonthpicker({
    fromDate: hentMinimumsVirkningstidspunkt(
      avdoedDoedsdato,
      erBosattUtland ? subYears(new Date(), 20) : new Date(behandling.soeknadMottattDato)
    ),
    toDate: addMonths(new Date(), 1),
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
      () =>
        setErrorTekst(
          'Kunne ikke sette virkningstidspunkt. Last siden på nytt og prøv igjen, meld sak hvis problemet vedvarer'
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
        <div>
          <Beskrivelse>{props.beskrivelse}</Beskrivelse>
          <InfobokserWrapper>
            <InfoWrapper>{props.children?.info}</InfoWrapper>
          </InfobokserWrapper>
        </div>

        <VurderingsContainerWrapper>
          {!vurdert ? (
            <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
          ) : (
            <VurderingsboksWrapper
              tittel={tittel}
              subtittelKomponent={
                <VStack gap="4">
                  {erBosattUtland && (
                    <div>
                      <Heading size="xsmall">
                        <HelpTextWrapper>
                          Kravdato utland
                          <HelpText strategy="fixed">
                            Skriv inn kravdato for søknad i utlandet, som hentes fra SED P2100.
                          </HelpText>
                        </HelpTextWrapper>
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
                <VurderingsTitle title={tittel} />

                {erBosattUtland && (
                  <DatoVelger
                    label={
                      <HelpTextWrapper>
                        Kravdato utland
                        <HelpText strategy="fixed">
                          Skriv inn kravdato for søknad i utlandet, som hentes fra SED P2100.
                        </HelpText>
                      </HelpTextWrapper>
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

                <SoeknadsoversiktTextArea value={begrunnelse} onChange={(e) => setBegrunnelse(e.target.value)} />

                {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}
              </VStack>
            </VurderingsboksWrapper>
          )}
        </VurderingsContainerWrapper>
      </LovtekstMedLenke>
    </>
  )
}

export default Virkningstidspunkt

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.3em;
`

export const Beskrivelse = styled.div`
  margin: 10px 0;
  max-width: 41em;
  white-space: pre-wrap;
`
