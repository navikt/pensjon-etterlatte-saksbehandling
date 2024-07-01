import React, { useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Button, Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { hentVarselbrev, opprettVarselbrev } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import Spinner from '~shared/Spinner'
import { BrevStatus, IBrev } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { hentBehandling } from '~shared/api/behandling'
import { useAppDispatch } from '~store/Store'
import { isPending, isPendingOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'
import { hentOppgaveForReferanseUnderBehandling, settOppgavePaaVentApi } from '~shared/api/oppgaver'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import BrevStatusTag from '~components/person/brev/BrevStatusTag'
import { formaterDato } from '~utils/formatering/dato'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const Varselbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const { sakId, revurderingsaarsak } = props.behandling
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [redigerbar, setKanRedigeres] = useState(
    behandlingErRedigerbar(props.behandling.status, props.behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter)
  )

  const [varselbrev, setVarselbrev] = useState<IBrev>()
  const { next } = useBehandlingRoutes()

  const [hentBrevStatus, hentBrev] = useApiCall(hentVarselbrev)
  const [opprettBrevStatus, opprettNyttVarselbrev] = useApiCall(opprettVarselbrev)
  const [, fetchBehandling] = useApiCall(hentBehandling)

  const [, requesthentOppgaveForBehandlingEkte] = useApiCall(hentOppgaveForReferanseUnderBehandling)
  const [, requestSettPaaVent] = useApiCall(settOppgavePaaVentApi)

  const onSubmit = () => {
    next()
  }

  const settPaaVent = async () => {
    requesthentOppgaveForBehandlingEkte(behandlingId!!, (oppgave) => {
      requestSettPaaVent(
        {
          oppgaveId: oppgave.id,
          settPaaVentRequest: {
            merknad: 'Manuelt: Varselbrev er sendt ut | ' + oppgave.merknad || '',
            paaVent: true,
          },
        },
        () => {
          if (revurderingsaarsak !== Revurderingaarsak.AKTIVITETSPLIKT) {
            next()
          }
        }
      )
    })
  }

  // Opppdaterer behandling for å sikre at siste hendelser er på plass
  useEffect(() => {
    if (behandlingId && varselbrev) {
      fetchBehandling(
        behandlingId,
        (behandling) => {
          dispatch(oppdaterBehandling(behandling))
        },
        () => dispatch(resetBehandling())
      )
      if ([BrevStatus.DISTRIBUERT, BrevStatus.JOURNALFOERT, BrevStatus.FERDIGSTILT].includes(varselbrev.status)) {
        setKanRedigeres(false)
      }
    }
  }, [varselbrev])

  useEffect(() => {
    if (!behandlingId || !sakId || !props.behandling.sendeBrev) return

    hentBrev(behandlingId, (brev, statusCode) => {
      if (statusCode === 200) {
        setVarselbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVarselbrev({ sakId, behandlingId }, (nyttBrev) => {
          setVarselbrev(nyttBrev)
        })
      }
    })
  }, [behandlingId, sakId])

  if (isPendingOrInitial(hentBrevStatus)) {
    return <Spinner visible label="Henter brev ..." />
  } else if (isPending(opprettBrevStatus)) {
    return <Spinner visible label="Ingen brev funnet. Oppretter brev ..." />
  }

  return (
    <>
      <BrevContent>
        <Sidebar>
          <Box paddingInline="16" paddingBlock="16 4">
            <Heading spacing size="large" level="1">
              Varselbrev
            </Heading>

            {varselbrev && (
              <>
                <div>
                  <BrevStatusTag status={varselbrev.status} />
                  <BodyShort spacing>
                    <b>Sist endret:</b> {formaterDato(varselbrev.statusEndret)}
                  </BodyShort>
                </div>

                <BrevTittel
                  brevId={varselbrev.id}
                  sakId={varselbrev.sakId}
                  tittel={varselbrev.tittel}
                  kanRedigeres={redigerbar}
                />
                <br />
                <BrevMottaker brev={varselbrev} kanRedigeres={redigerbar} />
              </>
            )}
          </Box>
        </Sidebar>

        {!!varselbrev && <RedigerbartBrev brev={varselbrev} kanRedigeres={redigerbar} />}

        {isFailureHandler({ apiResult: hentBrevStatus, errorMessage: 'Feil ved henting av brev' })}
        {isFailureHandler({ apiResult: opprettBrevStatus, errorMessage: 'Kunne ikke opprette brev' })}
      </BrevContent>

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(hentBrevStatus) || isPending(opprettBrevStatus)}
            >
              Neste side uten å sende varselbrev
            </Button>
            {varselbrev && (
              <NyttBrevHandlingerPanel
                brev={varselbrev}
                setKanRedigeres={setKanRedigeres}
                callback={settPaaVent}
                erAktivitetspliktVarsel={revurderingsaarsak === Revurderingaarsak.AKTIVITETSPLIKT}
              />
            )}
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}

const BrevContent = styled.div`
  display: flex;
  height: 75vh;
  max-height: 75vh;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
styled(Alert).attrs({ variant: 'warning' })`
  margin-bottom: 1em;
`
