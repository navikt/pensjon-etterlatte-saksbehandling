import styled from 'styled-components'
import { ErrorMessage, MonthPicker, useMonthpicker } from '@navikt/ds-react'
import { useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { formaterDatoTilYearMonth, formaterStringDato } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Beskrivelse, InfobokserWrapper, InfoWrapper, VurderingsContainerWrapper } from '../../styled'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus, Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { addMonths } from 'date-fns'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Info } from '../../Info'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsoversiktTextArea'
import { KildePdl } from '~shared/types/kilde'
import { hentMinimumsVirkningstidspunkt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/utils'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'

interface Props {
  behandlingId: string
  redigerbar: boolean
  virkningstidspunkt: Virkningstidspunkt | null
  avdoedDoedsdato: string | undefined
  avdoedDoedsdatoKilde: KildePdl | undefined
  soeknadMottattDato: string
  hjemmler: Hjemmel[]
  beskrivelse: string
  children?: { info: React.ReactNode }
}

export interface Hjemmel {
  lenke: string
  tittel: string
}

const Virkningstidspunkt = (props: Props) => {
  const dispatch = useAppDispatch()

  const [vurdert, setVurdert] = useState(props.virkningstidspunkt !== null)
  const [formData, setFormData] = useState<Date | null>(
    props.virkningstidspunkt ? new Date(props.virkningstidspunkt.dato) : null
  )
  const [, fastsettVirkningstidspunktRequest, resetToInitial] = useApiCall(fastsettVirkningstidspunkt)
  const [begrunnelse, setBegrunnelse] = useState<string>(props.virkningstidspunkt?.begrunnelse ?? '')
  const [errorTekst, setErrorTekst] = useState<string>('')

  const { monthpickerProps, inputProps } = useMonthpicker({
    fromDate: hentMinimumsVirkningstidspunkt(props.avdoedDoedsdato, props.soeknadMottattDato),
    toDate: addMonths(new Date(), 1),
    onMonthChange: (date: Date) => setFormData(date),
    inputFormat: 'dd.MM.yyyy',
    onValidate: (val) => {
      if (val.isBefore || val.isAfter) setErrorTekst('Dato er ikke gyldig')
      else setErrorTekst('')
    },
    defaultSelected: formData ?? undefined,
  } as UseMonthPickerOptions)

  const fastsett = (onSuccess?: () => void) => {
    setErrorTekst('')
    if (!formData) {
      return setErrorTekst('Du må velge dato')
    }
    if (begrunnelse.trim().length === 0) {
      return setErrorTekst('Begrunnelsen må fylles ut')
    }

    const harVirkningstidspunktEndretSeg =
      props.virkningstidspunkt?.dato !== formaterDatoTilYearMonth(formData) ||
      props.virkningstidspunkt?.begrunnelse !== begrunnelse
    if (harVirkningstidspunktEndretSeg) {
      return fastsettVirkningstidspunktRequest(
        { id: props.behandlingId, dato: formData, begrunnelse: begrunnelse },
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
    setFormData(props.virkningstidspunkt ? new Date(props.virkningstidspunkt.dato) : null)
    setBegrunnelse(props.virkningstidspunkt?.begrunnelse ?? '')
    setErrorTekst('')
    setVurdert(props.virkningstidspunkt !== null)
    onSuccess?.()
  }

  return (
    <>
      <LovtekstMedLenke
        tittel="Virkningstidspunkt"
        hjemler={props.hjemmler}
        status={Boolean(props.virkningstidspunkt) ? 'success' : 'warning'}
      >
        <div>
          <Beskrivelse>{props.beskrivelse}</Beskrivelse>
          <InfobokserWrapper>
            <InfoWrapper>
              {props.children?.info}
              <Info
                label="Virkningstidspunkt"
                tekst={props.virkningstidspunkt ? formaterStringDato(props.virkningstidspunkt.dato) : 'Ikke vurdert'}
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
                props.virkningstidspunkt
                  ? {
                      saksbehandler: props.virkningstidspunkt.kilde.ident,
                      tidspunkt: new Date(props.virkningstidspunkt.kilde.tidspunkt),
                    }
                  : undefined
              }
              redigerbar={props.redigerbar}
              lagreklikk={fastsett}
              avbrytklikk={reset}
              kommentar={props.virkningstidspunkt?.begrunnelse}
              defaultRediger={props.virkningstidspunkt === null}
            >
              <>
                <MonthPickerWrapper>
                  <MonthPicker {...monthpickerProps}>
                    <MonthPicker.Input label="Dato" {...inputProps} />
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
  margin-bottom: 12px;
`

export default Virkningstidspunkt
