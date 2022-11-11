import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Alert, Button, Label, Loader } from '@navikt/ds-react'
import { useRef, useState } from 'react'
import { GyldighetIcon } from '~shared/icons/gyldigIcon'
import { oppdaterVirkningstidspunkt, VurderingsResultat } from '~store/reducers/BehandlingReducer'
import { Calender, Edit } from '@navikt/ds-icons'

import { RedigerWrapper } from '../kommerBarnetTilgode/KommerBarnetTilGodeVurdering'
import { formaterStringDato, formaterStringTidspunkt } from '~utils/formattering'
import { fastsettVirkningstidspunkt } from '~shared/api/behandling'
import { useApiCall, isPending, isFailure } from '~shared/hooks/useApiCall'
import {
  Header,
  SoeknadOversiktWrapper,
  InfobokserWrapper,
  Infoboks,
  VurderingsContainer,
  VurderingsTitle,
  Undertekst,
} from '../../styled'
import { useAppDispatch, useAppSelector } from '~store/Store'

export const Info = ({ tekst, label }: { tekst: string; label: string }) => {
  return (
    <InfoElement>
      <Label size="small">{label}</Label>
      <div>{tekst}</div>
    </InfoElement>
  )
}

const Virkningstidspunkt = () => {
  const dispatch = useAppDispatch()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const [rediger, setRediger] = useState(behandling.virkningstidspunkt === null)
  const [formData, setFormData] = useState<Date | null>(
    behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null
  )
  const [virkningstidspunkt, fastsettVirkningstidspunktRequest] = useApiCall(fastsettVirkningstidspunkt)

  const datepickerRef: any = useRef(null)
  const toggleDatepicker = () => {
    datepickerRef.current.setOpen(true)
    datepickerRef.current.setFocus()
  }

  const avdoedDoedsdato = behandling.familieforhold?.avdoede?.opplysning?.doedsdato

  return (
    <>
      <Header>Virkningstidspunkt</Header>
      <SoeknadOversiktWrapper>
        <InfobokserWrapper>
          <Infoboks>
            Barnepensjon innvilges som hovedregel fra og med måneden etter dødsfall, men kan gis for opptil tre år før
            den måneden da kravet ble satt fram
          </Infoboks>
          <Info label="Dødsdato" tekst={avdoedDoedsdato ? formaterStringDato(avdoedDoedsdato) : ''} />
          <Info label="Søknad mottatt" tekst={formaterStringDato(behandling.soeknadMottattDato)} />
          <Info
            label="Virkningstidspunkt"
            tekst={behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt.dato) : 'Ikke satt'}
          />
        </InfobokserWrapper>

        <VurderingsContainer>
          <div>
            <GyldighetIcon
              status={
                behandling.virkningstidspunkt !== null ? VurderingsResultat.OPPFYLT : VurderingsResultat.IKKE_OPPFYLT
              }
              large
            />
          </div>
          <div>
            <VurderingsTitle>Virkningstidspunkt</VurderingsTitle>
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
                        fastsettVirkningstidspunktRequest({ dato: formData, id: behandling.id }, (res) => {
                          dispatch(oppdaterVirkningstidspunkt(res))
                          setRediger(false)
                        })
                      }}
                      size="small"
                      variant="primary"
                    >
                      Lagre {isPending(virkningstidspunkt) && <Loader />}
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
                  {behandling.virkningstidspunkt ? (
                    <>
                      <Undertekst gray>Manuellt av {behandling.virkningstidspunkt?.kilde.ident ?? ''}</Undertekst>
                      <Undertekst gray>
                        Sist endret{' '}
                        {behandling.virkningstidspunkt?.kilde.tidspunkt
                          ? `${formaterStringDato(behandling.virkningstidspunkt?.kilde.tidspunkt)}
                          kl.${formaterStringTidspunkt(behandling.virkningstidspunkt?.kilde.tidspunkt)}`
                          : ''}
                      </Undertekst>
                      <RedigerWrapper onClick={() => setRediger(true)}>
                        <Edit /> Rediger
                      </RedigerWrapper>
                    </>
                  ) : (
                    <Undertekst gray>Ikke satt</Undertekst>
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
