# Formularios Maipú — app Android

App que muestra el portal https://formulariosmaipu.netlify.app.

- Siempre carga la versión publicada en Netlify: si actualizas el portal allá, la app se actualiza sola (no hay que recompilar).
- Sin internet muestra la copia incluida en `app/src/main/assets/index.html`.
- Los formularios y enlaces externos se abren en el navegador del teléfono.
- El PDF de Mi Local para PDA se abre con el visor de PDF del teléfono.

## Cómo se obtiene el APK

Cada vez que se sube algo a `main`, GitHub compila solo y publica el APK en **Releases** (`formularios-maipu.apk`).
Una copia queda también en la rama `apk-build`.

**No borrar `app/firma.jks`**: es la firma fija. Gracias a ella las actualizaciones se instalan encima de la versión anterior sin desinstalar.

## Si cambia la dirección del portal

Editar `HOME` y `HOST` en `app/src/main/java/cl/easymaipu/formularios/MainActivity.kt`.

## Si actualizas la copia sin conexión

Reemplazar `app/src/main/assets/index.html` por el HTML nuevo y subir el cambio.
