import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Alert, BodyShort, Button, Heading, Label, Textarea } from '@navikt/ds-react'
import { useRef, useState } from 'react'
import { oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { Calender, Edit } from '@navikt/ds-icons'
import { RedigerWrapper } from '../kommerBarnetTilgode/KommerBarnetTilGodeVurdering'
import { formaterStringDato, formaterStringTidspunkt } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import {
  Beskrivelse,
  InfoElement,
  InfoWrapper,
  InfobokserWrapper,
  Undertekst,
  VurderingsContainerWrapper,
  VurderingsTitle,
} from '../../styled'
import { useAppDispatch } from '~store/Store'
import { Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { addMonths } from 'date-fns'
import { Soeknadsvurdering } from '../SoeknadsVurdering'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'

export const Info = ({ tekst, label }: { tekst: string; label: string }) => {
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

  const [rediger, setRediger] = useState(false)
  const [formData, setFormData] = useState<Date | null>(
    props.virkningstidspunkt ? new Date(props.virkningstidspunkt.dato) : null
  )
  const [virkningstidspunkt, fastsettVirkningstidspunktRequest] = useApiCall(fastsettVirkningstidspunkt)
  const [begrunnelse, setBegrunnelse] = useState<string>(props.virkningstidspunkt?.begrunnelse ?? '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>()

  const datepickerRef: any = useRef(null)
  const toggleDatepicker = () => {
    datepickerRef.current.setOpen(true)
    datepickerRef.current.setFocus()
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
        status={Boolean(formData) ? 'success' : 'warning'}
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
          <div>
            <div>
              {rediger ? (
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
                    onChange={(e) => {
                      const oppdatertBegrunnelse = e.target.value

                      setBegrunnelse(oppdatertBegrunnelse)
                      oppdatertBegrunnelse.length > 10 && setBegrunnelseError(undefined)
                    }}
                    minRows={3}
                    size="small"
                    error={begrunnelseError ? begrunnelseError : false}
                    autoComplete="off"
                  />

                  <ButtonContainer>
                    <Button
                      onClick={() => {
                        if (!formData) return

                        const begrunnelseErr =
                          begrunnelse.length < 11 ? 'Begrunnelsen må være minst 10 tegn' : undefined

                        setBegrunnelseError(begrunnelseErr)

                        if (begrunnelseErr === undefined) {
                          fastsettVirkningstidspunktRequest(
                            { dato: formData, id: props.behandlingId, begrunnelse: begrunnelse },
                            (res) => {
                              dispatch(oppdaterVirkningstidspunkt(res))
                              setRediger(false)
                            }
                          )
                        }
                      }}
                      loading={isPending(virkningstidspunkt)}
                      size="small"
                      variant="primary"
                    >
                      Lagre
                    </Button>
                    <Button size="small" variant="secondary" onClick={() => setRediger(false)}>
                      Avbryt
                    </Button>
                  </ButtonContainer>
                  {isFailure(virkningstidspunkt) && (
                    <ApiErrorAlert>Kunne ikke fastsette virkningstidspunkt</ApiErrorAlert>
                  )}
                </>
              ) : (
                <div>
                  {props.virkningstidspunkt ? (
                    <>
                      <VurderingsTitle title={'Virkningstidspunkt'} />

                      <Undertekst $gray>Manuelt av {props.virkningstidspunkt.kilde.ident}</Undertekst>
                      <Undertekst $gray spacing>
                        {`Sist endret ${formaterStringDato(
                          props.virkningstidspunkt.kilde.tidspunkt
                        )} kl.${formaterStringTidspunkt(props.virkningstidspunkt.kilde.tidspunkt)}`}
                      </Undertekst>

                      <Heading size="xsmall" level={'3'}>
                        Begrunnelse
                      </Heading>
                      <BodyShort size="small">{props.virkningstidspunkt.begrunnelse}</BodyShort>

                      {props.redigerbar && (
                        <RedigerWrapper onClick={() => setRediger(true)}>
                          <Edit /> Rediger
                        </RedigerWrapper>
                      )}
                    </>
                  ) : (
                    <RedigerWrapper onClick={() => setRediger(true)}>
                      <Button variant="secondary">Legg til vurdering</Button>
                    </RedigerWrapper>
                  )}
                </div>
              )}
            </div>
          </div>
        </VurderingsContainerWrapper>
      </Soeknadsvurdering>
    </>
  )
}

const DatePickerLabel = styled(Label)`
  margin-top: 12px;
`

const ButtonContainer = styled.div`
  display: flex;
  gap: 8px;
  margin-top: 16px;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;

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

const ApiErrorAlert = styled(Alert).attrs({ variant: 'error' })`
  margin-top: 8px;
`

export default Virkningstidspunkt
