import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { ErrorMessage, Label, Textarea } from '@navikt/ds-react'
import { useRef, useState } from 'react'
import { Calender } from '@navikt/ds-icons'
import { formaterStringDato } from '~utils/formattering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Beskrivelse, InfoElement, InfoWrapper, InfobokserWrapper, VurderingsContainerWrapper } from '../../styled'
import { useAppDispatch } from '~store/Store'
import { Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { addMonths } from 'date-fns'
import { Soeknadsvurdering } from '../SoeknadsVurdering'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'

const Info = ({ tekst, label }: { tekst: string; label: string }) => {
  return (
    <InfoElement>
      <Label size="small" as={'p'}>
        {label}
      </Label>
      <div>{tekst}</div>
    </InfoElement>
  )
}

interface Props {
  behandlingId: string
  redigerbar: boolean
  virkningstidspunkt: Virkningstidspunkt | null
  avdoedDoedsdato: string | undefined
  soeknadMottattDato: string
}

const Virkningstidspunkt = (props: Props) => {
  const dispatch = useAppDispatch()

  const [vurder, setVurder] = useState(props.virkningstidspunkt !== null)
  const [formData, setFormData] = useState<Date | null>(
    props.virkningstidspunkt ? new Date(props.virkningstidspunkt.dato) : null
  )
  const [, fastsettVirkningstidspunktRequest, resetToInitial] = useApiCall(fastsettVirkningstidspunkt)
  const [begrunnelse, setBegrunnelse] = useState<string>(props.virkningstidspunkt?.begrunnelse ?? '')
  const [errorTekst, setErrorTekst] = useState<string>()

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
    if (begrunnelse.length < 10) {
      return setErrorTekst('Begrunnelsen må være minst 10 tegn.')
    }

    fastsettVirkningstidspunktRequest({ id: props.behandlingId, dato: formData, begrunnelse: begrunnelse }, (res) => {
      dispatch(oppdaterVirkningstidspunkt(res))
      onSuccess?.()
    })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setFormData(props.virkningstidspunkt ? new Date(props.virkningstidspunkt.dato) : null)
    setBegrunnelse(props.virkningstidspunkt?.begrunnelse ?? '')
    setErrorTekst('')
    setVurder(props.virkningstidspunkt !== null)
    onSuccess?.()
  }

  return (
    <>
      <Soeknadsvurdering
        tittel="Virkningstidspunkt"
        vurderingsResultat={
          props.virkningstidspunkt !== null ? VurderingsResultat.OPPFYLT : VurderingsResultat.IKKE_OPPFYLT
        }
        hjemler={[
          { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-12', tittel: 'Folketrygdeloven § 22-12 første ledd' },
          { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13', tittel: '§ 22-13 fjerde ledd' },
        ]}
        status={Boolean(props.virkningstidspunkt) ? 'success' : 'warning'}
      >
        <div>
          <Beskrivelse>
            Barnepensjon innvilges som hovedregel fra og med måneden etter dødsfall, men kan gis for opptil tre år før
            den måneden da kravet ble satt fram.
          </Beskrivelse>
          <InfobokserWrapper>
            <InfoWrapper>
              <Info label="Dødsdato" tekst={props.avdoedDoedsdato ? formaterStringDato(props.avdoedDoedsdato) : ''} />
              <Info label="Søknad mottatt" tekst={formaterStringDato(props.soeknadMottattDato)} />
              <Info
                label="Virkningstidspunkt"
                tekst={props.virkningstidspunkt ? formaterStringDato(props.virkningstidspunkt.dato) : 'Ikke vurdert'}
              />
            </InfoWrapper>
          </InfobokserWrapper>
        </div>

        <VurderingsContainerWrapper>
          {!vurder ? (
            <LeggTilVurderingButton onClick={() => setVurder(true)}>Legg til vurdering</LeggTilVurderingButton>
          ) : (
            <VurderingsboksWrapper
              tittel={''}
              vurdering={
                props.virkningstidspunkt
                  ? {
                      saksbehandler: props.virkningstidspunkt.kilde.ident,
                      tidspunkt: new Date(props.virkningstidspunkt.kilde.tidspunkt),
                    }
                  : null
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
                    <Calender color="white" />
                  </KalenderIkon>
                </Datovelger>

                <Textarea
                  style={{ padding: '10px', marginBottom: '10px' }}
                  label="Begrunnelse"
                  hideLabel={false}
                  placeholder="Forklar begrunnelsen"
                  value={begrunnelse}
                  onChange={(e) => setBegrunnelse(e.target.value)}
                  minRows={3}
                  size="small"
                  autoComplete="off"
                />
                {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}
              </>
            </VurderingsboksWrapper>
          )}
        </VurderingsContainerWrapper>
      </Soeknadsvurdering>
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
