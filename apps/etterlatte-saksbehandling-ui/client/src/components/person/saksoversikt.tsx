import { Saksliste } from './saksliste'
import styled from 'styled-components'
import { Grunnlagsendringshendelse, IBehandlingsammendrag } from './typer'
import { useNavigate } from 'react-router-dom'
import { INasjonalitetsType } from '../behandling/fargetags/nasjonalitetsType'
import { Heading, Tag } from '@navikt/ds-react'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'
import { ToKolonner } from '../toKolonner/ToKolonner'
import { Grunnlagshendelser } from './grunnlagshendelser/Grunnlagsendringshendelser'
import { tagColors } from "~shared/Tags";
import { formaterEnumTilLesbarString } from "~utils/formattering";

export const Saksoversikt = ({
  behandlingliste,
  grunnlagshendelser,
}: {
  behandlingliste: IBehandlingsammendrag[] | undefined
  grunnlagshendelser: Grunnlagsendringshendelse[] | undefined
}) => {
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
          <Heading spacing size="xlarge" level="1">
            Barnepensjon
          </Heading>
          <div className="details">
            <Tag variant={tagColors[INasjonalitetsType.NASJONAL]}>
              {formaterEnumTilLesbarString(INasjonalitetsType.NASJONAL)}
            </Tag>
          </div>
        </HeadingWrapper>
        <ToKolonner>
          {{
            left: (
              <>
                {sakId !== undefined ? (
                  <EkstraHandlinger>
                    <ManueltOpphoerModal sakId={sakId} />
                  </EkstraHandlinger>
                ) : null}
                <div className="behandlinger">
                  <h2>Behandlinger</h2>
                  <Saksliste behandlinger={sortertListe} goToBehandling={goToBehandling} />
                </div>
              </>
            ),
            right: grunnlagshendelser?.length ? (
              <HendelseBorder>
                <Grunnlagshendelser hendelser={grunnlagshendelser} />
              </HendelseBorder>
            ) : null,
          }}
        </ToKolonner>
      </SaksoversiktWrapper>
    </>
  )
}

const HendelseBorder = styled.div`
  height: 100%;
  border-left: 2px solid lightgray;
  padding-left: 2rem;
`

const EkstraHandlinger = styled.div`
  display: flex;
  flex-direction: row-reverse;
`

export const IconButton = styled.div`
  padding-top: 1em;
  color: #000000;
  :hover {
    cursor: pointer;
  }
`

export const SaksoversiktWrapper = styled.div`
  min-width: 40em;
  max-width: 100%;

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
