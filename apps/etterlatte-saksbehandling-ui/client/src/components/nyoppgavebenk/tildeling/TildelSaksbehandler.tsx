import { useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Oppgavetype, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { Alert, Button, Loader } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'

import { mapAllApiResult } from '~shared/api/apiUtils'
import { ToolTip } from '~components/behandling/felles/ToolTip'

export const TildelSaksbehandler = (props: {
  oppgaveId: string
  oppdaterTildeling: (id: string, saksbehandler: string, versjon: number | null) => void
  erRedigerbar: boolean
  versjon: number | null
  type: Oppgavetype
}) => {
  const { oppgaveId, oppdaterTildeling, erRedigerbar, versjon, type } = props
  if (!erRedigerbar) {
    return null
  }

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [tildelSaksbehandlerSvar, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const tildelSaksbehandlerWrapper = () => {
    const nysaksbehandler = { saksbehandler: innloggetSaksbehandler.ident, versjon }

    tildelSaksbehandler({ oppgaveId, type, nysaksbehandler }, (result) => {
      oppdaterTildeling(oppgaveId, innloggetSaksbehandler.ident, result.versjon)
    })
  }

  return mapAllApiResult(
    tildelSaksbehandlerSvar,
    <Loader size="small" title="Setter saksbehandler" />,
    <Button icon={<PersonIcon />} variant="tertiary" size="small" onClick={tildelSaksbehandlerWrapper}>
      Tildel meg
    </Button>,
    (err) => (
      <Alert variant="error" size="small">
        {err.code === 'OPPGAVEN_HAR_ALLEREDE_SAKSBEHANDLER' ? (
          <>
            Allerede tildelt<ToolTip title="Mer info om feilen">{alleredeTildeltHjelpetekst}</ToolTip>
          </>
        ) : (
          'Tildeling feilet'
        )}
      </Alert>
    ),
    () => (
      <Alert variant="success" size="small">
        Oppgaven er tildelt deg
      </Alert>
    )
  )
}

const alleredeTildeltHjelpetekst =
  'Oppgaven er allerede tildelt. Hvis du ønsker å overta den, må du gå ' +
  'i oppgavelisten og ta bort "Ikke tildelt oppgave", huke av for "Vis alle" under Saksbehandler, ' +
  'og hente oppgaver på nytt. Klikk så på saksbehandler på aktuell oppgave og ta til deg oppgaven.'
