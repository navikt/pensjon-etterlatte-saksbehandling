import { useEffect } from 'react'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { isSuccess, Result } from '~shared/api/apiUtils'
import { sorterOppgaverEtterOpprettet } from '~components/oppgavebenk/oppgaveutils'
import { useAppSelector } from '~store/Store'

export const useOppgaverEffect = (props: {
  setHentedeOppgaver: React.Dispatch<React.SetStateAction<OppgaveDTO[]>>
  oppgaver: Result<Array<OppgaveDTO>>
  gosysOppgaver: Result<Array<OppgaveDTO>>
  hentAlleOppgaver: () => void
  filtrerGosysOppgaverForInnloggetSaksbehandler: boolean
}): void => {
  const {
    setHentedeOppgaver,
    oppgaver,
    gosysOppgaver,
    hentAlleOppgaver,
    filtrerGosysOppgaverForInnloggetSaksbehandler = false,
  } = props
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const filtrerKunInnloggetBrukerOppgaver = (oppgaver: Array<OppgaveDTO>) => {
    return oppgaver.filter((o) => o.saksbehandlerIdent === innloggetSaksbehandler.ident)
  }

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
}
