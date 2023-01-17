import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Alert, Button, Heading, Label } from '@navikt/ds-react'
import { useRef, useState } from 'react'
import { GyldighetIcon } from '~shared/icons/gyldigIcon'
import { oppdaterVirkningstidspunkt } from '~store/reducers/BehandlingReducer'
import { Calender, Edit } from '@navikt/ds-icons'
import { RedigerWrapper } from '../kommerBarnetTilgode/KommerBarnetTilGodeVurdering'
import { formaterStringDato, formaterStringTidspunkt } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import {
  Infoboks,
  InfobokserWrapper,
  SoeknadOversiktWrapper,
  Undertekst,
  VurderingsContainer,
  VurderingsTitle,
} from '../../styled'
import { useAppDispatch } from '~store/Store'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { IDetaljertBehandling, Virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { addMonths } from "date-fns";

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
  redigerbar: boolean
  behandling: IDetaljertBehandling
  virkningstidspunkt: Virkningstidspunkt | null
  avdoedDoedsdato: string | undefined
  soeknadMottattDato: string
  behandlingId: string
}

const Virkningstidspunkt = (props: Props) => {
  const dispatch = useAppDispatch()

  const [rediger, setRediger] = useState(false)
  const [formData, setFormData] = useState<Date | null>(
    props.virkningstidspunkt ? new Date(props.virkningstidspunkt.dato) : null
  )
  const [virkningstidspunkt, fastsettVirkningstidspunktRequest] = useApiCall(fastsettVirkningstidspunkt)

  const datepickerRef: any = useRef(null)
  const toggleDatepicker = () => {
    datepickerRef.current.setOpen(true)
    datepickerRef.current.setFocus()
  }

  return (
    <>
      <Heading size={'medium'} level={'2'}>
        Virkningstidspunkt
      </Heading>
      <SoeknadOversiktWrapper>
        <InfobokserWrapper>
          <Infoboks>
            Barnepensjon innvilges tidligst fra og med måneden etter dødsfall, og kan gis for opptil tre år før måneden
            kravet ble satt fram
          </Infoboks>
          <Info label="Dødsdato" tekst={props.avdoedDoedsdato ? formaterStringDato(props.avdoedDoedsdato) : ''} />
          <Info label="Søknad mottatt" tekst={formaterStringDato(props.soeknadMottattDato)} />
          <Info
            label="Virkningstidspunkt"
            tekst={props.virkningstidspunkt ? formaterStringDato(props.virkningstidspunkt.dato) : 'Ikke vurdert'}
          />
        </InfobokserWrapper>

        <VurderingsContainer>
          <div>
            <GyldighetIcon
              status={props.virkningstidspunkt !== null ? VurderingsResultat.OPPFYLT : VurderingsResultat.IKKE_OPPFYLT}
              large
            />
          </div>
          <div>
            <VurderingsTitle title={'Virkningstidspunkt'} />
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

                  <ButtonContainer>
                    <Button
                      onClick={() => {
                        if (!formData) return
                        fastsettVirkningstidspunktRequest({ dato: formData, id: props.behandlingId }, (res) => {
                          dispatch(oppdaterVirkningstidspunkt(res))
                          setRediger(false)
                        })
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
                      <Undertekst $gray>Manuelt av {props.virkningstidspunkt.kilde.ident}</Undertekst>
                      <Undertekst $gray spacing>
                        {`Sist endret ${formaterStringDato(
                          props.virkningstidspunkt.kilde.tidspunkt
                        )} kl.${formaterStringTidspunkt(props.virkningstidspunkt.kilde.tidspunkt)}`}
                      </Undertekst>
                    </>
                  ) : (
                    <Undertekst $gray>Ikke vurdert</Undertekst>
                  )}
                  {props.redigerbar && (
                    <RedigerWrapper onClick={() => setRediger(true)}>
                      <Edit /> Rediger
                    </RedigerWrapper>
                  )}
                </div>
              )}
            </div>
          </div>
        </VurderingsContainer>
      </SoeknadOversiktWrapper>
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

const InfoElement = styled.div`
  margin: 0 8px;
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
