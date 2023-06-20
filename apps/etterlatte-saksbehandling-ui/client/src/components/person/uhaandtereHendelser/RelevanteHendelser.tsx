import { Alert, Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { Grunnlagsendringshendelse, STATUS_IRRELEVANT } from '~components/person/typer'
import { FnrTilNavnMapContext } from '~components/person/uhaandtereHendelser/utils'
import { isSuccess, Result } from '~shared/hooks/useApiCall'
import React, { useMemo, useState } from 'react'
import { PersonerISakResponse } from '~shared/api/grunnlag'
import HistoriskeHendelser from '~components/person/uhaandtereHendelser/HistoriskeHendelser'
import { OpprettNyBehandling } from '~components/person/OpprettNyBehandling'
import OpprettRevurderingModal from '~components/person/OpprettRevurderingModal'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import UhaandtertHendelse from '~components/person/uhaandtereHendelser/UhaandtertHendelse'

type Props = {
  hendelser: Array<Grunnlagsendringshendelse>
  harAapenRevurdering: boolean
  grunnlag: Result<PersonerISakResponse>
  revurderinger: Array<Revurderingsaarsak>
  sakId: number
}

const RelevanteHendelser = (props: Props) => {
  const { hendelser, harAapenRevurdering, grunnlag, revurderinger, sakId } = props

  if (hendelser.length === 0) {
    return revurderinger.length > 0 ? <OpprettNyBehandling revurderinger={revurderinger} sakId={sakId} /> : null
  }

  const [visOpprettRevurderingsmodal, setVisOpprettRevurderingsmodal] = useState<boolean>(false)
  const [valgtHendelse, setValgtHendelse] = useState<Grunnlagsendringshendelse | undefined>(undefined)
  const startRevurdering = (hendelse: Grunnlagsendringshendelse) => {
    setValgtHendelse(hendelse)
    setVisOpprettRevurderingsmodal(true)
  }

  const navneMap = useMemo(() => {
    if (isSuccess(grunnlag)) {
      return grunnlag.data.personer
    } else {
      return {}
    }
  }, [grunnlag])

  const relevanteHendelser = hendelser.filter((h) => h.status !== STATUS_IRRELEVANT)
  const lukkedeHendelser = hendelser.filter((h) => h.status === STATUS_IRRELEVANT)

  return (
    <>
      <BorderWidth>
        <HendelserBorder>
          <FnrTilNavnMapContext.Provider value={navneMap}>
            {relevanteHendelser && relevanteHendelser.length > 0 && (
              <StyledAlert>
                Ny hendelse som kan kreve revurdering. Vurder om det har konsekvens for ytelsen.
              </StyledAlert>
            )}
            <Heading size="medium">Nye hendelser</Heading>
            {relevanteHendelser.map((hendelse) => (
              <UhaandtertHendelse
                key={hendelse.id}
                hendelse={hendelse}
                harAapenRevurdering={harAapenRevurdering}
                startRevurdering={startRevurdering}
                revurderinger={revurderinger}
              />
            ))}
          </FnrTilNavnMapContext.Provider>
        </HendelserBorder>
      </BorderWidth>
      {valgtHendelse && (
        <OpprettRevurderingModal
          sakId={sakId}
          valgtHendelse={valgtHendelse}
          open={visOpprettRevurderingsmodal}
          setOpen={setVisOpprettRevurderingsmodal}
          revurderinger={revurderinger}
        />
      )}
      <OpprettNyBehandling revurderinger={revurderinger} sakId={sakId} />
      <HistoriskeHendelser hendelser={lukkedeHendelser} />
    </>
  )
}

const HendelserBorder = styled.div`
  outline: solid;
  outline-offset: 25px;
`

const BorderWidth = styled.div`
  margin-top: 55px;
  margin-right: 10px;
  margin-left: 2px;
  max-width: 1000px;
`

const StyledAlert = styled(Alert).attrs({ variant: 'warning' })`
  display: inline-flex;
  margin: 1em 0;
`

export default RelevanteHendelser
