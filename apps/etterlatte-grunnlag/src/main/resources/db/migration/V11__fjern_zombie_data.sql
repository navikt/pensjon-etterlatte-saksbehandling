/* Ubrukt tabell - all data overføres i V4 */
drop table opplysning;

/* Fjerne ubrukte opplysningstyper */
delete from grunnlagshendelse where opplysning_type = 'AVDOED_INNTEKT_V1'