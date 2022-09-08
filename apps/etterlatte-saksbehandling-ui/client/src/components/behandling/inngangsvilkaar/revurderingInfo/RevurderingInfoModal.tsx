import React, { useContext, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppContext } from '../../../../store/AppContext'
import {
  IBehandlingStatus,
  IBehandlingsType,
  KriterieOpplysningsType,
  Kriterietype,
  VilkaarsType,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { hentBehandlesFraStatus, hentKriterierMedOpplysning } from '../../felles/utils'
import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { KildeDatoOpplysning } from '../vilkaar/KildeDatoOpplysning'
import styled from 'styled-components'
import { formaterStringDato } from '../../../../utils/formattering'

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
      opplysning: `Registrert dødsfall bruker ${formaterStringDato(soekerDoedsdato?.opplysning?.doedsdato)}`,
    },
  ]
}

const knapptekstForStatus = (status: IBehandlingStatus): string => {
  switch (status) {
    case IBehandlingStatus.UNDER_BEHANDLING:
    case IBehandlingStatus.RETURNERT:
      return 'Start revurdering'
    case IBehandlingStatus.FATTET_VEDTAK:
      return 'Attester revurdering'
    default:
      return 'Se revurdering'
  }
}

export const RevurderingsAarsakModal = ({ behandlingStatus }: { behandlingStatus: IBehandlingStatus }) => {
  const [open, setOpen] = useState(true)
  const navigate = useNavigate()
  const behandles = hentBehandlesFraStatus(behandlingStatus)

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
          {behandles ? (
            <BodyShort spacing>
              Se over vilkårsvurderingen og kontrollér nye opplysninger som har kommet inn i saken.
            </BodyShort>
          ) : null}
          <RevurderingOpplysningWrapper>
            <Heading spacing size="xsmall">
              {behandles
                ? 'Nye opplysninger som er årsak til revurdering'
                : 'Opplysninger som var årsak til revurdering'}
            </Heading>
            <RevurderingOpplysninger opplysninger={grunnlagForRevurdering} />
          </RevurderingOpplysningWrapper>
          <ButtonGroup>
            <Button variant="primary" onClick={onClose}>
              {knapptekstForStatus(behandlingStatus)}
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
