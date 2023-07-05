import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { ErrorMessage, Label } from '@navikt/ds-react'
import { useRef, useState } from 'react'
import { oppdaterBehandlingsstatus, oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { CalendarIcon } from '@navikt/aksel-icons'
import { formaterDatoTilYearMonth, formaterStringDato } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Beskrivelse, InfoWrapper, InfobokserWrapper, VurderingsContainerWrapper } from '../../styled'
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

  const datepickerRef: any = useRef(null)
  const toggleDatepicker = () => {
    datepickerRef.current.setOpen(true)
    datepickerRef.current.setFocus()
  }

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
        }
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
              tittel={''}
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
                {/* TODO ai: Erstatt med komponent fra design-biblioteket når det kommer ut */}
                <DatePickerLabel>Dato</DatePickerLabel>
                <Datovelger>
                  <div>
                    <DatePicker
                      ref={datepickerRef}
                      dateFormat={'dd.MM.yyyy'}
                      placeholderText={'dd.mm.åååå'}
                      selected={formData}
                      locale="nb"
                      onChange={(date: Date) => setFormData(date)}
                      autoComplete="off"
                      showMonthYearPicker
                      minDate={hentMinimumsVirkningstidspunkt(props.avdoedDoedsdato, props.soeknadMottattDato)}
                      maxDate={addMonths(new Date(), 1)}
                    />
                  </div>
                  <KalenderIkon
                    tabIndex={0}
                    onKeyPress={toggleDatepicker}
                    onClick={toggleDatepicker}
                    role="button"
                    title="Åpne datovelger"
                    aria-label="Åpne datovelger"
                  >
                    <CalendarIcon color="white" />
                  </KalenderIkon>
                </Datovelger>

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

const DatePickerLabel = styled(Label)`
  margin-top: 12px;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;
  margin-bottom: 12px;

  input {
    border-right: none;
    border-radius: 4px 0 0 4px;
    height: 48px;
    text-indent: 4px;
  }
`

const KalenderIkon = styled.div`
  padding: 4px 10px;
  cursor: pointer;
  background-color: #0167c5;
  border: 1px solid #000;
  border-radius: 0 4px 4px 0;
  height: 48px;
  line-height: 42px;
`

export default Virkningstidspunkt
