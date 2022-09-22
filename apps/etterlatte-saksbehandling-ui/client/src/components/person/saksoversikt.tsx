import { Saksliste } from './saksliste'
import styled from 'styled-components'
import { IBehandlingsammendrag } from './typer'
import { useNavigate } from 'react-router-dom'
import { INasjonalitetsType, NasjonalitetsType } from '../behandling/fargetags/nasjonalitetsType'
import { Heading } from '@navikt/ds-react'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'

export const Saksoversikt = ({ behandlingliste }: { behandlingliste: IBehandlingsammendrag[] | undefined }) => {
  const navigate = useNavigate()
  const behandlinger = behandlingliste ? behandlingliste : []
  const sakId = behandlinger[0]?.sak

  const sortertListe = behandlinger.sort((a, b) =>
    new Date(b.behandlingOpprettet!) > new Date(a.behandlingOpprettet!) ? 1 : -1
  )

  const goToBehandling = (behandlingsId: string) => {
    navigate(`/behandling/${behandlingsId}/soeknadsoversikt`)
  }

  return (
    <>
      <SaksoversiktWrapper>
        <HeadingWrapper>
          <Heading spacing size="xlarge" level="5">
            Barnepensjon
          </Heading>
          <div className="details">
            <NasjonalitetsType type={INasjonalitetsType.NASJONAL} />
          </div>
        </HeadingWrapper>

        <ManueltOpphoerModal sakId={sakId} />
        <div className="behandlinger">
          <h2>Behandlinger</h2>
          <Saksliste behandlinger={sortertListe} goToBehandling={goToBehandling} />
        </div>
      </SaksoversiktWrapper>
    </>
  )
}

export const IconButton = styled.div`
  padding-top: 1em;
  color: #000000;
  :hover {
    cursor: pointer;
  }
`

export const SaksoversiktWrapper = styled.div`
  min-width: 40em;
  max-width: 70%;

  margin: 3em 1em;
  .behandlinger {
    margin-top: 5em;
  }

  h1 {
    margin-bottom: 1em;
    text-transform: capitalize;
  }

  .button {
    margin-top: 4em;
    padding-left: 2em;
    padding-right: 2em;
  }
`

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    padding: 0.6em;
  }
`

export const InfoWrapper = styled.div`
  border: 1px solid #000000;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  padding: 3em;
`

export const Col = styled.div`
  font-weight: 400;
  font-size: 20px;
  line-height: 28px;
  margin-bottom: 10px;
`

export const Value = styled.div`
  font-style: normal;
  font-weight: 600;
  font-size: 20px;
  line-height: 28px;
`
