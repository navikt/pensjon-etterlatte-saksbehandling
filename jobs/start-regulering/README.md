# start-regulering
___
[Naisjob](https://doc.nais.io/naisjob/) for å poste en melding til kafka. Benyttes for å starte reguleringsflyten.


For å trigge denne jobben gjøres følgende:
1. Gå inn i Unleash og aktiver flagget start-regulering
2. Restart applikasjonen, for eksempel ved å slette podden (kubectl delete pod) eller redeploye
3. Etter endt kjøring, skru av reguleringsflagget igjen