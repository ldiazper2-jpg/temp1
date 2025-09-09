# MultiURL Downloader

Este proyecto se puede compilar **sin instalar nada en tu PC** usando **GitHub + Actions**.

## Pasos rápidos (solo navegador)

1. Descarga este ZIP y súbelo a un repositorio nuevo en GitHub.
2. En el repo, ve a **Settings → Secrets and variables → Actions → New repository secret** y agrega:

   - `MYAPP_KEYSTORE_BASE64` (opcional para release): tu keystore en Base64.
   - `MYAPP_STORE_PASSWORD`, `MYAPP_KEY_ALIAS`, `MYAPP_KEY_PASSWORD`.

3. Abre **Actions → Android CI (No Wrapper)** → **Run workflow**.

4. Tras terminar, descarga los artefactos: APK debug, APK release y AAB.

