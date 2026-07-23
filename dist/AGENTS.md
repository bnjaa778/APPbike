# AppBike Router Notes

Use this file as a routing map before changing AppBike code.

## Main Split

- Public edge API: `Z:\var\www\api.zizzio.cl\APIS\AppBikeExternal.php`
- Private/internal API: `Z:\srv\internal-auth\public\AppBikeInternal\AppBikeInternal.php`
- Public API tester: `Z:\var\www\api.zizzio.cl\APIS\AppBikeApiTester.html`
- Internal bike photos: `Z:\srv\internal-auth\public\AppBikeInternal\BikesPhotos\PersonalBikesPhotos`

## Route To External When

Change `AppBikeExternal.php` only for public transport concerns:

- CORS / OPTIONS / HTTP method handling.
- Accepted public actions allowlist.
- Reading JSON, x-www-form-urlencoded, or multipart/form-data.
- Forwarding payloads and uploaded files to the private API.
- Public-to-private URL: `APPBIKE_PRIVATE_API_URL`, defaulting to the internal API URL.
- Authorization header forwarding.

Do not add PostgreSQL writes here. External is only the public gateway.

## Route To Internal When

Change `AppBikeInternal.php` for business logic and data persistence:

- Login against `public.users`.
- User lookups and user bike lists.
- Bike CRUD in `public.bikes`.
- Future maintenance CRUD in `public.mantenciones_futuras`.
- Past maintenance CRUD in `public.mantenciones_pasadas`.
- Completing future maintenance by inserting into past maintenance and deleting the future row.
- Photo validation, storage, deletion, and `public.bikes."PhotoID"` updates.
- Database schema guards/indexes created by `asegurar_tablas_appbike`.

Internal accepts POST and returns JSON. Keep action names compatible with the public allowlist.

## Supported Action Groups

- Auth: `login`
- Users: `user.get`, `usuario.get`, `user.bikes`, `user.bikes.list`, `usuario.bikes`, `usuario.bikes.list`
- Bikes: `bike.create`, `bike.list`, `bike.get`, `bike.update`, `bike.delete`
- Photos: `bike.photo.upload`, `photo.upload`, `bike.photo.list`, `photo.list`, `bike.photo.get`, `photo.get`, `bike.photo.delete`, `photo.delete`, `bike.photo.set_primary`, `photo.set_primary`
- Summaries: `bike.summary`, `bike.dashboard`
- Future maintenance: `maintenance.future.create`, `maintenance.future.list`, `maintenance.future.get`, `maintenance.future.update`, `maintenance.future.delete`, `maintenance.future.complete`
- Past maintenance: `maintenance.past.create`, `maintenance.past.list`, `maintenance.past.get`, `maintenance.past.update`, `maintenance.past.delete`

## Data Model

- `public.users`: login and active user validation.
- `public.bikes`: bike records, user association via `user_id`, current photo via `"PhotoID"`.
- `public.mantenciones_futuras`: scheduled maintenance.
- `public.mantenciones_pasadas`: completed maintenance history.

## Photo Contract

- Multipart field name: `foto`.
- Allowed types: JPG, PNG, WEBP.
- Max size: 5 MB.
- Stored under the internal AppBike folder.
- Public payloads expose `photo_id` and `photo_path`; binary retrieval can return `content_base64`.

## Tester

Use `AppBikeApiTester.html` to exercise the public API from a browser. It points to `./AppBikeExternal.php` and covers login, users, bikes, photos, future maintenance, and past maintenance.

## Change Rule

If a behavior changes, update both sides of the contract:

- Internal switch/action handler.
- External allowlist.
- Tester button/payload, when the action is user-testable.
