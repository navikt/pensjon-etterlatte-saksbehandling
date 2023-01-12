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
import { useApiCall, isPending, isFailure } from '~shared/hooks/useApiCall'
import {
  SoeknadOversiktWrapper,
  InfobokserWrapper,
  Infoboks,
  VurderingsContainer,
  VurderingsTitle,
  Undertekst,
} from '../../styled'
import { useAppDispatch, useAppSelector } from '~store/Store'
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

const Virkningstidspunkt = ({ kunLesetilgang }: { kunLesetilgang: boolean }) => {
  const dispatch = useAppDispatch()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)

  const [rediger, setRediger] = useState(false)
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
      <Heading size={'medium'} level={'2'}>
        Virkningstidspunkt
      </Heading>
      <SoeknadOversiktWrapper>
        <InfobokserWrapper>
          <Infoboks>
            Barnepensjon innvilges tidligst fra og med måneden etter dødsfall, og kan gis for opptil tre år før måneden
            kravet ble satt fram
          </Infoboks>
          <Info label="Dødsdato" tekst={avdoedDoedsdato ? formaterStringDato(avdoedDoedsdato) : ''} />
          <Info label="Søknad mottatt" tekst={formaterStringDato(behandling.soeknadMottattDato)} />
          <Info
            label="Virkningstidspunkt"
            tekst={
              behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt.dato) : 'Ikke vurdert'
            }
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
                  {behandling.virkningstidspunkt ? (
                    <>
                      <Undertekst $gray>Manuelt av {behandling.virkningstidspunkt.kilde.ident}</Undertekst>
                      <Undertekst $gray spacing>
                        {`Sist endret ${formaterStringDato(
                          behandling.virkningstidspunkt.kilde.tidspunkt
                        )} kl.${formaterStringTidspunkt(behandling.virkningstidspunkt.kilde.tidspunkt)}`}
                      </Undertekst>
                    </>
                  ) : (
                    <Undertekst $gray>Ikke vurdert</Undertekst>
                  )}
                  {!kunLesetilgang && (
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
