import { Dispatch, SetStateAction, useState } from 'react'
import { addMonths, addYears } from 'date-fns'
import { Timeline } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { IAktivitetHendelse } from '~shared/types/Aktivitetsplikt'
import { AktivitetHendelseTimelinePin } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetHendelseTimelinePin'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { AktivitetspliktRedigeringModus } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'

interface Props {
  behandling?: IDetaljertBehandling
  sakId: number
  doedsdato: Date
  aktivitetHendelser: IAktivitetHendelse[]
  setAktivitetHendelser: Dispatch<SetStateAction<IAktivitetHendelse[]>>
  setAktivitetspliktRedigeringModus: Dispatch<SetStateAction<AktivitetspliktRedigeringModus>>
}

export const AktivitetspliktTimeline = ({
  behandling,
  sakId,
  doedsdato,
  aktivitetHendelser,
  setAktivitetHendelser,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [sluttdato, setSluttdato] = useState<Date>(addYears(doedsdato, 3))

  return (
    <Timeline startDate={doedsdato} endDate={sluttdato}>
      <Timeline.Pin date={doedsdato}>Dødsdato: {formaterDato(doedsdato)}</Timeline.Pin>
      <Timeline.Pin date={new Date()}>Dagens dato: {formaterDato(new Date())}</Timeline.Pin>
      <Timeline.Pin date={addMonths(doedsdato, 6)}>
        6 måneder etter dødsfall: {formaterDato(addMonths(doedsdato, 6))}
      </Timeline.Pin>
      <Timeline.Pin date={addMonths(doedsdato, 12)}>
        12 måneder etter dødsfall: {formaterDato(addMonths(doedsdato, 12))}
      </Timeline.Pin>
      {aktivitetHendelser.map((aktivitetHendelse, index) => (
        <AktivitetHendelseTimelinePin
          key={index}
          behandling={behandling}
          sakId={sakId}
          aktivitetHendelse={aktivitetHendelse}
          setAktivitetHendelser={setAktivitetHendelser}
          setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
        />
      ))}
    </Timeline>
  )
}
