import styled from 'styled-components'
import { ErrorMessage, MonthPicker, useMonthpicker } from '@navikt/ds-react'
import React, { useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { formaterDatoTilYearMonth, formaterStringDato } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Beskrivelse, InfobokserWrapper, InfoWrapper, VurderingsContainerWrapper } from '../soeknadsoversikt/styled'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears, subYears } from 'date-fns'
import { LovtekstMedLenke } from '../soeknadsoversikt/LovtekstMedLenke'
import { Info } from '../soeknadsoversikt/Info'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/LeggTilVurderingButton'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/SoeknadsoversiktTextArea'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/virkningstidspunkt/utils'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { DatoVelger } from '~shared/DatoVelger'

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
  const dispatch = useAppDispatch()

  const [vurdert, setVurdert] = useState(behandling.virkningstidspunkt !== null)
  const [virkningstidspunkt, setVirkningstidspunkt] = useState<Date | null>(
    behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null
  )
  const [, fastsettVirkningstidspunktRequest, resetToInitial] = useApiCall(fastsettVirkningstidspunkt)

  const [begrunnelse, setBegrunnelse] = useState<string>(behandling.virkningstidspunkt?.begrunnelse ?? '')
  const [errorTekst, setErrorTekst] = useState<string>('')

  const [kravdato, setKravdato] = useState<Date | undefined>(undefined)

  const avdoedDoedsdato = behandling.familieforhold?.avdoede?.opplysning?.doedsdato

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
    if (erBosattUtland) {
      if (!kravdato) {
        setErrorTekst('Kravdato kreves på bosatt utland saker')
      }
    }

    const harVirkningstidspunktEndretSeg =
      behandling.virkningstidspunkt?.dato !== formaterDatoTilYearMonth(virkningstidspunkt) ||
      behandling.virkningstidspunkt?.begrunnelse !== begrunnelse
    if (harVirkningstidspunktEndretSeg) {
      return fastsettVirkningstidspunktRequest(
        { id: behandling.id, dato: virkningstidspunkt, begrunnelse: begrunnelse, kravdato: kravdato },
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
    } else {
      onSuccess?.()
    }
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setVirkningstidspunkt(behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null)
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
            <InfoWrapper>
              {props.children?.info}
              <Info
                label="Virkningstidspunkt"
                tekst={
                  behandling.virkningstidspunkt
                    ? formaterStringDato(behandling.virkningstidspunkt.dato)
                    : 'Ikke vurdert'
                }
              />
            </InfoWrapper>
          </InfobokserWrapper>
        </div>

        <VurderingsContainerWrapper>
          {!vurdert ? (
            <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
          ) : (
            <VurderingsboksWrapper
              tittel=""
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
              <>
                {erBosattUtland && (
                  <>
                    <DatoVelger
                      label="Kravdato"
                      onChange={(date) => setKravdato(date)}
                      value={kravdato ?? undefined}
                      fromDate={subYears(new Date(), 18)}
                      toDate={addYears(new Date(), 2)}
                    />
                  </>
                )}
                <MonthPickerWrapper>
                  <MonthPicker {...monthpickerProps}>
                    <MonthPicker.Input label="Virkningstidspunkt" {...inputProps} />
                  </MonthPicker>
                </MonthPickerWrapper>

                <SoeknadsoversiktTextArea value={begrunnelse} onChange={(e) => setBegrunnelse(e.target.value)} />
                {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}
              </>
            </VurderingsboksWrapper>
          )}
        </VurderingsContainerWrapper>
      </LovtekstMedLenke>
    </>
  )
}

const MonthPickerWrapper = styled.div`
  margin-top: 2rem;
  margin-bottom: 12px;
`

export default Virkningstidspunkt
