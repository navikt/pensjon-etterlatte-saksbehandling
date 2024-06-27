import { formaterStringDato } from '~utils/formattering'

export const Soeknadsdato = ({ mottattDato }: { mottattDato: string }) => (
  <div>
    <strong>Søknad mottatt: </strong>
    {formaterStringDato(mottattDato)}
  </div>
)
