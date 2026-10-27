# Formularios Maipú — app Android

Portal de formularios de Easy Maipú empaquetado como app. El portal va **dentro** de la app
(`app/src/main/assets/index.html`), así que funciona sin depender de Netlify.

- Incluye la pantalla **Reporte de incidente** (`app/src/main/assets/reporte.html`), 100% local.
- Los formularios y enlaces externos se abren en el navegador del teléfono.
- El PDF de Mi Local para PDA se abre con el visor de PDF del teléfono.

## Cómo actualizar el contenido

1. Reemplazar `app/src/main/assets/index.html` por el HTML nuevo (por ejemplo, con la nueva versión del PDF de Mi Local).
2. Subir el cambio a `main`. GitHub compila solo y publica el APK en **Releases** (`formularios-maipu.apk`).
   Una copia queda también en la rama `apk-build`.
3. Instalar el APK nuevo encima del anterior (no hay que desinstalar).

**No borrar `app/firma.jks`**: es la firma fija. Gracias a ella las actualizaciones se instalan encima de la versión anterior.
