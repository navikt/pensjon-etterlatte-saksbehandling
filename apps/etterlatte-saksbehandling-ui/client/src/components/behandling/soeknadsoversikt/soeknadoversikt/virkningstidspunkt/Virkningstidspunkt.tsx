import styled from 'styled-components'
import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { oppdaterVirkningstidspunkt, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { DatovelgerPeriode } from '../../../inngangsvilkaar/vilkaar/DatovelgerPeriode'
import { Edit } from '@navikt/ds-icons'

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
                  <DatovelgerPeriode
                    label="Virkningstidspunkt"
                    dato={virkningstidspunkt}
                    setDato={setVirkningstidspunkt}
                    setErrorUndefined={() => 0}
                    error={undefined}
                  />
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
`

export default Virkningstidspunkt
