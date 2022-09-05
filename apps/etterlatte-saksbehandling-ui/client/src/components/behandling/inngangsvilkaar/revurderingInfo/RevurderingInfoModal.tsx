import React, { useContext, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppContext } from '../../../../store/AppContext'
import {
  IBehandlingsType,
  KriterieOpplysningsType,
  Kriterietype,
  VilkaarsType,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning } from '../../felles/utils'
import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { KildeDatoOpplysning } from '../vilkaar/KildeDatoOpplysning'
import styled from 'styled-components'

const useGrunnlagForRevurdering = (): RevurderingOpplysningType[] => {
  // Enn så lenge er dette hardkodet til å hente mottaker av ytelsens dødsdato fra vilkåret formaal.
  // Dette bør heller være basert på revurderingsårsak fra behandling
  const { state } = useContext(AppContext)
  const vilkaar = state.behandlingReducer?.vilkårsprøving?.vilkaar?.find(
    (vilkaar) => vilkaar.navn === VilkaarsType.FORMAAL_FOR_YTELSEN
  )

  if (state.behandlingReducer.behandlingType !== IBehandlingsType.REVURDERING) {
    return []
  }
  const soekerDoedsdato = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.SOEKER_ER_I_LIVE,
    KriterieOpplysningsType.DOEDSDATO
  )

  return [
    {
      kilde: soekerDoedsdato?.kilde,
      opplysning: 'Registrert dødsfall mottaker av ytelsen',
    },
  ]
}

export const RevurderingsAarsakModal = () => {
  const [open, setOpen] = useState(true)
  const navigate = useNavigate()

  const grunnlagForRevurdering = useGrunnlagForRevurdering()
  if (grunnlagForRevurdering.length === 0) {
    return null
  }

  function onClose() {
    setOpen(() => false)
  }

  return (
    <Modal open={open} aria-label="Revurderingsgrunnlag" onClose={onClose}>
      <Modal.Content>
        <RevurderingModalContentWrapper>
          <Heading spacing level="2" size="medium">
            Revurdering
          </Heading>
          <BodyShort spacing>
            Se over vilkårsvurderingen og kontrollér nye opplysninger som har kommet inn i saken.
          </BodyShort>
          <RevurderingOpplysninger opplysninger={grunnlagForRevurdering} />
          <ButtonGroup>
            <Button variant="primary" onClick={onClose}>
              Start revurdering
            </Button>
            <Button variant="tertiary" onClick={() => navigate('/')}>
              Avbryt
            </Button>
          </ButtonGroup>
        </RevurderingModalContentWrapper>
      </Modal.Content>
    </Modal>
  )
}

interface RevurderingOpplysningType {
  kilde?: {
    type: string
    tidspunktForInnhenting: string
  }
  opplysning: string
}

const RevurderingOpplysninger = ({ opplysninger }: { opplysninger: RevurderingOpplysningType[] }) => (
  <RevurderingOpplysningWrapper>
    <Heading spacing size="xsmall">
      Nye opplysninger som er årsak til revurdering
    </Heading>
    <UstiletListe>
      {opplysninger.map((opplysning) => (
        <li key={`${opplysning.opplysning}-${opplysning.kilde?.tidspunktForInnhenting}`}>
          <OpplysningMedIkon>
            <StatusIcon status={VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING} large />
            <div>
              <span>{opplysning.opplysning}</span>
              <KildeDatoOpplysning type={opplysning?.kilde?.type} dato={opplysning?.kilde?.tidspunktForInnhenting} />
            </div>
          </OpplysningMedIkon>
        </li>
      ))}
    </UstiletListe>
  </RevurderingOpplysningWrapper>
)

const UstiletListe = styled.ul`
  list-style: none;
`

const OpplysningMedIkon = styled.div`
  display: flex;
`

const RevurderingOpplysningWrapper = styled.div`
  padding: 20px;
`

const RevurderingModalContentWrapper = styled.div`
  padding: 20px;
`

const ButtonGroup = styled.div`
  display: flex;
  flex-direction: row-reverse;
  gap: 20px;
`
