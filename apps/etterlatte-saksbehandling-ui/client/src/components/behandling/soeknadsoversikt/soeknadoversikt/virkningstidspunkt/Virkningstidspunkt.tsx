import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import { Button } from '@navikt/ds-react'
import { useRef, useState } from 'react'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { oppdaterVirkningstidspunkt, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { Calender, Edit } from '@navikt/ds-icons'

import {
  Header,
  Infoboks,
  InfobokserWrapper,
  SoeknadOversiktWrapper,
  Undertekst,
  VurderingsContainer,
  VurderingsTitle,
} from '../../styled'
import { RedigerWrapper } from '../kommerBarnetTilgode/KommerBarnetTilGodeVurdering'
import { OversiktElement } from '../OversiktElement'
import { formaterStringDato, formaterStringTidspunkt } from '../../../../../utils/formattering'
import { useAppDispatch, useAppSelector } from '../../../../../store/Store'
import { fastsettVirkningstidspunkt } from '../../../../../shared/api/behandling'

const Virkningstidspunkt = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const datepickerRef: any = useRef(null)
  const toggleDatepicker = () => {
    datepickerRef.current.setOpen(true)
    datepickerRef.current.setFocus()
  }
  const [rediger, setRediger] = useState(behandling.virkningstidspunkt === null)
  const [virkningstidspunkt, setVirkningstidspunkt] = useState<Date | null>(null)

  const dispatch = useAppDispatch()

  const toggleRediger = () => setRediger(!rediger)
  const lagreVirkningstidspunkt = async (dato: Date | null) => {
    if (dato === null) return

    const response = await fastsettVirkningstidspunkt(behandling.id, dato)
    if (response.status === 'ok') {
      dispatch(oppdaterVirkningstidspunkt(response.data))
      toggleRediger()
    }
  }

  const avdoedDoedsdato = behandling.familieforhold?.avdoede?.opplysning?.doedsdato
  console.log(behandling)

  return (
    <>
      <Header>Virkningstidspunkt</Header>
      <SoeknadOversiktWrapper>
        <InfobokserWrapper>
          <Infoboks>
            Barnepensjon innvilges som hovedregel fra og med måneden etter dødsfall, men kan gis for opptil tre år før
            den måneden da kravet ble satt fram
          </Infoboks>
          <OversiktElement
            tekst={
              behandling.virkningstidspunkt?.kilde?.tidspunkt
                ? `PDL ${formaterStringDato(behandling.virkningstidspunkt!!.kilde.tidspunkt)}`
                : 'Ukjent'
            }
            label="Dødsdato"
            navn={avdoedDoedsdato ? formaterStringDato(avdoedDoedsdato) : ''}
            erOppfylt
          />
          <OversiktElement
            navn=""
            label="Søknad mottatt"
            tekst={formaterStringDato(behandling.soeknadMottattDato)}
            erOppfylt
          />
          <OversiktElement
            navn=""
            label="Virkningstidspunkt"
            tekst={behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt.dato) : 'Ikke satt'}
            erOppfylt
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
            <VurderingsTitle>Fastsett virkningstidspunkt</VurderingsTitle>
            <div>
              {rediger ? (
                <>
                  {/* TODO ai: Erstatt med komponent fra design-biblioteket når det kommer ut */}
                  <Datovelger>
                    <div>
                      <DatePicker
                        ref={datepickerRef}
                        dateFormat={'dd.MM.yyyy'}
                        placeholderText={'dd.mm.åååå'}
                        selected={virkningstidspunkt}
                        onChange={(date: Date) => setVirkningstidspunkt(date)}
                        autoComplete="off"
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
                    <Button onClick={() => lagreVirkningstidspunkt(virkningstidspunkt)} size="small" variant="primary">
                      Lagre
                    </Button>
                    <Button size="small" variant="secondary" onClick={toggleRediger}>
                      Avbryt
                    </Button>
                  </ButtonContainer>
                </>
              ) : (
                <div>
                  {behandling.virkningstidspunkt ? (
                    <>
                      <Undertekst gray>Manuellt av {behandling.virkningstidspunkt?.kilde.ident ?? ''}</Undertekst>
                      <Undertekst gray>
                        Sist endret{' '}
                        {behandling.virkningstidspunkt?.kilde.tidspunkt
                          ? formaterStringTidspunkt(behandling.virkningstidspunkt?.kilde.tidspunkt)
                          : ''}
                      </Undertekst>
                      <RedigerWrapper onClick={toggleRediger}>
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

export default Virkningstidspunkt
