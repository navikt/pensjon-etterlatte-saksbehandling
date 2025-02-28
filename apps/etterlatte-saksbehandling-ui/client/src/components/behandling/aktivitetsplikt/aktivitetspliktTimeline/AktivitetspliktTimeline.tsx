import { Dispatch, SetStateAction, useState } from 'react'
import { addMonths, addYears } from 'date-fns'
import { Timeline } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { AktivitetspliktType, IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { AktivitetHendelseTimelinePin } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetHendelseTimelinePin'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { AktivitetspliktRedigeringModus } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { AktivitetspliktPeriodeTimelinePeriod } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetspliktPeriodeTimelinePeriod'
import {
  Buildings2Icon,
  HatSchoolIcon,
  PencilIcon,
  PersonIcon,
  ReceptionIcon,
  RulerIcon,
  WaitingRoomIcon,
} from '@navikt/aksel-icons'

interface Props {
  behandling?: IDetaljertBehandling
  sakId: number
  doedsdato: Date
  aktivitetHendelser: IAktivitetHendelse[]
  setAktivitetHendelser: Dispatch<SetStateAction<IAktivitetHendelse[]>>
  aktivitetPerioder: IAktivitetPeriode[]
  setAktivitetPerioder: Dispatch<SetStateAction<IAktivitetPeriode[]>>
  setAktivitetspliktRedigeringModus: Dispatch<SetStateAction<AktivitetspliktRedigeringModus>>
}

export const AktivitetspliktTimeline = ({
  behandling,
  sakId,
  doedsdato,
  aktivitetHendelser,
  setAktivitetHendelser,
  aktivitetPerioder,
  setAktivitetPerioder,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [sluttdato, setSluttdato] = useState<Date>(addYears(doedsdato, 3))

  const velgVisningAvAktivitetspliktPeriodeTimelineRow = (aktivitetPeriode: IAktivitetPeriode) => {
    switch (aktivitetPeriode.type) {
      case AktivitetspliktType.ARBEIDSTAKER:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Arbeidstaker">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="success"
              statusLabel="Arbeidstaker"
              icon={<PersonIcon aria-hidden />}
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
      case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Selvstendig næringsdrivende">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="info"
              statusLabel="Selvstendig næringsdrivende"
              icon={<RulerIcon aria-hidden />}
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
      case AktivitetspliktType.ETABLERER_VIRKSOMHET:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Etablert virksomhet">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              icon={<Buildings2Icon aria-hidden />}
              status="danger"
              statusLabel="Etablert virksomhet"
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
      case AktivitetspliktType.ARBEIDSSOEKER:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Arbeidssøker">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="warning"
              statusLabel="Arbeidssøker"
              icon={<PencilIcon aria-hidden />}
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
      case AktivitetspliktType.UTDANNING:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Utdanning">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="neutral"
              statusLabel="Utdanning"
              icon={<HatSchoolIcon aria-hidden />}
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
      case AktivitetspliktType.OPPFOELGING_LOKALKONTOR:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Oppfølging lokalkontor">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="warning"
              statusLabel="Oppfølging lokalkontor"
              icon={<ReceptionIcon aria-hidden />}
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
      case AktivitetspliktType.INGEN_AKTIVITET:
        return (
          <Timeline.Row key={aktivitetPeriode.id} label="Ingen aktivitet">
            <AktivitetspliktPeriodeTimelinePeriod
              start={new Date(aktivitetPeriode.fom)}
              end={(!!aktivitetPeriode.tom && new Date(aktivitetPeriode.tom)) || addYears(doedsdato, 3)}
              status="neutral"
              statusLabel="Ingen aktivitet"
              icon={<WaitingRoomIcon aria-hidden />}
              behandling={behandling}
              sakId={sakId}
              aktivitetPeriode={aktivitetPeriode}
              setAktivitetPerioder={setAktivitetPerioder}
              setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
            />
          </Timeline.Row>
        )
    }
  }

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
          date={new Date(aktivitetHendelse.dato)}
          key={index}
          sakId={sakId}
          behandling={behandling}
          aktivitetHendelse={aktivitetHendelse}
          setAktivitetHendelser={setAktivitetHendelser}
          setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
        />
      ))}
      {aktivitetPerioder.map((aktivitetPeriode) => velgVisningAvAktivitetspliktPeriodeTimelineRow(aktivitetPeriode))}
    </Timeline>
  )
}
