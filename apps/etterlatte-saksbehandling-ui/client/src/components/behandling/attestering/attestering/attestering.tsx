import styled from 'styled-components'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'
import { useAppSelector } from '~store/Store'
import { Alert } from '@navikt/ds-react'
import { VedtakSammendrag } from "~components/person/typer";

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
  behandling: IDetaljertBehandling
  vedtak: VedtakSammendrag | undefined
}

export const Attestering = ({ setBeslutning, beslutning, behandling, vedtak }: Props) => {
  const { lastPage } = useBehandlingRoutes()

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const attestantOgSaksbehandlerErSammePerson = vedtak?.saksbehandlerId === innloggetSaksbehandler.ident

  return (
    <AttesteringWrapper>
      <div className="info">
        <Overskrift>Kontroller opplysninger og faglige vurderinger gjort under behandling.</Overskrift>
      </div>
      <TextWrapper>
        Beslutning
        {lastPage ? (
          <Beslutningsvalg
            beslutning={beslutning}
            setBeslutning={setBeslutning}
            behandling={behandling}
            disabled={attestantOgSaksbehandlerErSammePerson}
          />
        ) : (
          <Tekst>Se gjennom alle steg før du tar en beslutning.</Tekst>
        )}
        {attestantOgSaksbehandlerErSammePerson && (
          <Alert variant={'warning'}>Du kan ikke attestere en sak som du har saksbehandlet</Alert>
        )}
      </TextWrapper>
    </AttesteringWrapper>
  )
}

const AttesteringWrapper = styled.div`
  margin: 1em;

  .info {
    margin-top: 1em;
    margin-bottom: 1em;
    padding: 1em;
  }
`

const TextWrapper = styled.div`
  font-size: 18px;
  font-weight: 600;
  margin: 1em;
`

const Overskrift = styled.div`
  font-weight: 600;
  font-size: 16px;
  line-height: 22px;
  color: #3e3832;
`

const Tekst = styled.div`
  font-size: 18px;
  font-weight: 400;
  color: #3e3832;
  margin-top: 6px;
`
