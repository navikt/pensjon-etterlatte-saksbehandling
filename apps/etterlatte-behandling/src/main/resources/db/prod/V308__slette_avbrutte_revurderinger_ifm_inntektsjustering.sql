DELETE
FROM behandling
WHERE sak_id = 18068
  AND revurdering_aarsak = 'AARLIG_INNTEKTSJUSTERING'
  AND status = 'AVBRUTT'
  AND id in (
             'a4347614-2951-4452-9e3a-7501ec028544',
             'd81f34a5-4d06-4c01-975d-1c49615a67f1',
             '2c826388-1eb1-487e-845d-208b4585f1a7',
             '0b399ffa-7fd0-453e-8b13-18f2329049a1',
             'c20ce185-ceb5-4812-a58e-3a234d4a5fd2',
             'ef3f29b7-2dce-49be-9a77-68d8232a796e',
             '8743ca0e-ab3a-4a34-91bd-09cab8b3600a',
             'e1eafbc3-e81c-4852-b1a0-5176887957a3',
             'f0f201a0-bdc3-4fd4-8c94-8e5487d7f7cb',
             'd30dfce2-bd1a-43ac-810f-0fce9f4f4826',
             '03375210-a36c-4c9b-a0b4-dac27ab93d9f',
             '77ed23f6-1e73-4426-9b7d-98783b9b6be4',
             '1fb88743-2412-4399-b1e6-6a1fae227d99',
             'ef67fa74-df83-4dfd-a89b-603fca2d5f91',
             '8494a93c-a80c-426b-a55f-1ff4aeed4759');
