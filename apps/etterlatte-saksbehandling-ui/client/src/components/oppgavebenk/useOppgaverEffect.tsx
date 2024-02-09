import { useEffect, useState } from 'react'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { isSuccess, Result } from '~shared/api/apiUtils'
import { sorterOppgaverEtterOpprettet } from '~components/oppgavebenk/oppgaveutils'
import { useAppSelector } from '~store/Store'

export const useOppgaverEffect = (props: {
  oppgaver: Result<Array<OppgaveDTO>>
  gosysOppgaver: Result<Array<OppgaveDTO>>
  hentAlleOppgaver: () => void
  filtrerGosysOppgaverForInnloggetSaksbehandler: boolean
}): {
  hentedeOppgaver: Array<OppgaveDTO>
  setHentedeOppgaver: (value: ((prevState: Array<OppgaveDTO>) => Array<OppgaveDTO>) | Array<OppgaveDTO>) => void
} => {
  const { oppgaver, gosysOppgaver, hentAlleOppgaver, filtrerGosysOppgaverForInnloggetSaksbehandler = false } = props
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandlerIdent === innloggetSaksbehandler.ident)
  }

  const [hentedeOppgaver, setHentedeOppgaver] = useState<Array<OppgaveDTO>>([])

  useEffect(() => hentAlleOppgaver(), [])

  useEffect(() => {
    if (isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      const gosysoppgaverFiltrert = filtrerGosysOppgaverForInnloggetSaksbehandler
        ? filtrerKunInnloggetBrukerOppgaver(gosysOppgaver.data)
        : gosysOppgaver.data
      const alleOppgaver = sorterOppgaverEtterOpprettet([...oppgaver.data, ...gosysoppgaverFiltrert])
      setHentedeOppgaver(alleOppgaver)
    } else if (isSuccess(oppgaver) && !isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(sorterOppgaverEtterOpprettet(oppgaver.data))
    } else if (!isSuccess(oppgaver) && isSuccess(gosysOppgaver)) {
      setHentedeOppgaver(
        sorterOppgaverEtterOpprettet(
          filtrerGosysOppgaverForInnloggetSaksbehandler
            ? filtrerKunInnloggetBrukerOppgaver(gosysOppgaver.data)
            : gosysOppgaver.data
        )
      )
    }
  }, [oppgaver, gosysOppgaver])

  return { hentedeOppgaver, setHentedeOppgaver }
}
